package com.github.furqanhun.TimezoneSpoof

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
class TimezoneSpoof : Plugin() {

    override fun start(context: Context) {
        settingsTab = SettingsTab(
            TimezoneSpoofSettings::class.java,
            SettingsTab.Type.PAGE
        ).withArgs(settings)

        // forcefully hijack the jvm process timezone on startup
        applyTimezone(settings.getString("spoof_timezone_id", ""))
    }

    override fun stop(context: Context) {
        TimeZone.setDefault(originalTimeZone)
    }

    companion object {
        val originalTimeZone: TimeZone = TimeZone.getDefault()

        fun applyTimezone(spoofedId: String) {
            if (spoofedId == "") {
                TimeZone.setDefault(originalTimeZone)
            } else {
                TimeZone.setDefault(TimeZone.getTimeZone(spoofedId))
            }
        }
    }

    class TimezoneSpoofSettings(
        private val settings: SettingsAPI
    ) : SettingsPage() {

        override fun onViewBound(view: View) {
            super.onViewBound(view)

            setActionBarTitle("Timezone Spoof")

            val input = TextInput(view.context, "Enter IANA Timezone (e.g. America/New_York)").apply {
                editText.setText(settings.getString("spoof_timezone_id", ""))
            }

            val applyButton = com.aliucord.views.Button(view.context).apply {
                text = "Apply Timezone"
                setOnClickListener {
                    val id = input.editText.text.toString().trim()
                    settings.setString("spoof_timezone_id", id)

                    applyTimezone(id)
                    Utils.showToast(view.context, "Timezone spoofed! Please restart Discord.")
                }
            }

            val infoButton = com.aliucord.views.Button(view.context).apply {
                text = "Check Current Detected Timezone"
                setOnClickListener {
                    val current = TimeZone.getDefault()
                    Utils.showToast(view.context, "Currently detected (backend): ${current.id} (${current.displayName})")
                }
            }

            val clearButton = com.aliucord.views.Button(view.context).apply {
                text = "Reset to System Default"
                setOnClickListener {
                    settings.setString("spoof_timezone_id", "")
                    input.editText.setText("")
                    applyTimezone("")
                    Utils.showToast(view.context, "Reset to system default! Please restart Discord.")
                }
            }

            addView(input)
            addView(applyButton)
            addView(infoButton)
            addView(clearButton)
        }
    }
}
