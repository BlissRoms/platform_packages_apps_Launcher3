/*
 * Copyright (C) 2026 BlissRoms Project
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
package com.android.quickstep.util

import android.content.Context
import com.android.launcher3.LauncherPrefChangeListener
import com.android.launcher3.LauncherPrefs

object BlurDepthPref {

    private const val UNSET = -1

    @Volatile private var cachedRadius = UNSET
    private var listenerRegistered = false

    private val prefListener = LauncherPrefChangeListener { key ->
        if (key == LauncherPrefs.BLUR_DEPTH.sharedPrefKey) {
            cachedRadius = UNSET
        }
    }

    @JvmStatic
    fun getMaxBlurRadius(context: Context): Int {
        val cached = cachedRadius
        if (cached != UNSET) {
            return cached
        }
        val prefs = LauncherPrefs.get(context)
        synchronized(this) {
            if (!listenerRegistered) {
                prefs.addListener(prefListener, LauncherPrefs.BLUR_DEPTH)
                listenerRegistered = true
            }
        }
        return prefs.get(LauncherPrefs.BLUR_DEPTH).also { cachedRadius = it }
    }
}
