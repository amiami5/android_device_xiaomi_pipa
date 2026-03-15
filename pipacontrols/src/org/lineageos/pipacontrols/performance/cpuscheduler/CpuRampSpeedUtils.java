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

import java.io.File;

public final class CpuRampSpeedUtils {

    private static final String TAG = "CpuRampSpeed";

    public static final String PREF_KEY = "cpu_ramp_speed_key";

    public static final String PRESET_BATTERY    = "battery";
    public static final String PRESET_DEFAULT    = "default";
    public static final String PRESET_RESPONSIVE = "responsive";

    private static final String BASE = "/sys/devices/system/cpu/";

    private CpuRampSpeedUtils() {}

    // Check directory exists — not a specific file that only exists when schedutil is active
    public static boolean isSupported() {
        return new File(BASE + "cpu0/cpufreq/schedutil").exists();
    }

    public static void apply(String preset) {
        switch (preset) {
            case PRESET_BATTERY:
                writeCluster("cpu0", 1075200, 95, 0);
                writeCluster("cpu4", 1401600, 95, 0);
                writeCluster("cpu7", 1401600, 95, 0);
                break;
            case PRESET_RESPONSIVE:
                writeCluster("cpu0", 1401600, 70, 5000);
                writeCluster("cpu4", 1766400, 70, 5000);
                writeCluster("cpu7", 1862400, 70, 5000);
                break;
            default:
                writeCluster("cpu0", 1248000, 90, 0);
                writeCluster("cpu4", 1574400, 90, 0);
                writeCluster("cpu7", 1632000, 90, 0);
                break;
        }
        Log.i(TAG, "CPU ramp speed preset applied: " + preset);
    }

    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, PRESET_DEFAULT));
    }

    private static void writeCluster(String cpu, int hispeedFreq, int hispeedLoad, int downRateLimit) {
        String base = BASE + cpu + "/cpufreq/schedutil/";
        FileUtils.writeLine(base + "hispeed_freq",       String.valueOf(hispeedFreq));
        FileUtils.writeLine(base + "hispeed_load",       String.valueOf(hispeedLoad));
        FileUtils.writeLine(base + "down_rate_limit_us", String.valueOf(downRateLimit));
    }
}
