/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.touch;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.lineageos.pipacontrols.R;
import org.lineageos.pipacontrols.utils.KernelExecutor;

public class TouchBoostFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "TouchBoost";

    private SwitchPreference mPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.touch_boost);

        mPref = findPreference(TouchBoostUtils.PREF_KEY);
        if (mPref == null) return;

        if (!TouchBoostUtils.isSupported()) {
            mPref.setEnabled(false);
            mPref.setSummary(R.string.touch_boost_not_supported);
            return;
        }

        mPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Context appContext = requireContext().getApplicationContext();
        KernelExecutor.submit(() -> TouchBoostUtils.apply(enabled, appContext));
        Log.i(TAG, "Touch boost: " + enabled);
        return true;
    }
}
