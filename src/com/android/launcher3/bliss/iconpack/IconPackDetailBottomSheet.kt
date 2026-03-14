/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.Interpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.app.animation.Interpolators
import com.android.launcher3.DeviceProfile
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener
import com.android.launcher3.Insettable
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.bliss.iconpack.IconPackThemeFactory.ICON_PACK_FACTORY_ID
import com.android.launcher3.dagger.LauncherComponentProvider.appComponent
import com.android.launcher3.graphics.theme.ThemePreference.ThemeValue
import com.android.launcher3.views.AbstractSlideInView

/**
 * Bottom sheet that shows a full preview of an icon pack before applying it.
 *
 * Displays the pack's name, coverage statistics (e.g., "Themes 142 of 187 apps"), a progress bar,
 * and a grid showing all installed apps with their themed icons (full opacity) or original icons
 * (dimmed) for unthemed apps. An "Apply" button at the bottom commits the selection.
 *
 * Handles phone, tablet, foldable, and desktop form factors by adapting grid columns and content
 * width based on [DeviceProfile].
 */
class IconPackDetailBottomSheet(
    context: Context,
    attrs: AttributeSet?,
) :
    AbstractSlideInView<Launcher>(context, attrs, 0),
    Insettable,
    OnDeviceProfileChangeListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleView: TextView
    private lateinit var coverageView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var iconView: ImageView
    private lateinit var applyButton: Button
    private val mInsets = Rect()

    override fun onFinishInflate() {
        super.onFinishInflate()
        mContent = findViewById(R.id.bliss_icon_pack_detail_sheet)
        recyclerView = findViewById(R.id.bliss_icon_pack_detail_grid)
        titleView = findViewById(R.id.bliss_icon_pack_detail_title)
        coverageView = findViewById(R.id.bliss_icon_pack_detail_coverage)
        progressBar = findViewById(R.id.bliss_icon_pack_detail_progress)
        iconView = findViewById(R.id.bliss_icon_pack_detail_icon)
        applyButton = findViewById(R.id.bliss_icon_pack_detail_apply)
        setContentBackgroundWithParent(
            context.getDrawable(R.drawable.bg_rounded_corner_bottom_sheet)!!,
            mContent,
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
        handleClose(false)
    }

    override fun isOfType(type: Int): Boolean {
        return type and TYPE_ICON_PACK_DETAIL != 0
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
            bottomPadding,
        )
        requestLayout()
    }

    override fun getScrimColor(context: Context): Int {
        return 0x88000000.toInt()
    }

    override fun getIdleInterpolator(): Interpolator {
        return if (mActivityContext.deviceProfile.deviceProperties.isLargeScreen) {
            Interpolators.EMPHASIZED
        } else {
            super.getIdleInterpolator()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dp = mActivityContext.deviceProfile
        val widthUsed: Int =
            when {
                dp.deviceProperties.isLargeScreen -> {
                    maxOf(
                        2 * dp.allAppsProfile.leftRightMargin,
                        2 * (mInsets.left + mInsets.right),
                    )
                }
                mInsets.bottom > 0 -> {
                    mInsets.left + mInsets.right
                }
                else -> {
                    val padding = dp.workspaceProfile.workspacePadding
                    maxOf(
                        padding.left + padding.right,
                        2 * (mInsets.left + mInsets.right),
                    )
                }
            }

        val topPadding = dp.bottomSheetProfile.bottomSheetTopPadding
        measureChildWithMargins(mContent, widthMeasureSpec, widthUsed, heightMeasureSpec, topPadding)
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val contentWidth = mContent.measuredWidth
        val contentLeft =
            (width - contentWidth - mInsets.left - mInsets.right) / 2 + mInsets.left
        mContent.layout(
            contentLeft,
            height - mContent.measuredHeight,
            contentLeft + contentWidth,
            height,
        )
        setTranslationShift(mTranslationShift)
    }

    private fun populate(packInfo: IconPackInfo) {
        val iconPackManager = context.appComponent.iconPackManager
        val themePreference = context.appComponent.themePreference

        titleView.text = packInfo.label
        iconView.setImageDrawable(packInfo.icon)

        val coverage = iconPackManager.getIconPackCoverage(packInfo.packageName)

        coverageView.text = context.getString(
            R.string.bliss_icon_pack_detail_coverage,
            coverage.themedApps,
            coverage.totalApps,
        )
        progressBar.progress = coverage.coveragePercent

        val spanCount =
            if (mActivityContext.deviceProfile.deviceProperties.isLargeScreen) {
                TABLET_SPAN_COUNT
            } else {
                PHONE_SPAN_COUNT
            }
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)

        val spacing = resources.getDimensionPixelSize(R.dimen.bliss_icon_pack_detail_item_spacing)
        recyclerView.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    outRect.set(spacing, spacing, spacing, spacing)
                }
            }
        )

        recyclerView.adapter = IconPackDetailAdapter(coverage.items)

        applyButton.setOnClickListener {
            themePreference.setValue(ThemeValue(ICON_PACK_FACTORY_ID, packInfo.packageName))
            handleClose(true)
        }
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
        private const val PHONE_SPAN_COUNT = 5
        private const val TABLET_SPAN_COUNT = 8

        @JvmStatic
        fun show(launcher: Launcher, packInfo: IconPackInfo): IconPackDetailBottomSheet {
            closeAllOpenViews(launcher, true)
            val sheet =
                launcher.layoutInflater.inflate(
                    R.layout.bliss_icon_pack_detail_bottom_sheet,
                    launcher.dragLayer,
                    false,
                ) as IconPackDetailBottomSheet
            sheet.populate(packInfo)
            sheet.attachToContainer()
            sheet.animateOpen()
            return sheet
        }
    }
}
