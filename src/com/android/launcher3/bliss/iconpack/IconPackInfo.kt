/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.graphics.drawable.Drawable

/**
 * Represents an installed icon pack for display in the picker UI.
 *
 * @param packageName The icon pack's package name; empty string for system default.
 * @param label The user-visible name of the icon pack.
 * @param icon The icon pack's own app icon (for the picker card).
 * @param previewIcons Sample themed icons from the pack for visual preview (e.g., Phone,
 *   Messages, Camera, Settings). Empty list if previews are unavailable.
 */
data class IconPackInfo(
    val packageName: String,
    val label: CharSequence,
    val icon: Drawable?,
    val previewIcons: List<Drawable?> = emptyList(),
) {
    val isSystemDefault: Boolean
        get() = packageName.isEmpty()
}
