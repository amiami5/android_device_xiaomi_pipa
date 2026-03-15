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

public final class BoostDurationUtils {

    private static final String TAG = "BoostDuration";

    public static final String PREF_KEY = "boost_duration_key";

    public static final String DURATION_40  = "40";
    public static final String DURATION_80  = "80";
    public static final String DURATION_120 = "120";
    public static final String DURATION_200 = "200";

    private static final String PATH = "/sys/devices/system/cpu/cpu_boost/input_boost_ms";

    private BoostDurationUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    // Pure write — no side effects on other prefs
    public static void apply(String durationMs) {
        FileUtils.writeLine(PATH, durationMs);
        Log.i(TAG, "Boost duration applied: " + durationMs + "ms");
    }

    // Only restore if Touch Boost is enabled — TouchBoostUtils.restore() handles the write
    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(TouchBoostUtils.PREF_KEY, true)) {
            apply(prefs.getString(PREF_KEY, DURATION_120));
        }
    }
}
