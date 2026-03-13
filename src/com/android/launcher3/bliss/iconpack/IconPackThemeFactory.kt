/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import com.android.launcher3.graphics.theme.IconThemeFactory
import com.android.launcher3.icons.IconThemeController
import com.android.launcher3.logging.StatsLogManager.StatsLogger

/**
 * Factory that creates [IconPackThemeController] instances for Dagger's icon theme factory map.
 *
 * Registered under [ICON_PACK_FACTORY_ID] so that selecting an icon pack via
 * `ThemePreference.setValue(ThemeValue("icon-pack", packageName))` triggers the existing
 * theme change → cache invalidation → model reload chain automatically.
 */
object IconPackThemeFactory : IconThemeFactory {

    const val ICON_PACK_FACTORY_ID = "icon-pack"

    override fun createController(themeId: String): IconThemeController =
        IconPackThemeController(themeId)

    override fun logThemeEvent(themeId: String, logger: StatsLogger) {
        // Icon pack selection logging can be extended here
    }
}
