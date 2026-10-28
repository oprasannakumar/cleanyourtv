# Backup Restore Point - CleanYourTV Launcher

**Created / Updated**: October 28, 2026, 5:25 PM IST
**Status**: Verified Live on TV (192.168.29.165:5555)

---

## What is included in this Restore Point:

1. **Application Title & Recently Opened Meta**:
   - Prominently displayed above the first row of apps (`26sp` title, `14sp` meta).
   - Dynamically updates as the user navigates across apps via DPAD.
   - Computes human-readable relative time (e.g. `Last opened today, 12:53 pm`, `Last opened 1 minute ago`) using `LastOpened.java` backed by internal preferences and Android `UsageStatsManager`.

2. **On-TV Manual Sync Button**:
   - Top action bar includes an instant Sync button (`ic_sync.xml`) alongside Inputs, Wi-Fi, and Settings.
   - Clicking sync immediately refetches Telugu festivals, Google Calendar birthday/special events, and Panchangam, showing Telugu toast confirmations.

3. **Dock-Free Clean Layout**:
   - Dock completely removed (`dock_row=false`, `dock_band=false`).
   - Symmetrically displays exactly 3 rows of apps.
   - Fixed bounding container (`exact3RowsH`) with strict clip bounds, completely eliminating half-cut / peek-through icons from row 4.

4. **Live Widgets & Tickers**:
   - Real-time Clock & Live Weather for Vijayawada (e.g. 26°C, Drizzle).
   - Google Calendar Event Ticker with real-time Telugu countdowns (e.g. `TODAY: MAVAYYA GARU'S BIRTHDAY`, `IN 4 DAYS: (E.C.E) SHIVA'S BIRTHDAY`, `IN 4 DAYS: EKSHITHA'S BIRTHDAY`).
   - Telugu Festival Display with Diya emoji (`నేడు: అట్ల తద్దె`, `నవంబర్ 5: రమా ఏకాదశి`, `నవంబర్ 6: ధన త్రయోదశి`).
   - Detailed Vedic Panchangam (`ఆశ్వయుజ బహుళ తదియ (రా. 1:07 వరకు) • కృత్తిక (మ. 1:26 వరకు) • రాహు: 12:00 - 1:26 PM`).

---

## Verified Screenshots Included:
- `screenshots/final-home-birthday-mavayya.png` - "TODAY: MAVAYYA GARU'S BIRTHDAY" & "నేడు: అట్ల తద్దె"
- `screenshots/final-home-birthday-shiva.png` - "IN 4 DAYS: (E.C.E) SHIVA'S BIRTHDAY", "నవంబర్ 5: రమా ఏకాదశి" & Panchangam
- `screenshots/final-home-birthday-ekshitha.png` - "IN 4 DAYS: EKSHITHA'S BIRTHDAY", "ధన త్రయోదశి"
- `screenshots/final-ontv-sync-toast.png` - Live TV Toast confirmation on instant sync
- `screenshots/final-selected-app-header.png` - Focused app title and relative last opened time

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
