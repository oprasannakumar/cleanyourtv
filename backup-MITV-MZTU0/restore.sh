#!/bin/sh
# Restore script for MiTV-MZTU0
# Run: adb shell sh /sdcard/restore.sh
# Or from PC: adb -s 192.168.29.165:5555 shell sh /sdcard/restore.sh

TV="192.168.29.165:5555"
echo "=== Re-enabling all disabled packages ==="

# Telemetry / analytics
pm enable com.xiaomi.mitv.scenemode
pm enable com.xiaomi.wfdsinkhelperservice

# Streaming apps (that were not user-selected)
pm enable com.jio.media.stb.ondemand
pm enable com.jio.web.androidtv
pm enable com.suntv.sunnxt
pm enable com.spotify.tv.android
pm enable ahaflix.tv
pm enable com.yablio.sendfilestotv

# Mi content services
pm enable com.xiaomi.mitv.smartshare
pm enable com.mitv.toolhouse

# Amazon Prime Video (re-enabled per user request)
pm enable com.amazon.amazonvideo.livingroom

# Restore DNS if changed
settings put global private_dns_mode opportunistic
settings delete global private_dns_specifier

echo "=== Done. Reboot to confirm. ==="
