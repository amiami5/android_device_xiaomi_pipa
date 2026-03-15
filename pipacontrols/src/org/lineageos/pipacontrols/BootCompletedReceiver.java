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

import org.lineageos.pipacontrols.apppriority.AppPriorityUtils;
import org.lineageos.pipacontrols.keyboard.KeyboardUtils;
import org.lineageos.pipacontrols.stylus.PenUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "PipaControls";
    private static final long INIT_DELAY_MS = 3000;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) return;

        Log.i(TAG, ts() + "Boot completed — init in " + INIT_DELAY_MS + "ms");

        final Context appContext = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> new Thread(() -> initPeripherals(appContext), "pipa-boot-init").start(),
                INIT_DELAY_MS);
    }

    private static void initPeripherals(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean stylusEnabled   = prefs.getBoolean("stylus_mode_key", false);
        boolean forceRecognize  = prefs.getBoolean("force_recognize_stylus_key", false);
        boolean keyboardEnabled = prefs.getBoolean("keyboard_mode_key", false);

        if (keyboardEnabled) {
            try { KeyboardUtils.setup(context); } catch (Exception e) {
                Log.e(TAG, ts() + "Keyboard init failed: " + e.getMessage()); }
        }

        if (stylusEnabled || forceRecognize) {
            try { PenUtils.setup(context); } catch (Exception e) {
                Log.e(TAG, ts() + "Pen init failed: " + e.getMessage()); }
        }

        try { AppPriorityUtils.restore(context); } catch (Exception e) {
            Log.e(TAG, ts() + "App priority restore failed: " + e.getMessage()); }
    }

    private static String ts() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
