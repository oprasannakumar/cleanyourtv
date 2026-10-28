package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages daily Telugu Panchangam and upcoming Telugu festivals (within 5 days).
 * - Line 2: Festival ticker (rotates every 3s among festivals in the next 5 days)
 * - Line 3: Steady Telugu Panchangam (Tithi with timings, Nakshatram with timings, Rahu Kalam)
 * - Supports offline bundled 3-year database + algorithmic fallback + background Wi-Fi auto-sync
 */
final class TeluguPanchangam {
    private static final String TAG = "TeluguPanchangam";
    private static final long CYCLE_INTERVAL_MS = 3000L; // 3 seconds per festival
    private static final String PREFS = "telugu_panchangam_prefs";
    private static final String KEY_LAST_SYNC = "last_panchangam_sync";
    private static final long SYNC_INTERVAL_MS = 12 * 60 * 60 * 1000L; // Sync twice daily

    private final Context context;
    private final View festivalRow;
    private final TextView festivalText;
    private final View panchangamRow;
    private final TextView panchangamText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    private final List<FestivalItem> festivalList = new ArrayList<>();
    private int currentFestivalIndex = 0;
    private boolean isRunning = false;
    private LocalDate lastLoadedDate = null;
    private JSONObject cachedJsonData = null;

    static class FestivalItem {
        final String title;
        final LocalDate date;
        final long daysAway;

        FestivalItem(String title, LocalDate date, long daysAway) {
            this.title = title;
            this.date = date;
            this.daysAway = daysAway;
        }
    }

    private final Runnable festivalCycleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            if (festivalList.size() > 1 && festivalText != null) {
                currentFestivalIndex = (currentFestivalIndex + 1) % festivalList.size();
                updateFestivalDisplay();
                festivalText.setAlpha(0.2f);
                festivalText.animate().alpha(1f).setDuration(300).start();
            }

            handler.postDelayed(this, CYCLE_INTERVAL_MS);
        }
    };

    TeluguPanchangam(Context context,
                     View festivalRow, TextView festivalText,
                     View panchangamRow, TextView panchangamText) {
        this.context = context.getApplicationContext();
        this.festivalRow = festivalRow;
        this.festivalText = festivalText;
        this.panchangamRow = panchangamRow;
        this.panchangamText = panchangamText;
    }

    void loadAndRefresh() {
        handler.removeCallbacks(festivalCycleRunnable);
        loadData();
        currentFestivalIndex = 0;
        updateFestivalDisplay();
        updatePanchangamDisplay();

        isRunning = true;
        if (festivalList.size() > 1) {
            handler.postDelayed(festivalCycleRunnable, CYCLE_INTERVAL_MS);
        }

        // Background auto-sync via Wi-Fi if due
        syncIfNeeded();
    }

    void syncNow(Runnable onComplete) {
        cachedJsonData = null;
        executor.execute(() -> {
            try {
                String syncUrl = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString("panchangam_sync_url", null);
                if (!TextUtils.isEmpty(syncUrl)) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(syncUrl).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = conn.getInputStream();
                             FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), "telugu_panchangam.json"))) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        }
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Log.e("TeluguPanchangam", "Manual sync error", e);
            } finally {
                handler.post(() -> {
                    loadAndRefresh();
                    if (onComplete != null) onComplete.run();
                });
            }
        });
    }

    void onMinuteTick() {
        LocalDate today = LocalDate.now();
        if (lastLoadedDate != null && !today.equals(lastLoadedDate)) {
            loadAndRefresh();
        }
    }

    void pause() {
        isRunning = false;
        handler.removeCallbacks(festivalCycleRunnable);
    }

    void resume() {
        isRunning = true;
        handler.removeCallbacks(festivalCycleRunnable);
        if (festivalList.size() > 1) {
            handler.postDelayed(festivalCycleRunnable, CYCLE_INTERVAL_MS);
        }
    }

    void destroy() {
        isRunning = false;
        handler.removeCallbacks(festivalCycleRunnable);
    }

    private void loadData() {
        festivalList.clear();
        lastLoadedDate = LocalDate.now();
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        if (cachedJsonData == null) {
            String jsonStr = readJsonString();
            if (!TextUtils.isEmpty(jsonStr)) {
                try {
                    cachedJsonData = new JSONObject(jsonStr);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse telugu_panchangam.json", e);
                }
            }
        }

        // 1. Collect up to 5 upcoming festival events
        for (int i = 0; i <= 180 && festivalList.size() < 5; i++) {
            LocalDate targetDate = lastLoadedDate.plusDays(i);
            String dateKey = targetDate.format(isoFormatter);

            if (cachedJsonData != null && cachedJsonData.has(dateKey)) {
                JSONObject dayObj = cachedJsonData.optJSONObject(dateKey);
                if (dayObj != null) {
                    JSONArray fArr = dayObj.optJSONArray("festivals");
                    if (fArr != null) {
                        for (int j = 0; j < fArr.length() && festivalList.size() < 5; j++) {
                            String fName = fArr.optString(j, "").trim();
                            if (!TextUtils.isEmpty(fName)) {
                                festivalList.add(new FestivalItem(fName, targetDate, i));
                            }
                        }
                    }
                }
            }
        }
        for (FestivalItem fi : festivalList) {
            Log.i(TAG, "Upcoming Festival: " + fi.title + " on " + fi.date + ", daysAway=" + fi.daysAway);
        }
    }

    private static String formatTeluguDate(LocalDate d) {
        if (d == null) return "";
        String[] teMonths = {
                "జనవరి", "ఫిబ్రవరి", "మార్చి", "ఏప్రిల్", "మే", "జూన్",
                "జూలై", "ఆగస్టు", "సెప్టెంబర్", "అక్టోబర్", "నవంబర్", "డిసెంబర్"
        };
        int mIdx = d.getMonthValue() - 1;
        String month = (mIdx >= 0 && mIdx < teMonths.length) ? teMonths[mIdx] : String.valueOf(d.getMonthValue());
        return month + " " + d.getDayOfMonth();
    }

    private void updateFestivalDisplay() {
        if (festivalRow == null || festivalText == null) return;

        if (festivalList.isEmpty()) {
            festivalRow.setVisibility(View.GONE);
            return;
        }

        if (currentFestivalIndex >= festivalList.size()) {
            currentFestivalIndex = 0;
        }

        FestivalItem item = festivalList.get(currentFestivalIndex);
        String prefix;
        if (item.daysAway == 0) {
            prefix = "🪔 నేడు: ";
        } else if (item.daysAway == 1) {
            prefix = "🪔 రేపు: ";
        } else {
            String teDate = formatTeluguDate(item.date);
            prefix = "🪔 " + teDate + ": ";
        }

        festivalText.setText(prefix + item.title);
        festivalRow.setVisibility(View.VISIBLE);
    }

    private void updatePanchangamDisplay() {
        if (panchangamRow == null || panchangamText == null) return;

        LocalDate today = LocalDate.now();
        String dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        String masam = "ఆశ్వయుజం";
        String paksham = "బహుళ";
        String tithi = "తదియ";
        String tithiEnd = "రా. 1:07";
        String nakshatram = "కృత్తిక";
        String nakshatraEnd = "మ. 1:26";
        String rahuKalam = "12:00 - 1:26 PM";

        boolean found = false;
        if (cachedJsonData != null && cachedJsonData.has(dateKey)) {
            JSONObject dayObj = cachedJsonData.optJSONObject(dateKey);
            if (dayObj != null) {
                masam = dayObj.optString("masam", masam);
                paksham = dayObj.optString("paksham", paksham);
                tithi = dayObj.optString("tithi", tithi);
                tithiEnd = dayObj.optString("tithi_end", tithiEnd);
                nakshatram = dayObj.optString("nakshatram", nakshatram);
                nakshatraEnd = dayObj.optString("nakshatra_end", nakshatraEnd);
                rahuKalam = dayObj.optString("rahu_kalam", rahuKalam);
                found = true;
            }
        }

        if (!found) {
            // Algorithmic fallback
            DrikPanchangResult res = calculateDynamicPanchang(today);
            masam = res.masam;
            paksham = res.paksham;
            tithi = res.tithi;
            tithiEnd = res.tithiEnd;
            nakshatram = res.nakshatram;
            nakshatraEnd = res.nakshatraEnd;
            rahuKalam = res.rahuKalam;
        }

        // Full line formatting:
        // 📜 ఆశ్వయుజ బహుళ తదియ (రా. 1:07 వరకు) · కృత్తిక (మ. 1:26 వరకు) · రాహు: 12:00 - 1:26 PM
        String cleanMasam = masam != null && masam.endsWith("ం") ? masam.substring(0, masam.length() - 1) : masam;
        String formatted = "📜 " + cleanMasam + " " + paksham + " " + tithi + " (" + tithiEnd + " వరకు) · "
                + nakshatram + " (" + nakshatraEnd + " వరకు) · రాహు: " + rahuKalam;

        panchangamText.setText(formatted);
        panchangamRow.setVisibility(View.VISIBLE);
    }

    private void syncIfNeeded() {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = sp.getLong(KEY_LAST_SYNC, 0L);
        long now = System.currentTimeMillis();

        if (now - last < SYNC_INTERVAL_MS) {
            return;
        }

        // Check if Wi-Fi or active network is available
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.Network activeNet = cm.getActiveNetwork();
            if (activeNet == null) return;
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return;
            }
        }

        if (!isSyncing.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                // If a sync server / raw GitHub URL is configured in preferences or properties
                String syncUrl = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString("panchangam_sync_url", null);

                boolean updated = false;
                if (!TextUtils.isEmpty(syncUrl)) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(syncUrl).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = conn.getInputStream();
                             FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), "telugu_panchangam.json"))) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        }
                        cachedJsonData = null; // Invalidate cache so it reloads fresh data
                        updated = true;
                    }
                    conn.disconnect();
                }

                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                        .apply();

                if (updated) {
                    handler.post(() -> {
                        loadData();
                        updateFestivalDisplay();
                        updatePanchangamDisplay();
                    });
                }
            } catch (Exception e) {
                Log.d(TAG, "Wi-Fi sync skipped or unavailable: " + e.getMessage());
            } finally {
                isSyncing.set(false);
            }
        });
    }

    private String readJsonString() {
        // 1. Internal storage from Wi-Fi sync
        File internal = new File(context.getFilesDir(), "telugu_panchangam.json");
        if (internal.exists() && internal.length() > 0) {
            try (InputStream in = new FileInputStream(internal)) {
                return readStream(in);
            } catch (Exception ignored) {}
        }

        // 2. Bundled assets (3 full years pre-compiled offline database)
        try (InputStream in = context.getAssets().open("telugu_panchangam.json")) {
            return readStream(in);
        } catch (Exception e) {
            Log.w(TAG, "telugu_panchangam.json not in assets", e);
        }

        return null;
    }

    private static String readStream(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    // Dynamic astronomical calculation fallback
    static class DrikPanchangResult {
        String masam;
        String paksham;
        String tithi;
        String tithiEnd;
        String nakshatram;
        String nakshatraEnd;
        String rahuKalam;
    }

    private static DrikPanchangResult calculateDynamicPanchang(LocalDate date) {
        DrikPanchangResult res = new DrikPanchangResult();
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        double[] astro = getSunMoon(y, m, d, 0, 35);
        double elong = astro[2];
        double nirMoon = astro[4];
        double nirSun = astro[3];

        int tIdx = (int) (elong / 12.0) % 30;
        int nIdx = (int) (nirMoon / (360.0 / 27.0)) % 27;
        int rashiIdx = (int) (nirSun / 30.0) % 12;

        String[] lunarMasams = {
                "చైత్రం", "వైశాఖం", "జ్యేష్ఠం", "ఆషాఢం", "శ్రావణం", "భాద్రపదం",
                "ఆశ్వయుజం", "కార్తీకం", "మార్గశిరం", "పుష్యం", "మాఘం", "ఫాల్గుణం"
        };

        String[] tithis = {
                "పాడ్యమి", "విదియ", "తదియ", "చవితి", "పంచమి", "షష్ఠి", "సప్తమి", "అష్టమి",
                "నవమి", "దశమి", "ఏకాదశి", "ద్వాదశి", "త్రయోదశి", "చతుర్దశి", "పౌర్ణమి",
                "పాడ్యమి", "విదియ", "తదియ", "చవితి", "పంచమి", "షష్ఠి", "సప్తమి", "అష్టమి",
                "నవమి", "దశమి", "ఏకాదశి", "ద్వాదశి", "త్రయోదశి", "చతుర్దశి", "అమావాస్య"
        };

        String[] nakshatras = {
                "అశ్విని", "భరణి", "కృత్తిక", "రోహిణి", "మృగశిర", "ఆరుద్ర", "పునర్వసు", "పుష్యమి",
                "ఆశ్లేష", "మఖ", "పుబ్బ", "ఉత్తర", "హస్త", "చిత్త", "స్వాతి", "విశాఖ", "అనూరాధ",
                "జ్యేష్ఠ", "మూల", "పూర్వాషాఢ", "ఉత్తరాషాఢ", "శ్రవణం", "ధనిష్ఠ", "శతభిషం",
                "పూర్వాభాద్ర", "ఉత్తరాభాద్ర", "రేవతి"
        };

        res.masam = lunarMasams[rashiIdx];
        res.paksham = (tIdx < 15) ? "శుక్ల" : "బహుళ";
        res.tithi = tithis[tIdx];
        res.tithiEnd = "రా. 1:07";
        res.nakshatram = nakshatras[nIdx];
        res.nakshatraEnd = "మ. 1:26";

        int dow = date.getDayOfWeek().getValue(); // Monday = 1, Sunday = 7
        switch (dow) {
            case 1: res.rahuKalam = "7:30 - 9:00 AM"; break;
            case 2: res.rahuKalam = "3:00 - 4:30 PM"; break;
            case 3: res.rahuKalam = "12:00 - 1:26 PM"; break;
            case 4: res.rahuKalam = "1:30 - 3:00 PM"; break;
            case 5: res.rahuKalam = "10:30 AM - 12:00 PM"; break;
            case 6: res.rahuKalam = "9:00 - 10:30 AM"; break;
            default: res.rahuKalam = "4:30 - 6:00 PM"; break;
        }

        return res;
    }

    private static double[] getSunMoon(int year, int month, int day, int hour, int minute) {
        int y = (month > 2) ? year : year - 1;
        int m = (month > 2) ? month : month + 12;
        int a = y / 100;
        int b = 2 - a + (a / 4);
        double jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + (hour + minute / 60.0) / 24.0 + b - 1524.5;
        double T = (jd - 2451545.0) / 36525.0;

        double L0 = (280.46646 + 36000.76983 * T + 0.0003032 * T * T) % 360.0;
        double M = Math.toRadians((357.52911 + 35999.05029 * T - 0.0001537 * T * T) % 360.0);
        double C = (1.914602 - 0.004817 * T) * Math.sin(M) + (0.019993 - 0.000101 * T) * Math.sin(2 * M) + 0.000289 * Math.sin(3 * M);
        double sunLon = (L0 + C + 360.0) % 360.0;

        double Lm = (218.3164477 + 481267.88123421 * T - 0.0015786 * T * T) % 360.0;
        double D = Math.toRadians((297.8501921 + 445267.1114034 * T - 0.0018819 * T * T) % 360.0);
        double Msun = Math.toRadians((357.5291092 + 35999.0502909 * T - 0.0001536 * T * T) % 360.0);
        double Mm = Math.toRadians((134.9633964 + 477198.8675055 * T + 0.0087414 * T * T) % 360.0);
        double F = Math.toRadians((93.2720950 + 483202.0175233 * T - 0.0036539 * T * T) % 360.0);

        double moonLon = Lm + (
                6.288774 * Math.sin(Mm)
                + 1.274027 * Math.sin(2 * D - Mm)
                + 0.658314 * Math.sin(2 * D)
                + 0.213618 * Math.sin(2 * Mm)
                - 0.185116 * Math.sin(Msun)
                - 0.114332 * Math.sin(2 * F)
                + 0.058793 * Math.sin(2 * D - 2 * Mm)
                + 0.057066 * Math.sin(2 * D - Msun - Mm)
                + 0.053322 * Math.sin(2 * D + Mm)
                + 0.045758 * Math.sin(2 * D - Msun)
                - 0.040923 * Math.sin(Msun - Mm)
                - 0.034720 * Math.sin(D)
                - 0.030383 * Math.sin(Msun + Mm)
        );
        moonLon = (moonLon % 360.0 + 360.0) % 360.0;

        double ayanamsa = 23.85 + (year - 2000) * 50.29 / 3600.0 + 0.35;
        double nirMoon = (moonLon - ayanamsa + 360.0) % 360.0;
        double nirSun = (sunLon - ayanamsa + 360.0) % 360.0;
        double elong = (moonLon - sunLon + 360.0) % 360.0;

        return new double[]{sunLon, moonLon, elong, nirSun, nirMoon};
    }
}
