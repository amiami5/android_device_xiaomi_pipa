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
import org.lineageos.pipacontrols.performance.cpuscheduler.CoreMigrationUtils;
import org.lineageos.pipacontrols.performance.cpuscheduler.CpuRampSpeedUtils;
import org.lineageos.pipacontrols.performance.cpuscheduler.PreferBigCoresUtils;
import org.lineageos.pipacontrols.performance.gpufreqcap.GpuFreqCapUtils;
import org.lineageos.pipacontrols.performance.gpugovernor.GpuGovernorUtils;
import org.lineageos.pipacontrols.touch.BigCoresTouchUtils;
import org.lineageos.pipacontrols.touch.BoostDurationUtils;
import org.lineageos.pipacontrols.touch.TouchBoostUtils;

public final class PerformanceResetUtils {

    private static final String TAG = "PerformanceReset";

    private static final String[] PERF_KEYS = {
        CpuGovernorUtils.PREF_KEY,
        CpuFreqCapUtils.PREF_KEY,
        GpuGovernorUtils.PREF_KEY,
        GpuFreqCapUtils.PREF_KEY,
        PreferBigCoresUtils.PREF_KEY,
        CoreMigrationUtils.PREF_KEY,
        CpuRampSpeedUtils.PREF_KEY,
        AppPriorityUtils.PREF_KEY,
        TouchBoostUtils.PREF_KEY,
        BigCoresTouchUtils.PREF_KEY,
        BoostDurationUtils.PREF_KEY,
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
        GpuFreqCapUtils.apply(GpuFreqCapUtils.CAP_NONE);
        PreferBigCoresUtils.apply(false);
        CoreMigrationUtils.apply(CoreMigrationUtils.PRESET_DEFAULT);
        CpuRampSpeedUtils.apply(CpuRampSpeedUtils.PRESET_DEFAULT);
        AppPriorityUtils.apply(AppPriorityUtils.LEVEL_OFF);
        // Touch Boost default is on at 120ms — restore writes the default duration
        TouchBoostUtils.apply(true, context);
        BigCoresTouchUtils.apply(false);

        Log.i(TAG, "All performance and touch settings reset to defaults");
    }
}
