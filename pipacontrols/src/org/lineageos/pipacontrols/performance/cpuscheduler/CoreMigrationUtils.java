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

public final class CoreMigrationUtils {

    private static final String TAG = "CoreMigration";

    public static final String PREF_KEY = "core_migration_key";

    public static final String PRESET_CONSERVATIVE = "conservative";
    public static final String PRESET_DEFAULT      = "default";
    public static final String PRESET_AGGRESSIVE   = "aggressive";

    private static final String PATH_UPMIGRATE        = "/proc/sys/kernel/sched_upmigrate";
    private static final String PATH_DOWNMIGRATE      = "/proc/sys/kernel/sched_downmigrate";
    private static final String PATH_GROUP_UPMIGRATE   = "/proc/sys/kernel/sched_group_upmigrate";
    private static final String PATH_GROUP_DOWNMIGRATE = "/proc/sys/kernel/sched_group_downmigrate";

    private CoreMigrationUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH_UPMIGRATE)
                && FileUtils.fileExists(PATH_DOWNMIGRATE);
    }

    public static void apply(String preset) {
        switch (preset) {
            case PRESET_CONSERVATIVE:
                FileUtils.writeLine(PATH_UPMIGRATE,        "98 98");
                FileUtils.writeLine(PATH_DOWNMIGRATE,      "90 90");
                FileUtils.writeLine(PATH_GROUP_UPMIGRATE,  "120");
                FileUtils.writeLine(PATH_GROUP_DOWNMIGRATE,"100");
                break;
            case PRESET_AGGRESSIVE:
                FileUtils.writeLine(PATH_UPMIGRATE,        "85 85");
                FileUtils.writeLine(PATH_DOWNMIGRATE,      "75 75");
                FileUtils.writeLine(PATH_GROUP_UPMIGRATE,  "80");
                FileUtils.writeLine(PATH_GROUP_DOWNMIGRATE,"65");
                break;
            default: // default
                FileUtils.writeLine(PATH_UPMIGRATE,        "95 95");
                FileUtils.writeLine(PATH_DOWNMIGRATE,      "85 85");
                FileUtils.writeLine(PATH_GROUP_UPMIGRATE,  "100");
                FileUtils.writeLine(PATH_GROUP_DOWNMIGRATE,"85");
                break;
        }
        Log.i(TAG, "Core migration preset applied: " + preset);
    }

    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, PRESET_DEFAULT));
    }
}
