/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance.cpugovernor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class CpuGovernorUtils {

    private static final String TAG = "CpuGovernor";

    public static final String PREF_KEY = "cpu_governor_key";

    public static final String GOV_SCHEDUTIL   = "schedutil";
    public static final String GOV_PERFORMANCE = "performance";
    public static final String GOV_POWERSAVE   = "powersave";

    // policy0 = LITTLE cores, policy4 = BIG cores, policy7 = PRIME core
    private static final String PATH_POLICY0 = "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor";
    private static final String PATH_POLICY4 = "/sys/devices/system/cpu/cpufreq/policy4/scaling_governor";
    private static final String PATH_POLICY7 = "/sys/devices/system/cpu/cpufreq/policy7/scaling_governor";

    private CpuGovernorUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH_POLICY0)
                && FileUtils.fileExists(PATH_POLICY4)
                && FileUtils.fileExists(PATH_POLICY7);
    }

    public static void apply(String governor) {
        FileUtils.writeLine(PATH_POLICY0, governor);
        FileUtils.writeLine(PATH_POLICY4, governor);
        FileUtils.writeLine(PATH_POLICY7, governor);
        Log.i(TAG, "Governor applied: " + governor);
    }

    // Restore saved governor on boot — sysfs does not persist across reboots
    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, GOV_SCHEDUTIL));
    }
}
