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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.HiddenAppsActivity;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import android.util.Log;
import android.view.MenuItem;

import java.util.Objects;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class AppDrawer extends SettingsActivity implements PreferenceFragment.OnPreferenceStartFragmentCallback {

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new AppDrawerSettingsFragment()).commit();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        Fragment instantiate = Fragment.instantiate(this, preference.getFragment(), preference.getExtras());
        if (instantiate instanceof DialogFragment) {
            ((DialogFragment) instantiate).show(getFragmentManager(), preference.getKey());
        } else {
            getFragmentManager().beginTransaction().replace(android.R.id.content, instantiate).addToBackStack(preference.getKey()).commit();
        }
        return true;
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class AppDrawerSettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ActionBar actionBar;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.app_drawer_preferences, rootKey);

            actionBar=getActivity().getActionBar();
            assert actionBar != null;
            actionBar.setDisplayHomeAsUpEnabled(true);

            SwitchPreference allAppsShowLabel = (SwitchPreference) findPreference(Utilities.ALLAPPS_SHOW_LABEL);
            allAppsShowLabel.setOnPreferenceChangeListener(this);

            Preference hiddenApp = findPreference(Utilities.KEY_HIDDEN_APPS);
            hiddenApp.setOnPreferenceClickListener(this);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }
        }

        /**
         * Initializes a preference. This is called for every preference. Returning false here
         * will remove that preference from the list.
         */
        protected boolean initPreference(Preference preference) {
            switch (preference.getKey()) {
                case Utilities.ALLAPPS_SHOW_LABEL:
                    return true;
                case Utilities.KEY_HIDDEN_APPS:
                    return true;
            }
            return true;
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, final Object newValue) {
            switch (preference.getKey()) {
                case Utilities.ALLAPPS_SHOW_LABEL:
                    LauncherAppState.getInstanceNoCreate().setNeedsRestart();
                    return true;
            }
            return false;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case Utilities.KEY_HIDDEN_APPS:
                    startActivity(new Intent(getActivity(), HiddenAppsActivity.class));
                    return false;
            }
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
