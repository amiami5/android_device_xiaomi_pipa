/*
 * Copyright (C) 2023-2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.stylus.PenUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenStateReceiver extends BroadcastReceiver {

    private static final String TAG = "PipaControls";

    private static final String STYLUS_MODE_KEY            = "stylus_mode_key";
    private static final String FORCE_RECOGNIZE_STYLUS_KEY = "force_recognize_stylus_key";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_SCREEN_ON.equals(intent.getAction())) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean stylusModeEnabled = prefs.getBoolean(STYLUS_MODE_KEY, false);
        boolean forceRecognize    = prefs.getBoolean(FORCE_RECOGNIZE_STYLUS_KEY, false);

        if (!stylusModeEnabled && !forceRecognize) return;

        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                if (!PenUtils.isSetup()) {
                    logInfo("PenUtils not initialized — running full setup");
                    PenUtils.setup(appContext);
                } else {
                    logInfo("Re-applying pen mode after screen on");
                    PenUtils.enablePenMode();
                }
            } catch (Exception e) {
                logError("Error handling screen on: " + e.getMessage());
            }
        }, "pipa-screen-on").start();
    }

    private void logInfo(String message)  { Log.i(TAG, ts() + message); }
    private void logError(String message) { Log.e(TAG, ts() + message); }

    private static String ts() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
