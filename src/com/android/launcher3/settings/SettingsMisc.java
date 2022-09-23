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

import static com.android.launcher3.BuildConfig.IS_STUDIO_BUILD;
import static com.android.launcher3.InvariantDeviceProfile.TYPE_MULTI_DISPLAY;
import static com.android.launcher3.InvariantDeviceProfile.TYPE_TABLET;
import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.Flags;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.util.DisplayController;
import com.android.launcher3.util.SettingsCache;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

/**
 * Miscellaneous settings activity for Launcher.
 */
public class SettingsMisc extends CollapsingToolbarBaseActivity {

    public static final String FIXED_LANDSCAPE_MODE = "pref_fixed_landscape_mode";

    private static final String SUGGESTIONS_KEY = "pref_suggestions";
    protected static final String DPS_PACKAGE = "com.google.android.as";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, new MiscSettingsFragment())
                    .commit();
        }
    }

    /**
     * This fragment shows the miscellaneous preferences.
     */
    public static class MiscSettingsFragment extends SettingsBasePreferenceFragment implements
            SettingsCache.OnChangeListener {

        @VisibleForTesting
        static final String DEVELOPER_OPTIONS_KEY = "pref_developer_options";

        protected boolean mDeveloperOptionsEnabled = false;
        private boolean mRestartOnResume = false;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            if (BuildConfig.IS_DEBUG_DEVICE) {
                Uri devUri = Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED);
                SettingsCache settingsCache = SettingsCache.INSTANCE.get(getContext());
                mDeveloperOptionsEnabled = settingsCache.getValue(devUri);
                settingsCache.register(devUri, this);
            }
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.launcher_misc_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }
        }

        private boolean initPreference(Preference preference) {
            DisplayController.Info info = DisplayController.INSTANCE.get(getContext()).getInfo();
            switch (preference.getKey()) {
                case ALLOW_ROTATION_PREFERENCE_KEY:
                    if (Flags.oneGridSpecs() && !info.isRotationAllowed()) {
                        return false;
                    }
                    if (info.isTablet(info.realBounds)) {
                        // Launcher supports rotation by default. No need to show this setting.
                        return false;
                    }
                    // Initialize the UI once
                    preference.setDefaultValue(RotationHelper.getAllowRotationDefaultValue(info));
                    return true;
                case DEVELOPER_OPTIONS_KEY:
                    if (IS_STUDIO_BUILD) {
                        preference.setOrder(0);
                    }
                    return mDeveloperOptionsEnabled;
                case FIXED_LANDSCAPE_MODE:
                    if (!Flags.oneGridSpecs()
                            // adding this condition until fixing b/378972567
                            || InvariantDeviceProfile.INSTANCE.get(getContext()).deviceType
                            == TYPE_MULTI_DISPLAY
                            || InvariantDeviceProfile.INSTANCE.get(getContext()).deviceType
                            == TYPE_TABLET
                            || info.isRotationAllowed()) {
                        return false;
                    }
                    // When the setting changes rotate the screen accordingly to showcase the result
                    // of the setting
                    preference.setOnPreferenceChangeListener(
                            (pref, newValue) -> {
                                getActivity().setRequestedOrientation(
                                        (boolean) newValue
                                                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                : ActivityInfo.SCREEN_ORIENTATION_USER
                                );
                                return true;
                            }
                    );
                    return !info.isTablet(info.realBounds);
                case SUGGESTIONS_KEY:
                    // Show if Device Personalization Services is present.
                    if (!isDPSEnabled(getContext())) return false;
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
            }
            return true;
        }

        public static boolean isDPSEnabled(Context context) {
            try {
                return context.getPackageManager().getApplicationInfo(DPS_PACKAGE, 0).enabled;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mRestartOnResume) {
                recreateActivityNow();
            }
        }

        @Override
        public void onSettingsChanged(boolean isEnabled) {
            // Developer options changed, try recreate
            tryRecreateActivity();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (BuildConfig.IS_DEBUG_DEVICE) {
                SettingsCache.INSTANCE.get(getContext())
                        .unregister(Settings.Global.getUriFor(
                                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED), this);
            }
        }

        protected void tryRecreateActivity() {
            if (isResumed()) {
                recreateActivityNow();
            } else {
                mRestartOnResume = true;
            }
        }

        private void recreateActivityNow() {
            android.app.Activity activity = getActivity();
            if (activity != null) {
                activity.recreate();
            }
        }
    }
}
