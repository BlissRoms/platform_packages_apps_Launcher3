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

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * App drawer settings activity for Launcher.
 */
public class SettingsAppDrawer extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new AppDrawerSettingsFragment())
                    .commit();
        }
    }

    /**
     * This fragment shows the app drawer preferences.
     */
    public static class AppDrawerSettingsFragment extends SettingsBasePreferenceFragment {

        private static final String KEY_SEARCH_PLACEMENT = "pref_allapps_search_placement";
        private static final String KEY_OPEN_KEYBOARD = "pref_drawer_open_keyboard";
        private static final String SEARCH_PLACEMENT_HIDDEN = "hidden";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_app_drawer_preferences, rootKey);

            ListPreference searchPlacement = findPreference(KEY_SEARCH_PLACEMENT);
            Preference openKeyboard = findPreference(KEY_OPEN_KEYBOARD);
            if (searchPlacement != null && openKeyboard != null) {
                openKeyboard.setEnabled(!SEARCH_PLACEMENT_HIDDEN.equals(searchPlacement.getValue()));
                searchPlacement.setOnPreferenceChangeListener((pref, value) -> {
                    openKeyboard.setEnabled(!SEARCH_PLACEMENT_HIDDEN.equals(value));
                    return true;
                });
            }
        }
    }
}
