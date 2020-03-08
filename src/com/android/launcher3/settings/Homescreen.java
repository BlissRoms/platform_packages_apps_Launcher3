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

import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;
import static com.android.launcher3.states.RotationHelper.getAllowRotationDefaultValue;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragment.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragment.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

public class Homescreen extends SettingsActivity
        implements OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new HomescreenSettingsFragment()).commit();
        }

        Utilities.getPrefs(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_MINUS_ONE:
                LauncherAppState.getInstanceNoCreate().setNeedsRestart();
                break;
            default:
                break;
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

    public static class HomescreenSettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        ActionBar actionBar;
        ListPreference gridColumns;
        ListPreference gridRows;
        ListPreference hotseatColumns;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.home_screen_preferences, rootKey);

            actionBar=getActivity().getActionBar();
            assert actionBar != null;
            actionBar.setDisplayHomeAsUpEnabled(true);

            Preference rotationPref = findPreference(ALLOW_ROTATION_PREFERENCE_KEY);
            rotationPref.setDefaultValue(getAllowRotationDefaultValue());

            gridColumns = (ListPreference) findPreference(Utilities.GRID_COLUMNS);
            gridColumns.setSummary(gridColumns.getEntry());

            gridRows = (ListPreference) findPreference(Utilities.GRID_ROWS);
            gridRows.setSummary(gridRows.getEntry());

            hotseatColumns = (ListPreference) findPreference(Utilities.HOTSEAT_ICONS);
            hotseatColumns.setSummary(hotseatColumns.getEntry());

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
                case Utilities.KEY_FEED_INTEGRATION:
                    preference.setOnPreferenceChangeListener(this);
                    return hasPackageInstalled(Utilities.PACKAGE_NAME);
                case ALLOW_ROTATION_PREFERENCE_KEY:
                case Utilities.DESKTOP_SHOW_LABEL:
                case Utilities.DESKTOP_SHOW_QUICKSPACE:
                case Utilities.GRID_COLUMNS:
                case Utilities.GRID_ROWS:
                case Utilities.HOTSEAT_ICONS:
                    preference.setOnPreferenceChangeListener(this);
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
            String key = preference.getKey();
            if (Utilities.GRID_COLUMNS.equals(key)) {
                int index = gridColumns.findIndexOfValue((String) newValue);
                gridColumns.setSummary(gridColumns.getEntries()[index]);
            } else if (Utilities.GRID_ROWS.equals(key)) {
                int index = gridRows.findIndexOfValue((String) newValue);
                gridRows.setSummary(gridRows.getEntries()[index]);
            } else if (Utilities.HOTSEAT_ICONS.equals(key)) {
                int index = hotseatColumns.findIndexOfValue((String) newValue);
                hotseatColumns.setSummary(hotseatColumns.getEntries()[index]);
            }
            LauncherAppState.getInstanceNoCreate().setNeedsRestart();
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            return false;
        }

        private boolean hasPackageInstalled(String pkgName) {
            try {
                ApplicationInfo ai = getContext().getPackageManager()
                        .getApplicationInfo(pkgName, 0);
                return ai.enabled;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
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
