/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.badge

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

/**
 * Renders notification badge count numbers on top of app icons.
 *
 * Draws a filled circle with a contrasting count text in the top-right corner of the icon bounds.
 * The badge size scales proportionally to the icon size for correct rendering across all
 * DPIs and screen resolutions.
 *
 * @param iconSize The icon size in pixels, used to calculate proportional badge dimensions.
 */
class BlissBadgeRenderer(iconSize: Int) {

    private val badgeRadius = iconSize * BADGE_RADIUS_FRACTION
    private val badgeCenterOffset = iconSize * BADGE_CENTER_OFFSET_FRACTION
    private val textSize = iconSize * TEXT_SIZE_FRACTION

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    /**
     * Draws a count badge at the top-right corner of [iconBounds].
     *
     * @param canvas The canvas to draw on.
     * @param iconBounds The icon's bounding rectangle.
     * @param count The notification count to display.
     * @param badgeColor The badge background color (typically the notification dot color).
     * @param scale The current dot animation scale (0–1).
     */
    fun draw(canvas: Canvas, iconBounds: Rect, count: Int, badgeColor: Int, scale: Float) {
        if (count <= 0 || scale <= 0f) return

        val displayText = if (count > MAX_DISPLAY_COUNT) "$MAX_DISPLAY_COUNT+" else count.toString()

        // Position: top-right corner of icon
        val cx = iconBounds.right - badgeCenterOffset
        val cy = iconBounds.top + badgeCenterOffset

        // Scale radius for wider numbers
        val extraWidth = if (displayText.length > 1) {
            textSize * (displayText.length - 1) * EXTRA_WIDTH_PER_CHAR
        } else {
            0f
        }
        val radius = (badgeRadius + extraWidth) * scale

        // Draw badge background
        circlePaint.color = badgeColor
        textPaint.color = contrastColor(badgeColor)
        if (extraWidth > 0f) {
            // Draw pill shape for multi-digit numbers
            val left = cx - radius
            val top = cy - badgeRadius * scale
            val right = cx + radius
            val bottom = cy + badgeRadius * scale
            canvas.drawRoundRect(left, top, right, bottom, badgeRadius * scale, badgeRadius * scale, circlePaint)
        } else {
            canvas.drawCircle(cx, cy, radius, circlePaint)
        }

        // Draw count text
        textPaint.textSize = textSize * scale
        val textBaseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(displayText, cx, textBaseline, textPaint)
    }

    companion object {
        /** Badge radius as a fraction of icon size. */
        private const val BADGE_RADIUS_FRACTION = 0.12f

        /** Offset from icon corner to badge center, as a fraction of icon size. */
        private const val BADGE_CENTER_OFFSET_FRACTION = 0.12f

        /** Text size as a fraction of icon size. */
        private const val TEXT_SIZE_FRACTION = 0.15f

        /** Extra width per additional character for multi-digit counts. */
        private const val EXTRA_WIDTH_PER_CHAR = 0.3f

        /** Maximum count shown before displaying "99+". */
        private const val MAX_DISPLAY_COUNT = 99

        /**
         * Returns black or white depending on which has better contrast against [color].
         * Uses the WCAG relative luminance formula.
         */
        private fun contrastColor(color: Int): Int {
            val r = Color.red(color) / 255.0
            val g = Color.green(color) / 255.0
            val b = Color.blue(color) / 255.0
            val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            return if (luminance > 0.179) Color.BLACK else Color.WHITE
        }
    }
}
