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

import static android.os.Process.myUserHandle;

import android.content.Intent;
import android.content.pm.LauncherApps;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.lineage.LineageUtils;
import com.android.launcher3.lineage.trust.TrustAppsActivity;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * Home screen settings activity for Launcher.
 */
public class SettingsHomescreen extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new HomescreenSettingsFragment())
                    .commit();
        }
    }

    /**
     * This fragment shows the home screen preferences.
     */
    public static class HomescreenSettingsFragment extends SettingsBasePreferenceFragment {

        private static final String KEY_MINUS_ONE = "pref_enable_minus_one";
        private static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";
        private static final String KEY_TRUST_APPS = "pref_trust_apps";
        private static final String KEY_SUGGESTIONS = "pref_suggestions";
        private static final String SUGGESTIONS_PACKAGE = "com.google.android.as";
        private static final String SHOW_HOTSEAT_QSB_KEY = "pref_show_hotseat_qsb";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_home_screen_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            filterPreferenceGroup(screen);
        }

        private void filterPreferenceGroup(PreferenceGroup group) {
            for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = group.getPreference(i);
                if (!initPreference(preference)) {
                    group.removePreference(preference);
                } else if (preference instanceof PreferenceGroup) {
                    filterPreferenceGroup((PreferenceGroup) preference);
                }
            }
        }

        private boolean initPreference(Preference preference) {
            String key = preference.getKey();
            if (key == null) return true;
            LauncherApps launcherApps = getContext().getSystemService(LauncherApps.class);
            switch (key) {
                case KEY_MINUS_ONE:
                    return launcherApps != null &&
                            launcherApps.isPackageEnabled(SEARCH_PACKAGE, myUserHandle());
                case KEY_TRUST_APPS:
                    preference.setOnPreferenceClickListener(p -> {
                        LineageUtils.showLockScreen(getActivity(),
                                getString(R.string.trust_apps_manager_name), () -> {
                            Intent intent = new Intent(getActivity(), TrustAppsActivity.class);
                            startActivity(intent);
                        });
                        return true;
                    });
                    return true;
                case KEY_SUGGESTIONS:
                    if (preference.getIntent() == null || preference.getIntent().resolveActivity(
                            getContext().getPackageManager()) == null) {
                        return false;
                    }
                    return launcherApps != null &&
                            launcherApps.isPackageEnabled(SUGGESTIONS_PACKAGE, myUserHandle());
                case SHOW_HOTSEAT_QSB_KEY:
                    return com.android.launcher3.Flags.enableQsbOnHotseat() && launcherApps != null &&
                            launcherApps.isPackageEnabled(SEARCH_PACKAGE, myUserHandle());
            }
            return true;
        }
    }
}
