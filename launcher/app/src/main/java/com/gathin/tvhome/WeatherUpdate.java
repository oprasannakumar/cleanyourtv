package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class WeatherUpdate {
    private static final String TAG = "WeatherUpdate";
    private static final String PREFS = "tvhome_weather_v4";
    private static final String KEY_TEXT = "cached_text";
    private static final String KEY_ICON = "cached_icon";
    private static final String KEY_TIME = "cached_time";
    private static final long UPDATE_INTERVAL_MS = 30 * 60 * 1000L; // 30 minutes

    private final Context context;
    private final View weatherRow;
    private final ImageView weatherIcon;
    private final TextView weatherView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean fetching = new AtomicBoolean(false);

    private static class WeatherResult {
        final String text;
        final int iconRes;

        WeatherResult(String text, int iconRes) {
            this.text = text;
            this.iconRes = iconRes;
        }
    }

    WeatherUpdate(Context context, View weatherRow, ImageView weatherIcon, TextView weatherView) {
        this.context = context.getApplicationContext();
        this.weatherRow = weatherRow;
        this.weatherIcon = weatherIcon;
        this.weatherView = weatherView;
    }

    void initAndRefresh() {
        if (weatherRow == null || weatherView == null) return;

        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cached = sp.getString(KEY_TEXT, null);
        int cachedIcon = sp.getInt(KEY_ICON, 0);
        String configCity = context.getString(R.string.weather_city);
        boolean cityMismatch = !TextUtils.isEmpty(configCity) && cached != null
                && !cached.toLowerCase(Locale.ROOT).contains(configCity.toLowerCase(Locale.ROOT));

        if (!TextUtils.isEmpty(cached) && !cityMismatch && cachedIcon != 0) {
            weatherView.setText(cached);
            if (weatherIcon != null) {
                weatherIcon.setImageResource(cachedIcon);
                weatherIcon.setVisibility(View.VISIBLE);
            }
            weatherRow.setVisibility(View.VISIBLE);
        }

        long lastTime = sp.getLong(KEY_TIME, 0);
        long now = System.currentTimeMillis();
        if (now - lastTime >= UPDATE_INTERVAL_MS || TextUtils.isEmpty(cached) || cityMismatch || cachedIcon == 0) {
            fetchAsync();
        }
    }

    void onMinuteTick() {
        if (weatherRow == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lastTime = sp.getLong(KEY_TIME, 0);
        if (System.currentTimeMillis() - lastTime >= UPDATE_INTERVAL_MS) {
            fetchAsync();
        }
    }

    void destroy() {
        executor.shutdown();
    }

    private void fetchAsync() {
        if (!fetching.compareAndSet(false, true)) return;

        executor.execute(() -> {
            try {
                WeatherResult result = performFetch();
                if (result != null && !TextUtils.isEmpty(result.text)) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_TEXT, result.text)
                            .putInt(KEY_ICON, result.iconRes)
                            .putLong(KEY_TIME, System.currentTimeMillis())
                            .apply();

                    mainHandler.post(() -> {
                        if (weatherView != null && weatherRow != null) {
                            weatherView.setText(result.text);
                            if (weatherIcon != null && result.iconRes != 0) {
                                weatherIcon.setImageResource(result.iconRes);
                                weatherIcon.setVisibility(View.VISIBLE);
                            }
                            weatherRow.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "Weather fetch failed: " + e.getMessage());
            } finally {
                fetching.set(false);
            }
        });
    }

    private WeatherResult performFetch() {
        double lat = Double.NaN;
        double lon = Double.NaN;
        String city = "";

        // 1. Check if exact coordinates are set in config
        try {
            String latStr = context.getString(R.string.weather_latitude);
            String lonStr = context.getString(R.string.weather_longitude);
            if (!TextUtils.isEmpty(latStr) && !TextUtils.isEmpty(lonStr)) {
                lat = Double.parseDouble(latStr.trim());
                lon = Double.parseDouble(lonStr.trim());
                String configCity = context.getString(R.string.weather_city);
                city = !TextUtils.isEmpty(configCity) ? configCity : "Vijayawada";
            }
        } catch (Exception ignored) {}

        // 2. Check if user set an explicit city in config to geocode
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            String configCity = context.getString(R.string.weather_city);
            if (!TextUtils.isEmpty(configCity) && !configCity.trim().isEmpty()) {
                try {
                    String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                            + URLEncoder.encode(configCity.trim(), "UTF-8") + "&count=1";
                    JSONObject geo = fetchJson(geoUrl);
                    if (geo != null && geo.has("results")) {
                        JSONArray results = geo.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject first = results.getJSONObject(0);
                            lat = first.optDouble("latitude", Double.NaN);
                            lon = first.optDouble("longitude", Double.NaN);
                            city = first.optString("name", configCity);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Geocoding failed for " + configCity, e);
                }
            }
        }

        // Automatic IP location if no explicit city or geocoding failed
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            // Try https://ipwho.is/ (accurate on IPv6)
            try {
                JSONObject ipwho = fetchJson("https://ipwho.is/");
                if (ipwho != null && ipwho.optBoolean("success", false)) {
                    lat = ipwho.optDouble("latitude", Double.NaN);
                    lon = ipwho.optDouble("longitude", Double.NaN);
                    city = ipwho.optString("city", "");
                }
            } catch (Exception ignored) {}
        }

        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            // Try https://freeipapi.com/api/json
            try {
                JSONObject freeip = fetchJson("https://freeipapi.com/api/json");
                if (freeip != null) {
                    lat = freeip.optDouble("latitude", Double.NaN);
                    lon = freeip.optDouble("longitude", Double.NaN);
                    city = freeip.optString("cityName", "");
                }
            } catch (Exception ignored) {}
        }

        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            // Try http://ip-api.com/json
            try {
                JSONObject ipLoc = fetchJson("http://ip-api.com/json");
                if (ipLoc != null && "success".equalsIgnoreCase(ipLoc.optString("status"))) {
                    lat = ipLoc.optDouble("lat", Double.NaN);
                    lon = ipLoc.optDouble("lon", Double.NaN);
                    city = ipLoc.optString("city", "");
                }
            } catch (Exception ignored) {}
        }

        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            return null;
        }

        // Fetch Open-Meteo current weather including is_day
        String weatherUrl = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,is_day",
                lat, lon);

        JSONObject weatherJson = fetchJson(weatherUrl);
        if (weatherJson == null) return null;

        JSONObject current = weatherJson.optJSONObject("current");
        if (current == null) return null;

        double temp = current.optDouble("temperature_2m", Double.NaN);
        int code = current.optInt("weather_code", -1);
        boolean isDay = current.optInt("is_day", 1) == 1;
        if (Double.isNaN(temp)) return null;

        int roundedTemp = (int) Math.round(temp);
        ConditionInfo conditionInfo = getConditionInfo(temp, code, isDay);

        StringBuilder sb = new StringBuilder();
        sb.append(roundedTemp).append("°C");
        if (!TextUtils.isEmpty(conditionInfo.label)) {
            sb.append(" · ").append(conditionInfo.label);
        }
        if (!TextUtils.isEmpty(city)) {
            sb.append(" · ").append(city);
        }

        return new WeatherResult(sb.toString(), conditionInfo.iconRes);
    }

    private static JSONObject fetchJson(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 CleanYourTv");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return new JSONObject(sb.toString());
            }
        } catch (Exception e) {
            Log.w(TAG, "Request failed for " + urlStr + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static class ConditionInfo {
        final String label;
        final int iconRes;

        ConditionInfo(String label, int iconRes) {
            this.label = label;
            this.iconRes = iconRes;
        }
    }

    private static ConditionInfo getConditionInfo(double temp, int code, boolean isDay) {
        // Hot condition override (> 35°C on clear/sunny days)
        if (temp >= 35.0 && (code == 0 || code == 1)) {
            return new ConditionInfo(isDay ? "Hot & Sunny" : "Hot", R.drawable.ic_weather_hot);
        }

        switch (code) {
            case 0: // Clear sky
                return new ConditionInfo(
                        isDay ? "Sunny" : "Clear",
                        isDay ? R.drawable.ic_weather_sunny : R.drawable.ic_weather_clear_night);
            case 1: // Mainly clear
                return new ConditionInfo(
                        isDay ? "Mainly Sunny" : "Mainly Clear",
                        isDay ? R.drawable.ic_weather_sunny : R.drawable.ic_weather_clear_night);
            case 2: // Partly cloudy
                return new ConditionInfo(
                        "Partly Cloudy",
                        isDay ? R.drawable.ic_weather_partly_cloudy_day : R.drawable.ic_weather_partly_cloudy_night);
            case 3: // Overcast
                return new ConditionInfo("Cloudy", R.drawable.ic_weather_cloudy);
            case 45:
            case 48: // Fog
                return new ConditionInfo("Foggy", R.drawable.ic_weather_fog);
            case 51:
            case 53:
            case 55:
            case 56:
            case 57: // Drizzle
                return new ConditionInfo("Drizzle", R.drawable.ic_weather_rainy);
            case 61:
            case 63:
            case 65:
            case 66:
            case 67: // Rain
                return new ConditionInfo("Raining", R.drawable.ic_weather_rainy);
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86: // Snow
                return new ConditionInfo("Snowing", R.drawable.ic_weather_snow);
            case 80:
            case 81:
            case 82: // Showers
                return new ConditionInfo("Rain Showers", R.drawable.ic_weather_rainy);
            case 95:
            case 96:
            case 99: // Thunderstorm
                return new ConditionInfo("Thunderstorm", R.drawable.ic_weather_thunderstorm);
            default:
                return new ConditionInfo(
                        isDay ? "Sunny" : "Clear",
                        isDay ? R.drawable.ic_weather_sunny : R.drawable.ic_weather_clear_night);
        }
    }
}
