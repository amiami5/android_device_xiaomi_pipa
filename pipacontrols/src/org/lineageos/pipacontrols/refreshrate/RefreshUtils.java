/*
 * Copyright (C) 2023-2026 The LineageOS Project
 * Copyright (C) 2025-2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.stylus;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class RefreshUtils {

    private static final String TAG = "XiaomiRefreshUtils";

    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE  = "min_refresh_rate";
    private static final String KEY_USER_REFRESH_RATE = "user_refresh_rate";
    private static final String KEY_PEN_MODE          = "pen_mode";
    private static final String PREF_FILE_NAME        = "pen_refresh_prefs";

    private static final float PEN_MIN_RATE = 60f;
    private static final float PEN_MAX_RATE = 120f;

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;
    private final Handler mHandler;

    private ContentObserver mPeakRateObserver;
    private boolean mObserverRegistered = false;
    private boolean mPenModeActive      = false;
    private float   mTargetMinRate      = PEN_MIN_RATE;
    private float   mTargetMaxRate      = PEN_MAX_RATE;
    private boolean mSelfWrite          = false;

    protected RefreshUtils(Context context) {
        mContext     = context.getApplicationContext();
        mSharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        mHandler     = new Handler(Looper.getMainLooper());
        logInfo("RefreshUtils initialized");
    }

    private void registerObserver() {
        if (mObserverRegistered) return;

        mPeakRateObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                if (!mPenModeActive || mSelfWrite) return;

                float current = Settings.System.getFloat(
                        mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, -1f);

                if (Math.abs(current - mTargetMaxRate) > 1f) {
                    logInfo("peak_refresh_rate overridden to " + current + "Hz, restoring " + mTargetMaxRate + "Hz");
                    applyRates(mTargetMinRate, mTargetMaxRate);
                }
            }
        };

        Uri peakRateUri = Settings.System.getUriFor(KEY_PEAK_REFRESH_RATE);
        mContext.getContentResolver().registerContentObserver(peakRateUri, false, mPeakRateObserver);
        mObserverRegistered = true;
        logInfo("ContentObserver registered on peak_refresh_rate");
    }

    private void unregisterObserver() {
        if (!mObserverRegistered || mPeakRateObserver == null) return;
        mContext.getContentResolver().unregisterContentObserver(mPeakRateObserver);
        mObserverRegistered = false;
        logInfo("ContentObserver unregistered");
    }

    private void applyRates(float minRate, float maxRate) {
        mSelfWrite = true;
        try {
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minRate);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxRate);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_USER_REFRESH_RATE, maxRate);
            logInfo("Applied refresh rates: min=" + minRate + " peak=" + maxRate);
        } finally {
            mHandler.postDelayed(() -> mSelfWrite = false, 300);
        }
    }

    protected void setPenRefreshRate() {
        setRefreshRateRange(PEN_MIN_RATE, PEN_MAX_RATE);
    }

    protected void setFixedRefreshRate(float fixedRate) {
        setRefreshRateRange(fixedRate, fixedRate);
    }

    private void setRefreshRateRange(float minRate, float maxRate) {
        mTargetMinRate = minRate;
        mTargetMaxRate = maxRate;

        if (!mSharedPrefs.getBoolean(KEY_PEN_MODE, false)) {
            float savedPeak = Settings.System.getFloat(
                    mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 144f);
            float savedMin  = Settings.System.getFloat(
                    mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 144f);
            float savedUser = Settings.System.getFloat(
                    mContext.getContentResolver(), KEY_USER_REFRESH_RATE, 0f);

            mSharedPrefs.edit()
                    .putFloat(KEY_PEAK_REFRESH_RATE, savedPeak)
                    .putFloat(KEY_MIN_REFRESH_RATE, savedMin)
                    .putFloat(KEY_USER_REFRESH_RATE, savedUser)
                    .putBoolean(KEY_PEN_MODE, true)
                    .apply();

            logInfo("Saved pre-pen rates: peak=" + savedPeak + " min=" + savedMin);
        }

        mPenModeActive = true;
        registerObserver();
        applyRates(minRate, maxRate);
    }

    protected void setDefaultRefreshRate() {
        mPenModeActive = false;
        unregisterObserver();

        float restorePeak = mSharedPrefs.getFloat(KEY_PEAK_REFRESH_RATE, 144f);
        float restoreMin  = mSharedPrefs.getFloat(KEY_MIN_REFRESH_RATE, 144f);
        float restoreUser = mSharedPrefs.getFloat(KEY_USER_REFRESH_RATE, 0f);

        mSharedPrefs.edit().putBoolean(KEY_PEN_MODE, false).apply();

        mSelfWrite = true;
        try {
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, restoreMin);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, restorePeak);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_USER_REFRESH_RATE, restoreUser);
            logInfo("Restored refresh rates: peak=" + restorePeak + " min=" + restoreMin);
        } finally {
            mHandler.postDelayed(() -> mSelfWrite = false, 300);
        }
    }

    protected void cleanup() {
        unregisterObserver();
    }

    private void logInfo(String message) {
        Log.i(TAG, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] " + message);
    }
}
