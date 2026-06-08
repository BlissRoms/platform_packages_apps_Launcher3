/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import com.android.launcher3.icons.BaseIconFactory
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.IconThemeController
import com.android.launcher3.icons.SourceHint
import com.android.launcher3.icons.ThemedBitmap

/**
 * Wraps an [IconThemeController] so icon-pack drawables are never themed. A force-theme controller
 * otherwise generates a monochrome bitmap and paints a shape plate behind pack-supplied icons.
 */
class IconPackExemptThemeController(private val delegate: IconThemeController) :
    IconThemeController by delegate {

    override fun createThemedBitmap(
        icon: AdaptiveIconDrawable,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint?,
    ): ThemedBitmap =
        if (icon.wrapsIconPackDrawable()) ThemedBitmap.NOT_SUPPORTED
        else delegate.createThemedBitmap(icon, info, factory, sourceHint)
}

private fun AdaptiveIconDrawable.wrapsIconPackDrawable(): Boolean {
    var layer: Drawable? = foreground
    while (layer != null) {
        if (layer is IconPackDrawable) return true
        layer = (layer as? InsetDrawable)?.drawable
    }
    return false
}
