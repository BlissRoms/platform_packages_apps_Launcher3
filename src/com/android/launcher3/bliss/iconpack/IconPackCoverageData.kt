/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.content.ComponentName
import android.graphics.drawable.Drawable

/**
 * Coverage statistics and per-app icon data for an icon pack against installed apps.
 *
 * @param totalApps Total number of launcher activities on the device.
 * @param themedApps Number of apps that have a themed icon in the pack.
 * @param items Per-app coverage details sorted with themed icons first.
 */
data class IconPackCoverageData(
    val totalApps: Int,
    val themedApps: Int,
    val items: List<IconPackCoverageItem>,
) {
    /** Coverage as a percentage (0–100). */
    val coveragePercent: Int
        get() = if (totalApps > 0) (themedApps * 100) / totalApps else 0
}

/**
 * A single app's icon comparison: original system icon vs themed icon from the pack.
 *
 * @param componentName The app's launcher activity component.
 * @param label The app's display name.
 * @param originalIcon The app's original system icon.
 * @param themedIcon The icon pack's themed icon, or null if no mapping exists.
 */
data class IconPackCoverageItem(
    val componentName: ComponentName,
    val label: String,
    val originalIcon: Drawable?,
    val themedIcon: Drawable?,
)
