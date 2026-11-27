/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.stylus;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerGlobal;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ScreenshotHelper;

public class PenKeyHandler implements DeviceKeyHandler {

    private static final String TAG = "PenKeyHandler";

    private final Context mContext;
    private final Handler mHandler;
    private final PowerManager mPowerManager;
    private final ScreenshotHelper mScreenshotHelper;

    private String mUpperAction = PenButtonConstants.DEFAULT_UPPER_ACTION;
    private String mLowerAction = PenButtonConstants.DEFAULT_LOWER_ACTION;

    public PenKeyHandler(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mPowerManager = context.getSystemService(PowerManager.class);
        mScreenshotHelper = new ScreenshotHelper(context);

        loadSettings();

        ContentResolver resolver = mContext.getContentResolver();
        ContentObserver observer = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                loadSettings();
            }
        };
        resolver.registerContentObserver(
                Settings.System.getUriFor(PenButtonConstants.SETTING_UPPER_BUTTON),
                false, observer, UserHandle.USER_ALL);
        resolver.registerContentObserver(
                Settings.System.getUriFor(PenButtonConstants.SETTING_LOWER_BUTTON),
                false, observer, UserHandle.USER_ALL);
    }

    private void loadSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        String upper = Settings.System.getStringForUser(
                resolver, PenButtonConstants.SETTING_UPPER_BUTTON, UserHandle.USER_CURRENT);
        String lower = Settings.System.getStringForUser(
                resolver, PenButtonConstants.SETTING_LOWER_BUTTON, UserHandle.USER_CURRENT);
        mUpperAction = TextUtils.isEmpty(upper)
                ? PenButtonConstants.DEFAULT_UPPER_ACTION : upper;
        mLowerAction = TextUtils.isEmpty(lower)
                ? PenButtonConstants.DEFAULT_LOWER_ACTION : lower;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        final int scanCode = event.getScanCode();
        final int keyCode = event.getKeyCode();

        final boolean isUpper = scanCode == PenButtonConstants.SCAN_CODE_UPPER
                && keyCode == KeyEvent.KEYCODE_PAGE_UP;
        final boolean isLower = scanCode == PenButtonConstants.SCAN_CODE_LOWER
                && keyCode == KeyEvent.KEYCODE_PAGE_DOWN;
        if (!isUpper && !isLower) {
            return event;
        }

        final InputDevice device = event.getDevice();
        if (device == null
                || !PenButtonConstants.PEN_KEYBOARD_DEVICE_NAME.equals(device.getName())) {
            return event;
        }

        loadSettings();
        final String action = isUpper ? mUpperAction : mLowerAction;

        if (PenButtonConstants.ACTION_NONE.equals(action)) {
            return null;
        }

        // Pass through original key event if mapped to its native keycode
        if ((isUpper && PenButtonConstants.ACTION_PAGE_UP.equals(action))
                || (isLower && PenButtonConstants.ACTION_PAGE_DOWN.equals(action))) {
            return event;
        }

        if (PenButtonConstants.ACTION_SCREENSHOT.equals(action)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0
                    && isInteractive()) {
                mHandler.post(() -> mScreenshotHelper.takeScreenshot(
                        WindowManager.ScreenshotSource.SCREENSHOT_KEY_OTHER, mHandler, null));
            }
            return null;
        }

        if (action.startsWith(PenButtonConstants.ACTION_APP_PREFIX)) {
            if (event.getAction() == KeyEvent.ACTION_UP
                    && (event.getFlags() & KeyEvent.FLAG_CANCELED) == 0
                    && isInteractive()) {
                String pkg = action.substring(PenButtonConstants.ACTION_APP_PREFIX.length());
                if (!TextUtils.isEmpty(pkg)) {
                    mHandler.post(() -> launchApp(pkg));
                }
            }
            return null;
        }

        final int mappedKeyCode = PenButtonConstants.getMappedKeyCode(action);
        if (mappedKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            final KeyEvent mappedEvent = new KeyEvent(
                    event.getDownTime(),
                    event.getEventTime(),
                    event.getAction(),
                    mappedKeyCode,
                    event.getRepeatCount(),
                    event.getMetaState(),
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    event.getFlags() | KeyEvent.FLAG_FROM_SYSTEM,
                    InputDevice.SOURCE_KEYBOARD);
            mHandler.post(() -> InputManagerGlobal.getInstance().injectInputEvent(
                    mappedEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC));
            return null;
        }

        return event;
    }

    private boolean isInteractive() {
        return mPowerManager == null || mPowerManager.isInteractive();
    }

    private void launchApp(String packageName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent == null) {
                Log.w(TAG, "No launch intent for package: " + packageName);
                return;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch app: " + packageName, e);
        }
    }
}
