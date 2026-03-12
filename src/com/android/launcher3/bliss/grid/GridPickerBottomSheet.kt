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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.view.animation.Interpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.android.app.animation.Interpolators
import com.android.launcher3.DeviceProfile
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener
import com.android.launcher3.Insettable
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.LauncherPrefs.Companion.GRID_NAME
import com.android.launcher3.R
import com.android.launcher3.views.AbstractSlideInView

/**
 * Bottom sheet that allows users to select a workspace grid size from the
 * predefined grid options in device_profiles.xml. Handles phone, tablet,
 * foldable, and multi-display form factors by adapting content width and
 * margins based on [DeviceProfile].
 */
class GridPickerBottomSheet(
    context: Context,
    attrs: AttributeSet?
) : AbstractSlideInView<Launcher>(context, attrs, 0),
    Insettable,
    OnDeviceProfileChangeListener {

    private lateinit var mGridOptionsContainer: LinearLayout
    private var mCurrentGridName: String? = null
    private val mInsets = Rect()

    override fun onFinishInflate() {
        super.onFinishInflate()
        mContent = findViewById(R.id.grid_picker_sheet)
        mGridOptionsContainer = findViewById(R.id.grid_options_container)
        setContentBackgroundWithParent(
            context.getDrawable(R.drawable.bg_rounded_corner_bottom_sheet)!!,
            mContent
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mActivityContext.addOnDeviceProfileChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mActivityContext.removeOnDeviceProfileChangeListener(this)
    }

    override fun onDeviceProfileChanged(dp: DeviceProfile) {
        // Configuration changed (rotation, fold/unfold) — dismiss to avoid stale layout
        handleClose(false)
    }

    override fun isOfType(type: Int): Boolean {
        return type and TYPE_GRID_PICKER != 0
    }

    override fun handleClose(animate: Boolean) {
        handleClose(animate, DEFAULT_CLOSE_DURATION.toLong())
    }

    override fun getShiftRange(): Float {
        return mContent.height.toFloat() + bottomOffsetPx
    }

    override fun setInsets(insets: Rect) {
        mInsets.set(insets)
        val bottomPadding = insets.bottom.coerceAtLeast(0)
        mContent.setPadding(
            mContent.paddingLeft,
            mContent.paddingTop,
            mContent.paddingRight,
            bottomPadding
        )
        requestLayout()
    }

    override fun getScrimColor(context: Context): Int {
        // Semi-transparent black scrim matching system bottom sheet pattern
        return 0x88000000.toInt()
    }

    override fun getIdleInterpolator(): Interpolator {
        // Tablets use EMPHASIZED for smoother close; phones use default ACCELERATE
        return if (mActivityContext.deviceProfile.deviceProperties.isTablet) {
            Interpolators.EMPHASIZED
        } else {
            super.getIdleInterpolator()
        }
    }

    /**
     * Measures content accounting for system insets and device type.
     * On tablets, applies larger horizontal margins using [DeviceProfile.allAppsLeftRightMargin].
     * On phones in landscape, respects left/right insets for display cutouts.
     * On phones in portrait, uses workspace padding for consistent alignment.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dp = mActivityContext.deviceProfile
        val widthUsed: Int = when {
            dp.deviceProperties.isTablet -> {
                maxOf(
                    2 * dp.allAppsLeftRightMargin,
                    2 * (mInsets.left + mInsets.right)
                )
            }
            mInsets.bottom > 0 -> {
                // Portrait phone — use left/right insets
                mInsets.left + mInsets.right
            }
            else -> {
                // Landscape phone — use workspace padding or insets, whichever is larger
                val padding = dp.mWorkspaceProfile.workspacePadding
                maxOf(
                    padding.left + padding.right,
                    2 * (mInsets.left + mInsets.right)
                )
            }
        }

        val topPadding = dp.bottomSheetProfile.bottomSheetTopPadding
        measureChildWithMargins(
            mContent, widthMeasureSpec, widthUsed,
            heightMeasureSpec, topPadding
        )
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val contentWidth = mContent.measuredWidth
        // Center horizontally, accounting for left/right insets (display cutouts)
        val contentLeft =
            (width - contentWidth - mInsets.left - mInsets.right) / 2 + mInsets.left
        mContent.layout(
            contentLeft,
            height - mContent.measuredHeight,
            contentLeft + contentWidth,
            height
        )
        setTranslationShift(mTranslationShift)
    }

    private fun populateGridOptions() {
        mGridOptionsContainer.removeAllViews()
        mCurrentGridName = LauncherPrefs.get(context).get(GRID_NAME)

        val gridOptions = getValidGridOptions(context)
        val inflater = LayoutInflater.from(context)

        for (gridOption in gridOptions) {
            val cardView = inflater.inflate(
                R.layout.grid_preview_card, mGridOptionsContainer, false
            )
            val dotPreview =
                cardView.findViewById<GridDotPreviewView>(R.id.grid_dot_preview)
            val label = cardView.findViewById<TextView>(R.id.grid_label)

            dotPreview.numColumns = gridOption.numColumns
            dotPreview.numRows = gridOption.numRows
            // Use numColumns as default hotseat count (matches XML default)
            dotPreview.numHotseatIcons = gridOption.numColumns

            label.text = context.getString(
                R.string.grid_picker_option_label,
                gridOption.numColumns, gridOption.numRows
            )

            val isCurrent = gridOption.name == mCurrentGridName
            cardView.isActivated = isCurrent

            val baseDescription = context.getString(
                R.string.grid_preview_content_description,
                gridOption.numColumns, gridOption.numRows
            )
            cardView.contentDescription = if (isCurrent) {
                "$baseDescription, ${context.getString(R.string.grid_current_label)}"
            } else {
                baseDescription
            }

            cardView.setOnClickListener {
                if (gridOption.name == mCurrentGridName) return@setOnClickListener
                GridChangeConfirmationDialog.show(
                    context,
                    gridOption.numColumns,
                    gridOption.numRows
                ) {
                    applyGridChange(gridOption.name)
                }
            }

            mGridOptionsContainer.addView(cardView)
        }
    }

    private fun applyGridChange(gridName: String) {
        InvariantDeviceProfile.INSTANCE.get(context).setCurrentGrid(gridName)
        handleClose(true)
    }

    private fun animateOpen() {
        if (mIsOpen || mOpenCloseAnimation.animationPlayer.isRunning) {
            return
        }
        mIsOpen = true
        setUpDefaultOpenAnimation().start()
    }

    companion object {
        private const val DEFAULT_CLOSE_DURATION = 200

        @JvmStatic
        fun show(launcher: Launcher): GridPickerBottomSheet {
            closeAllOpenViews(launcher, true)
            val sheet = launcher.layoutInflater.inflate(
                R.layout.grid_picker_bottom_sheet,
                launcher.dragLayer,
                false
            ) as GridPickerBottomSheet
            sheet.populateGridOptions()
            sheet.attachToContainer()
            sheet.animateOpen()
            return sheet
        }

        @JvmStatic
        fun isGridPickerAvailable(launcher: Launcher): Boolean {
            return getValidGridOptions(launcher).size >= 2
        }

        private fun getValidGridOptions(
            context: Context
        ): List<InvariantDeviceProfile.GridOption> {
            val idp = InvariantDeviceProfile.INSTANCE.get(context)
            return idp.parseAllGridOptions(context)
        }
    }
}
