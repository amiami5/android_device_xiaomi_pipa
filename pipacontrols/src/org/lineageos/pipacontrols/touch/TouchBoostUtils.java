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

public final class TouchBoostUtils {

    private static final String TAG = "TouchBoost";

    public static final String PREF_KEY = "touch_boost_key";

    private static final String PATH = "/sys/devices/system/cpu/cpu_boost/input_boost_ms";

    public static final int DEFAULT_BOOST_MS = 120;

    private TouchBoostUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    // When enabling, write the saved duration value; when disabling, write 0
    public static void apply(boolean enabled, Context context) {
        if (enabled) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String duration = prefs.getString(BoostDurationUtils.PREF_KEY,
                    String.valueOf(DEFAULT_BOOST_MS));
            FileUtils.writeLine(PATH, duration);
            Log.i(TAG, "Touch boost enabled, duration=" + duration);
        } else {
            FileUtils.writeLine(PATH, "0");
            Log.i(TAG, "Touch boost disabled");
        }
    }

    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getBoolean(PREF_KEY, true), context);
    }
}
