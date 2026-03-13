/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable

/**
 * A [Drawable] that composites an icon pack's mask/back/upon layers onto an original icon.
 *
 * The compositing order (bottom to top):
 * 1. [backDrawable] — background layer
 * 2. Original icon, scaled by [scaleFactor] and masked by [maskDrawable]
 * 3. [uponDrawable] — overlay layer
 *
 * The composited result is cached as a bitmap and only regenerated when the size changes.
 *
 * @param originalIcon The app's original system icon to transform.
 * @param backDrawable Optional background layer (drawn behind the icon).
 * @param maskDrawable Optional alpha mask (opaque = keep, transparent = erase).
 * @param uponDrawable Optional overlay layer (drawn on top).
 * @param scaleFactor Scale factor for the original icon within the composited bounds.
 */
class IconPackMaskedDrawable(
    private val originalIcon: Drawable,
    private val backDrawable: Drawable?,
    private val maskDrawable: Drawable?,
    private val uponDrawable: Drawable?,
    private val scaleFactor: Float,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    private var cachedBitmap: Bitmap? = null
    private var cachedSize = 0

    override fun draw(canvas: Canvas) {
        val size = minOf(bounds.width(), bounds.height())
        if (size <= 0) return

        val bitmap = getOrCreateBitmap(size)
        canvas.drawBitmap(bitmap, bounds.left.toFloat(), bounds.top.toFloat(), paint)
    }

    private fun getOrCreateBitmap(size: Int): Bitmap {
        cachedBitmap?.let { if (cachedSize == size) return it }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fullRect = Rect(0, 0, size, size)

        // 1. Draw background layer
        backDrawable?.let {
            it.bounds = fullRect
            it.draw(canvas)
        }

        // 2. Draw scaled + masked original icon
        val scaledSize = (size * scaleFactor).toInt()
        val offset = (size - scaledSize) / 2
        val scaledRect = Rect(offset, offset, offset + scaledSize, offset + scaledSize)

        if (maskDrawable != null) {
            // Render icon into temporary bitmap, then apply mask via DST_IN
            val iconBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val iconCanvas = Canvas(iconBitmap)
            originalIcon.bounds = scaledRect
            originalIcon.draw(iconCanvas)

            // Render mask into another bitmap
            val maskBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskBitmap)
            maskDrawable.bounds = fullRect
            maskDrawable.draw(maskCanvas)

            // Apply: keep icon pixels only where mask is opaque
            iconCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
            maskBitmap.recycle()

            canvas.drawBitmap(iconBitmap, 0f, 0f, paint)
            iconBitmap.recycle()
        } else {
            originalIcon.bounds = scaledRect
            originalIcon.draw(canvas)
        }

        // 3. Draw overlay layer
        uponDrawable?.let {
            it.bounds = fullRect
            it.draw(canvas)
        }

        cachedBitmap = bitmap
        cachedSize = size
        return bitmap
    }

    override fun getIntrinsicWidth(): Int = originalIcon.intrinsicWidth

    override fun getIntrinsicHeight(): Int = originalIcon.intrinsicHeight

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun invalidateSelf() {
        super.invalidateSelf()
        cachedBitmap?.recycle()
        cachedBitmap = null
    }
}
