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
import java.util.TimeZone

@AliucordPlugin(requiresRestart = true)
class SpoofTimezone : Plugin() {

    // Store the real system timezone for fallback
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    override fun start(context: Context) {
        settingsTab = SettingsTab(
            SpoofTimezoneSettings::class.java,
            SettingsTab.Type.PAGE
        ).withArgs(settings, originalTimeZone)

        // forcefully hijack the jvm process timezone on startup
        applyTimezone(settings.getString("spoof_timezone_id", null), originalTimeZone)
    }

    override fun stop(context: Context) {
        TimeZone.setDefault(originalTimeZone)
    }

    companion object {
        fun applyTimezone(spoofedId: String?, original: TimeZone) {
            if (spoofedId.isNullOrBlank()) {
                TimeZone.setDefault(original)
            } else {
                TimeZone.setDefault(TimeZone.getTimeZone(spoofedId))
            }
        }
    }

    class SpoofTimezoneSettings(
        private val settings: SettingsAPI,
        private val originalTimeZone: TimeZone
    ) : SettingsPage() {

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

                        applyTimezone(id, originalTimeZone)
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
