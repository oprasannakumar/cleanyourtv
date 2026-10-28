# Final Deployment Backup

- **Date:** 2026-10-28 11:35 IST
- **Target Device:** Xiaomi Mi TV 4A (`MiTV-MZTU0`), Android 14 (API 34), IP `192.168.29.165:5555`
- **Package:** `com.gathin.tvhome`

## Changes Summary
1. **Three Dedicated Lines Under Clock:**
   - **Line 1 (Events / Birthdays):** Rotates through top upcoming birthdays/anniversaries (3s interval, Amber `#FFD54F`, 15sp font).
   - **Line 2 (Telugu Festivals):** Rotates through upcoming 5 Telugu festival events with relative day countdown and Telugu date (3s interval, Saffron `#FFFF8A65`, 15sp font).
   - **Line 3 (Telugu Panchangam):** Steady native Telugu Panchangam with Masam, Paksham, Tithi with exact end timings, Nakshatram with exact end timings, and Rahu Kalam (Silver `#FFE0E0E0`, 14sp font).
2. **Larger Font Sizes for High TV Visibility:**
   - Events/Birthdays: 15sp medium font
   - Telugu Festivals: 15sp medium font
   - Telugu Panchangam & Tithi: 14sp regular font
3. **Panchangam & Festivals Engine (`TeluguPanchangam.java`):**
   - High-precision 3-year pre-compiled Telugu Panchangam & Festivals database (`telugu_panchangam.json`).
   - Algorithmic astronomical Drik Ganitha fallback engine.
   - Background auto-sync over Wi-Fi.
4. **Clean Launcher:**
   - All wallpapers, ambient glows, cinematic art, and animation delays removed.
   - Custom Launcher Settings removed; Gear button opens Android TV system settings directly.
   - Pure usage-based automatic app sort mode.
