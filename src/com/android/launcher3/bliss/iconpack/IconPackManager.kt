/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.Log
import com.android.launcher3.bliss.iconpack.IconPackThemeFactory.ICON_PACK_FACTORY_ID
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.graphics.theme.ThemePreference
import com.android.launcher3.util.DaggerSingletonTracker
import com.android.launcher3.util.Executors.MODEL_EXECUTOR
import com.android.launcher3.util.SimpleBroadcastReceiver
import com.android.launcher3.util.SimpleBroadcastReceiver.Companion.packageFilter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Singleton manager for icon pack discovery, parsing, and icon resolution.
 *
 * Discovers installed icon packs via standard ADW/Nova intent actions, parses their
 * `appfilter.xml` files, and provides icon lookups by [ComponentName].
 */
@LauncherAppSingleton
class IconPackManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val themePreference: ThemePreference,
    lifecycle: DaggerSingletonTracker,
) {

    private val packageManager: PackageManager = context.packageManager

    /** Cached parsed appfilter mappings keyed by icon pack package name. */
    private val cachedMappings = ConcurrentHashMap<String, Map<ComponentName, String>>()

    /** Cached mask/back/upon configs keyed by icon pack package name. */
    private val cachedMaskConfigs = ConcurrentHashMap<String, IconPackMaskConfig>()

    /** Cached calendar icon configs keyed by icon pack package name. */
    private val cachedCalendarConfigs = ConcurrentHashMap<String, List<CalendarIconConfig>>()

    /** Cached Resources for icon pack packages. */
    private val cachedResources = ConcurrentHashMap<String, Resources>()

    init {
        val receiver =
            SimpleBroadcastReceiver(context, MODEL_EXECUTOR) { intent ->
                val packageName =
                    intent.data?.schemeSpecificPart ?: return@SimpleBroadcastReceiver
                onPackageChanged(packageName)
            }
        receiver.register(
            packageFilter(
                null,
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED,
            )
        )
        lifecycle.addCloseable(receiver)
    }

    /**
     * Returns the package name of the currently active icon pack, or null if system default.
     */
    fun getActiveIconPackPackage(): String? {
        val value = themePreference.value ?: return null
        return if (value.factoryId == ICON_PACK_FACTORY_ID) value.themeId else null
    }

    /**
     * Discovers all installed icon packs on the device.
     *
     * Queries the package manager for apps declaring the standard ADW/Nova icon pack intent
     * actions. Each result includes sample preview icons for well-known apps.
     *
     * @return A list of [IconPackInfo] representing available icon packs. Does NOT include
     *   the system default entry (the caller should prepend it).
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val packs = mutableMapOf<String, IconPackInfo>()

        for (action in ICON_PACK_INTENT_ACTIONS) {
            val intent = Intent(action)
            val resolveInfos =
                packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) continue
                if (packs.containsKey(pkg)) continue

                packs[pkg] =
                    IconPackInfo(
                        packageName = pkg,
                        label = ri.loadLabel(packageManager),
                        icon = ri.loadIcon(packageManager),
                        previewIcons = loadPreviewIcons(pkg),
                    )
            }
        }

        return packs.values.sortedBy { it.label.toString().lowercase() }
    }

    /**
     * Loads the system default icons for the preview categories.
     *
     * For each category, tries all known package names until one is found installed on the device.
     *
     * @return A list of drawables from the system icon set for each preview category.
     */
    fun loadSystemDefaultPreviewIcons(): List<Drawable?> {
        return PREVIEW_COMPONENT_CANDIDATES.map { candidates ->
            candidates.firstNotNullOfOrNull { component ->
                try {
                    val appInfo =
                        packageManager.getApplicationInfo(component.packageName, 0)
                    packageManager.getApplicationIcon(appInfo)
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
        }
    }

    /**
     * Computes icon coverage data for a given icon pack against installed launcher activities.
     *
     * For each installed app, checks whether the icon pack has a themed icon mapping and loads
     * both the original system icon and the themed icon (if available).
     *
     * @param iconPackPackage The icon pack's package name.
     * @return An [IconPackCoverageData] with matched/total counts and per-app icon pairs.
     */
    fun getIconPackCoverage(iconPackPackage: String): IconPackCoverageData {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activities = launcherApps.getActivityList(null, Process.myUserHandle())
        val mapping = getOrParseMappings(iconPackPackage)
        val density = context.resources.displayMetrics.densityDpi

        val items = mutableListOf<IconPackCoverageItem>()
        var themedCount = 0

        for (activityInfo in activities) {
            val cn = activityInfo.componentName
            val appLabel = activityInfo.label?.toString() ?: cn.packageName
            val originalIcon = activityInfo.getIcon(density)
            val drawableName = mapping[cn]
            val themedIcon = if (drawableName != null) {
                loadDrawableFromPack(iconPackPackage, drawableName, density)
            } else {
                null
            }

            if (themedIcon != null) themedCount++

            items.add(
                IconPackCoverageItem(
                    componentName = cn,
                    label = appLabel,
                    originalIcon = originalIcon,
                    themedIcon = themedIcon,
                )
            )
        }

        // Sort: themed icons first, then alphabetically
        items.sortWith(compareByDescending<IconPackCoverageItem> { it.themedIcon != null }
            .thenBy { it.label.lowercase() })

        return IconPackCoverageData(
            totalApps = activities.size,
            themedApps = themedCount,
            items = items,
        )
    }

    /**
     * Loads a dynamic calendar icon for a [ComponentName] from the active icon pack.
     *
     * Checks if the active icon pack defines a `<calendar>` entry for this component. If so,
     * loads the day-specific drawable (e.g., `"calendar_15"` for the 15th).
     *
     * @param componentName The component to look up.
     * @param density The target display density.
     * @return The calendar day icon, or null if no calendar config exists for this component.
     */
    fun loadCalendarIcon(componentName: ComponentName, density: Int): Drawable? {
        val activePackage = getActiveIconPackPackage() ?: return null

        // Ensure configs are parsed
        getOrParseMappings(activePackage)
        val calendarConfigs = cachedCalendarConfigs[activePackage] ?: return null

        val config = calendarConfigs.find { it.componentName == componentName } ?: return null
        val dayOfMonth = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        val drawableName = "${config.drawablePrefix}$dayOfMonth"

        return loadDrawableFromPack(activePackage, drawableName, density)
    }

    /**
     * Loads the icon drawable for a [ComponentName] from the active icon pack.
     *
     * @param componentName The component to look up.
     * @param density The target display density.
     * @return The icon pack drawable, or null if no mapping exists (caller falls back to system).
     */
    fun loadIconForComponent(componentName: ComponentName, density: Int): Drawable? {
        val activePackage = getActiveIconPackPackage() ?: return null

        val mapping = getOrParseMappings(activePackage)
        val drawableName = mapping[componentName] ?: return null

        return loadDrawableFromPack(activePackage, drawableName, density)
    }

    /**
     * Applies the active icon pack's mask/back/upon layers to an unthemed icon.
     *
     * Called when the active icon pack has no specific mapping for an app, but defines global
     * compositing layers (iconback, iconmask, iconupon) to style unthemed icons for visual
     * consistency.
     *
     * @param originalIcon The app's original system icon.
     * @param componentName The component for deterministic layer variant selection.
     * @param density The target display density.
     * @return A [IconPackMaskedDrawable] with layers applied, or null if no masking is available.
     */
    fun applyMaskToIcon(
        originalIcon: Drawable,
        componentName: ComponentName,
        density: Int,
    ): Drawable? {
        val activePackage = getActiveIconPackPackage() ?: return null

        // Ensure mappings (and mask config) are parsed
        getOrParseMappings(activePackage)
        val config = cachedMaskConfigs[activePackage] ?: return null
        if (!config.hasMaskingLayers) return null

        val seed = componentName.hashCode()
        val backDrawable = selectLayer(activePackage, config.iconBackDrawables, seed, density)
        val maskDrawable = selectLayer(activePackage, config.iconMaskDrawables, seed, density)
        val uponDrawable = selectLayer(activePackage, config.iconUponDrawables, seed, density)

        return IconPackMaskedDrawable(
            originalIcon = originalIcon,
            backDrawable = backDrawable,
            maskDrawable = maskDrawable,
            uponDrawable = uponDrawable,
            scaleFactor = config.scaleFactor,
        )
    }

    /**
     * Selects and loads a drawable from a list of layer resource names.
     *
     * Uses the [seed] (typically a component hashcode) for deterministic selection — the same
     * app always gets the same background/overlay variant, providing visual variety across
     * icons while keeping each icon's appearance stable.
     */
    private fun selectLayer(
        iconPackPackage: String,
        drawableNames: List<String>,
        seed: Int,
        density: Int,
    ): Drawable? {
        if (drawableNames.isEmpty()) return null
        val index = ((seed.toLong() and 0xFFFFFFFFL) % drawableNames.size).toInt()
        return loadDrawableFromPack(iconPackPackage, drawableNames[index], density)
    }

    private fun getOrParseMappings(iconPackPackage: String): Map<ComponentName, String> {
        return cachedMappings.getOrPut(iconPackPackage) {
            val resources = getPackResources(iconPackPackage) ?: return@getOrPut emptyMap()
            val result = IconPackParser.parseAppFilterFull(resources, iconPackPackage)
            cachedMaskConfigs[iconPackPackage] = result.maskConfig
            cachedCalendarConfigs[iconPackPackage] = result.calendarIcons
            result.mappings
        }
    }

    private fun loadDrawableFromPack(
        iconPackPackage: String,
        drawableName: String,
        density: Int,
    ): Drawable? {
        val resources = getPackResources(iconPackPackage) ?: return null
        val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
        if (resId == 0) return null

        return try {
            resources.getDrawableForDensity(resId, density, null)
        } catch (e: Resources.NotFoundException) {
            Log.w(TAG, "Drawable $drawableName not found in $iconPackPackage", e)
            null
        }
    }

    private fun getPackResources(iconPackPackage: String): Resources? {
        return cachedResources.getOrPut(iconPackPackage) {
            try {
                packageManager.getResourcesForApplication(iconPackPackage)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Icon pack package not found: $iconPackPackage", e)
                return null
            }
        }
    }

    /**
     * Loads sample themed icons from an icon pack for each preview category.
     *
     * For each category (Phone, Messages, Camera, Settings), tries all known component name
     * variants until a match is found in the icon pack's appfilter mapping.
     */
    private fun loadPreviewIcons(iconPackPackage: String): List<Drawable?> {
        val mapping = getOrParseMappings(iconPackPackage)
        if (mapping.isEmpty()) return emptyList()

        val density = context.resources.displayMetrics.densityDpi
        return PREVIEW_COMPONENT_CANDIDATES.map { candidates ->
            val drawableName = candidates.firstNotNullOfOrNull { mapping[it] }
            if (drawableName != null) {
                loadDrawableFromPack(iconPackPackage, drawableName, density)
            } else {
                null
            }
        }
    }

    private fun onPackageChanged(packageName: String) {
        cachedMappings.remove(packageName)
        cachedMaskConfigs.remove(packageName)
        cachedCalendarConfigs.remove(packageName)
        cachedResources.remove(packageName)

        val activePackage = getActiveIconPackPackage() ?: return
        if (activePackage != packageName) return

        if (!isPackageInstalled(packageName)) {
            Log.i(TAG, "Active icon pack $packageName was removed, reverting to default")
            themePreference.setValue(null)
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private const val TAG = "IconPackManager"

        /** Standard intent actions used by icon packs to advertise themselves. */
        private val ICON_PACK_INTENT_ACTIONS =
            listOf(
                "org.adw.launcher.THEMES",
                "com.novalauncher.THEME",
                "com.gau.go.launcherex.theme",
            )

        /**
         * Well-known app components used for icon pack preview. Each inner list contains
         * alternative component names for the same app category (e.g., Google Dialer vs AOSP
         * Dialer). The first match found in an icon pack's appfilter is used.
         */
        private val PREVIEW_COMPONENT_CANDIDATES =
            listOf(
                // Phone / Dialer
                listOf(
                    ComponentName(
                        "com.google.android.dialer",
                        "com.google.android.dialer.extensions.GoogleDialtactsActivity",
                    ),
                    ComponentName(
                        "com.android.dialer",
                        "com.android.dialer.main.impl.MainActivity",
                    ),
                    ComponentName("com.android.dialer", "com.android.dialer.DialtactsActivity"),
                ),
                // Messages
                listOf(
                    ComponentName(
                        "com.google.android.apps.messaging",
                        "com.google.android.apps.messaging.ui.ConversationListActivity",
                    ),
                    ComponentName(
                        "com.android.messaging",
                        "com.android.messaging.ui.conversationlist.ConversationListActivity",
                    ),
                ),
                // Camera
                listOf(
                    ComponentName(
                        "com.google.android.GoogleCamera",
                        "com.android.camera.CameraLauncher",
                    ),
                    ComponentName(
                        "com.android.camera2",
                        "com.android.camera.CameraLauncher",
                    ),
                    ComponentName("com.android.camera", "com.android.camera.Camera"),
                ),
                // Settings
                listOf(
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings",
                    ),
                ),
            )

        /**
         * Flat list of the first component from each category, used for system default preview.
         */
        val PREVIEW_COMPONENTS: List<ComponentName> =
            PREVIEW_COMPONENT_CANDIDATES.map { it.first() }
    }
}
