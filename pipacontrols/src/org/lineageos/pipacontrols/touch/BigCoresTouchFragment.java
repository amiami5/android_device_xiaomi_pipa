/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.touch;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.lineageos.pipacontrols.R;
import org.lineageos.pipacontrols.utils.KernelExecutor;

public class BigCoresTouchFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "BigCoresOnTouch";

    private SwitchPreference mPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.big_cores_touch);

        mPref = findPreference(BigCoresTouchUtils.PREF_KEY);
        if (mPref == null) return;

        if (!BigCoresTouchUtils.isSupported()) {
            mPref.setEnabled(false);
            mPref.setSummary(R.string.big_cores_touch_not_supported);
            return;
        }

        mPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        KernelExecutor.submit(() -> BigCoresTouchUtils.apply(enabled));
        Log.i(TAG, "Big cores on touch: " + enabled);
        return true;
    }
}
