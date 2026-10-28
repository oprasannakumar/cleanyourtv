@echo off
setlocal
echo ===================================================
echo   Restore Point: CleanYourTV Weather Deployment
echo   Device IP: 192.168.29.165:5555
echo ===================================================
echo.

set SCRIPT_DIR=%~dp0
set TV_IP=192.168.29.165:5555
set APK_PATH=%SCRIPT_DIR%tvhome-weather-icons.apk

echo [1/3] Connecting to TV (%TV_IP%)...
adb connect %TV_IP%

echo [2/3] Installing launcher APK to TV and enabling services...
adb -s %TV_IP% push "%APK_PATH%" /data/local/tmp/tvhome.apk
adb -s %TV_IP% shell pm install -r /data/local/tmp/tvhome.apk
adb -s %TV_IP% shell rm -f /data/local/tmp/tvhome.apk
adb -s %TV_IP% shell pm enable com.xiaomi.mitv.smartshare
adb -s %TV_IP% shell pm enable com.xiaomi.wfdsinkhelperservice

echo [3/3] Restoring codebase files...
set LAUNCHER_DIR=%SCRIPT_DIR%..\launcher
if exist "%LAUNCHER_DIR%" (
    copy /Y "%SCRIPT_DIR%WeatherUpdate.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%MainActivity.java" "%LAUNCHER_DIR%\app\src\main\java\com\gathin\tvhome\" >nul
    copy /Y "%SCRIPT_DIR%activity_main.xml" "%LAUNCHER_DIR%\app\src\main\res\layout\" >nul
    copy /Y "%SCRIPT_DIR%config.xml" "%LAUNCHER_DIR%\app\src\main\res\values\" >nul
    copy /Y "%SCRIPT_DIR%AndroidManifest.xml" "%LAUNCHER_DIR%\app\src\main\" >nul
    copy /Y "%SCRIPT_DIR%strings.xml" "%LAUNCHER_DIR%\app\src\main\res\values\" >nul
    copy /Y "%SCRIPT_DIR%drawables\*.xml" "%LAUNCHER_DIR%\app\src\main\res\drawable\" >nul
    echo Source files restored to project tree.
)

echo.
echo ===================================================
echo   RESTORE COMPLETED SUCCESSFULLY!
echo ===================================================
pause
