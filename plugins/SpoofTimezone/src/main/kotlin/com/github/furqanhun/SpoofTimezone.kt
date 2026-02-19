package com.github.furqanhun.SpoofTimezone

import android.content.Context
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

    override fun start(context: Context) {
        settingsTab = SettingsTab(
            SpoofTimezoneSettings::class.java,
            SettingsTab.Type.PAGE
        ).withArgs(settings)

        // forcefully hijack the jvm process timezone on startup
        applyTimezone(settings.getString("spoof_timezone_id", null))
    }

    override fun stop(context: Context) {
        TimeZone.setDefault(originalTimeZone)
    }

    companion object {
        val originalTimeZone: TimeZone = TimeZone.getDefault()

        fun applyTimezone(spoofedId: String?) {
            if (spoofedId == null || spoofedId.length == 0) {
                TimeZone.setDefault(originalTimeZone)
            } else {
                TimeZone.setDefault(TimeZone.getTimeZone(spoofedId))
            }
        }
    }

    class SpoofTimezoneSettings(
        private val settings: SettingsAPI
    ) : SettingsPage() {

        override fun onViewBound(view: View) {
            super.onViewBound(view)

            setActionBarTitle("Spoof Timezone")

            val input = TextInput(view.context).apply {
                editText.hint = "Enter Timezone ID (e.g. America/New_York)"
                editText.setText(settings.getString("spoof_timezone_id", ""))
            }

            val applyButton = com.aliucord.views.Button(view.context).apply {
                text = "Apply Timezone"
                setOnClickListener {
                    // Safe native string conversion and trim
                    val id = input.editText.text.toString().trim()
                    settings.setString("spoof_timezone_id", id)

                    applyTimezone(id)
                    Utils.showToast(view.context, "Applied! Force restart Discord to sync UI.")
                }
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
                    applyTimezone("")
                    Utils.showToast(view.context, "Reset to default! Restart Discord.")
                }
            }

            addView(input)
            addView(applyButton)
            addView(infoButton)
            addView(clearButton)
        }
    }
}
