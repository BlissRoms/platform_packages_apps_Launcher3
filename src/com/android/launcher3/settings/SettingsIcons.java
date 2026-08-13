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

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.SettingsCache.NOTIFICATION_BADGING_URI;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.util.SafeCloseable;
import com.android.launcher3.util.SettingsCache;

import kotlin.Unit;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * Icons settings activity for Launcher.
 */
public class SettingsIcons extends CollapsingToolbarBaseActivity implements OnPreferenceStartFragmentCallback {

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        if (getSupportFragmentManager().isStateSaved()) {
            return false;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        if (f instanceof DialogFragment) {
            f.setArguments(pref.getExtras());
            ((DialogFragment) f).show(fm, pref.getKey());
        } else {
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra(SettingsActivity.EXTRA_FRAGMENT_ARGS, pref.getExtras()));
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new IconsSettingsFragment())
                    .commit();
        }
    }

    /**
     * This fragment shows the icons preferences.
     */
    public static class IconsSettingsFragment extends SettingsBasePreferenceFragment {

        private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";
        private static final String KEY_NOTIFICATION_BADGE_COUNTS = "pref_notification_badge_counts";

        private @Nullable SafeCloseable mSettingCacheSafeCloseable;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSettingCacheSafeCloseable = SettingsCache.INSTANCE.get(getContext())
                    .getListenableRef(NOTIFICATION_BADGING_URI).forEach(
                            MAIN_EXECUTOR, this::onSettingsChanged);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mSettingCacheSafeCloseable != null) {
                mSettingCacheSafeCloseable.close();
                mSettingCacheSafeCloseable = null;
            }
        }

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
                            .getValue(NOTIFICATION_BADGING_URI);
                    preference.setEnabled(dotsEnabled);
                    if (!dotsEnabled) {
                        preference.setSummary(
                                R.string.bliss_notification_badge_counts_disabled_summary);
                    }
                    return BuildConfig.NOTIFICATION_DOTS_ENABLED;
            }
            return true;
        }

        private Unit onSettingsChanged(boolean isEnabled) {
            // Notification dots toggled, re-evaluate badge counts preference
            PreferenceScreen screen = getPreferenceScreen();
            if (screen != null) {
                for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                    initPreference(screen.getPreference(i));
                }
            }
            return null;
        }
    }
}
