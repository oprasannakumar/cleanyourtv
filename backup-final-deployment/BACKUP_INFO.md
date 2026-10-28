# Restore Point: Telugu Panchangam & Clean Launcher

- **Date:** 2026-10-28 11:50 IST
- **Target Device:** Xiaomi Mi TV 4A (`MiTV-MZTU0`), Android 14 (API 34), IP `192.168.29.165:5555`
- **Package:** `com.gathin.tvhome`
- **Git Commit:** `9ad787ce`
- **Git Tag:** `restore-point-panchangam-festivals`

## Restoring
To restore everything (both installing the working APK directly to the TV and restoring source files to the project tree):
- Double click or run `restore.bat` (Command Prompt) or `restore.ps1` (PowerShell).

## Included Artifacts & Files
1. `app-debug-clean-final.apk` - Production-ready debug APK installed on the TV.
2. `TeluguPanchangam.java` - Panchangam calculation & 5 upcoming festival rotation engine.
3. `CalendarTicker.java` - Upcoming birthday/anniversary event ticker.
4. `CalendarSync.java` - Google Calendar Wi-Fi background sync.
5. `WeatherUpdate.java` - Live Vijayawada weather update engine.
6. `MainActivity.java` - Core launcher activity with direct system settings integration.
7. `AppAdapter.java` - Automatic usage-based app sorting.
8. `activity_main.xml` - Three-line layout under the clock (Birthdays, Festivals, Panchangam).
9. `config.xml` - Clean config with wallpapers, animations, and launcher settings disabled.
10. `telugu_panchangam.json` - 3-year Drik Ganitha Panchangam & festival dataset (2026, 2027, 2028).
11. `calendar_events.json` - Synced Google Calendar birthdays & events.
12. `generate_panchangam.py` - Offline ephemeris generator script.

## Verified UI Behavior
- **Line 1 (Birthdays):** Rotates every 3s through upcoming birthdays in 15sp `#FFD54F`.
- **Line 2 (Festivals):** Rotates every 3s through upcoming 5 Telugu festivals in 15sp `#FFFF8A65`.
  - Today: `🪔 నేడు: <festival>`
  - Tomorrow: `🪔 రేపు: <festival>`
  - Beyond tomorrow: `🪔 <నెల తేదీ>: <festival>` (e.g. `🪔 నవంబర్ 6: ధన త్రయోదశి (ధన్‌తేరస్)`)
- **Line 3 (Panchangam):** Steady native Telugu Panchangam in 14sp `#FFE0E0E0`:
  `📜 ఆశ్వయుజ బహుళ తదియ (రా. 1:07 వరకు) · కృత్తిక (మ. 1:26 వరకు) · రాహు: 12:00 - 1:26 PM`
- **Settings:** Gear icon opens Android TV System Settings directly with zero intermediate menus.
- **Speed:** Instant responsiveness, zero animation delays, no wallpapers/ambient overhead.
