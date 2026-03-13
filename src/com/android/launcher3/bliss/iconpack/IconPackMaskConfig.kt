/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

/**
 * Global compositing configuration parsed from an icon pack's `appfilter.xml`.
 *
 * Icon packs use these layers to transform unthemed icons (apps without an explicit drawable
 * mapping) into a visually consistent style:
 *
 * 1. Scale the original icon by [scaleFactor]
 * 2. Apply [iconMaskDrawables] to crop/shape the scaled icon via alpha masking
 * 3. Draw a randomly-selected [iconBackDrawables] behind the masked icon
 * 4. Draw a randomly-selected [iconUponDrawables] on top
 *
 * @param iconBackDrawables Resource names for background layers (`<iconback img1="..." />`).
 * @param iconMaskDrawables Resource names for alpha masks (`<iconmask img1="..." />`).
 * @param iconUponDrawables Resource names for overlay layers (`<iconupon img1="..." />`).
 * @param scaleFactor Scale factor for the original icon before compositing (`<scale factor="..." />`).
 */
data class IconPackMaskConfig(
    val iconBackDrawables: List<String>,
    val iconMaskDrawables: List<String>,
    val iconUponDrawables: List<String>,
    val scaleFactor: Float,
) {
    /** Returns true if this config has any layers that can be applied to unthemed icons. */
    val hasMaskingLayers: Boolean
        get() = iconBackDrawables.isNotEmpty() ||
            iconMaskDrawables.isNotEmpty() ||
            iconUponDrawables.isNotEmpty()

    companion object {
        /** Default config with no layers and identity scale. */
        val EMPTY = IconPackMaskConfig(
            iconBackDrawables = emptyList(),
            iconMaskDrawables = emptyList(),
            iconUponDrawables = emptyList(),
            scaleFactor = 1.0f,
        )
    }
}
