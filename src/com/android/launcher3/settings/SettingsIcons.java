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
import androidx.preference.PreferenceScreen;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.util.SettingsCache;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * Icons settings activity for Launcher.
 */
public class SettingsIcons extends CollapsingToolbarBaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new IconsSettingsFragment())
                    .commit();
        }
        LauncherPrefs.getPrefs(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        LauncherPrefs.getPrefs(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LauncherPrefs.ICON_SIZE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.FONT_SIZE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.ALLAPPS_THEMED_ICONS.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_DESKTOP_LABELS.getSharedPrefKey().equals(key) ||
                LauncherPrefs.NOTIFICATION_BADGE_COUNTS.getSharedPrefKey().equals(key)) {
            LauncherAppState.setNeedsRecreate();
        }
    }

    /**
     * This fragment shows the icons preferences.
     */
    public static class IconsSettingsFragment extends SettingsBasePreferenceFragment implements
            SettingsCache.OnChangeListener {

        private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";
        private static final String KEY_NOTIFICATION_BADGE_COUNTS = "pref_notification_badge_counts";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_icons_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }
        }

        private boolean initPreference(Preference preference) {
            switch (preference.getKey()) {
                case NOTIFICATION_DOTS_PREFERENCE_KEY:
                    return BuildConfig.NOTIFICATION_DOTS_ENABLED;
                case KEY_NOTIFICATION_BADGE_COUNTS:
                    boolean dotsEnabled = SettingsCache.INSTANCE.get(getContext())
                            .getValue(SettingsCache.NOTIFICATION_BADGING_URI);
                    preference.setEnabled(dotsEnabled);
                    if (!dotsEnabled) {
                        preference.setSummary(
                                R.string.bliss_notification_badge_counts_disabled_summary);
                    }
                    return BuildConfig.NOTIFICATION_DOTS_ENABLED;
            }
            return true;
        }

        @Override
        public void onSettingsChanged(boolean isEnabled) {
            // Notification dots toggled, re-evaluate badge counts preference
            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                initPreference(screen.getPreference(i));
            }
        }
    }
}
