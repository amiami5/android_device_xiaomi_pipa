/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.stylus;

import android.view.KeyEvent;

public final class PenButtonConstants {

    public static final String PEN_KEYBOARD_DEVICE_NAME = "Xiaomi Smart Pen Keyboard";

    public static final int SCAN_CODE_UPPER = 104;
    public static final int SCAN_CODE_LOWER = 109;

    public static final String SETTING_UPPER_BUTTON = "pipa_stylus_button_upper";
    public static final String SETTING_LOWER_BUTTON = "pipa_stylus_button_lower";

    public static final String ACTION_NONE = "none";
    public static final String ACTION_PAGE_UP = "page_up";
    public static final String ACTION_PAGE_DOWN = "page_down";
    public static final String ACTION_VOLUME_UP = "volume_up";
    public static final String ACTION_VOLUME_DOWN = "volume_down";
    public static final String ACTION_MEDIA_PLAY_PAUSE = "media_play_pause";
    public static final String ACTION_MEDIA_NEXT = "media_next";
    public static final String ACTION_MEDIA_PREVIOUS = "media_previous";
    public static final String ACTION_BACK = "back";
    public static final String ACTION_HOME = "home";
    public static final String ACTION_SCREENSHOT = "screenshot";
    public static final String ACTION_APP_PREFIX = "app:";

    public static final String DEFAULT_UPPER_ACTION = ACTION_VOLUME_UP;
    public static final String DEFAULT_LOWER_ACTION = ACTION_VOLUME_DOWN;

    private PenButtonConstants() {}

    public static int getMappedKeyCode(String action) {
        if (action == null) {
            return KeyEvent.KEYCODE_UNKNOWN;
        }
        switch (action) {
            case ACTION_PAGE_UP:
                return KeyEvent.KEYCODE_PAGE_UP;
            case ACTION_PAGE_DOWN:
                return KeyEvent.KEYCODE_PAGE_DOWN;
            case ACTION_VOLUME_UP:
                return KeyEvent.KEYCODE_VOLUME_UP;
            case ACTION_VOLUME_DOWN:
                return KeyEvent.KEYCODE_VOLUME_DOWN;
            case ACTION_MEDIA_PLAY_PAUSE:
                return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
            case ACTION_MEDIA_NEXT:
                return KeyEvent.KEYCODE_MEDIA_NEXT;
            case ACTION_MEDIA_PREVIOUS:
                return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            case ACTION_BACK:
                return KeyEvent.KEYCODE_BACK;
            case ACTION_HOME:
                return KeyEvent.KEYCODE_HOME;
            default:
                return KeyEvent.KEYCODE_UNKNOWN;
        }
    }
}
