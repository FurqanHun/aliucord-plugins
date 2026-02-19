package com.github.furqanhun.SpoofTimezone

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.TextInput
import com.aliucord.patcher.*
import java.util.TimeZone

@AliucordPlugin(requiresRestart = true)
class SpoofTimezone : Plugin() {

    // Store the real system timezone for fallback
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    override fun start(context: Context) {
        settingsTab = SettingsTab(
            SpoofTimezoneSettings::class.java,
            SettingsTab.Type.PAGE
        ).withArgs(settings)

        try {
            patcher.patch(
                TimeZone::class.java.getDeclaredMethod("getDefault"),
                Hook { callFrame ->
                    val spoofedId = settings.getString("spoof_timezone_id", null)

                    callFrame.result = if (spoofedId.isNullOrBlank()) {
                        originalTimeZone
                    } else {
                        val tz = TimeZone.getTimeZone(spoofedId)
                        logger.info("Returning spoofed timezone: ${tz.id}")
                        tz
                    }
                }
            )

            val currentSetting = settings.getString("spoof_timezone_id", "not set")
            Utils.showToast("SpoofTimezone loaded! Current: $currentSetting")
        } catch (e: Exception) {
            logger.error("Failed to patch TimeZone.getDefault()", e)
            Utils.showToast("Failed to patch timezone: ${e.message}")
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    class SpoofTimezoneSettings(private val settings: SettingsAPI) : SettingsPage() {

        override fun onViewBound(view: View) {
            super.onViewBound(view)

            setActionBarTitle("Spoof Timezone")

            val input = TextInput(view.context).apply {
                editText.hint = "Enter Timezone ID (e.g. America/New_York)"

                editText.setText(settings.getString("spoof_timezone_id", ""))

                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val id = s.toString().trim()
                        settings.setString("spoof_timezone_id", id)
                        Utils.showToast(view.context, "Set to: $id (Restart required)")
                    }
                })
            }

            val infoButton = com.aliucord.views.Button(view.context).apply {
                text = "Check Current Detected Timezone"
                setOnClickListener {
                    val current = TimeZone.getDefault()
                    Utils.showToast(view.context, "Current: ${current.id} (${current.displayName})")
                }
            }

            val clearButton = com.aliucord.views.Button(view.context).apply {
                text = "Reset to System Default"
                setOnClickListener {
                    settings.setString("spoof_timezone_id", "")
                    input.editText.setText("")
                    Utils.showToast(view.context, "Reset! Restart Discord to apply.")
                }
            }

            addView(input)
            addView(infoButton)
            addView(clearButton)
        }
    }
}
