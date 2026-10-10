/*
 * Copyright (C) 2026 amisuke
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.stylus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import org.lineageos.pipacontrols.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Watches the pen's reverse wireless charging state exposed by the nu1665 driver and
 * - shows a persistent status bar notification while the pen is charging
 * - shows a heads-up notification when charging starts or the pen is attached but not charging
 * - publishes the precise pen battery level as METADATA_MAIN_BATTERY so that Settings can
 *   show it instead of the 10% steps reported over BLE.
 */
public class PenChargeMonitor {

    private static final String TAG = "PenChargeMonitor";

    private static final String WIRELESS = "/sys/class/power_supply/wireless/";
    private static final String FUDA     = "/sys/class/power_supply/fuda/";

    private static final String PATH_STATE = WIRELESS + "reverse_pen_chg_state";
    private static final String PATH_SOC   = WIRELESS + "reverse_pen_soc";
    private static final String PATH_HALL3 = FUDA + "reverse_chg_hall3";
    private static final String PATH_HALL4 = FUDA + "reverse_chg_hall4";

    // reverse_pen_chg_state
    private static final int STATE_CHARGING = 4;

    private static final String PEN_NAME = "Xiaomi Smart Pen";

    private static final String CHANNEL_STATUS = "pen_charge_status";
    private static final String CHANNEL_ALERT  = "pen_charge_alert";
    private static final String CHANNEL_START  = "pen_charge_start";
    private static final int ID_STATUS = 0x70656e01;
    private static final int ID_ALERT  = 0x70656e02;

    private static final long POLL_SCREEN_ON_MS  = 1000;
    private static final long POLL_SCREEN_OFF_MS = 5000;
    private static final long NOT_CHARGING_GRACE_MS   = 3000;
    private static final long DETACH_DEBOUNCE_MS      = 2000;
    private static final long CHARGE_LOST_DEBOUNCE_MS = 2000;
    private static final long BLE_LEVEL_RECHECK_MS    = 30000;

    // BLE battery service reports in 10% steps; use the precise value only while it agrees
    private static final int BLE_LEVEL_TOLERANCE = 10;

    private static PenChargeMonitor sInstance;

    private final Context mContext;
    private final NotificationManager mNm;
    private final PowerManager mPm;
    private final Handler mHandler;

    private BluetoothDevice mPen;
    private int mPublishedLevel = Integer.MIN_VALUE;
    private long mLastLevelCheck;

    private boolean mAttached;
    private long mDetachedSince = -1;
    private long mAttachedSince;
    private long mNotChargingSince = -1;
    private long mChargeLostSince = -1;
    private boolean mCharging;
    private boolean mStartAlertShown;
    private boolean mNotChargingAlertShown;
    private int mLastShownSoc = -1;
    private int mLastSoc = -1;

    public static synchronized void start(Context context) {
        if (sInstance != null) return;
        sInstance = new PenChargeMonitor(context.getApplicationContext());
        sInstance.mHandler.post(sInstance::poll);
    }

    private PenChargeMonitor(Context context) {
        mContext = context;
        mNm = context.getSystemService(NotificationManager.class);
        mPm = context.getSystemService(PowerManager.class);

        HandlerThread thread = new HandlerThread("pen-charge-monitor");
        thread.start();
        mHandler = new Handler(thread.getLooper());

        mNm.createNotificationChannel(new NotificationChannel(CHANNEL_STATUS,
                context.getString(R.string.pen_charge_channel_status),
                NotificationManager.IMPORTANCE_LOW));
        mNm.createNotificationChannel(new NotificationChannel(CHANNEL_ALERT,
                context.getString(R.string.pen_charge_channel_alert),
                NotificationManager.IMPORTANCE_HIGH));
        // heads-up only, the sound is reserved for the not-charging warning
        final NotificationChannel start = new NotificationChannel(CHANNEL_START,
                context.getString(R.string.pen_charge_channel_start),
                NotificationManager.IMPORTANCE_HIGH);
        start.setSound(null, null);
        start.enableVibration(false);
        mNm.createNotificationChannel(start);
    }

    private void poll() {
        try {
            evaluate();
        } catch (Exception e) {
            Log.e(TAG, "poll failed", e);
        }
        mHandler.postDelayed(this::poll,
                mPm.isInteractive() ? POLL_SCREEN_ON_MS : POLL_SCREEN_OFF_MS);
    }

    private void evaluate() {
        final long now = SystemClock.elapsedRealtime();
        final int state = readInt(PATH_STATE, -1);
        final int soc = readInt(PATH_SOC, -1);
        final int hall3 = readInt(PATH_HALL3, 1);
        final int hall4 = readInt(PATH_HALL4, 1);

        // hall gpio is low while the pen is magnetically attached
        final boolean attachedNow = hall3 == 0 || hall4 == 0;
        final boolean socValid = soc > 0 && soc <= 100;

        if (attachedNow) {
            mDetachedSince = -1;
            if (!mAttached) {
                mAttached = true;
                mAttachedSince = now;
                mStartAlertShown = false;
                mNotChargingAlertShown = false;
            }
        } else if (mAttached) {
            // the hall sensors chatter while the pen is being positioned
            if (mDetachedSince < 0) mDetachedSince = now;
            if (now - mDetachedSince >= DETACH_DEBOUNCE_MS) {
                onDetached();
            }
        }

        if (mAttached) {
            final boolean charging = state == STATE_CHARGING;
            if (charging) {
                mNotChargingSince = -1;
                mChargeLostSince = -1;
                if (!mCharging) {
                    mCharging = true;
                    mNm.cancel(ID_ALERT);
                    // the state chatters while the pen is being positioned, alert only once
                    if (!mStartAlertShown) {
                        mStartAlertShown = true;
                        showAlert(CHANNEL_START,
                                mContext.getString(R.string.pen_charge_started_title),
                                levelText(socValid ? soc : -1));
                    }
                }
                if (socValid && soc != mLastShownSoc) {
                    mLastShownSoc = soc;
                    showStatus(soc);
                } else if (!socValid && mLastShownSoc == -1) {
                    showStatus(-1);
                }
            } else {
                if (mCharging) {
                    if (mChargeLostSince < 0) mChargeLostSince = now;
                    if (now - mChargeLostSince >= CHARGE_LOST_DEBOUNCE_MS) {
                        mCharging = false;
                        mChargeLostSince = -1;
                        mNm.cancel(ID_STATUS);
                        mLastShownSoc = -1;
                    }
                }
                if (!mCharging) {
                    if (mNotChargingSince < 0) mNotChargingSince = now;
                    // the soc may be unreadable once charging stopped at full
                    final boolean full = (socValid ? soc : mLastSoc) >= 100;
                    if (!mNotChargingAlertShown && !full
                            && now - mNotChargingSince >= NOT_CHARGING_GRACE_MS) {
                        mNotChargingAlertShown = true;
                        showAlert(CHANNEL_ALERT,
                                mContext.getString(R.string.pen_charge_not_charging_title),
                                mContext.getString(R.string.pen_charge_not_charging_text));
                    }
                }
            }
        }

        if (socValid) mLastSoc = soc;
        updateBluetoothLevel(now, socValid ? soc : mLastSoc);
    }

    private void onDetached() {
        mAttached = false;
        mCharging = false;
        mDetachedSince = -1;
        mNotChargingSince = -1;
        mChargeLostSince = -1;
        mLastShownSoc = -1;
        mNm.cancel(ID_STATUS);
        mNm.cancel(ID_ALERT);
    }

    private String levelText(int soc) {
        return soc > 0 ? mContext.getString(R.string.pen_charge_level, soc)
                : mContext.getString(R.string.pen_charge_in_progress);
    }

    private void showStatus(int soc) {
        Notification n = new Notification.Builder(mContext, CHANNEL_STATUS)
                .setSmallIcon(R.drawable.ic_pen_charging)
                .setContentTitle(mContext.getString(R.string.pen_charge_status_title))
                .setContentText(levelText(soc))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .build();
        mNm.notify(ID_STATUS, n);
    }

    private void showAlert(String channel, String title, String text) {
        Notification n = new Notification.Builder(mContext, channel)
                .setSmallIcon(R.drawable.ic_pen_charging)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setTimeoutAfter(8000)
                .build();
        mNm.notify(ID_ALERT, n);
    }

    private void updateBluetoothLevel(long now, int soc) {
        // attached and charging: the value is live, otherwise only re-check occasionally
        final boolean live = mCharging;
        if (!live && now - mLastLevelCheck < BLE_LEVEL_RECHECK_MS) return;
        mLastLevelCheck = now;

        final BluetoothDevice pen = findPen();
        if (pen == null) return;

        int level = -1;
        if (soc > 0) {
            if (live) {
                level = soc;
            } else {
                // not charging: the kernel only holds the last value seen while attached,
                // so trust it only if it still matches what the pen reports over BLE
                final int ble = pen.getBatteryLevel();
                if (ble >= 0 && Math.abs(ble - soc) < BLE_LEVEL_TOLERANCE) level = soc;
            }
        }

        // the metadata is persisted by the bluetooth stack, start from what it already holds
        if (mPublishedLevel == Integer.MIN_VALUE) mPublishedLevel = readPublishedLevel(pen);
        if (level == mPublishedLevel) return;
        mPublishedLevel = level;
        try {
            final boolean ok = pen.setMetadata(BluetoothDevice.METADATA_MAIN_BATTERY,
                    Integer.toString(level).getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "main battery metadata -> " + level + " ok=" + ok);
        } catch (Exception e) {
            Log.e(TAG, "setMetadata failed", e);
            mPublishedLevel = Integer.MIN_VALUE;
        }
    }

    private static int readPublishedLevel(BluetoothDevice pen) {
        try {
            final byte[] value = pen.getMetadata(BluetoothDevice.METADATA_MAIN_BATTERY);
            if (value == null) return -1;
            return Integer.parseInt(new String(value, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private BluetoothDevice findPen() {
        if (mPen != null && mPen.getBondState() == BluetoothDevice.BOND_BONDED) return mPen;
        // unpaired or re-paired: look it up again and republish
        mPen = null;
        mPublishedLevel = Integer.MIN_VALUE;
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return null;
        final Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        if (bonded == null) return null;
        for (BluetoothDevice d : bonded) {
            if (PEN_NAME.equals(d.getName())) {
                mPen = d;
                break;
            }
        }
        return mPen;
    }

    private static int readInt(String path, int fallback) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            return Integer.parseInt(r.readLine().trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
