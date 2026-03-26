/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AppDrawerStyle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * App drawer settings activity for Launcher.
 */
public class SettingsAppDrawer extends CollapsingToolbarBaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new AppDrawerSettingsFragment())
                    .commit();
        }
        LauncherPrefs.getPrefs(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LauncherPrefs.getPrefs(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LauncherPrefs.ALL_APPS_SEARCH_PLACEMENT.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DRAWER_SCROLLBAR.getSharedPrefKey().equals(key) ||
                LauncherPrefs.ALL_APPS_DARK_TEXT.getSharedPrefKey().equals(key) ||
                LauncherPrefs.APP_DRAWER_STYLE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.ENABLE_TWOLINE_ALLAPPS_TOGGLE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_DRAWER_LABELS.getSharedPrefKey().equals(key) ||
                LauncherPrefs.ROW_HEIGHT.getSharedPrefKey().equals(key) ||
                LauncherPrefs.APP_DRAWER_OPACITY.getSharedPrefKey().equals(key)) {
            LauncherAppState.setNeedsRecreate();
        }
    }

    /**
     * This fragment shows the app drawer preferences.
     */
    public static class AppDrawerSettingsFragment extends SettingsBasePreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String KEY_SEARCH_PLACEMENT = "pref_allapps_search_placement";
        private static final String KEY_OPEN_KEYBOARD = "pref_drawer_open_keyboard";
        private static final String KEY_APP_DRAWER_STYLE = "pref_app_drawer_style";

        private ListPreference mSearchPlacementPref;
        private ListPreference mDrawerStylePref;
        private Preference mOpenKeyboardPref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_app_drawer_preferences, rootKey);

            mSearchPlacementPref = findPreference(KEY_SEARCH_PLACEMENT);
            mDrawerStylePref = findPreference(KEY_APP_DRAWER_STYLE);
            mOpenKeyboardPref = findPreference(KEY_OPEN_KEYBOARD);
            updateOpenKeyboardEnabled();
            updateDrawerStyleSummary();

            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (KEY_SEARCH_PLACEMENT.equals(key) || KEY_APP_DRAWER_STYLE.equals(key)) {
                updateOpenKeyboardEnabled();
                updateDrawerStyleSummary();
            }
        }

        private void updateOpenKeyboardEnabled() {
            if (mOpenKeyboardPref == null || mSearchPlacementPref == null) return;
            boolean searchVisible = !"hidden".equals(mSearchPlacementPref.getValue());
            mOpenKeyboardPref.setEnabled(searchVisible);
        }

        private void updateDrawerStyleSummary() {
            if (mDrawerStylePref == null) {
                return;
            }
            String style = mDrawerStylePref.getValue();
            mDrawerStylePref.setSummary(mDrawerStylePref.getEntry());
        }
    }
}
