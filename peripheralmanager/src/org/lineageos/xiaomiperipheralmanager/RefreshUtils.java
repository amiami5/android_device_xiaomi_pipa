package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

public final class RefreshUtils {
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String KEY_USER_REFRESH_RATE = "user_refresh_rate";
    private static final String KEY_PEN_MODE = "pen_mode";
    private static final String PREF_FILE_NAME = "pen_refresh_prefs";

    private Context mContext;
    private SharedPreferences mSharedPrefs;

    protected RefreshUtils(Context context) {
        mContext = context;
        mSharedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    protected void setPenRefreshRate() {
        boolean penMode = mSharedPrefs.getBoolean(KEY_PEN_MODE, false);

        if (!penMode) {
            float maxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 144f);
            float minRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 144f);
            float userRate = Settings.System.getFloat(mContext.getContentResolver(), "user_refresh_rate", 0f);

            // Update default values in SharedPreferences
            mSharedPrefs.edit()
                    .putFloat(KEY_MIN_REFRESH_RATE, minRate)
                    .putFloat(KEY_PEAK_REFRESH_RATE, maxRate)
                    .putFloat(KEY_USER_REFRESH_RATE, userRate)
                    .putBoolean(KEY_PEN_MODE, true)
                    .apply();

            // Set fixed refresh rates for pen mode
            Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, 60f);
            Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, 120f);
            Settings.System.putFloat(mContext.getContentResolver(), "user_refresh_rate", 120f);
        }
    }

    protected void setDefaultRefreshRate() {
        float defaultMinRate = mSharedPrefs.getFloat(KEY_MIN_REFRESH_RATE, 144f);
        float defaultMaxRate = mSharedPrefs.getFloat(KEY_PEAK_REFRESH_RATE, 144f);
        float defaultUserRate = mSharedPrefs.getFloat(KEY_USER_REFRESH_RATE, 0f);

        mSharedPrefs.edit().putBoolean(KEY_PEN_MODE, false).apply();

        // Set the values in the Settings.System directly
        Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, defaultMinRate);
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, defaultMaxRate);
        Settings.System.putFloat(mContext.getContentResolver(), "user_refresh_rate", defaultUserRate);
    }
}
