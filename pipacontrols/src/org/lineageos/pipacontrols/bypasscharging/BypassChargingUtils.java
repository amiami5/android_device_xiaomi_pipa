/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.bypasscharging;

import android.util.Log;

import org.lineageos.pipacontrols.utils.FileUtils;

public final class BypassChargingUtils {

    private static final String TAG = "BypassChargingUtils";

    public static final String BYPASS_CHARGING_PATH =
            "/sys/class/power_supply/battery/input_suspend";

    public static final String KEY_BYPASS_CHARGING = "bypass_charging_enable";

    private BypassChargingUtils() {}

    public static boolean isSupported() {
        return FileUtils.fileExists(BYPASS_CHARGING_PATH);
    }

    public static boolean getCurrentHardwareState() {
        if (!isSupported()) return false;
        final String value = FileUtils.readOneLine(BYPASS_CHARGING_PATH);
        return value != null && "1".equals(value.trim());
    }

    public static void setEnabled(boolean enable) {
        if (!isSupported()) {
            Log.w(TAG, "setEnabled: sysfs node not found, ignoring");
            return;
        }
        final boolean ok = FileUtils.writeLine(BYPASS_CHARGING_PATH, enable ? "1" : "0");
        if (!ok) {
            Log.e(TAG, "setEnabled: failed to write to " + BYPASS_CHARGING_PATH);
        }
    }
}
