/*
 * Copyright (C) 2026 The LineageOS Project
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.pipacontrols.keyboard.KeyboardSettingsActivity;
import org.lineageos.pipacontrols.lid.LidSettingsActivity;
import org.lineageos.pipacontrols.stylus.StylusSettingsActivity;

public class PipaControlsFragment extends PreferenceFragmentCompat {

    private static final String TAG = "PipaControls";

    private static final String KEY_STYLUS   = "pipa_stylus";
    private static final String KEY_KEYBOARD = "pipa_keyboard";
    private static final String KEY_LID      = "pipa_lid";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pipa_controls);

        wireActivity(KEY_STYLUS,   StylusSettingsActivity.class);
        wireActivity(KEY_KEYBOARD, KeyboardSettingsActivity.class);
        wireActivity(KEY_LID,      LidSettingsActivity.class);

        Log.i(TAG, "Pipa Controls fragment created");
    }

    private void wireActivity(String key, Class<?> activityClass) {
        Preference pref = findPreference(key);
        if (pref == null) {
            Log.w(TAG, "Preference not found: " + key);
            return;
        }
        pref.setOnPreferenceClickListener(p -> {
            startActivity(new Intent(requireContext(), activityClass));
            return true;
        });
    }
}
