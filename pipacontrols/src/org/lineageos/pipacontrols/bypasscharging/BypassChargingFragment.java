/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.bypasscharging;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.pipacontrols.R;

public class BypassChargingFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "BypassChargingFragment";

    private MainSwitchPreference mSwitchBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.bypass_charging);

        mSwitchBar = findPreference(BypassChargingUtils.KEY_BYPASS_CHARGING);

        if (!BypassChargingUtils.isSupported()) {
            if (mSwitchBar != null) {
                mSwitchBar.setEnabled(false);
                mSwitchBar.setSummary(R.string.bypass_charging_not_supported);
            }
            return;
        }

        if (mSwitchBar != null) {
            mSwitchBar.setPersistent(false);
            mSwitchBar.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BypassChargingUtils.isSupported()) {
            syncUiWithHardware();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchBar && newValue instanceof Boolean) {
            final boolean enable = (Boolean) newValue;
            BypassChargingUtils.setEnabled(enable);
            updateSummary(enable);
            return true;
        }
        return false;
    }

    private void syncUiWithHardware() {
        if (mSwitchBar == null) return;
        final boolean active = BypassChargingUtils.getCurrentHardwareState();
        mSwitchBar.setOnPreferenceChangeListener(null);
        mSwitchBar.setChecked(active);
        mSwitchBar.setOnPreferenceChangeListener(this);
        updateSummary(active);
    }

    private void updateSummary(boolean active) {
        if (mSwitchBar == null) return;
        mSwitchBar.setSummary(active
                ? getString(R.string.bypass_charging_status_on)
                : getString(R.string.bypass_charging_status_off));
    }
}
