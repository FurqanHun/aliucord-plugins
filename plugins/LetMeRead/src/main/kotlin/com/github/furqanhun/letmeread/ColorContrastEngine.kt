package com.github.furqanhun.letmeread

import android.util.LruCache
import androidx.core.graphics.ColorUtils

object ColorContrastEngine {

    const val MODE_LIGHTNESS_SHIFT = 0
    const val MODE_FALLBACK = 1

    private val cache = LruCache<Long, Int>(512)

    fun clearCache() {
        cache.evictAll()
    }

    fun ensureContrast(
        roleColor: Int,
        bgColor: Int,
        minRatio: Double,
        mode: Int = MODE_LIGHTNESS_SHIFT,
    ): Int {
        if (roleColor == 0) return roleColor

        val ratioInt = (minRatio * 10).toInt() and 0xFF
        val cacheKey = (roleColor.toLong() and 0xFFFFFFL) or
            ((bgColor.toLong() and 0xFFFFFFL) shl 24) or
            (ratioInt.toLong() shl 48) or
            ((mode.toLong() and 0xFL) shl 56)

        cache.get(cacheKey)?.let { return it }

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(roleColor, hsl)

        val isDarkBg = ColorUtils.calculateLuminance(bgColor) < 0.5
        val targetRatio = if (isDarkBg) {
            if (hsl[0] in 200f..285f) minRatio + 0.35 else minRatio
        } else {
            if (hsl[0] in 35f..95f) minRatio + 0.35 else minRatio
        }

        val currentContrast = ColorUtils.calculateContrast(roleColor, bgColor)
        if (currentContrast >= targetRatio) {
            cache.put(cacheKey, roleColor)
            return roleColor
        }

        val adjustedColor = if (mode == MODE_FALLBACK) {
            if (isDarkBg) 0xFFFFFFFF.toInt() else 0xFF1E1F22.toInt()
        } else {
            shiftLightness(roleColor, bgColor, targetRatio, isDarkBg)
        }

        cache.put(cacheKey, adjustedColor)
        return adjustedColor
    }

    fun ensureTextContrast(
        textColor: Int,
        bgColor: Int,
        minRatio: Double,
    ): Int {
        if (textColor == 0) return textColor

        val ratioInt = (minRatio * 10).toInt() and 0xFF
        val cacheKey = (textColor.toLong() and 0xFFFFFFL) or
            ((bgColor.toLong() and 0xFFFFFFL) shl 24) or
            (ratioInt.toLong() shl 48) or
            (0x2L shl 56)

        cache.get(cacheKey)?.let { return it }

        val currentContrast = ColorUtils.calculateContrast(textColor, bgColor)
        if (currentContrast >= minRatio) {
            cache.put(cacheKey, textColor)
            return textColor
        }

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(textColor, hsl)
        if (hsl[1] > 0.35f) {
            hsl[1] = 0.35f
        }

        val isDarkBg = ColorUtils.calculateLuminance(bgColor) < 0.5
        val step = if (isDarkBg) 0.03f else -0.03f

        while (ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), bgColor) < minRatio) {
            hsl[2] = (hsl[2] + step).coerceIn(0.05f, 0.98f)
            if (hsl[2] <= 0.05f || hsl[2] >= 0.98f) break
        }

        var resultColor = ColorUtils.HSLToColor(hsl)
        if (ColorUtils.calculateContrast(resultColor, bgColor) < minRatio) {
            val reverseHsl = FloatArray(3)
            ColorUtils.colorToHSL(textColor, reverseHsl)
            if (reverseHsl[1] > 0.35f) reverseHsl[1] = 0.35f
            val reverseStep = -step
            while (ColorUtils.calculateContrast(ColorUtils.HSLToColor(reverseHsl), bgColor) < minRatio) {
                reverseHsl[2] = (reverseHsl[2] + reverseStep).coerceIn(0.05f, 0.98f)
                if (reverseHsl[2] <= 0.05f || reverseHsl[2] >= 0.98f) break
            }
            val contrast1 = ColorUtils.calculateContrast(resultColor, bgColor)
            val contrast2 = ColorUtils.calculateContrast(ColorUtils.HSLToColor(reverseHsl), bgColor)
            if (contrast2 > contrast1) {
                resultColor = ColorUtils.HSLToColor(reverseHsl)
            }
        }

        cache.put(cacheKey, resultColor)
        return resultColor
    }

    private fun shiftLightness(
        color: Int,
        bgColor: Int,
        targetRatio: Double,
        isDarkBg: Boolean,
    ): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        val originalSat = hsl[1]

        val step = if (isDarkBg) 0.03f else -0.03f

        while (ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), bgColor) < targetRatio) {
            hsl[2] = (hsl[2] + step).coerceIn(0f, 1f)
            if (originalSat > 0.15f) {
                hsl[1] = originalSat.coerceIn(0.40f, 0.85f)
            }
            if (hsl[2] == 0f || hsl[2] == 1f) break
        }

        if (ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), bgColor) >= targetRatio) {
            return ColorUtils.HSLToColor(hsl)
        }

        val reverseHsl = FloatArray(3)
        ColorUtils.colorToHSL(color, reverseHsl)
        val reverseStep = -step

        while (ColorUtils.calculateContrast(ColorUtils.HSLToColor(reverseHsl), bgColor) < targetRatio) {
            reverseHsl[2] = (reverseHsl[2] + reverseStep).coerceIn(0f, 1f)
            if (originalSat > 0.15f) {
                reverseHsl[1] = originalSat.coerceIn(0.40f, 0.85f)
            }
            if (reverseHsl[2] == 0f || reverseHsl[2] == 1f) break
        }

        val contrast1 = ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), bgColor)
        val contrast2 = ColorUtils.calculateContrast(ColorUtils.HSLToColor(reverseHsl), bgColor)

        return if (contrast2 > contrast1) {
            ColorUtils.HSLToColor(reverseHsl)
        } else {
            ColorUtils.HSLToColor(hsl)
        }
    }
}
