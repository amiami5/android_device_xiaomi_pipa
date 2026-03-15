/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.touch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class BigCoresTouchUtils {

    private static final String TAG = "BigCoresOnTouch";

    public static final String PREF_KEY = "big_cores_touch_key";

    private static final String PATH = "/sys/devices/system/cpu/cpu_boost/sched_boost_on_input";

    private BigCoresTouchUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    public static void apply(boolean enabled) {
        FileUtils.writeLine(PATH, enabled ? "1" : "0");
        Log.i(TAG, "Big cores on touch: " + enabled);
    }

    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getBoolean(PREF_KEY, false));
    }
}
