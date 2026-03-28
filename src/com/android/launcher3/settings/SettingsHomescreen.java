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
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.Utilities;
import com.android.launcher3.R;
import com.android.launcher3.lineage.LineageUtils;
import com.android.launcher3.lineage.trust.TrustAppsActivity;
import com.android.launcher3.util.VibratorWrapper;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * Home screen settings activity for Launcher.
 */
public class SettingsHomescreen extends CollapsingToolbarBaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences.OnSharedPreferenceChangeListener mStyleShadowListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new HomescreenSettingsFragment())
                    .commit();
        }
        LauncherPrefs.getPrefs(this).registerOnSharedPreferenceChangeListener(this);

        SharedPreferences prefs = LauncherPrefs.getPrefs(this);
        if (prefs.contains(LauncherPrefs.SHOW_QUICKSPACE_ALT.getSharedPrefKey()) && !prefs.contains("pref_quickspace_style")) {
            boolean wasAlt = prefs.getBoolean(LauncherPrefs.SHOW_QUICKSPACE_ALT.getSharedPrefKey(), false);
            prefs.edit().putString("pref_quickspace_style", wasAlt ? "1" : "0").apply();
        }
        mStyleShadowListener = (sp, key) -> {
            if (LauncherPrefs.QUICKSPACE_STYLE.getSharedPrefKey().equals(key)) {
                boolean isExtended = "1".equals(sp.getString(key, "0"));
                sp.edit().putBoolean(LauncherPrefs.SHOW_QUICKSPACE_ALT.getSharedPrefKey(), isExtended).apply();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(mStyleShadowListener);
    }

    @Override
    public void onDestroy() {
        LauncherPrefs.getPrefs(this).unregisterOnSharedPreferenceChangeListener(mStyleShadowListener);
        LauncherPrefs.getPrefs(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LauncherPrefs.DOCK_SEARCH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DOCK_SEARCH_PIXEL_STYLE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DOCK_THEME.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SEARCH_RADIUS_SIZE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.DOCK_MUSIC_SEARCH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_QSB_OPACITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_HOTSEAT_BG.getSharedPrefKey().equals(key) ||
                LauncherPrefs.HOTSEAT_OPACITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.QUICKSPACE_STYLE.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_PSONALITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_NOWPLAYING.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_WEATHER.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_WEATHER_CITY.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_WEATHER_PROVIDER.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_WEATHER_TEXT.getSharedPrefKey().equals(key) ||
                LauncherPrefs.SHOW_QUICKSPACE_BLUETOOTH.getSharedPrefKey().equals(key) ||
                LauncherPrefs.AUTO_HIDE_DOTS.getSharedPrefKey().equals(key)) {
            LauncherAppState.setNeedsRecreate();
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
        private static final String KEY_GENERAL_CATEGORY = "general_category";
        private static final String KEY_DOCK_SEARCH = "pref_dock_search";
        private static final String KEY_DOCK_MUSIC_SEARCH = "pref_dock_music_search";
        private static final String KEY_HOTSEAT_QSB_OPACITY = "pref_hotseat_qsb_opacity";
        private static final String KEY_HOTSEAT_QSB_STROKE_WIDTH = "pref_hotseat_qsb_stroke_width";
        private static final String KEY_WEATHER_PROVIDER = "pref_quickspace_weather_provider";
        private static final String KEY_WEATHER_CITY = "pref_quickspace_weather_city";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_home_screen_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            filterPreferenceGroup(screen);

            if (!VibratorWrapper.INSTANCE.get(getContext()).hasVibrator()) {
                PreferenceCategory generalCategory = (PreferenceCategory) findPreference(KEY_GENERAL_CATEGORY);
                Preference d2SHaptic = screen.findPreference(LauncherPrefs.SLEEP_GESTURE_HAPTIC.getSharedPrefKey());
                generalCategory.removePreference(d2SHaptic);
            }

            ListPreference providerPref = findPreference(KEY_WEATHER_PROVIDER);
            SwitchPreferenceCompat cityPref = findPreference(KEY_WEATHER_CITY);
            if (providerPref != null && cityPref != null) {
                updateCityToggleState(cityPref, providerPref.getValue());
                providerPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    updateCityToggleState(cityPref, (String) newValue);
                    return true;
                });
            }
        }

        private void updateCityToggleState(SwitchPreferenceCompat cityPref, String providerValue) {
            boolean omniJawsPossible = "omnijaws".equals(providerValue) || "auto".equals(providerValue);
            cityPref.setEnabled(omniJawsPossible);
        }

        private void filterPreferenceGroup(PreferenceGroup group) {
            for (int i = group.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = group.getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    filterPreferenceGroup((PreferenceGroup) preference);
                } else if (!initPreference(preference)) {
                    group.removePreference(preference);
                }
            }
        }

        private boolean initPreference(Preference preference) {
            LauncherApps launcherApps = getContext().getSystemService(LauncherApps.class);
            switch (preference.getKey()) {
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
                    if (launcherApps == null ||
                            !launcherApps.isPackageEnabled(SUGGESTIONS_PACKAGE, myUserHandle())) {
                        return false;
                    }
                    preference.setOnPreferenceClickListener(p -> {
                        try {
                            getContext().startActivity(new Intent(
                                    "android.settings.ACTION_CONTENT_SUGGESTIONS_SETTINGS"));
                        } catch (android.content.ActivityNotFoundException e) {
                            // Settings activity not available on this ROM, silently ignore
                        }
                        return true;
                    });
                    return true;
                case KEY_DOCK_SEARCH:
                case KEY_DOCK_MUSIC_SEARCH:
                case KEY_HOTSEAT_QSB_OPACITY:
                case KEY_HOTSEAT_QSB_STROKE_WIDTH:
                    return Utilities.isGSAEnabled(getContext());
            }
            return true;
        }
    }
}
