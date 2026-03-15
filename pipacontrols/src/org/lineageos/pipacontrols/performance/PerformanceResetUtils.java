/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.apppriority.AppPriorityUtils;
import org.lineageos.pipacontrols.performance.cpufreqcap.CpuFreqCapUtils;
import org.lineageos.pipacontrols.performance.cpugovernor.CpuGovernorUtils;
import org.lineageos.pipacontrols.performance.gpugovernor.GpuGovernorUtils;

public final class PerformanceResetUtils {

    private static final String TAG = "PerformanceReset";

    private static final String[] PERF_KEYS = {
        CpuGovernorUtils.PREF_KEY,
        CpuFreqCapUtils.PREF_KEY,
        GpuGovernorUtils.PREF_KEY,
        AppPriorityUtils.PREF_KEY,
    };

    private PerformanceResetUtils() {}

    public static void resetAll(Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();

        for (String key : PERF_KEYS) editor.remove(key);
        editor.apply();

        CpuGovernorUtils.apply(CpuGovernorUtils.GOV_SCHEDUTIL);
        CpuFreqCapUtils.apply(CpuFreqCapUtils.CAP_NONE);
        GpuGovernorUtils.apply(GpuGovernorUtils.GOV_ADRENO_TZ);
        AppPriorityUtils.apply(AppPriorityUtils.LEVEL_OFF);

        Log.i(TAG, "All performance settings reset to defaults");
    }
}
