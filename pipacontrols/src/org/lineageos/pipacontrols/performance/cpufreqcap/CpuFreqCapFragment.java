/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.performance.cpufreqcap;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.pipacontrols.R;
import org.lineageos.pipacontrols.utils.KernelExecutor;

public class CpuFreqCapFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "CpuFreqCap";

    private ListPreference mCapPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.cpu_freq_cap);

        mCapPref = findPreference(CpuFreqCapUtils.PREF_KEY);
        if (mCapPref == null) return;

        if (!CpuFreqCapUtils.isSupported()) {
            mCapPref.setEnabled(false);
            mCapPref.setSummary(R.string.cpu_freq_cap_not_supported);
            return;
        }

        mCapPref.setOnPreferenceChangeListener(this);
        updateSummary(mCapPref.getValue());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String cap = (String) newValue;
        KernelExecutor.submit(() -> CpuFreqCapUtils.apply(cap));
        updateSummary(cap);
        Log.i(TAG, "CPU freq cap changed to: " + cap);
        return true;
    }

    private void updateSummary(String cap) {
        if (mCapPref == null || cap == null) return;
        int idx = mCapPref.findIndexOfValue(cap);
        if (idx >= 0) mCapPref.setSummary(mCapPref.getEntries()[idx]);
    }
}
