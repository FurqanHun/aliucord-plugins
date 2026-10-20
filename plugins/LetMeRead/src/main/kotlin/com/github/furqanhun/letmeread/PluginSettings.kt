package com.github.furqanhun.letmeread

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.DimenUtils
import com.aliucord.widgets.BottomSheet
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting

class PluginSettings(private val settings: SettingsAPI) : BottomSheet() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        val chatToggle = Utils.createCheckedSetting(
            context,
            CheckedSetting.ViewType.CHECK,
            "Chat Messages & Replies",
            "Adjust author names in chat messages and reply indicators",
        ).apply {
            isChecked = settings.getBool(KEY_CHAT_ENABLED, true)
            setOnCheckedListener {
                settings.setBool(KEY_CHAT_ENABLED, it)
                refreshViews()
            }
        }
        addView(chatToggle)

        val chatTextToggle = Utils.createCheckedSetting(
            context,
            CheckedSetting.ViewType.CHECK,
            "Chat Message Content",
            "Adjust message body text when theme colors clash with the background",
        ).apply {
            isChecked = settings.getBool(KEY_CHAT_TEXT_ENABLED, false)
            setOnCheckedListener {
                settings.setBool(KEY_CHAT_TEXT_ENABLED, it)
                refreshViews()
            }
        }
        addView(chatTextToggle)

        val memberListToggle = Utils.createCheckedSetting(
            context,
            CheckedSetting.ViewType.CHECK,
            "Member List Sidebar",
            "Adjust member names in the channel member list drawer",
        ).apply {
            isChecked = settings.getBool(KEY_MEMBER_LIST_ENABLED, true)
            setOnCheckedListener {
                settings.setBool(KEY_MEMBER_LIST_ENABLED, it)
                refreshViews()
            }
        }
        addView(memberListToggle)

        addDivider(context)

        val currentMode = settings.getInt(KEY_MODE, ColorContrastEngine.MODE_LIGHTNESS_SHIFT)
        val modeToggle = Utils.createCheckedSetting(
            context,
            CheckedSetting.ViewType.SWITCH,
            "Preserve Color Tint (HSL Shift)",
            if (currentMode == ColorContrastEngine.MODE_LIGHTNESS_SHIFT) {
                "Keeps role hue and boosts brightness until readable"
            } else {
                "Replaces unreadable colors with pure white/dark fallback"
            },
        ).apply {
            isChecked = currentMode == ColorContrastEngine.MODE_LIGHTNESS_SHIFT
            setOnCheckedListener { checked ->
                val newMode = if (checked) {
                    ColorContrastEngine.MODE_LIGHTNESS_SHIFT
                } else {
                    ColorContrastEngine.MODE_FALLBACK
                }
                settings.setInt(KEY_MODE, newMode)
                setSubtext(
                    if (checked) {
                        "Keeps role hue and boosts brightness until readable"
                    } else {
                        "Replaces unreadable colors with pure white/dark fallback"
                    },
                )
                refreshViews()
            }
        }
        addView(modeToggle)

        addDivider(context)

        val sliderContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padH = DimenUtils.dpToPx(16)
            val padV = DimenUtils.dpToPx(8)
            setPadding(padH, padV, padH, padV)
        }

        var currentRatio = settings.getFloat(KEY_CONTRAST_RATIO, 3.5f).toDouble().coerceIn(1.5, 7.0)

        val formatRatioText = { ratio: Double ->
            val formatted = String.format("%.1f", ratio)
            val indicator = when {
                ratio < 3.0 -> "Subtle"
                ratio < 4.0 -> "Balanced"
                ratio < 5.5 -> "High (WCAG AA)"
                else -> "Maximum (WCAG AAA)"
            }
            "Contrast Ratio: $formatted:1 ($indicator)"
        }

        val headerText = TextView(context).apply {
            text = formatRatioText(currentRatio)
            textSize = 16f
            val headerColorId = Utils.getResId("colorHeaderPrimary", "attr")
            if (headerColorId != 0) {
                setTextColor(ColorCompat.getThemedColor(context, headerColorId))
            }
        }

        val subtext = TextView(context).apply {
            text = "Minimum contrast threshold (1.5:1 to 7.0:1). 3.5:1 is recommended for balanced roles."
            textSize = 12f
            val subColorId = Utils.getResId("colorInteractiveNormal", "attr")
            if (subColorId != 0) {
                setTextColor(ColorCompat.getThemedColor(context, subColorId))
            }
            setPadding(0, DimenUtils.dpToPx(2), 0, DimenUtils.dpToPx(8))
        }

        val slider = SeekBar(context).apply {
            max = 55
            progress = ((currentRatio - 1.5) * 10).toInt().coerceIn(0, 55)
            splitTrack = false
            val progressResId = Utils.getResId("drawable_settings_progress", "drawable")
            val thumbResId = Utils.getResId("drawable_thumb_white", "drawable")
            if (progressResId != 0) {
                progressDrawable = androidx.core.content.ContextCompat.getDrawable(context, progressResId)
            }
            if (thumbResId != 0) {
                thumb = androidx.core.content.ContextCompat.getDrawable(context, thumbResId)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val ratio = 1.5 + (progress * 0.1)
                    headerText.text = formatRatioText(ratio)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val ratio = 1.5 + (seekBar.progress * 0.1)
                    settings.setFloat(KEY_CONTRAST_RATIO, ratio.toFloat())
                    refreshViews()
                }
            })
        }

        sliderContainer.addView(headerText)
        sliderContainer.addView(subtext)
        sliderContainer.addView(slider)
        addView(sliderContainer)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        refreshViews()
    }

    private fun refreshViews() {
        try {
            LetMeRead.refreshChat()
            val root = activity?.findViewById<android.view.ViewGroup>(android.R.id.content)
            if (root != null) {
                notifyRecyclers(root)
            }
        } catch (_: Throwable) {}
    }

    private fun notifyRecyclers(viewGroup: android.view.ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is androidx.recyclerview.widget.RecyclerView) {
                child.adapter?.notifyDataSetChanged()
            } else if (child is android.view.ViewGroup) {
                notifyRecyclers(child)
            }
        }
    }

    private fun addDivider(context: Context) {
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2,
            ).apply {
                setMargins(0, DimenUtils.dpToPx(8), 0, DimenUtils.dpToPx(8))
            }
            setBackgroundColor(0x22FFFFFF)
        }
        addView(divider)
    }

    companion object {
        const val KEY_CHAT_ENABLED = "letmeread_chat_enabled"
        const val KEY_CHAT_TEXT_ENABLED = "letmeread_chat_text_enabled"
        const val KEY_MEMBER_LIST_ENABLED = "letmeread_member_list_enabled"
        const val KEY_MODE = "letmeread_mode"
        const val KEY_CONTRAST_RATIO = "letmeread_contrast_ratio"
    }
}
