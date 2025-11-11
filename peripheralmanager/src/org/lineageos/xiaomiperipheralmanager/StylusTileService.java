/*
 * Copyright (C) 2025 The LineageOS Project
 * Copyright (C) 2025 SheoranPranshu
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomiperipheralmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StylusTileService extends TileService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "XiaomiStylusTile";
    private static final String STYLUS_MODE_KEY = "stylus_mode_key";
    private static final String STYLUS_REFRESH_RATE_KEY = "stylus_refresh_rate_key";
    private static final String FORCE_RECOGNIZE_STYLUS_KEY = "force_recognize_stylus_key";
    
    private SharedPreferences mPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        logInfo("Stylus tile service created");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        updateTile();
        logDebug("Tile listening started");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        logDebug("Tile listening stopped");
    }

    @Override
    public void onClick() {
        boolean currentState = mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        boolean newState = !currentState;
        
        mPreferences.edit().putBoolean(STYLUS_MODE_KEY, newState).apply();
        
        PenUtils.onStylusModeChanged(newState);
        
        if (newState) {
            String refreshRate = mPreferences.getString(STYLUS_REFRESH_RATE_KEY, "dynamic");
            PenUtils.setRefreshRateMode(refreshRate);
        }
        
        logInfo("Stylus mode toggled to: " + newState);
        updateTile();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (STYLUS_MODE_KEY.equals(key) || STYLUS_REFRESH_RATE_KEY.equals(key) || 
            FORCE_RECOGNIZE_STYLUS_KEY.equals(key)) {
            updateTile();
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isEnabled = mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        String refreshRate = mPreferences.getString(STYLUS_REFRESH_RATE_KEY, "dynamic");
        
        tile.setLabel(getString(R.string.stylus_title));
        
        String subtitle;
        switch (refreshRate) {
            case "60":
                subtitle = "60Hz";
                break;
            case "120":
                subtitle = "120Hz";
                break;
            case "dynamic":
            default:
                subtitle = "Dynamic";
                break;
        }
        
        if (isEnabled) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setSubtitle(subtitle);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(subtitle);
        }
        
        tile.updateTile();
        logDebug("Tile updated - State: " + (isEnabled ? "Active" : "Inactive") + ", Mode: " + subtitle);
    }

    private void logDebug(String message) {
        Log.d(TAG, getTimestamp() + message);
    }
    
    private void logInfo(String message) {
        Log.i(TAG, getTimestamp() + message);
    }
    
    private String getTimestamp() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
