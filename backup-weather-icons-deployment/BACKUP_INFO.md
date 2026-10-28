# Backup: CleanYourTV Launcher Deployment (Weather & Icons)
**Date:** October 27, 2026 - 22:18 IST  
**Target Device:** Xiaomi Mi TV 4A / MiTV-MZTU0 (Android 14, API 34)  
**Package:** `com.gathin.tvhome`

## Included in this Backup:
1. `tvhome-weather-icons.apk` (Release APK, 602 KB, signed with debug keystore)
2. `WeatherUpdate.java` (Weather service integrating Open-Meteo, exact GPS coordinates, conditions, and caching)
3. `MainActivity.java` (DPAD right fix, USB input entry, WeatherManager lifecycle hooks, icon-only buttons)
4. `activity_main.xml` (Header layout with weather row, weather icon, weather text, and date)
5. `config.xml` (Configured with dock apps, icon-only buttons, exact coordinates, Vijayawada)
6. `AndroidManifest.xml` (Includes INTERNET permission, package queries, cleartext traffic)
7. `strings.xml` (Localized strings and weather configuration keys)
8. `drawables/`:
   - `ic_usb.xml` (USB Drive vector icon)
   - `ic_weather_sunny.xml` (Golden sun vector)
   - `ic_weather_clear_night.xml` (Crescent moon vector)
   - `ic_weather_partly_cloudy_day.xml` (Sun behind cloud)
   - `ic_weather_partly_cloudy_night.xml` (Moon behind cloud)
   - `ic_weather_cloudy.xml` (Overcast layered cloud)
   - `ic_weather_rainy.xml` (Cloud with rain drops)
   - `ic_weather_thunderstorm.xml` (Dark cloud with lightning bolt)
   - `ic_weather_fog.xml` (Cloud with fog mist lines)
   - `ic_weather_snow.xml` (Cloud with snowflakes)
   - `ic_weather_hot.xml` (High-heat sun with flare rays)

## Verified Features in this State:
- **Exact Coordinates Weather:** `16.546171° N, 80.637555° E` (Vijayawada) with live temperature, WMO condition text, and matching vector icons.
- **Icon-Only Header Buttons:** Round, label-free icons for Inputs, Wi-Fi, and Settings.
- **DPAD Navigation Fix:** Pressing Right Arrow in the Inputs menu transfers cursor focus to the Wi-Fi icon without opening the network screen.
- **USB Drive Option:** Integrated directly inside the Inputs dropdown with shortcut to Mi Media Explorer / VLC.
- **Dock Apps:** JioTV+, NetMirror, JioHotstar, Prime Video, Sony LIV, Airtel Xstream.
- **AdGuard DNS:** Active on TV (`dns.adguard-dns.com`).
- **All Existing Apps Preserved:** Zero packages disabled or removed.

## Quick Restore Instructions:
```powershell
adb connect 192.168.29.165:5555
adb -s 192.168.29.165:5555 push tvhome-weather-icons.apk /data/local/tmp/tvhome.apk
adb -s 192.168.29.165:5555 shell pm install -r /data/local/tmp/tvhome.apk
adb -s 192.168.29.165:5555 shell rm -f /data/local/tmp/tvhome.apk
```
