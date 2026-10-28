package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class CalendarSync {
    private static final String TAG = "CalendarSync";
    private static final String PREFS = "calendar_sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    private static final long SYNC_INTERVAL_MS = 4 * 60 * 60 * 1000L; // Every 4 hours

    // User's secret iCal feed for standalone direct sync
    private static final String SECRET_ICAL_URL =
            "https://calendar.google.com/calendar/ical/oprasannak%40gmail.com/private-88a17daba1e1c37a39d399f3168422e9/basic.ics";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    static void syncIfNeeded(Context context, Runnable onComplete) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = sp.getLong(KEY_LAST_SYNC, 0);
        long now = System.currentTimeMillis();

        if (now - last >= SYNC_INTERVAL_MS) {
            syncNow(context, onComplete);
        }
    }

    static void syncNow(Context context, Runnable onComplete) {
        if (!isSyncing.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                boolean success = performSync(context.getApplicationContext());
                if (success) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                            .apply();

                    if (onComplete != null) {
                        mainHandler.post(onComplete);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Calendar background sync error: " + e.getMessage());
            } finally {
                isSyncing.set(false);
            }
        });
    }

    private static boolean performSync(Context context) {
        try {
            LocalDate today = LocalDate.now();
            String timeMin = today.format(DateTimeFormatter.ISO_LOCAL_DATE) + "T00:00:00Z";
            String timeMax = today.plusDays(40).format(DateTimeFormatter.ISO_LOCAL_DATE) + "T23:59:59Z";

            List<JSONObject> allEvents = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            // 1. Try Google Calendar OAuth API (fetches Google Contacts birthdays + holidays)
            String authJsonStr = readAuthJson(context);
            if (!TextUtils.isEmpty(authJsonStr)) {
                try {
                    JSONObject auth = new JSONObject(authJsonStr);
                    String clientId = auth.optString("client_id", "");
                    String clientSecret = auth.optString("client_secret", "");
                    String refreshToken = auth.optString("refresh_token", "");

                    if (!TextUtils.isEmpty(clientId) && !TextUtils.isEmpty(clientSecret) && !TextUtils.isEmpty(refreshToken)) {
                        String accessToken = refreshAccessToken(clientId, clientSecret, refreshToken);
                        if (!TextUtils.isEmpty(accessToken)) {
                            // Primary personal calendar (includes Contact Birthdays)
                            List<JSONObject> primary = fetchEvents(accessToken, "primary", timeMin, timeMax);
                            for (JSONObject item : primary) {
                                String summary = item.optString("summary", "");
                                String start = extractStartDate(item);
                                String eventType = item.optString("eventType", "default");
                                if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(summary)) {
                                    boolean isBday = "birthday".equalsIgnoreCase(eventType) || summary.toLowerCase(Locale.ROOT).contains("birthday");
                                    boolean isAnni = summary.toLowerCase(Locale.ROOT).contains("anniversary");
                                    String t = isBday ? "birthday" : (isAnni ? "anniversary" : "event");

                                    String key = start + "_" + summary.toLowerCase(Locale.ROOT).trim();
                                    if (seen.add(key)) {
                                        JSONObject ev = new JSONObject();
                                        ev.put("title", summary);
                                        ev.put("date", start);
                                        ev.put("type", t);
                                        allEvents.add(ev);
                                    }
                                }
                            }

                            // Indian holidays calendar
                            String holidayCal = URLEncoder.encode("en.indian#holiday@group.v.calendar.google.com", "UTF-8");
                            List<JSONObject> holidays = fetchEvents(accessToken, holidayCal, timeMin, timeMax);
                            for (JSONObject item : holidays) {
                                String summary = item.optString("summary", "");
                                String start = extractStartDate(item);
                                if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(summary)) {
                                    String key = start + "_" + summary.toLowerCase(Locale.ROOT).trim();
                                    if (seen.add(key)) {
                                        JSONObject ev = new JSONObject();
                                        ev.put("title", summary);
                                        ev.put("date", start);
                                        ev.put("type", "festival");
                                        allEvents.add(ev);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "OAuth sync failed, falling back to iCal: " + e.getMessage());
                }
            }

            // 2. Fetch from user's secret iCal feed (direct standalone sync)
            List<JSONObject> icalEvents = fetchIcalEvents(SECRET_ICAL_URL, today);
            for (JSONObject item : icalEvents) {
                String summary = item.optString("title", "");
                String date = item.optString("date", "");
                String key = date + "_" + summary.toLowerCase(Locale.ROOT).trim();
                if (seen.add(key)) {
                    allEvents.add(item);
                }
            }

            if (allEvents.isEmpty()) {
                return false;
            }

            // Sort all by date
            Collections.sort(allEvents, (a, b) -> a.optString("date", "").compareTo(b.optString("date", "")));

            JSONArray finalArray = new JSONArray();
            for (JSONObject ev : allEvents) {
                finalArray.put(ev);
            }

            // 3. Save to external storage file
            File outFile = new File(context.getExternalFilesDir(null), "calendar_events.json");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(finalArray.toString(2).getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }

            Log.i(TAG, "Calendar sync successful! Cached " + finalArray.length() + " events to " + outFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Sync failed", e);
            return false;
        }
    }

    private static List<JSONObject> fetchIcalEvents(String urlStr, LocalDate today) {
        List<JSONObject> list = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (TvHome)");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String line;
                boolean inEvent = false;
                String summary = null;
                String dtStart = null;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if ("BEGIN:VEVENT".equals(line)) {
                        inEvent = true;
                        summary = null;
                        dtStart = null;
                    } else if ("END:VEVENT".equals(line)) {
                        if (inEvent && !TextUtils.isEmpty(summary) && !TextUtils.isEmpty(dtStart)) {
                            String date = null;
                            if (dtStart.length() >= 8) {
                                String y = dtStart.substring(0, 4);
                                String m = dtStart.substring(4, 6);
                                String d = dtStart.substring(6, 8);
                                date = y + "-" + m + "-" + d;
                            }
                            if (date != null) {
                                try {
                                    LocalDate evDate = LocalDate.parse(date);
                                    if (!evDate.isBefore(today) && !evDate.isAfter(today.plusDays(40))) {
                                        boolean isBday = summary.toLowerCase(Locale.ROOT).contains("birthday");
                                        boolean isAnni = summary.toLowerCase(Locale.ROOT).contains("anniversary");
                                        JSONObject ev = new JSONObject();
                                        ev.put("title", summary);
                                        ev.put("date", date);
                                        ev.put("type", isBday ? "birthday" : (isAnni ? "anniversary" : "event"));
                                        list.add(ev);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        inEvent = false;
                    } else if (inEvent) {
                        if (line.startsWith("SUMMARY:")) {
                            summary = line.substring(8);
                        } else if (line.startsWith("DTSTART")) {
                            int colon = line.indexOf(':');
                            if (colon >= 0 && colon < line.length() - 1) {
                                dtStart = line.substring(colon + 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error fetching iCal feed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return list;
    }

    private static String extractStartDate(JSONObject item) {
        JSONObject startObj = item.optJSONObject("start");
        if (startObj == null) return null;
        String date = startObj.optString("date", null);
        if (TextUtils.isEmpty(date)) {
            String dt = startObj.optString("dateTime", null);
            if (!TextUtils.isEmpty(dt) && dt.length() >= 10) {
                date = dt.substring(0, 10);
            }
        }
        return date;
    }

    private static String refreshAccessToken(String clientId, String clientSecret, String refreshToken) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://oauth2.googleapis.com/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "client_id=" + URLEncoder.encode(clientId, "UTF-8")
                    + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
                    + "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8")
                    + "&grant_type=refresh_token";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String resp = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(resp);
                return json.optString("access_token", null);
            } else {
                Log.w(TAG, "Token refresh response code: " + code + ", msg: " + readStream(conn.getErrorStream()));
            }
        } catch (Exception e) {
            Log.w(TAG, "Token refresh request failed", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static List<JSONObject> fetchEvents(String accessToken, String calendarId, String timeMin, String timeMax) {
        List<JSONObject> results = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            String q = "timeMin=" + URLEncoder.encode(timeMin, "UTF-8")
                    + "&timeMax=" + URLEncoder.encode(timeMax, "UTF-8")
                    + "&singleEvents=true&orderBy=startTime&maxResults=50";
            URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events?" + q);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int code = conn.getResponseCode();
            if (code == 200) {
                String resp = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(resp);
                JSONArray items = json.optJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        results.add(items.getJSONObject(i));
                    }
                }
            } else {
                Log.w(TAG, "Fetch events code: " + code + " for " + calendarId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error fetching events for " + calendarId, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return results;
    }

    private static String readAuthJson(Context context) {
        File ext = new File(context.getExternalFilesDir(null), "calendar_auth.json");
        if (ext.exists() && ext.length() > 0) {
            try (InputStream in = ext.toURI().toURL().openStream()) {
                return readStream(in);
            } catch (Exception ignored) {}
        }
        try (InputStream in = context.getAssets().open("calendar_auth.json")) {
            return readStream(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readStream(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
