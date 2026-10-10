/*
 * Copyright (C) 2026 amisuke
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols;

import android.app.Application;
import android.util.Log;

import org.lineageos.pipacontrols.stylus.PenChargeMonitor;

public class PipaControlsApplication extends Application {

    private static final String TAG = "PipaControlsApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // the app is persistent, so this also runs when the process is restarted after a crash,
        // which LOCKED_BOOT_COMPLETED does not cover
        try { PenChargeMonitor.start(this); } catch (Exception e) {
            Log.e(TAG, "Pen charge monitor failed: " + e.getMessage()); }
    }
}
