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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.app.animation.Interpolators
import com.android.launcher3.DeviceProfile
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener
import com.android.launcher3.Insettable
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.bliss.iconpack.IconPackThemeFactory.ICON_PACK_FACTORY_ID
import com.android.launcher3.dagger.LauncherComponentProvider.appComponent
import com.android.launcher3.graphics.theme.ThemePreference
import com.android.launcher3.graphics.theme.ThemePreference.ThemeValue
import com.android.launcher3.views.AbstractSlideInView

/**
 * Bottom sheet for selecting an icon pack from installed icon packs on the device.
 *
 * Displays a vertical list of icon pack cards with the "System Default" option always first. Each
 * card shows the pack's app icon, a row of sample themed icons (Phone, Messages, Camera, Settings),
 * and the pack label. Tapping a card instantly applies the icon pack via the theme preference
 * system, which triggers cache invalidation and model reload. Handles phone, tablet, foldable,
 * and desktop form factors by adapting content width and margins based on [DeviceProfile].
 */
class IconPackPickerBottomSheet(
    context: Context,
    attrs: AttributeSet?,
) :
    AbstractSlideInView<Launcher>(context, attrs, 0),
    Insettable,
    OnDeviceProfileChangeListener {

    private lateinit var recyclerView: RecyclerView
    private val mInsets = Rect()

    override fun onFinishInflate() {
        super.onFinishInflate()
        mContent = findViewById(R.id.bliss_icon_pack_picker_sheet)
        recyclerView = findViewById(R.id.bliss_icon_pack_grid)
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
        return type and TYPE_ICON_PACK_PICKER != 0
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
        return if (mActivityContext.deviceProfile.deviceProperties.isTablet) {
            Interpolators.EMPHASIZED
        } else {
            super.getIdleInterpolator()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dp = mActivityContext.deviceProfile
        val widthUsed: Int =
            when {
                dp.deviceProperties.isTablet -> {
                    maxOf(
                        2 * dp.allAppsLeftRightMargin,
                        2 * (mInsets.left + mInsets.right),
                    )
                }
                mInsets.bottom > 0 -> {
                    mInsets.left + mInsets.right
                }
                else -> {
                    val padding = dp.mWorkspaceProfile.workspacePadding
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

    private fun populateIconPacks() {
        val iconPackManager = context.appComponent.iconPackManager
        val themePreference = context.appComponent.themePreference

        recyclerView.layoutManager = LinearLayoutManager(context)

        val spacing = resources.getDimensionPixelSize(R.dimen.bliss_icon_pack_card_margin)
        recyclerView.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    outRect.set(0, spacing, 0, spacing)
                }
            }
        )

        val installedPacks = iconPackManager.getInstalledIconPacks()
        val systemDefault =
            IconPackInfo(
                packageName = "",
                label = context.getString(R.string.bliss_icon_pack_system_default),
                icon = context.packageManager.getApplicationIcon(context.packageName),
                previewIcons = iconPackManager.loadSystemDefaultPreviewIcons(),
            )
        val allPacks = listOf(systemDefault) + installedPacks
        val activePackage = iconPackManager.getActiveIconPackPackage()

        recyclerView.adapter =
            IconPackPreviewAdapter(allPacks, activePackage) { selectedPack ->
                applyIconPack(themePreference, selectedPack)
            }
    }

    private fun applyIconPack(themePreference: ThemePreference, pack: IconPackInfo) {
        if (pack.isSystemDefault) {
            themePreference.setValue(null)
        } else {
            themePreference.setValue(ThemeValue(ICON_PACK_FACTORY_ID, pack.packageName))
        }
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
        fun show(launcher: Launcher): IconPackPickerBottomSheet {
            closeAllOpenViews(launcher, true)
            val sheet =
                launcher.layoutInflater.inflate(
                    R.layout.bliss_icon_pack_picker_bottom_sheet,
                    launcher.dragLayer,
                    false,
                ) as IconPackPickerBottomSheet
            sheet.populateIconPacks()
            sheet.attachToContainer()
            sheet.animateOpen()
            return sheet
        }

        @JvmStatic
        fun isIconPackPickerAvailable(@Suppress("UNUSED_PARAMETER") launcher: Launcher): Boolean {
            return true
        }
    }
}
