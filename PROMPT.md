# The prompt

Paste this into Claude Code, Antigravity, or another coding assistant with shell access on a computer that is
on the same Wi-Fi as the TV. Works on Android TV / Google TV sets (Xiaomi, TCL, Hisense, Sony,
OnePlus, Realme, Nvidia Shield and most others). It will not work on Samsung Tizen, LG webOS or
Fire TV. For Samsung, LG, Roku and other non-Android TVs, use https://cleanyour.tv/not-android instead:
it turns off the tracking with the remote. If you cloned this repo, mention that: the assistant can reuse `tools/` and `launcher/`
instead of writing them from scratch, and only needs to edit `launcher/.../values/config.xml`.

Before you start, on the TV: Settings → Device Preferences → About → press OK on "Build" 7 times,
then go back → Developer options → turn on USB debugging (and Network/ADB debugging if your TV
shows it). Keep the TV on. Some boxes cut power to their USB port while USB debugging is on, so a
USB speaker or drive plugged into it goes dead until you switch debugging off again. A USB Ethernet
adapter on that port can drop the same way, so plan to do the whole session in one sitting.

---

I want to debloat my Android TV and replace its launcher with a clean, cultural, personalized smart dashboard.
My TV model is: <MODEL NAME HERE>. Work on the real device, step by step, and verify everything you
do with screenshots pulled from the TV. Ask me before anything that depends on my preferences.
Where these steps name a file under `tools/`, `docs/` or `examples/` that is not in your working
folder, you are running without this repo: clone it first with
`git clone https://github.com/oprasannakumar/cleanyourtv` and work from there, so `tools/disable.sh` and the
rest keep their safety checks, rather than stopping or writing your own.

Onboarding (Stop and ask me first before proceeding)
- Before modifying or building anything, stop and ask me for:
  1. **Google Calendar Secret iCal URL**: Ask for my calendar's secret iCal URL to sync my personal events and birthdays. (Explain how: go to https://calendar.google.com → 3 dots next to calendar → Settings and sharing → 'Integrate calendar' → copy 'Secret address in iCal format').
  2. **City / Town for Weather**: Ask for my preferred location (e.g. "Vijayawada") to configure local weather updates and GPS coordinates.
  3. **TV IP Address**: Ask for my TV's local IP address if not automatically discovered.

Connect
- Find the TV on my network (hostname, ARP table, or ask me for the IP) and connect over ADB on port 5555.
  If the TV is in another home, I'll give you its Tailscale address instead (docs/REMOTE.md): use that,
  don't scan my local network.
- If the stock adb binary fails with "No route to host" on macOS even though the port is open, that is
  macOS Local Network privacy blocking adb. Don't debug it: use the Python `adb-shell` package in a venv
  as the ADB client instead. If its file push hangs on larger files, transfer files as base64 chunks
  through the shell and verify the md5.
- If your own commands can't reach the TV at all while it answers from an ordinary terminal (a sandbox that
  blocks the local network), ask me to run `tools/relay.py <tv ip>` in a terminal and leave it running, then
  use `TV_HOST=127.0.0.1 TV_PORT=15555` (`tools/relay.md`).
- If port 5555 stays closed, the TV may only offer Wireless debugging (Google TV sets often do): look for
  `_adb-tls-connect._tcp` with `dns-sd -B` or `adb mdns services`, ask me for the code and port under
  Developer options > Wireless debugging > Pair device with pairing code, then `adb pair`, `adb connect`
  and `adb tcpip 5555`. Android switches Wireless debugging off whenever Wi-Fi drops.
- Tell me when the "Allow USB debugging?" prompt will appear on the TV so I can accept it.
- If `adb install` hangs on Android 14 it is waiting for an on-screen confirmation; push the APK and run
  `pm install -r` over the shell instead.

Back up first
- Save the full package list, disabled-package list, getprop output and all settings to a backup folder
  before changing anything, and write a restore script that re-enables everything you disable.
- Ask me what is plugged into the TV's USB ports, and ask separately whether the TV's network comes
  through a USB Ethernet adapter. Prefer Wireless debugging if the TV has it.
- When you finish, close network ADB if requested, or keep port 5555 open if I use local ADB controls.

Debloat
- List installed packages and sort them into: manufacturer telemetry/analytics, manufacturer content
  and ad services (PatchWall, recommendation rows, content providers), background services I probably
  don't need (share/cast duplicates, cleaners), preloaded apps, and streaming apps.
- Disable unwanted telemetry and ad packages with `pm disable-user --user 0`, never uninstall system packages.
- Never disable core TV services: HDMI/tuner input services, remote Bluetooth pairing, on-screen keyboard,
  Google Play Store and Play Services.
- Before touching streaming apps, voice assistant, Chromecast, Miracast or system updates, ask me
  which ones I use with a checklist question and disable only what I don't select.
- Disable in small batches and screenshot between them. Wait 15 seconds before rebooting.

Launcher Layout & Core Features
- Build a lightweight native Android TV launcher (Java, AndroidX RecyclerView, minSdk matching TV).
- **No Dock (Pure 3 Full Rows)**: Disable the dock row and dock band completely (`dock_row=false`, `dock_band=false`). Symmetrically display exactly 3 full rows of applications. Compute the height mathematically (`exact3RowsH = 3 * itemHeightPx + padding`) with strict clipping (`clipChildren="true"`, `clipToPadding="true"`) so row 4 NEVER peeks through or gets cut off at the bottom.
- **Pure Usage Sorting**: Sort apps automatically based on actual launch frequency and recent usage so the most-used apps stay conveniently in the first row.
- **Selected App Title & Last Opened Time**: Directly above the first row of apps, display the currently focused application's details:
  - **App Title**: Large, bold typography (`26sp`, `#FFFFFF`).
  - **Metadata**: Display human-friendly relative time alongside the title (e.g., `• Last opened today, 12:53 pm`, `• Last opened 1 minute ago`, or `• Not opened recently`).
  - Backed by Android's `UsageStatsManager` with internal launch preference fallback. Dynamically updates upon DPAD selection.
- **Elevated Top Action Bar**:
  - Position quick utility buttons higher up in the top-right corner:
    1. **Inputs Button**: Quick-switch dropdown between HDMI 1/2/3, AV, USB, and Miracast.
    2. **Wi-Fi Indicator**: Shows live Wi-Fi signal and Internet status.
    3. **Manual Sync Button**: An instant refresh icon button (`ic_sync.xml`) that pulls the latest calendar, festivals, panchangam, and weather on demand, displaying a toast notification on completion.
    4. **Settings Button**: Direct one-click shortcut to TV system settings.
- **Clock & Weather**:
  - Real-time 12-hour formatted clock with AM/PM and current date.
  - Live local weather with temperature, condition text, and matching vector weather icon.
- **Calendar, Festival & Panchangam Ticker**:
  - Directly under the clock and weather, display a cycling/stacked cultural and personal event ticker:
    1. **Google Calendar Events** (`🎂`): Autonomous sync from the user's secret iCal URL with relative countdowns (`🎂 TODAY: [NAME]'S BIRTHDAY`, `🎂 TOMORROW: [EVENT]`, `🎂 IN [X] DAYS: [EVENT]`).
    2. **Telugu Festivals** (`🪔`): Integrated 365-day Telugu festival database with relative day indicators (`🪔 నేడు: [పండుగ]`, `🪔 రేపు: [పండుగ]`, `🪔 [తేదీ]: [పండుగ]`).
    3. **Vedic Daily Panchangam** (`📜`): Real-time daily Tithi (with end time), Nakshatram (with end time), and Rahu Kalam timings.

Verify & Restore Point
- After each change, inject remote key presses over ADB, capture screenshots (`adb shell screencap -p /sdcard/screen.png`), and inspect them:
  - Confirm exactly 3 rows are visible without clipping.
  - Confirm the selected app name and last-opened time update properly above row 1.
  - Confirm all ticker items (Calendar, Festivals, Panchangam) and sync buttons work cleanly.
- Create a complete backup restore point directory (`backup-final-deployment/`) containing:
  - The compiled APK (`app-debug.apk`)
  - All modified source and layout files (`MainActivity.java`, `LastOpened.java`, `activity_main.xml`, `config.xml`, `ic_sync.xml`)
  - One-click restore scripts (`restore.ps1` and `restore.bat`)
  - Detailed `BACKUP_INFO.md` documentation.
- Commit all changes to Git and provide the push script (`push-to-github.bat` / `push-to-github.ps1`) to publish to GitHub without HTTP timeout issues.

---

Expect to answer onboarding questions (Google Calendar link, City, TV IP) and debloat preferences. Everything is reversible.
