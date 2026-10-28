@echo off
setlocal
echo =====================================================
echo   Restore Point: Telugu Panchangam & Clean Launcher
echo   Device IP: 192.168.29.165:5555
echo =====================================================
echo.

set SCRIPT_DIR=%~dp0
set TV_IP=192.168.29.165:5555
set APK_PATH=%SCRIPT_DIR%app-debug-clean-final.apk

echo [1/3] Connecting to TV (%TV_IP%)...
adb connect %TV_IP%

echo [2/3] Installing launcher APK to TV and restarting...
adb -s %TV_IP% install -r "%APK_PATH%"
adb -s %TV_IP% shell am start -S -n com.gathin.tvhome/.MainActivity

echo [3/3] Restoring codebase files to project tree...
set LAUNCHER_DIR=%SCRIPT_DIR%..\launcher
if exist "%LAUNCHER_DIR%" (
    copy /Y "%SCRIPT_DIR%TeluguPanchangam.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%CalendarTicker.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%CalendarSync.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%WeatherUpdate.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%AppAdapter.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%MainActivity.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%activity_main.xml" "%LAUNCHER_DIR%\app\src\main\res\layout\" >nul
    copy /Y "%SCRIPT_DIR%config.xml" "%LAUNCHER_DIR%\app\src\main\res\values\" >nul
    copy /Y "%SCRIPT_DIR%AndroidManifest.xml" "%LAUNCHER_DIR%\app\src\main\" >nul
    copy /Y "%SCRIPT_DIR%telugu_panchangam.json" "%LAUNCHER_DIR%\app\src\main\assets\" >nul
    copy /Y "%SCRIPT_DIR%calendar_events.json" "%LAUNCHER_DIR%\app\src\main\assets\" >nul
    echo Source files restored to project tree.
)

echo.
echo =====================================================
echo   RESTORE COMPLETED SUCCESSFULLY!
echo =====================================================
pause
