/*
 * Copyright (C) 2023-2026 The LineageOS Project
 * Copyright (C) 2025-2026 nullpointer1101
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.pipacontrols.stylus;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.pipacontrols.R;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StylusSettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "XiaomiStylusSettings";

    private static final String STYLUS_MODE_KEY         = "stylus_mode_key";
    private static final String FORCE_RECOGNIZE_KEY     = "force_recognize_stylus_key";
    private static final String FOOTER_KEY              = "footer_key";
    private static final String STYLUS_REFRESH_RATE_KEY = "stylus_refresh_rate_key";
    private static final String BUTTON_UPPER_KEY        = "stylus_button_upper_key";
    private static final String BUTTON_LOWER_KEY        = "stylus_button_lower_key";

    private static final String ACTION_LAUNCH_APP_MENU  = "__launch_app__";

    private static final String RATE_DYNAMIC = "dynamic";
    private static final String RATE_60      = "60";
    private static final String RATE_120     = "120";

    private SharedPreferences    mPrefs;
    private MainSwitchPreference mStylusModePref;
    private SwitchPreference     mForceRecognizePref;
    private ListPreference       mRefreshRatePref;
    private Preference           mUpperButtonPref;
    private Preference           mLowerButtonPref;
    private FooterPreference     mFooterPref;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private String mHandlingKey = null;

    private final BroadcastReceiver mTileChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Tile-change broadcast — refreshing UI");
            mHandler.post(() -> refreshUI());
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.stylus_settings);

        mPrefs              = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mStylusModePref     = findPreference(STYLUS_MODE_KEY);
        mForceRecognizePref = findPreference(FORCE_RECOGNIZE_KEY);
        mRefreshRatePref    = findPreference(STYLUS_REFRESH_RATE_KEY);
        mUpperButtonPref    = findPreference(BUTTON_UPPER_KEY);
        mLowerButtonPref    = findPreference(BUTTON_LOWER_KEY);
        mFooterPref         = findPreference(FOOTER_KEY);

        if (mUpperButtonPref != null) {
            mUpperButtonPref.setOnPreferenceClickListener(pref -> {
                showButtonActionDialog(
                        PenButtonConstants.SETTING_UPPER_BUTTON,
                        PenButtonConstants.DEFAULT_UPPER_ACTION,
                        mUpperButtonPref);
                return true;
            });
        }
        if (mLowerButtonPref != null) {
            mLowerButtonPref.setOnPreferenceClickListener(pref -> {
                showButtonActionDialog(
                        PenButtonConstants.SETTING_LOWER_BUTTON,
                        PenButtonConstants.DEFAULT_LOWER_ACTION,
                        mLowerButtonPref);
                return true;
            });
        }

        refreshUI();
        logInfo("Stylus settings created");
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                mTileChangeReceiver,
                new IntentFilter(StylusTileService.ACTION_STYLUS_CHANGED));
        refreshUI();
        logInfo("Stylus settings resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        try {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(mTileChangeReceiver);
        } catch (IllegalArgumentException ignored) {}
        logInfo("Stylus settings paused");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key != null && key.equals(mHandlingKey)) {
            logDebug("Skipping guarded callback for key: " + key);
            return;
        }

        mHandlingKey = key;
        try {
            if (STYLUS_MODE_KEY.equals(key)) {
                boolean enabled = prefs.getBoolean(key, false);
                logInfo("Stylus mode → " + enabled);
                PenUtils.onStylusModeChanged(requireContext(), enabled);
                if (enabled) {
                    PenUtils.setRefreshRateMode(prefs.getString(STYLUS_REFRESH_RATE_KEY, RATE_DYNAMIC));
                }
                notifyTile();

            } else if (FORCE_RECOGNIZE_KEY.equals(key)) {
                boolean enabled = prefs.getBoolean(key, false);
                logInfo("Force recognize → " + enabled);
                PenUtils.onForceRecognizeChanged(requireContext(), enabled);
                notifyTile();

            } else if (STYLUS_REFRESH_RATE_KEY.equals(key)) {
                String rate = prefs.getString(key, RATE_DYNAMIC);
                logInfo("Refresh rate → " + rate);
                if (prefs.getBoolean(STYLUS_MODE_KEY, false)) {
                    PenUtils.setRefreshRateMode(rate);
                }
                notifyTile();
            }

            refreshUI();

        } finally {
            mHandler.post(() -> {
                if (key != null && key.equals(mHandlingKey)) mHandlingKey = null;
            });
        }
    }

    private void notifyTile() {
        LocalBroadcastManager.getInstance(requireContext())
                .sendBroadcast(new Intent(StylusTileService.ACTION_STYLUS_CHANGED));
        TileService.requestListeningState(requireContext(),
                new ComponentName(requireContext(), StylusTileService.class));
    }

    private void refreshUI() {
        final boolean stylusMode  = mPrefs.getBoolean(STYLUS_MODE_KEY, false);
        final boolean forceRecog  = mPrefs.getBoolean(FORCE_RECOGNIZE_KEY, false);
        final String  refreshRate = mPrefs.getString(STYLUS_REFRESH_RATE_KEY, RATE_DYNAMIC);

        if (mStylusModePref != null && mStylusModePref.isChecked() != stylusMode) {
            mStylusModePref.setChecked(stylusMode);
        }
        if (mForceRecognizePref != null && mForceRecognizePref.isChecked() != forceRecog) {
            mForceRecognizePref.setChecked(forceRecog);
        }
        if (mRefreshRatePref != null) {
            String current = mRefreshRatePref.getValue();
            if (current == null || !current.equals(refreshRate)) {
                mRefreshRatePref.setValue(refreshRate);
            }
        }

        updateButtonSummary(
                mUpperButtonPref,
                PenButtonConstants.SETTING_UPPER_BUTTON,
                PenButtonConstants.DEFAULT_UPPER_ACTION);
        updateButtonSummary(
                mLowerButtonPref,
                PenButtonConstants.SETTING_LOWER_BUTTON,
                PenButtonConstants.DEFAULT_LOWER_ACTION);

        updateFooter(stylusMode, forceRecog, refreshRate);
    }

    private void updateButtonSummary(Preference pref, String settingKey, String defaultAction) {
        if (pref == null) {
            return;
        }
        String action = Settings.System.getStringForUser(
                requireContext().getContentResolver(),
                settingKey,
                UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(action)) {
            action = defaultAction;
        }
        pref.setSummary(getActionSummary(action));
    }

    private String getActionSummary(String action) {
        if (action.startsWith(PenButtonConstants.ACTION_APP_PREFIX)) {
            String pkg = action.substring(PenButtonConstants.ACTION_APP_PREFIX.length());
            String appLabel = pkg;
            try {
                PackageManager pm = requireContext().getPackageManager();
                appLabel = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            return getString(R.string.stylus_button_action_launch_app_summary, appLabel);
        }
        switch (action) {
            case PenButtonConstants.ACTION_NONE:
                return getString(R.string.stylus_button_action_none);
            case PenButtonConstants.ACTION_PAGE_UP:
                return getString(R.string.stylus_button_action_page_up);
            case PenButtonConstants.ACTION_PAGE_DOWN:
                return getString(R.string.stylus_button_action_page_down);
            case PenButtonConstants.ACTION_VOLUME_UP:
                return getString(R.string.stylus_button_action_volume_up);
            case PenButtonConstants.ACTION_VOLUME_DOWN:
                return getString(R.string.stylus_button_action_volume_down);
            case PenButtonConstants.ACTION_MEDIA_PLAY_PAUSE:
                return getString(R.string.stylus_button_action_media_play_pause);
            case PenButtonConstants.ACTION_MEDIA_NEXT:
                return getString(R.string.stylus_button_action_media_next);
            case PenButtonConstants.ACTION_MEDIA_PREVIOUS:
                return getString(R.string.stylus_button_action_media_previous);
            case PenButtonConstants.ACTION_BACK:
                return getString(R.string.stylus_button_action_back);
            case PenButtonConstants.ACTION_HOME:
                return getString(R.string.stylus_button_action_home);
            case PenButtonConstants.ACTION_SCREENSHOT:
                return getString(R.string.stylus_button_action_screenshot);
            default:
                return action;
        }
    }

    private void showButtonActionDialog(
            String settingKey, String defaultAction, Preference targetPref) {
        final Context context = requireContext();
        final String[] actionValues = new String[] {
            PenButtonConstants.ACTION_NONE,
            PenButtonConstants.ACTION_PAGE_UP,
            PenButtonConstants.ACTION_PAGE_DOWN,
            PenButtonConstants.ACTION_VOLUME_UP,
            PenButtonConstants.ACTION_VOLUME_DOWN,
            PenButtonConstants.ACTION_MEDIA_PLAY_PAUSE,
            PenButtonConstants.ACTION_MEDIA_NEXT,
            PenButtonConstants.ACTION_MEDIA_PREVIOUS,
            PenButtonConstants.ACTION_BACK,
            PenButtonConstants.ACTION_HOME,
            PenButtonConstants.ACTION_SCREENSHOT,
            ACTION_LAUNCH_APP_MENU,
        };
        final String[] actionLabels = new String[] {
            context.getString(R.string.stylus_button_action_none),
            context.getString(R.string.stylus_button_action_page_up),
            context.getString(R.string.stylus_button_action_page_down),
            context.getString(R.string.stylus_button_action_volume_up),
            context.getString(R.string.stylus_button_action_volume_down),
            context.getString(R.string.stylus_button_action_media_play_pause),
            context.getString(R.string.stylus_button_action_media_next),
            context.getString(R.string.stylus_button_action_media_previous),
            context.getString(R.string.stylus_button_action_back),
            context.getString(R.string.stylus_button_action_home),
            context.getString(R.string.stylus_button_action_screenshot),
            context.getString(R.string.stylus_button_action_launch_app),
        };

        String current = Settings.System.getStringForUser(
                context.getContentResolver(), settingKey, UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(current)) {
            current = defaultAction;
        }

        int checkedItem = -1;
        if (current.startsWith(PenButtonConstants.ACTION_APP_PREFIX)) {
            checkedItem = actionValues.length - 1;
        } else {
            for (int i = 0; i < actionValues.length; i++) {
                if (actionValues[i].equals(current)) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.stylus_button_action_dialog_title)
                .setSingleChoiceItems(actionLabels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    String chosen = actionValues[which];
                    if (ACTION_LAUNCH_APP_MENU.equals(chosen)) {
                        showAppPickerDialog(settingKey, defaultAction, targetPref);
                    } else {
                        saveButtonAction(settingKey, defaultAction, chosen, targetPref);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAppPickerDialog(
            String settingKey, String defaultAction, Preference targetPref) {
        Context context = requireContext();
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        List<AppEntry> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo info : resolveInfos) {
            String pkg = info.activityInfo.packageName;
            if (!seenPackages.add(pkg)) {
                continue;
            }
            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            apps.add(new AppEntry(pkg, label, icon));
        }

        Collator collator = Collator.getInstance();
        apps.sort(Comparator.comparing((AppEntry a) -> a.label, collator));

        AppListAdapter adapter = new AppListAdapter(context, apps);
        new AlertDialog.Builder(context)
                .setTitle(R.string.stylus_button_select_app_title)
                .setAdapter(adapter, (dialog, which) -> {
                    AppEntry selected = apps.get(which);
                    saveButtonAction(
                            settingKey,
                            defaultAction,
                            PenButtonConstants.ACTION_APP_PREFIX + selected.packageName,
                            targetPref);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveButtonAction(
            String settingKey, String defaultAction, String action, Preference targetPref) {
        ContentResolver resolver = requireContext().getContentResolver();
        Settings.System.putStringForUser(
                resolver, settingKey, action, UserHandle.USER_CURRENT);
        updateButtonSummary(targetPref, settingKey, defaultAction);
    }

    private static class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;

        AppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private static class AppListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final List<AppEntry> mItems;

        AppListAdapter(Context context, List<AppEntry> items) {
            mInflater = LayoutInflater.from(context);
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public AppEntry getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.app_list_item, parent, false);
            }
            AppEntry item = getItem(position);
            ImageView iconView = view.findViewById(R.id.app_icon);
            TextView textView = view.findViewById(R.id.app_label);

            if (item.icon != null) {
                iconView.setImageDrawable(item.icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setImageDrawable(null);
                iconView.setVisibility(View.INVISIBLE);
            }
            textView.setText(item.label);
            return view;
        }
    }

    private void updateFooter(boolean stylusMode, boolean forceRecog, String refreshRate) {
        if (mFooterPref == null) return;

        final boolean penActive    = PenUtils.isPenModeEnabled();
        final boolean penConnected = PenUtils.isPenConnected();

        final String statusText;
        if (!stylusMode) {
            statusText = getString(R.string.stylus_status_inactive) + " (Off)";
        } else if (penConnected) {
            statusText = getString(R.string.stylus_status_active) + " (Pen Connected)";
        } else if (forceRecog) {
            statusText = getString(R.string.stylus_status_active) + " (Force Mode)";
        } else {
            statusText = getString(R.string.stylus_status_inactive) + " (No Pen)";
        }

        mFooterPref.setTitle(
                getString(R.string.stylus_more_info)
                        + "\n\n"
                        + getString(R.string.stylus_footer_status, statusText)
                        + "\n"
                        + getString(R.string.stylus_footer_refresh_rate,
                                getRateLabel(refreshRate)
                                + " (" + (penActive ? "Applied" : "Saved") + ")"));
    }

    private String getRateLabel(String value) {
        switch (value) {
            case RATE_60:  return getString(R.string.refresh_rate_60hz);
            case RATE_120: return getString(R.string.refresh_rate_120hz);
            default:       return getString(R.string.refresh_rate_dynamic);
        }
    }

    private void logDebug(String msg) { Log.d(TAG, ts() + msg); }
    private void logInfo(String msg)  { Log.i(TAG, ts() + msg); }

    private static String ts() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "] ";
    }
}
