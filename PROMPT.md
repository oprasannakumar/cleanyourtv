# Master Android TV Transformation Prompt

> Paste this prompt into an AI Coding Assistant (Antigravity, Claude Code, etc.) with terminal access to transform any Android TV / Google TV into an ad-free, cultural, personalized smart dashboard.

---

```markdown
You are an expert Android TV developer and systems engineer. I want you to customize and transform this open-source TV launcher (CleanYourTV) into an ultra-clean, ad-free, personalized smart dashboard for my TV.

================================================================================
STEP 0: ONBOARDING & USER INPUTS (STOP AND ASK FIRST)
================================================================================
Before making any changes or compiling code, STOP and ask me for the following required details:

1. **Google Calendar Secret iCal Link**:
   - Ask me for my Google Calendar secret iCal URL to sync personal events and birthdays.
   - (Provide instructions: On PC/phone browser, go to https://calendar.google.com -> Click the 3 dots next to your calendar -> Settings and sharing -> Scroll to 'Integrate calendar' -> Copy the 'Secret address in iCal format').
2. **City / Location for Weather**:
   - Ask for my preferred city/town (e.g. "Vijayawada") to configure local weather updates.
3. **TV IP Address**:
   - Ask for my TV's local IP address (or find it on the local network via ADB on port 5555).

*Do NOT proceed to Step 1 until I provide this information.*

================================================================================
STEP 1: MINIMALIST 3-ROW APP GRID (ZERO HALF-ICONS & NO DOCK)
================================================================================
- **Remove Dock Completely**: Disable the dock row and dock band in configuration (`dock_row=false`, `dock_band=false`).
- **Strict 3 Full Rows**: Symmetrically display exactly 3 full rows of applications. Compute the container height mathematically (`3 * itemHeightPx + padding`) with strict clipping (`clipChildren="true"`, `clipToPadding="true"`) so row 4 NEVER peeks through or gets cut off at the bottom.
- **Pure Usage Sorting**: Sort apps automatically based on actual launch frequency and recent usage so the most-used apps are always at the top.

================================================================================
STEP 2: SELECTED APP TITLE & "LAST OPENED" HEADER
================================================================================
- Directly above the first row of apps, display the currently focused application's details:
  - **App Title**: Large, bold, readable font (`26sp`, `#FFFFFF`).
  - **Metadata**: Display human-friendly relative time alongside the title (e.g., `• Last opened today, 12:53 pm`, `• Last opened 1 minute ago`, or `• Not opened recently`).
- Use Android's `UsageStatsManager` with an internal launch preference fallback to resolve real app launch timestamps.
- Dynamically update this header whenever the user moves focus across the grid using DPAD navigation.

================================================================================
STEP 3: GOOGLE CALENDAR SYNC & EVENT TICKER
================================================================================
- Integrate the user's provided Google Calendar secret iCal URL:
  - Fetch upcoming events/birthdays autonomously in the background (every 15–30 minutes) and cache them locally in `calendar_events.json`.
  - Display a dynamic countdown event ticker under the clock:
    - `🎂 TODAY: [EVENT NAME]` (when the event is today)
    - `🎂 TOMORROW: [EVENT NAME]`
    - `🎂 IN [X] DAYS: [EVENT NAME]` (for events within the next 5 days)

================================================================================
STEP 4: REGIONAL FESTIVALS & VEDIC PANCHANGAM
================================================================================
- Display regional festivals (e.g. Telugu festivals) using an integrated 365-day offline/online database:
  - `🪔 నేడు: [పండుగ]` (for today's festival)
  - `🪔 రేపు: [పండుగ]` (for tomorrow's festival)
  - `🪔 [తేదీ]: [పండుగ]` (for upcoming festivals)
- Display daily Vedic Panchangam:
  - `📜 [తిథి (ముగింపు సమయం)] • [నక్షత్రం (ముగింపు సమయం)] • రాహు కాలం: [సమయం]`

================================================================================
STEP 5: TOP ACTION BAR & ON-TV INSTANT SYNC
================================================================================
- Elevate the quick utility buttons neatly in the top-right corner:
  1. **Inputs Button**: Quick-switch between HDMI 1/2/3, AV, USB, and Miracast.
  2. **Wi-Fi Indicator**: Shows live Wi-Fi connection strength and Internet status.
  3. **Manual Sync Button**: An instant refresh button (`ic_sync.xml`) that pulls the latest calendar, festivals, and weather on demand, displaying a toast notification on completion.
  4. **Settings Button**: Direct one-click shortcut to TV system settings.
- Real-time Clock (12h format with AM/PM) and live local weather with condition icons.

================================================================================
STEP 6: COMPILE, VERIFY ON TV & RESTORE POINT
================================================================================
1. **Build**: Compile the APK with `./gradlew assembleDebug`.
2. **Deploy**: Push and install to the TV via ADB (`adb install -r app-debug.apk`).
3. **Verify Live**: Capture a screenshot via `adb shell screencap -p /sdcard/screen.png` and verify that all 3 rows fit cleanly with zero clipping, and the app title & last opened time update smoothly.
4. **Backup**: Create a `backup-final-deployment/` folder containing the compiled APK, source files, and one-click `restore.ps1` / `restore.bat` scripts.
```
