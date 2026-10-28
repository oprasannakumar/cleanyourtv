# Backup Restore Point - CleanYourTV Launcher

**Created / Updated**: October 28, 2026, 2:25 PM IST
**Status**: Verified Live on TV (192.168.29.165:5555)

---

## What is included in this Restore Point:
1. **Application Title & Recently Opened Meta**:
   - Prominently displayed above the first row of apps (`26sp` title, `14sp` meta).
   - Dynamically updates as the user navigates across apps via DPAD.
   - Computes human-readable relative time (e.g. `Last opened today, 12:53 pm`, `Last opened 1 minute ago`) using `LastOpened.java` backed by internal preferences and Android `UsageStatsManager`.
2. **On-TV Manual Sync Button**:
   - Top action bar includes an instant Sync button (`ic_sync.xml`) alongside Inputs, Wi-Fi, and Settings.
   - Elevated position to maximize screen real estate.
   - Clicking sync immediately refetches Telugu festivals, Google Calendar birthday/special events, and Panchangam, showing Telugu toast confirmations.
3. **Dock-Free Clean Layout**:
   - Dock completely removed (`dock_row=false`, `dock_band=false`).
   - Symmetrically displays exactly 3 rows of apps.
   - Fixed bounding container (`exact3RowsH`) with strict clip bounds, completely eliminating half-cut / peek-through icons from row 4.
4. **Live Widgets**:
   - Real-time Clock & Live Weather for Vijayawada (e.g. 25°C, Rain Showers).
   - Google Calendar Event Ticker with real-time Telugu countdowns (e.g. "TODAY: MAVAYYA GARU'S BIRTHDAY", "IN 4 DAYS: (E.C.E) SHIVA'S BIRTHDAY").
   - Telugu Festival Display with Diya emoji.
   - Detailed Vedic Panchangam (Tithi, Nakshatram, Rahu Kalam).

---

## Restore Instructions:
Run either script from this directory:
- PowerShell: `./restore.ps1`
- Windows CMD: `restore.bat`

Or manually via ADB:
```cmd
adb push app-debug.apk /data/local/tmp/app.apk
adb shell pm install -r /data/local/tmp/app.apk
adb shell rm /data/local/tmp/app.apk
adb shell am start -n com.gathin.tvhome/.MainActivity
```
