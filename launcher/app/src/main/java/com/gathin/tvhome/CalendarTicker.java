package com.gathin.tvhome;

import android.content.Context;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CalendarTicker {
    private static final String TAG = "CalendarTicker";
    private static final long CYCLE_INTERVAL_MS = 3000L; // 3 seconds gap

    private final Context context;
    private final View tickerRow;
    private final TextView tickerText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<EventItem> events = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRunning = false;
    private LocalDate lastLoadedDate = null;

    private final Runnable cycleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            if (events.size() > 1 && tickerText != null) {
                currentIndex = (currentIndex + 1) % events.size();
                updateDisplay();
                tickerText.setAlpha(0.2f);
                tickerText.animate().alpha(1f).setDuration(300).start();
            }

            handler.postDelayed(this, CYCLE_INTERVAL_MS);
        }
    };

    static class EventItem implements Comparable<EventItem> {
        final String title;
        final LocalDate date;
        final String type;
        long daysAway;

        EventItem(String title, LocalDate date, String type) {
            this.title = title;
            this.date = date;
            this.type = type;
        }

        @Override
        public int compareTo(EventItem other) {
            return Long.compare(this.daysAway, other.daysAway);
        }
    }

    CalendarTicker(Context context, View tickerRow, TextView tickerText) {
        this.context = context.getApplicationContext();
        this.tickerRow = tickerRow;
        this.tickerText = tickerText;
    }

    void loadAndRefresh() {
        if (tickerRow == null || tickerText == null) return;

        handler.removeCallbacks(cycleRunnable);
        loadEvents();
        currentIndex = 0;
        updateDisplay();

        isRunning = true;
        if (events.size() > 1) {
            handler.postDelayed(cycleRunnable, CYCLE_INTERVAL_MS);
        }

        // Standalone background sync: refresh directly from Google Calendar via Wi-Fi
        CalendarSync.syncIfNeeded(context, () -> {
            loadEvents();
            updateDisplay();
        });
    }

    void syncNow(Runnable onComplete) {
        CalendarSync.syncNow(context, () -> handler.post(() -> {
            loadEvents();
            updateDisplay();
            if (onComplete != null) onComplete.run();
        }));
    }

    void onMinuteTick() {
        LocalDate today = LocalDate.now();
        if (lastLoadedDate != null && !today.equals(lastLoadedDate)) {
            loadEvents();
            if (currentIndex >= events.size()) currentIndex = 0;
            updateDisplay();

            // Daily rollover: trigger a live calendar sync
            CalendarSync.syncNow(context, () -> {
                loadEvents();
                updateDisplay();
            });
        }
    }

    void pause() {
        isRunning = false;
        handler.removeCallbacks(cycleRunnable);
    }

    void resume() {
        isRunning = true;
        handler.removeCallbacks(cycleRunnable);
        if (events.size() > 1) {
            handler.postDelayed(cycleRunnable, CYCLE_INTERVAL_MS);
        }

        // Check if Wi-Fi sync is due (every 15 min) whenever returning to Home
        CalendarSync.syncIfNeeded(context, () -> {
            loadEvents();
            updateDisplay();
        });
    }

    void destroy() {
        isRunning = false;
        handler.removeCallbacks(cycleRunnable);
    }

    private void loadEvents() {
        events.clear();
        lastLoadedDate = LocalDate.now();
        String json = readJsonString();
        if (TextUtils.isEmpty(json)) return;

        try {
            JSONArray arr = new JSONArray(json);
            LocalDate today = LocalDate.now();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String title = obj.optString("title", "");
                String dateStr = obj.optString("date", "");
                String type = obj.optString("type", "event");

                if (!TextUtils.isEmpty(dateStr) && !TextUtils.isEmpty(title)) {
                    try {
                        LocalDate d = LocalDate.parse(dateStr);
                        long days = ChronoUnit.DAYS.between(today, d);
                        // Upcoming events (top 5 upcoming birthdays/anniversaries)
                        boolean isBdayOrAnni = "birthday".equalsIgnoreCase(type) ||
                                "anniversary".equalsIgnoreCase(type) ||
                                title.toLowerCase().contains("birthday") ||
                                title.toLowerCase().contains("anniversary");

                        if (isBdayOrAnni && days >= 0 && days <= 60) {
                            EventItem item = new EventItem(title, d, type);
                            item.daysAway = days;
                            events.add(item);
                        }
                    } catch (Exception ignored) {}
                }
            }

            Collections.sort(events);
            if (events.size() > 5) {
                events.subList(5, events.size()).clear();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing calendar events", e);
        }
    }

    private void updateDisplay() {
        if (events.isEmpty()) {
            if (tickerRow != null) tickerRow.setVisibility(View.GONE);
            return;
        }

        if (currentIndex >= events.size()) currentIndex = 0;
        EventItem item = events.get(currentIndex);

        String icon;
        if ("birthday".equalsIgnoreCase(item.type)) {
            icon = "🎂 ";
        } else if ("festival".equalsIgnoreCase(item.type)) {
            icon = "🪔 ";
        } else if ("anniversary".equalsIgnoreCase(item.type)) {
            icon = "💍 ";
        } else {
            icon = "📅 ";
        }

        String when;
        if (item.daysAway == 0) {
            when = "TODAY";
        } else if (item.daysAway == 1) {
            when = "TOMORROW";
        } else {
            when = "IN " + item.daysAway + " DAYS";
        }

        String formatted = icon + when + ": " + item.title.toUpperCase();
        tickerText.setText(formatted);
        tickerRow.setVisibility(View.VISIBLE);
    }

    private String readJsonString() {
        // 1. Try internal synced storage
        File internal = new File(context.getFilesDir(), "calendar_events.json");
        if (internal.exists() && internal.length() > 0) {
            try (InputStream in = new FileInputStream(internal)) {
                return readStream(in);
            } catch (Exception ignored) {}
        }

        // 2. Try external app storage
        File external = new File(context.getExternalFilesDir(null), "calendar_events.json");
        if (external.exists() && external.length() > 0) {
            try (InputStream in = new FileInputStream(external)) {
                return readStream(in);
            } catch (Exception ignored) {}
        }

        // 3. Fallback to bundled asset
        try (InputStream in = context.getAssets().open("calendar_events.json")) {
            return readStream(in);
        } catch (Exception e) {
            Log.w(TAG, "No calendar_events.json found in assets", e);
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
}
