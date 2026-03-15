/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.apppriority;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class AppPriorityUtils {

    private static final String TAG = "AppPriority";

    public static final String PREF_KEY = "app_priority_key";

    private static final String PATH_TOP_APP    = "/dev/stune/top-app/schedtune.boost";
    private static final String PATH_FOREGROUND = "/dev/stune/foreground/schedtune.boost";

    public static final String LEVEL_OFF    = "off";
    public static final String LEVEL_LOW    = "low";
    public static final String LEVEL_MEDIUM = "medium";
    public static final String LEVEL_HIGH   = "high";

    private AppPriorityUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(PATH_TOP_APP)
                && FileUtils.fileExists(PATH_FOREGROUND);
    }

    public static void apply(String level) {
        int topApp;
        int foreground;

        switch (level) {
            case LEVEL_LOW:    topApp = 5;  foreground = 0; break;
            case LEVEL_MEDIUM: topApp = 10; foreground = 5; break;
            case LEVEL_HIGH:   topApp = 15; foreground = 5; break;
            default:           topApp = 0;  foreground = 0; break;
        }

        FileUtils.writeLine(PATH_TOP_APP,    String.valueOf(topApp));
        FileUtils.writeLine(PATH_FOREGROUND, String.valueOf(foreground));

        Log.i(TAG, "apply level=" + level + " top-app=" + topApp + " foreground=" + foreground);
    }

    // Restore saved value on boot — cgroup resets to 0 every reboot
    public static void restore(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        apply(prefs.getString(PREF_KEY, LEVEL_OFF));
    }
}
