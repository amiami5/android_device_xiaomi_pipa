/*
 * Copyright (C) 2023-2026 The LineageOS Project
 * Copyright (C) 2025-2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.stylus;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import android.view.InputDevice;

import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PenUtils {

    private static final String TAG   = "XiaomiPenUtils";
    private static final boolean DEBUG = SystemProperties.getBoolean(
            "persist.xiaomi.peripherals.debug", false);

    private static final int PEN_VENDOR_ID  = 6421;
    private static final int PEN_PRODUCT_ID = 19841;

    private static final String STYLUS_MODE_KEY         = "stylus_mode_key";
    private static final String FORCE_STYLUS_KEY        = "force_recognize_stylus_key";
    private static final String STYLUS_REFRESH_RATE_KEY = "stylus_refresh_rate_key";

    private static InputManager      mInputManager;
    private static SharedPreferences mPreferences;
    private static RefreshUtils      mRefreshUtils;
    private static Handler           mHandler;
    private static Context           mContext;

    private static boolean mPenModeEnabled     = false;
    private static boolean mIsPenConnected     = false;
    private static boolean mListenerRegistered = false;
    private static boolean mIsSetup            = false;
    private static String  mCurrentRefreshMode = "dynamic";

    public static boolean isSetup() {
        return mIsSetup;
    }

    public static void setup(Context context) {
        mContext      = context.getApplicationContext();
        mInputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
        mPreferences  = PreferenceManager.getDefaultSharedPreferences(mContext);
        mRefreshUtils = new RefreshUtils(mContext);
        mHandler      = new Handler(Looper.getMainLooper());
        mIsSetup      = true;

        mCurrentRefreshMode = mPreferences.getString(STYLUS_REFRESH_RATE_KEY, "dynamic");

        boolean stylusEnabled  = mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        boolean forceRecognize = mPreferences.getBoolean(FORCE_STYLUS_KEY, false);

        logInfo("Setup — stylus=" + stylusEnabled + ", force=" + forceRecognize
                + ", rate=" + mCurrentRefreshMode);

        if (stylusEnabled || forceRecognize) {
            registerInputDeviceListener();
        }

        refreshPenMode();
    }

    private static void registerInputDeviceListener() {
        if (mListenerRegistered || mInputManager == null) return;
        mInputManager.registerInputDeviceListener(mInputDeviceListener, mHandler);
        mListenerRegistered = true;
        logInfo("InputDeviceListener registered");
    }

    private static void unregisterInputDeviceListener() {
        if (!mListenerRegistered || mInputManager == null) return;
        mInputManager.unregisterInputDeviceListener(mInputDeviceListener);
        mListenerRegistered = false;
        logInfo("InputDeviceListener unregistered");
    }

    private static void updateListenerState() {
        if (mPreferences == null) return;
        boolean stylusOn = mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        boolean forceOn  = mPreferences.getBoolean(FORCE_STYLUS_KEY, false);
        if (stylusOn || forceOn) {
            registerInputDeviceListener();
        } else {
            unregisterInputDeviceListener();
        }
    }

    public static void enablePenMode() {
        if (mPenModeEnabled) {
            logDebug("Pen mode already enabled");
            return;
        }
        logInfo("Enabling pen mode");
        mPenModeEnabled = true;
        SystemProperties.set("persist.vendor.parts.pen", "18");

        boolean stylusModeEnabled = mPreferences != null
                && mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        if (mRefreshUtils != null && stylusModeEnabled) {
            applyRefreshRateMode(mCurrentRefreshMode);
        }
    }

    public static void disablePenMode() {
        if (!mPenModeEnabled) {
            logDebug("Pen mode already disabled");
            return;
        }
        logInfo("Disabling pen mode");
        mPenModeEnabled = false;
        SystemProperties.set("persist.vendor.parts.pen", "2");
        if (mRefreshUtils != null) {
            mRefreshUtils.setDefaultRefreshRate();
        }
    }

    private static void refreshPenMode() {
        if (mPreferences == null || mInputManager == null) {
            logError("refreshPenMode called before setup");
            return;
        }
        boolean forceRecognize    = mPreferences.getBoolean(FORCE_STYLUS_KEY, false);
        boolean stylusModeEnabled = mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        boolean penDetected       = false;

        for (int id : mInputManager.getInputDeviceIds()) {
            if (isDeviceXiaomiPen(id)) {
                penDetected = true;
                logDebug("Xiaomi pen detected (device ID: " + id + ")");
                break;
            }
        }

        mIsPenConnected = penDetected;

        boolean shouldEnablePen = stylusModeEnabled && (penDetected || forceRecognize);

        logInfo("refreshPenMode — stylus=" + stylusModeEnabled
                + ", pen=" + penDetected + ", force=" + forceRecognize
                + " → enable=" + shouldEnablePen);

        if (shouldEnablePen) {
            enablePenMode();
        } else {
            disablePenMode();
        }
    }

    private static void applyRefreshRateMode(String mode) {
        if (mRefreshUtils == null) {
            logError("RefreshUtils not initialized");
            return;
        }
        mCurrentRefreshMode = mode;
        switch (mode) {
            case "60":
                mRefreshUtils.setFixedRefreshRate(60f);
                logInfo("Applied fixed 60Hz");
                break;
            case "120":
                mRefreshUtils.setFixedRefreshRate(120f);
                logInfo("Applied fixed 120Hz");
                break;
            default:
                mRefreshUtils.setPenRefreshRate();
                logInfo("Applied dynamic 60-120Hz");
                break;
        }
    }

    public static void setRefreshRateMode(String mode) {
        logInfo("Refresh rate mode → " + mode);
        mCurrentRefreshMode = mode;
        boolean stylusModeEnabled = mPreferences != null
                && mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        if (mPenModeEnabled && mRefreshUtils != null && stylusModeEnabled) {
            applyRefreshRateMode(mode);
        }
    }

    public static void onStylusModeChanged(Context context, boolean enabled) {
        if (!mIsSetup) setup(context);
        updateListenerState();
        refreshPenMode();
    }

    public static void onForceRecognizeChanged(Context context, boolean enabled) {
        if (!mIsSetup) setup(context);
        updateListenerState();
        boolean stylusModeEnabled = mPreferences != null
                && mPreferences.getBoolean(STYLUS_MODE_KEY, false);
        if (stylusModeEnabled) {
            refreshPenMode();
        } else {
            logInfo("Stylus Mode is off — force recognize change has no pen effect");
        }
    }

    private static final InputDeviceListener mInputDeviceListener = new InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int id) {
            logDebug("Device added: " + id);
            if (isDeviceXiaomiPen(id)) {
                logInfo("Xiaomi pen connected");
                mHandler.postDelayed(() -> refreshPenMode(), 100);
            }
        }

        @Override
        public void onInputDeviceRemoved(int id) {
            logDebug("Device removed: " + id);
            mHandler.postDelayed(() -> refreshPenMode(), 100);
        }

        @Override
        public void onInputDeviceChanged(int id) {
            logDebug("Device changed: " + id);
            if (isDeviceXiaomiPen(id)) {
                logInfo("Xiaomi pen changed");
                mHandler.postDelayed(() -> refreshPenMode(), 100);
            }
        }
    };

    private static boolean isDeviceXiaomiPen(int id) {
        try {
            InputDevice device = mInputManager.getInputDevice(id);
            if (device == null) return false;
            boolean isPen = device.getVendorId() == PEN_VENDOR_ID
                    && device.getProductId() == PEN_PRODUCT_ID;
            if (isPen) logDebug("Found Xiaomi pen: " + device.getName());
            return isPen;
        } catch (Exception e) {
            logError("Error checking device: " + e.getMessage());
            return false;
        }
    }

    public static boolean isPenConnected()        { return mIsPenConnected; }
    public static boolean isPenModeEnabled()      { return mPenModeEnabled; }
    public static String  getCurrentRefreshMode() { return mCurrentRefreshMode; }

    public static void cleanup() {
        unregisterInputDeviceListener();
        if (mRefreshUtils != null) mRefreshUtils.cleanup();
        mIsSetup = false;
        logInfo("PenUtils cleaned up");
    }

    private static void logDebug(String message) {
        if (DEBUG) Log.d(TAG, ts() + message);
    }
    private static void logInfo(String message)  { Log.i(TAG, ts() + message); }
    private static void logError(String message) { Log.e(TAG, ts() + message); }

    private static String ts() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
