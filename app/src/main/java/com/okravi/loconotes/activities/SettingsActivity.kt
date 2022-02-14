package com.okravi.loconotes.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.okravi.loconotes.databinding.ActivitySettingsBinding

private var binding: ActivitySettingsBinding? = null
private const val sharedPrefFile = "loconotesSettings"

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding?.root)
    }

    override fun onResume() {
        super.onResume()

        when (readSetting("sortOrder")) {
            "default" -> {
                binding?.rbSortByProximity?.isChecked = true
                binding?.llSettingsNotesUpdate?.visibility = View.VISIBLE
            }
            "proximity" -> {
                binding?.rbSortByProximity?.isChecked = true
                binding?.llSettingsNotesUpdate?.visibility = View.VISIBLE
            }
            "placeName" -> {
                binding?.rbSortByName?.isChecked = true
                binding?.llSettingsNotesUpdate?.visibility = View.GONE
            }
            "dateModified" -> {
                binding?.rbSortByDateModified?.isChecked = true
                binding?.llSettingsNotesUpdate?.visibility = View.GONE
            }
        }

        when (readSetting("updateNoteListMethod")) {
            "default" -> {
                binding?.rbUpdateNotesListAutomatically?.isChecked = true
            }
            "automatic" -> {
                binding?.rbUpdateNotesListAutomatically?.isChecked = true
            }
            "manual" -> {
                binding?.rbUpdateNotesListManually?.isChecked = true
            }
        }

        when (readSetting("updatePlacesListMethod")) {
            "default" -> {
                binding?.rbUpdatePlacesListAutomatically?.isChecked = true
            }
            "automatic" -> {
                binding?.rbUpdatePlacesListAutomatically?.isChecked = true
            }
            "manual" -> {
                binding?.rbUpdatePlacesListManually?.isChecked = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    fun onRadioButtonClicked(view: View) {

        if (view is RadioButton) {
            // Is the button now checked?
            val checked = view.isChecked

            // Check which radio button was clicked
            when (view.getId()) {
                binding?.rbSortByDateModified?.id ->
                    if (checked) {
                        saveSetting("sortOrder", "dateModified")
                        binding?.llSettingsNotesUpdate?.visibility = View.GONE
                    }
                binding?.rbSortByName?.id ->
                    if (checked) {
                        saveSetting("sortOrder", "placeName")
                        binding?.llSettingsNotesUpdate?.visibility = View.GONE
                    }
                binding?.rbSortByProximity?.id ->
                    if (checked) {
                        saveSetting("sortOrder", "proximity")
                        binding?.llSettingsNotesUpdate?.visibility = View.VISIBLE
                    }
                binding?.rbUpdateNotesListAutomatically?.id ->
                    if (checked) {
                        saveSetting("updateNoteListMethod", "automatic")
                    }
                binding?.rbUpdateNotesListManually?.id ->
                    if (checked) {
                        saveSetting("updateNoteListMethod", "manual")
                    }
                binding?.rbUpdatePlacesListAutomatically?.id ->
                    if (checked) {
                        saveSetting("updatePlacesListMethod", "automatic")
                    }
                binding?.rbUpdatePlacesListManually?.id ->
                    if (checked) {
                        saveSetting("updatePlacesListMethod", "manual")
                    }
            }
        }
    }

    private fun readSetting(setting: String): String? {
        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)

        return sharedPreferences.getString(setting, "default")
    }

    private fun saveSetting(setting: String, value: String){
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,Context.MODE_PRIVATE)
        val editor:SharedPreferences.Editor = sharedPreferences.edit()

        editor.putString(setting, value)
        editor.apply()
    }
}