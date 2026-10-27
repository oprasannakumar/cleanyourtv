package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public final class AppSwitcher {
    private static final String TAG = "AppSwitcher";
    private static final String PREFS = "tvhome_app_switcher";
    private static final String KEY_CURRENT_PKG = "current_pkg";
    private static final String KEY_PREVIOUS_PKG = "previous_pkg";

    private AppSwitcher() {}

    /**
     * Record an app launch so we know the most recent two apps.
     */
    public static void recordAppLaunch(Context ctx, String pkg) {
        if (ctx == null || TextUtils.isEmpty(pkg) || pkg.equals(ctx.getPackageName())) {
            return;
        }

        SharedPreferences sp = prefs(ctx);
        String current = sp.getString(KEY_CURRENT_PKG, null);

        if (pkg.equals(current)) {
            // Same app re-launched, keep existing order
            return;
        }

        // Shift current to previous, set new current
        sp.edit()
                .putString(KEY_PREVIOUS_PKG, current)
                .putString(KEY_CURRENT_PKG, pkg)
                .apply();

        Log.d(TAG, "Recorded app switch state: current=" + pkg + ", previous=" + current);
    }

    /**
     * Perform the Alt-Tab / switch to the other recent app.
     * Returns true if an app was switched to, false otherwise.
     */
    public static boolean switchApp(Context ctx) {
        if (ctx == null) return false;

        SharedPreferences sp = prefs(ctx);
        String current = sp.getString(KEY_CURRENT_PKG, null);
        String previous = sp.getString(KEY_PREVIOUS_PKG, null);

        String targetPkg = null;

        // If we have a previous app, switch to it and swap them!
        if (!TextUtils.isEmpty(previous)) {
            targetPkg = previous;
            // Swap them so the next double tap flips right back!
            sp.edit()
                    .putString(KEY_PREVIOUS_PKG, current)
                    .putString(KEY_CURRENT_PKG, previous)
                    .apply();
        } else if (!TextUtils.isEmpty(current)) {
            // Only 1 app in history
            targetPkg = current;
        }

        if (TextUtils.isEmpty(targetPkg)) {
            Toast.makeText(ctx, "No recent app to switch to", Toast.LENGTH_SHORT).show();
            return false;
        }

        PackageManager pm = ctx.getPackageManager();
        Intent launchIntent = pm.getLeanbackLaunchIntentForPackage(targetPkg);
        if (launchIntent == null) {
            launchIntent = pm.getLaunchIntentForPackage(targetPkg);
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            CharSequence label = targetPkg;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(targetPkg, 0));
            } catch (Exception ignored) {}

            Toast.makeText(ctx, "Switching to " + label, Toast.LENGTH_SHORT).show();
            try {
                ctx.startActivity(launchIntent);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Failed to switch to " + targetPkg, e);
            }
        }

        return false;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
