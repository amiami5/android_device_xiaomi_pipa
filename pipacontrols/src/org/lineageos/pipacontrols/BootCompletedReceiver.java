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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.keyboard.KeyboardUtils;
import org.lineageos.pipacontrols.stylus.PenUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "PipaControls";

    private static final long INIT_DELAY_MS = 3000;

    private static final String STYLUS_MODE_KEY     = "stylus_mode_key";
    private static final String FORCE_RECOGNIZE_KEY = "force_recognize_stylus_key";
    private static final String KEYBOARD_MODE_KEY   = "keyboard_mode_key";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) return;

        logInfo("Boot completed — scheduling peripheral init in " + INIT_DELAY_MS + " ms");

        final Context appContext = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> new Thread(() -> initPeripherals(appContext), "pipa-boot-init").start(),
                INIT_DELAY_MS);
    }

    private static void initPeripherals(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean stylusEnabled   = prefs.getBoolean(STYLUS_MODE_KEY, false);
        boolean forceRecognize  = prefs.getBoolean(FORCE_RECOGNIZE_KEY, false);
        boolean keyboardEnabled = prefs.getBoolean(KEYBOARD_MODE_KEY, false);

        logInfo("Init — stylus=" + stylusEnabled
                + ", force=" + forceRecognize
                + ", keyboard=" + keyboardEnabled);

        // Keyboard — skip entirely when user has not enabled it.
        if (keyboardEnabled) {
            try {
                KeyboardUtils.setup(context);
                logInfo("Keyboard service initialized");
            } catch (Exception e) {
                logError("Failed to initialize keyboard service: " + e.getMessage());
            }
        } else {
            logInfo("Keyboard service skipped (disabled by user)");
        }

        // Stylus — skip entirely when both stylus mode and force recognize are off.
        if (stylusEnabled || forceRecognize) {
            try {
                PenUtils.setup(context);
                logInfo("Pen service initialized");
            } catch (Exception e) {
                logError("Failed to initialize pen service: " + e.getMessage());
            }
        } else {
            logInfo("Pen service skipped (stylus features disabled)");
        }
    }

    private static void logInfo(String message)  { Log.i(TAG, ts() + message); }
    private static void logError(String message) { Log.e(TAG, ts() + message); }

    private static String ts() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
