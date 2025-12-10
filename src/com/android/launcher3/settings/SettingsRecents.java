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

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.DisplayController;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * Recents settings activity for Launcher.
 */
public class SettingsRecents extends CollapsingToolbarBaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String RECENTS_CATEGORY_ACTION = "recents_category_actions";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new RecentsSettingsFragment())
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
        if (LauncherPrefs.RECENTS_MEMINFO.getSharedPrefKey().equals(key) ||
                LauncherPrefs.RECENTS_MEMINFO_ZRAM.getSharedPrefKey().equals(key)) {
            LauncherAppState.setNeedsRecreate();
        }
    }

    /**
     * This fragment shows the recents preferences.
     */
    public static class RecentsSettingsFragment extends SettingsBasePreferenceFragment {

        private static final String KEY_RECENTS_LENS = "pref_recents_lens";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_recents_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            filterPreferenceGroup(screen);
        }

        private void filterPreferenceGroup(PreferenceGroup group) {
            for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = group.getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    if (RECENTS_CATEGORY_ACTION.equals(preference.getKey())) {
                        DisplayController.Info info =
                                DisplayController.INSTANCE.get(getContext()).getInfo();
                        if (info.isTablet(info.realBounds)) {
                            group.removePreference(preference);
                            continue;
                        }
                    }
                    filterPreferenceGroup((PreferenceGroup) preference);
                } else if (!initPreference(preference)) {
                    group.removePreference(preference);
                }
            }
        }

        /**
         * Initializes a preference. This is called for every preference. Returning false here
         * will remove that preference from the list.
         */
        private boolean initPreference(Preference preference) {
            String key = preference.getKey();
            if (key == null) {
                return true;
            }

            if (key.equals(KEY_RECENTS_LENS) && !Utilities.isGSAEnabled(getContext())) {
                LauncherPrefs.getPrefs(getContext()).edit()
                        .putBoolean(KEY_RECENTS_LENS, false).apply();
                return false;
            }

            return true;
        }
    }
}
