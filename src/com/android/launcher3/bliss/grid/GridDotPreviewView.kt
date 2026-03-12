/*
 * Copyright (C) 2026 The BlissROMs Project
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
package com.android.launcher3.bliss.grid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.android.launcher3.R
import com.android.launcher3.util.Themes

/**
 * Custom view that draws a dot-grid pattern representing a workspace grid layout.
 * Workspace dots are drawn in the upper portion of the view, with hotseat dots
 * drawn below a thin divider line.
 */
class GridDotPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var numColumns: Int = 4
        set(value) {
            field = value
            invalidate()
        }

    var numRows: Int = 5
        set(value) {
            field = value
            invalidate()
        }

    var numHotseatIcons: Int = 4
        set(value) {
            field = value
            invalidate()
        }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Themes.getAttrColor(context, android.R.attr.textColorSecondary)
        alpha = 153 // 60% opacity
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
        color = dotPaint.color
        alpha = 51 // 20% opacity
    }

    private val dotRadius =
        resources.getDimension(R.dimen.grid_preview_dot_radius)
    private val hotseatDotRadius =
        resources.getDimension(R.dimen.grid_preview_hotseat_dot_radius)
    private val padding = 12f * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val drawWidth = w - 2 * padding
        val drawHeight = h - 2 * padding

        if (drawWidth <= 0 || drawHeight <= 0) return

        // Workspace takes 80%, divider gap 5%, hotseat 15%
        val workspaceHeight = drawHeight * 0.80f
        val dividerY = padding + workspaceHeight + drawHeight * 0.025f
        val hotseatTop = dividerY + drawHeight * 0.025f
        val hotseatHeight = drawHeight * 0.15f

        // Draw workspace dots
        if (numColumns > 0 && numRows > 0) {
            val cellW = drawWidth / numColumns
            val cellH = workspaceHeight / numRows
            for (row in 0 until numRows) {
                for (col in 0 until numColumns) {
                    val cx = padding + cellW * col + cellW / 2
                    val cy = padding + cellH * row + cellH / 2
                    canvas.drawCircle(cx, cy, dotRadius, dotPaint)
                }
            }
        }

        // Draw divider line
        canvas.drawLine(
            padding, dividerY, w - padding, dividerY, dividerPaint
        )

        // Draw hotseat dots
        if (numHotseatIcons > 0) {
            val hotseatCellW = drawWidth / numHotseatIcons
            val hotseatCy = hotseatTop + hotseatHeight / 2
            for (i in 0 until numHotseatIcons) {
                val cx = padding + hotseatCellW * i + hotseatCellW / 2
                canvas.drawCircle(cx, hotseatCy, hotseatDotRadius, dotPaint)
            }
        }
    }
}
