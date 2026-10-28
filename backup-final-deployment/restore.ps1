# PowerShell Restore Script for CleanYourTV Telugu Panchangam Launcher
$ErrorActionPreference = "Stop"
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  Restore Point: Telugu Panchangam & Clean Launcher" -ForegroundColor Cyan
Write-Host "  Device IP: 192.168.29.165:5555" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$tvIp = "192.168.29.165:5555"
$apkPath = Join-Path $scriptDir "app-debug-clean-final.apk"

Write-Host "`n[1/3] Connecting to TV ($tvIp)..." -ForegroundColor Yellow
adb connect $tvIp

Write-Host "[2/3] Installing launcher APK to TV..." -ForegroundColor Yellow
adb -s $tvIp install -r $apkPath
adb -s $tvIp shell am start -S -n com.gathin.tvhome/.MainActivity

Write-Host "[3/3] Restoring codebase files to project tree..." -ForegroundColor Yellow
$launcherDir = Join-Path $scriptDir "..\launcher"
if (Test-Path $launcherDir) {
    Copy-Item (Join-Path $scriptDir "TeluguPanchangam.java") (Join-Path $launcherDir "app\src\main\java\com\gathin\tvhome\") -Force
    Copy-Item (Join-Path $scriptDir "CalendarTicker.java") (Join-Path $launcherDir "app\src\main\java\com\gathin\tvhome\") -Force
    Copy-Item (Join-Path $scriptDir "CalendarSync.java") (Join-Path $launcherDir "app\src\main\java\com\gathin\tvhome\") -Force
    Copy-Item (Join-Path $scriptDir "WeatherUpdate.java") (Join-Path $launcherDir "app\src\main\java\com\gathin\tvhome\") -Force
    Copy-Item (Join-Path $scriptDir "AppAdapter.java") (Join-Path $launcherDir "app\src\main\java\com\gathin\tvhome\") -Force
    Copy-Item (Join-Path $scriptDir "MainActivity.java") (Join-Path $launcherDir "app\src\main\java\com\gathin\tvhome\") -Force
    Copy-Item (Join-Path $scriptDir "activity_main.xml") (Join-Path $launcherDir "app\src\main\res\layout\") -Force
    Copy-Item (Join-Path $scriptDir "config.xml") (Join-Path $launcherDir "app\src\main\res\values\") -Force
    Copy-Item (Join-Path $scriptDir "AndroidManifest.xml") (Join-Path $launcherDir "app\src\main\") -Force
    Copy-Item (Join-Path $scriptDir "telugu_panchangam.json") (Join-Path $launcherDir "app\src\main\assets\") -Force
    Copy-Item (Join-Path $scriptDir "calendar_events.json") (Join-Path $launcherDir "app\src\main\assets\") -Force
    Write-Host "Source files restored successfully." -ForegroundColor Green
}

Write-Host "`n=====================================================" -ForegroundColor Green
Write-Host "  RESTORE COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
