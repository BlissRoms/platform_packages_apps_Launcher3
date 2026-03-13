/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.content.ComponentName
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Log
import org.xmlpull.v1.XmlPullParser

/**
 * Parses the ADW/Nova-format `appfilter.xml` from an icon pack APK.
 *
 * Extracts two types of data:
 * 1. Per-app icon mappings: [ComponentName] to drawable resource name
 * 2. Global compositing config: mask, back, upon layers and scale factor for unthemed icons
 */
object IconPackParser {

    private const val TAG = "IconPackParser"

    private const val TAG_ITEM = "item"
    private const val TAG_ICONBACK = "iconback"
    private const val TAG_ICONMASK = "iconmask"
    private const val TAG_ICONUPON = "iconupon"
    private const val TAG_SCALE = "scale"
    private const val ATTR_COMPONENT = "component"
    private const val ATTR_DRAWABLE = "drawable"
    private const val ATTR_FACTOR = "factor"
    private const val DEFAULT_SCALE_FACTOR = 1.0f
    private const val MAX_IMG_ATTRIBUTES = 8

    /** Combined result of parsing an icon pack's appfilter.xml. */
    data class ParseResult(
        val mappings: Map<ComponentName, String>,
        val maskConfig: IconPackMaskConfig,
    )

    /**
     * Parses `appfilter.xml` from the given icon pack [resources].
     *
     * @param resources Resources obtained via `PackageManager.getResourcesForApplication()`
     * @param iconPackPackage The icon pack's package name (for resource lookup).
     * @return A [ParseResult] containing both per-app mappings and global mask config.
     */
    fun parseAppFilterFull(
        resources: Resources,
        iconPackPackage: String,
    ): ParseResult {
        val appFilterId = resources.getIdentifier("appfilter", "xml", iconPackPackage)
        if (appFilterId == 0) {
            Log.w(TAG, "No appfilter.xml found in $iconPackPackage")
            return ParseResult(emptyMap(), IconPackMaskConfig.EMPTY)
        }

        val mappings = mutableMapOf<ComponentName, String>()
        val iconBackImages = mutableListOf<String>()
        val iconMaskImages = mutableListOf<String>()
        val iconUponImages = mutableListOf<String>()
        var scaleFactor = DEFAULT_SCALE_FACTOR

        try {
            resources.getXml(appFilterId).use { parser ->
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) continue

                    when (parser.name) {
                        TAG_ITEM -> parseItem(parser, mappings)
                        TAG_ICONBACK -> parseImgAttributes(parser, iconBackImages)
                        TAG_ICONMASK -> parseImgAttributes(parser, iconMaskImages)
                        TAG_ICONUPON -> parseImgAttributes(parser, iconUponImages)
                        TAG_SCALE -> {
                            scaleFactor = parser.getAttributeValue(null, ATTR_FACTOR)
                                ?.toFloatOrNull() ?: DEFAULT_SCALE_FACTOR
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse appfilter.xml from $iconPackPackage", e)
        }

        val maskConfig = IconPackMaskConfig(
            iconBackDrawables = iconBackImages,
            iconMaskDrawables = iconMaskImages,
            iconUponDrawables = iconUponImages,
            scaleFactor = scaleFactor,
        )
        return ParseResult(mappings, maskConfig)
    }

    /**
     * Parses `appfilter.xml` and returns only the per-app icon mappings.
     *
     * @param resources Resources obtained via `PackageManager.getResourcesForApplication()`
     * @param iconPackPackage The icon pack's package name (for resource lookup).
     * @return A map of [ComponentName] to drawable name, or an empty map on failure.
     */
    fun parseAppFilter(
        resources: Resources,
        iconPackPackage: String,
    ): Map<ComponentName, String> = parseAppFilterFull(resources, iconPackPackage).mappings

    private fun parseItem(parser: XmlResourceParser, out: MutableMap<ComponentName, String>) {
        val componentStr = parser.getAttributeValue(null, ATTR_COMPONENT) ?: return
        val drawableName = parser.getAttributeValue(null, ATTR_DRAWABLE) ?: return
        if (drawableName.isEmpty()) return

        val componentName = parseComponentName(componentStr) ?: return
        out[componentName] = drawableName
    }

    /**
     * Extracts `img1`, `img2`, ..., `img8` attribute values from an element.
     *
     * Icon packs use numbered `img` attributes to provide multiple layer variants (e.g.,
     * `<iconback img1="back1" img2="back2" />`). A random variant is chosen per icon.
     */
    private fun parseImgAttributes(parser: XmlResourceParser, out: MutableList<String>) {
        for (i in 1..MAX_IMG_ATTRIBUTES) {
            val value = parser.getAttributeValue(null, "img$i") ?: break
            if (value.isNotEmpty()) out.add(value)
        }
        // Some icon packs use "img" without a number for single-image elements
        if (out.isEmpty()) {
            val value = parser.getAttributeValue(null, "img")
            if (!value.isNullOrEmpty()) out.add(value)
        }
    }

    /**
     * Parses a component string from appfilter.xml. Handles both formats:
     * - `ComponentInfo{com.example.app/com.example.app.MainActivity}`
     * - `com.example.app/com.example.app.MainActivity`
     */
    private fun parseComponentName(raw: String): ComponentName? {
        val stripped = raw
            .removePrefix("ComponentInfo{")
            .removeSuffix("}")
            .trim()

        val slashIndex = stripped.indexOf('/')
        if (slashIndex <= 0 || slashIndex >= stripped.length - 1) return null

        val pkg = stripped.substring(0, slashIndex)
        val cls = stripped.substring(slashIndex + 1)
        return ComponentName(pkg, cls)
    }
}
