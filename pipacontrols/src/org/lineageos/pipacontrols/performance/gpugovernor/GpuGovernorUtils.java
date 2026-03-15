/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance.gpugovernor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class GpuGovernorUtils {

    private static final String TAG = "GpuGovernor";

    public static final String PREF_KEY = "gpu_governor_key";

    public static final String GOV_ADRENO_TZ    = "msm-adreno-tz";
    public static final String GOV_PERFORMANCE  = "performance";
    public static final String GOV_POWERSAVE    = "powersave";
    public static final String GOV_SIMPLE_OD    = "simple_ondemand";

    private static final String PATH = "/sys/class/kgsl/kgsl-3d0/devfreq/governor";

    private GpuGovernorUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH);
    }

    public static void apply(String governor) {
        FileUtils.writeLine(PATH, governor);
        Log.i(TAG, "GPU governor applied: " + governor);
    }

    // Restore on boot — sysfs does not persist across reboots
    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, GOV_ADRENO_TZ));
    }
}
