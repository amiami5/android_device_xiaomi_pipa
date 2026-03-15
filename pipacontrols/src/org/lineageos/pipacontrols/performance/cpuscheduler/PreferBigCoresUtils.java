/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance.cpuscheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class PreferBigCoresUtils {

    private static final String TAG = "PreferBigCores";

    public static final String PREF_KEY = "prefer_big_cores_key";

    private static final String PATH = "/proc/sys/kernel/sched_boost";

    private PreferBigCoresUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    public static void apply(boolean enabled) {
        FileUtils.writeLine(PATH, enabled ? "1" : "0");
        Log.i(TAG, "Prefer big cores: " + enabled);
    }

    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getBoolean(PREF_KEY, false));
    }
}
