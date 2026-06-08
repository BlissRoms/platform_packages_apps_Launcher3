/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import com.android.launcher3.icons.BaseIconFactory
import com.android.launcher3.icons.BitmapInfo

/** Renders an icon-pack drawable in its own shape, bypassing [BaseIconFactory]'s default wrap. */
class IconPackDrawable(inner: Drawable) : DrawableWrapper(inner), BitmapInfo.Extender {

    override fun getUpdatedBitmapInfo(
        info: BitmapInfo,
        factory: BaseIconFactory,
    ): BitmapInfo {
        val src = drawable ?: return info
        val size = factory.iconBitmapSize
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val savedBounds = src.bounds
        src.setBounds(0, 0, size, size)
        if (src is AdaptiveIconDrawable) {
            src.foreground?.draw(canvas)
        } else {
            src.draw(canvas)
        }
        src.bounds = savedBounds
        // Clear FLAG_FULL_BLEED so the icon blits flat instead of being clipped to the launcher
        // icon shape (and gaining a shape shadow) by FullBleedDrawableDelegate.
        return info.copy(icon = bitmap, flags = info.flags and BitmapInfo.FLAG_FULL_BLEED.inv())
    }

    override fun drawForPersistence() = Unit
}
