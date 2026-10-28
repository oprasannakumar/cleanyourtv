# Restore Script: CleanYourTV Weather Deployment
$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Restore Point: CleanYourTV Weather Deployment" -ForegroundColor Cyan
Write-Host "  Device IP: 192.168.29.165:5555" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$tvIp = "192.168.29.165:5555"
$apkPath = Join-Path $scriptDir "tvhome-weather-icons.apk"
$launcherDir = Join-Path (Split-Path -Parent $scriptDir) "launcher"

Write-Host "`n[1/3] Connecting to TV ($tvIp)..." -ForegroundColor Yellow
adb connect $tvIp

Write-Host "`n[2/3] Installing launcher APK to TV and enabling services..." -ForegroundColor Yellow
adb -s $tvIp push "$apkPath" /data/local/tmp/tvhome.apk
adb -s $tvIp shell pm install -r /data/local/tmp/tvhome.apk
adb -s $tvIp shell rm -f /data/local/tmp/tvhome.apk
adb -s $tvIp shell pm enable com.xiaomi.mitv.smartshare
adb -s $tvIp shell pm enable com.xiaomi.wfdsinkhelperservice

Write-Host "`n[3/3] Restoring source files to project tree..." -ForegroundColor Yellow
if (Test-Path $launcherDir) {
    Copy-Item (Join-Path $scriptDir "WeatherUpdate.java") "$launcherDir\app\src\main\java\com\gathin\tvhome\" -Force
    Copy-Item (Join-Path $scriptDir "MainActivity.java") "$launcherDir\app\src\main\java\com\gathin\tvhome\" -Force
    Copy-Item (Join-Path $scriptDir "activity_main.xml") "$launcherDir\app\src\main\res\layout\" -Force
    Copy-Item (Join-Path $scriptDir "config.xml") "$launcherDir\app\src\main\res\values\" -Force
    Copy-Item (Join-Path $scriptDir "AndroidManifest.xml") "$launcherDir\app\src\main\" -Force
    Copy-Item (Join-Path $scriptDir "strings.xml") "$launcherDir\app\src\main\res\values\" -Force
    Copy-Item (Join-Path $scriptDir "drawables\*.xml") "$launcherDir\app\src\main\res\drawable\" -Force
    Write-Host "Code files restored to workspace." -ForegroundColor Green
}

Write-Host "`n===================================================" -ForegroundColor Green
Write-Host "  RESTORE COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
