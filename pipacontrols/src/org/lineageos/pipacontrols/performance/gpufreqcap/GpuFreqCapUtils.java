/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance.gpufreqcap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class GpuFreqCapUtils {

    private static final String TAG = "GpuFreqCap";

    public static final String PREF_KEY = "gpu_freq_cap_key";

    public static final String CAP_NONE    = "670000000";
    public static final String CAP_HIGH    = "587000000";
    public static final String CAP_MEDIUM  = "525000000";
    public static final String CAP_BALANCED = "490000000";
    public static final String CAP_LOW     = "400000000";
    public static final String CAP_MIN     = "305000000";

    private static final String PATH = "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq";

    private GpuFreqCapUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    public static void apply(String freq) {
        FileUtils.writeLine(PATH, freq);
        Log.i(TAG, "GPU freq cap applied: " + freq);
    }

    // Restore on boot — sysfs does not persist across reboots
    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, CAP_NONE));
    }
}
