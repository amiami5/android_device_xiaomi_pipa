/*
 * Copyright (C) 2025-2026 The LineageOS Project
 * Copyright (C) 2025-2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.stylus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.lineageos.pipacontrols.R;

public class StylusTileService extends TileService {

    private static final String TAG = "XiaomiStylusTile";

    static final String ACTION_STYLUS_CHANGED =
            "org.lineageos.pipacontrols.STYLUS_MODE_CHANGED";

    private static final String STYLUS_MODE_KEY         = "stylus_mode_key";
    private static final String STYLUS_REFRESH_RATE_KEY = "stylus_refresh_rate_key";

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mDelayedUpdateRunnable = () -> {
        boolean current = PreferenceManager
                .getDefaultSharedPreferences(StylusTileService.this)
                .getBoolean(STYLUS_MODE_KEY, false);
        Log.d(TAG, "Delayed retry — stylus=" + current);
        updateTile(current);
    };

    private final BroadcastReceiver mSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_STYLUS_CHANGED.equals(intent.getAction())) return;
            boolean active = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getBoolean(STYLUS_MODE_KEY, false);
            Log.d(TAG, "LocalBroadcast — stylus=" + active);
            updateTile(active);
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mSettingsReceiver, new IntentFilter(ACTION_STYLUS_CHANGED));

        boolean active = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(STYLUS_MODE_KEY, false);
        Log.d(TAG, "onStartListening — stylus=" + active
                + ", tile=" + (getQsTile() != null ? "ok" : "null"));

        updateTile(active);

        mHandler.removeCallbacks(mDelayedUpdateRunnable);
        mHandler.postDelayed(mDelayedUpdateRunnable, 350);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        mHandler.removeCallbacks(mDelayedUpdateRunnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSettingsReceiver);
    }

    @Override
    public void onClick() {
        final Tile tile = getQsTile();
        if (tile == null) {
            Log.w(TAG, "onClick — tile is null");
            return;
        }

        final boolean newState = (tile.getState() != Tile.STATE_ACTIVE);
        Log.d(TAG, "onClick — newState=" + newState);

        updateTile(newState);

        final String refreshRate = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(STYLUS_REFRESH_RATE_KEY, "dynamic");

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(STYLUS_MODE_KEY, newState)
                .apply();

        PenUtils.onStylusModeChanged(this, newState);
        if (newState) PenUtils.setRefreshRateMode(refreshRate);

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ACTION_STYLUS_CHANGED));
    }

    private void updateTile(boolean active) {
        final Tile tile = getQsTile();
        if (tile == null) {
            Log.w(TAG, "updateTile — tile is null");
            return;
        }

        final String rate = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(STYLUS_REFRESH_RATE_KEY, "dynamic");

        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(this,
                active ? R.drawable.ic_stylus_tile
                       : R.drawable.ic_stylus_tile_off));
        tile.setSubtitle(rateLabel(rate));
        tile.updateTile();
    }

    private static String rateLabel(String rate) {
        switch (rate) {
            case "60":  return "60 Hz";
            case "120": return "120 Hz";
            default:    return "Dynamic";
        }
    }
}
