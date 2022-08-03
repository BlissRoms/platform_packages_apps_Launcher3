/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.launcher3

import android.content.Context
import android.widget.Toast;
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.icons.IconCache
import com.android.launcher3.icons.LauncherIconProvider
import com.android.launcher3.util.DaggerSingletonObject
import javax.inject.Inject
import javax.inject.Named

/** A collection of common dependencies used across Launcher */
@Deprecated("Inject the specific targets directly instead of using LauncherAppState")
data class LauncherAppState
@Inject
constructor(
    @ApplicationContext val context: Context,
    val iconProvider: LauncherIconProvider,
    val iconCache: IconCache,
    val model: LauncherModel,
    val invariantDeviceProfile: InvariantDeviceProfile,
    @Named("SAFE_MODE") val isSafeModeEnabled: Boolean,
) {

    // Held strongly: SharedPreferences only keeps weak references to its listeners.
    private val restartPrefListener = LauncherPrefChangeListener { key ->
        if (RESTART_PREFS.any { it.sharedPrefKey == key }) setNeedsRestart()
    }

    init {
        LauncherPrefs.get(context).addListener(restartPrefListener, *RESTART_PREFS)
    }

    fun setNeedsRestart() {
        needsRestart = true
    }

    fun checkIfRestartNeeded() {
        // we destroyed Settings activity with the back button
        // so we force a restart now if needed without waiting for home button press
        if (needsRestart) {
            Toast.makeText(context, R.string.restarting_launcher_changes, Toast.LENGTH_SHORT).show();
            Utilities.restart()
        }
    }

    companion object {
        @JvmField var needsRestart: Boolean = false

        /** Preferences that are only applied when the launcher process starts. */
        private val RESTART_PREFS: Array<Item> = arrayOf(
            LauncherPrefs.SHOW_STATUS_BAR,
            LauncherPrefs.RECENTS_MEMINFO,
            LauncherPrefs.SHORT_PARALLAX,
            LauncherPrefs.SINGLE_PAGE_CENTER,
            LauncherPrefs.DRAWER_SCROLLBAR,
            LauncherPrefs.DARK_STATUS_BAR,
            LauncherPrefs.AUTO_HIDE_DOTS,
            LauncherPrefs.BLUR_DEPTH,
        )

        @JvmField var INSTANCE = DaggerSingletonObject { it.launcherAppState }

        @JvmStatic fun getInstance(context: Context) = INSTANCE[context]

        /** Shorthand for [.getInvariantDeviceProfile] */
        @JvmStatic fun getIDP(context: Context) = InvariantDeviceProfile.INSTANCE[context]
    }
}
