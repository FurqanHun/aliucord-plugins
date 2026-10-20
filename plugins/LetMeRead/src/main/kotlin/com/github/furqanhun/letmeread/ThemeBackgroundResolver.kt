package com.github.furqanhun.letmeread

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import com.aliucord.Utils
import com.discord.utilities.color.ColorCompat

object ThemeBackgroundResolver {

    private var cachedWallpaperBitmap: Bitmap? = null
    private var cachedOverallChatBg: Int? = null
    private var cachedMemberSidebarBg: Int? = null
    private val spatialGridCache = IntArray(128)

    fun invalidate() {
        cachedWallpaperBitmap = null
        cachedOverallChatBg = null
        cachedMemberSidebarBg = null
        spatialGridCache.fill(0)
        ColorContrastEngine.clearCache()
    }

    fun getChatOverallBackgroundColor(context: Context): Int {
        cachedOverallChatBg?.let { return it }

        val engineColor = getEngineBackgroundColor(isSecondary = false)
        val wp = getWallpaperBitmap()

        val bg = if (wp != null) {
            val avgWpColor = getAverageWallpaperColor(wp)
            val baseWp = applyWallpaperOverlay(avgWpColor)
            if (engineColor != null) {
                ColorUtils.compositeColors(engineColor, baseWp)
            } else {
                baseWp
            }
        } else if (engineColor != null) {
            if (Color.alpha(engineColor) == 255) {
                engineColor
            } else {
                val discordBase = getThemedColor(context, "colorBackgroundPrimary")
                ColorUtils.compositeColors(engineColor, discordBase)
            }
        } else {
            val baseColor = getThemedColor(context, "colorBackgroundPrimary")
            applyWallpaperOverlay(baseColor)
        }

        cachedOverallChatBg = bg
        return bg
    }

    private fun getAverageWallpaperColor(bitmap: Bitmap): Int {
        return try {
            val bw = bitmap.width
            val bh = bitmap.height
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0

            for (xi in 1..4) {
                for (yi in 1..4) {
                    val x = (bw * xi / 5).coerceIn(0, bw - 1)
                    val y = (bh * yi / 5).coerceIn(0, bh - 1)
                    val p = bitmap.getPixel(x, y)
                    rSum += Color.red(p)
                    gSum += Color.green(p)
                    bSum += Color.blue(p)
                    count++
                }
            }
            Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
        } catch (_: Throwable) {
            0xFF121212.toInt()
        }
    }

    fun getChatBackgroundColor(view: View): Int {
        findViewBackgroundColor(view)?.let { return it }

        val engineColor = getEngineBackgroundColor(isSecondary = false)
        val wpColor = getWallpaperColor(view)

        if (wpColor != null) {
            val baseWp = applyWallpaperOverlay(wpColor)
            return if (engineColor != null) {
                ColorUtils.compositeColors(engineColor, baseWp)
            } else {
                baseWp
            }
        }

        if (engineColor != null) {
            if (Color.alpha(engineColor) == 255) {
                return engineColor
            }
            val discordBase = getThemedColor(view.context, "colorBackgroundPrimary")
            return ColorUtils.compositeColors(engineColor, discordBase)
        }

        val baseColor = getThemedColor(view.context, "colorBackgroundPrimary")
        return applyWallpaperOverlay(baseColor)
    }

    fun getMemberSidebarBackgroundColor(view: View): Int {
        cachedMemberSidebarBg?.let { return it }

        val bg = findViewBackgroundColor(view)?.let { bg ->
            if (Color.alpha(bg) == 255) return@let bg
            val engineColor = getEngineBackgroundColor(isSecondary = true)
                ?: getEngineBackgroundColor(isSecondary = false)
            val base = engineColor ?: getThemedColor(view.context, "colorBackgroundPrimary")
            ColorUtils.compositeColors(bg, base)
        } ?: run {
            val engineColor = getEngineBackgroundColor(isSecondary = true)
                ?: getEngineBackgroundColor(isSecondary = false)
            if (engineColor != null) {
                if (Color.alpha(engineColor) == 255) return@run engineColor
                val discordBase = getThemedColor(view.context, "colorBackgroundPrimary")
                ColorUtils.compositeColors(engineColor, discordBase)
            } else {
                getThemedColor(view.context, "colorBackgroundPrimary")
            }
        }

        cachedMemberSidebarBg = bg
        return bg
    }

    private fun findViewBackgroundColor(view: View): Int? {
        val highlightId = Utils.getResId("chat_list_adapter_item_highlighted_bg", "id")
        if (highlightId != 0) {
            val highlightView = view.findViewById<View>(highlightId)
            if (highlightView != null && highlightView.visibility == View.VISIBLE) {
                val hlBg = highlightView.background
                if (hlBg != null) {
                    val color = extractDrawableColor(hlBg, view)
                    if (color != null) {
                        return ensureOpaque(color, view)
                    }
                }
            }
        }

        var current: View? = view
        var depth = 0
        while (current != null && depth < 12) {
            val bg = current.background
            if (bg != null) {
                val color = extractDrawableColor(bg, view)
                if (color != null && Color.alpha(color) >= 120) {
                    return ensureOpaque(color, view)
                }
            }
            val parent = current.parent
            current = if (parent is View) parent else null
            depth++
        }
        return null
    }

    private fun ensureOpaque(color: Int, view: View): Int {
        if (Color.alpha(color) == 255) return color
        val base = getWallpaperColor(view)?.let { applyWallpaperOverlay(it) }
            ?: getThemedColor(view.context, "colorBackgroundPrimary")
        return ColorUtils.compositeColors(color, base)
    }

    private fun extractDrawableColor(drawable: Drawable, view: View): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is android.graphics.drawable.RippleDrawable) {
            return null
        }
        when (drawable) {
            is ColorDrawable -> {
                if (drawable.color != 0) return drawable.color
            }
            is BitmapDrawable -> {
                drawable.bitmap?.let { return applyWallpaperOverlay(getSpatialWallpaperColor(it, view)) }
            }
            is LayerDrawable -> {
                for (i in 0 until drawable.numberOfLayers) {
                    if (drawable.getId(i) == android.R.id.mask) continue
                    val child = drawable.getDrawable(i)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && child is android.graphics.drawable.RippleDrawable) {
                        continue
                    }
                    extractDrawableColor(child, view)?.let { return it }
                }
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && drawable is GradientDrawable) {
                    drawable.color?.defaultColor?.let {
                        if (it != 0) return it
                    }
                }
            }
        }
        return null
    }

    private fun getEngineBackgroundColor(isSecondary: Boolean): Int? {
        getThemerBackgroundColor(isSecondary)?.let { return it }
        getYutaBackgroundColor(isSecondary)?.let { return it }
        return null
    }

    private fun getThemerBackgroundColor(isSecondary: Boolean): Int? {
        try {
            val rmClass = Class.forName("dev.vendicated.aliucordplugs.themer.ResourceManager")
            val getColorMethod = rmClass.getDeclaredMethod("getColorForName", String::class.java)

            val keys = if (isSecondary) {
                arrayOf(
                    "primary_700",
                    "background_secondary",
                    "colorBackgroundSecondary",
                    "primary_dark_700",
                    "primary_630",
                    "primary_dark_660",
                )
            } else {
                arrayOf(
                    "primary_dark_600",
                    "background",
                    "colorBackgroundPrimary",
                    "primary_600",
                    "primary_660",
                    "primary_800",
                    "primary_dark_630",
                    "primary_dark_800",
                    "dark_grey_2",
                )
            }

            for (key in keys) {
                val color = getColorMethod.invoke(null, key) as? Int
                if (color != null && color != 0) {
                    return color
                }
            }

            val attrName = if (isSecondary) "colorBackgroundSecondary" else "colorBackgroundPrimary"
            val attrId = Utils.getResId(attrName, "attr")
            if (attrId != 0) {
                val getAttrMethod = rmClass.getDeclaredMethod("getAttrForId", Int::class.javaPrimitiveType)
                val attrColor = getAttrMethod.invoke(null, attrId) as? Int
                if (attrColor != null && attrColor != 0) {
                    return attrColor
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    private fun getYutaBackgroundColor(isSecondary: Boolean): Int? {
        try {
            val ceClass = Class.forName("com.github.yutaplug.themes.ColorEngine")
            val state = ceClass.getDeclaredField("state").apply { isAccessible = true }.get(null) ?: return null
            val colorsField = try {
                state.javaClass.getDeclaredField("colors").apply { isAccessible = true }.get(state) as? Map<*, *>
            } catch (_: Throwable) {
                null
            }
            if (colorsField != null) {
                val keys = if (isSecondary) {
                    arrayOf("primary_700", "background_secondary", "colorBackgroundSecondary", "primary_dark_700")
                } else {
                    arrayOf("primary_dark_600", "background", "colorBackgroundPrimary", "primary_600", "primary_800")
                }
                for (key in keys) {
                    val color = colorsField[key] as? Int
                    if (color != null && color != 0) {
                        return color
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    private fun getWallpaperBitmap(): Bitmap? {
        try {
            val rmClass = Class.forName("dev.vendicated.aliucordplugs.themer.ResourceManager")
            val customBg = rmClass.getDeclaredField("customBg").apply { isAccessible = true }.get(null)
            if (customBg is BitmapDrawable) {
                return customBg.bitmap
            }
        } catch (_: Throwable) {}

        try {
            val ceClass = Class.forName("com.github.yutaplug.themes.ColorEngine")
            val state = ceClass.getDeclaredField("state").apply { isAccessible = true }.get(null)
            if (state != null) {
                for (field in state.javaClass.declaredFields) {
                    field.isAccessible = true
                    val value = field.get(state)
                    if (value is Bitmap) {
                        return value
                    } else if (value is BitmapDrawable) {
                        return value.bitmap
                    }
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun getWallpaperColor(view: View): Int? {
        val bitmap = getWallpaperBitmap() ?: return null
        return getSpatialWallpaperColor(bitmap, view)
    }

    private fun getSpatialWallpaperColor(bitmap: Bitmap, view: View): Int {
        if (cachedWallpaperBitmap !== bitmap) {
            cachedWallpaperBitmap = bitmap
            spatialGridCache.fill(0)
        }

        val displayMetrics = view.context.resources.displayMetrics
        val screenW = displayMetrics.widthPixels.coerceAtLeast(1)
        val screenH = displayMetrics.heightPixels.coerceAtLeast(1)

        val location = IntArray(2)
        view.getLocationOnScreen(location)

        val viewX = (location[0] + view.width / 2).coerceIn(0, screenW)
        val viewY = (location[1] + view.height / 2).coerceIn(0, screenH)

        val col = ((viewX.toFloat() / screenW) * 8).toInt().coerceIn(0, 7)
        val row = ((viewY.toFloat() / screenH) * 16).toInt().coerceIn(0, 15)
        val cellIndex = row * 8 + col

        val cached = spatialGridCache[cellIndex]
        if (cached != 0) return cached

        val normX = (col + 0.5f) / 8f
        val normY = (row + 0.5f) / 16f

        val bw = bitmap.width
        val bh = bitmap.height
        val px = (normX * bw).toInt().coerceIn(0, bw - 1)
        val py = (normY * bh).toInt().coerceIn(0, bh - 1)

        val sample = sampleBitmapArea(bitmap, px, py)
        spatialGridCache[cellIndex] = sample
        return sample
    }

    private fun sampleBitmapArea(bitmap: Bitmap, cx: Int, cy: Int): Int {
        return try {
            val bw = bitmap.width
            val bh = bitmap.height
            val offsets = intArrayOf(-2, 0, 2)
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var count = 0

            for (dx in offsets) {
                for (dy in offsets) {
                    val x = (cx + dx).coerceIn(0, bw - 1)
                    val y = (cy + dy).coerceIn(0, bh - 1)
                    val p = bitmap.getPixel(x, y)
                    rSum += Color.red(p)
                    gSum += Color.green(p)
                    bSum += Color.blue(p)
                    count++
                }
            }
            Color.rgb(rSum / count, gSum / count, bSum / count)
        } catch (_: Throwable) {
            0xFF121212.toInt()
        }
    }

    private fun getThemedColor(context: Context, attrName: String): Int {
        val attrId = Utils.getResId(attrName, "attr")
        if (attrId != 0) {
            val color = ColorCompat.getThemedColor(context, attrId)
            if (color != 0) return color
        }

        return if (isCurrentThemeLight(context)) {
            0xFFFFFFFF.toInt()
        } else {
            0xFF36393F.toInt()
        }
    }

    private fun isCurrentThemeLight(context: Context): Boolean {
        return try {
            val storeStreamClass = Class.forName("com.discord.stores.StoreStream")
            val getStoreMethod = storeStreamClass.getMethod("getUserSettingsSystem")
            val store = getStoreMethod.invoke(null)
            val getThemeMethod = store.javaClass.getMethod("getTheme")
            val theme = getThemeMethod.invoke(store) as? String
            theme == "light"
        } catch (_: Throwable) {
            val nightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightMode == android.content.res.Configuration.UI_MODE_NIGHT_NO
        }
    }

    private fun applyWallpaperOverlay(baseColor: Int): Int {
        val overlayAlpha = getWallpaperOverlayAlpha() ?: return baseColor
        val alpha = overlayAlpha.coerceIn(0, 255)
        if (alpha == 0) return baseColor
        val scrim = ColorUtils.setAlphaComponent(Color.BLACK, alpha)
        return ColorUtils.compositeColors(scrim, baseColor)
    }

    private fun getWallpaperOverlayAlpha(): Int? {
        try {
            val rmClass = Class.forName("dev.vendicated.aliucordplugs.themer.ResourceManager")
            val customBg = rmClass.getDeclaredField("customBg").apply { isAccessible = true }.get(null)
            val animatedBgUri = rmClass.getDeclaredField("animatedBgUri").apply { isAccessible = true }.get(null)
            if (customBg != null || animatedBgUri != null) {
                return rmClass.getDeclaredField("overlayAlpha").apply { isAccessible = true }.getInt(null)
            }
        } catch (_: Throwable) {}

        try {
            val ceClass = Class.forName("com.github.yutaplug.themes.ColorEngine")
            val state = ceClass.getDeclaredField("state").apply { isAccessible = true }.get(null) ?: return null
            val hasBg = state.javaClass.getMethod("getHasBackground").invoke(state) as? Boolean ?: false
            if (hasBg) {
                return state.javaClass.getMethod("getOverlayAlpha").invoke(state) as? Int ?: 150
            }
        } catch (_: Throwable) {}

        return null
    }
}
