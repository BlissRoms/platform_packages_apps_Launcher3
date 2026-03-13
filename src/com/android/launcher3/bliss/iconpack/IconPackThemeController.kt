/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import com.android.launcher3.icons.BaseIconFactory
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.IconThemeController
import com.android.launcher3.icons.SourceHint
import com.android.launcher3.icons.ThemedBitmap

/**
 * Lightweight [IconThemeController] marker for icon packs.
 *
 * This controller does NOT produce themed bitmaps — icon pack drawables are loaded at the
 * [loadPackageIcon][com.android.launcher3.icons.LauncherIconProviderImpl.loadPackageIcon] level,
 * before the bitmap factory processes them. This controller exists so that:
 *
 * 1. `ThemeManager.iconState.themeCode` changes when the icon pack selection changes,
 *    triggering the existing cache invalidation and model reload chain.
 * 2. The theme system recognizes that an icon theme is active.
 *
 * @param iconPackPackage The package name of the selected icon pack.
 */
class IconPackThemeController(
    val iconPackPackage: String,
) : IconThemeController {

    override val themeID: String = iconPackPackage

    override fun createThemedBitmap(
        icon: AdaptiveIconDrawable,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint?,
    ): ThemedBitmap = ThemedBitmap.NOT_SUPPORTED

    override fun createThemedAdaptiveIcon(
        context: Context,
        icon: AdaptiveIconDrawable,
        bitmapInfo: BitmapInfo?,
    ): AdaptiveIconDrawable = icon

    override fun decode(
        data: ByteArray,
        iconInfo: BitmapInfo,
        iconFactory: BaseIconFactory,
        sourceHint: SourceHint,
    ): ThemedBitmap = ThemedBitmap.NOT_SUPPORTED
}
