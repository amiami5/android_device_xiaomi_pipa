/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance.cpufreqcap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class CpuFreqCapUtils {

    private static final String TAG = "CpuFreqCap";

    public static final String PREF_KEY = "cpu_freq_cap_key";

    public static final String CAP_NONE   = "none";
    public static final String CAP_HIGH   = "2457600";
    public static final String CAP_MEDIUM = "1632000";
    public static final String CAP_LOW    = "1305600";

    private static final String PATH = "/sys/module/msm_performance/parameters/cpu_max_freq";

    // All cores uncapped except cpu7 which gets the chosen cap
    private static final String FMT_NOCAP = "0:4294967295 1:4294967295 2:4294967295 3:4294967295 4:4294967295 5:4294967295 6:4294967295 7:4294967295";
    private static final String FMT       = "0:4294967295 1:4294967295 2:4294967295 3:4294967295 4:4294967295 5:4294967295 6:4294967295 7:%s";

    private CpuFreqCapUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    public static void apply(String cap) {
        String value = CAP_NONE.equals(cap) ? FMT_NOCAP : String.format(FMT, cap);
        FileUtils.writeLine(PATH, value);
        Log.i(TAG, "CPU freq cap applied: " + cap);
    }

    // Restore on boot — sysfs does not persist across reboots
    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, CAP_NONE));
    }
}
