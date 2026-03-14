/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.content.ComponentName

/**
 * Configuration for a dynamic calendar icon from an icon pack.
 *
 * Icon packs define calendar icons via `<calendar>` tags in `appfilter.xml`:
 * ```xml
 * <calendar component="ComponentInfo{pkg/cls}" prefix="calendar_" />
 * ```
 *
 * For day N (1–31), the drawable name is `"{prefix}{N}"` (e.g., `"calendar_15"` for the 15th).
 *
 * @param componentName The calendar app's launcher activity component.
 * @param drawablePrefix The prefix for day-specific drawables.
 */
data class CalendarIconConfig(
    val componentName: ComponentName,
    val drawablePrefix: String,
)
