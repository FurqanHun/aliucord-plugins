package com.github.furqanhun.TimezoneSpoof

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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

            val allTimezones = TimeZone.getAvailableIDs()

            val input = TextInput(view.context, "Enter IANA Timezone (e.g. America/New_York)").apply {
                editText.setText(settings.getString("spoof_timezone_id", ""))
            }

            val resultsLayout = LinearLayout(view.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 0, 30, 30)
            }

            // auto comp logic
            input.editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString()?.trim() ?: ""
                    resultsLayout.removeAllViews() // rm old results

                    if (query.length < 2) return

                    val matches = allTimezones
                        .filter { it.contains(query, ignoreCase = true) }
                        .take(5)

                    matches.forEach { tzId ->
                        val row = TextView(view.context).apply {
                            text = tzId
                            setTextColor(Color.LTGRAY)
                            textSize = 16f
                            setPadding(20, 25, 20, 25)

                            setOnClickListener {
                                input.editText.setText(tzId)
                                input.editText.setSelection(tzId.length) // mv cursor to end
                                resultsLayout.removeAllViews()
                            }
                        }
                        resultsLayout.addView(row)
                    }
                }
            })

            val applyButton = com.aliucord.views.Button(view.context).apply {
                text = "Apply Timezone"
                setOnClickListener {
                    val id = input.editText.text.toString().trim()

                    // valid check
                    if (id.isNotEmpty() && !allTimezones.contains(id)) {
                        Utils.showToast(view.context, "Invalid Timezone ID! Use the search dropdown.")
                        return@setOnClickListener
                    }

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
                    resultsLayout.removeAllViews()
                    Utils.showToast(view.context, "Reset to system default! Please restart Discord.")
                }
            }

            addView(input)
            addView(resultsLayout)
            addView(applyButton)
            addView(infoButton)
            addView(clearButton)
        }
    }
}
