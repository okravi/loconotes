package com.okravi.loconotes.activities

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import com.google.android.gms.common.internal.Constants
import com.okravi.loconotes.Constants.SORT_METHOD
import com.okravi.loconotes.databinding.ActivitySettingsBinding

private var binding: ActivitySettingsBinding? = null
//private lateinit var mSharedPreferences: SharedPreferences
private const val sharedPrefFile = "loconotesSettings"

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding?.root)



        //mSharedPreferences = getSharedPreferences(Constants.SORT_METHOD, Context.MODE_PRIVATE)

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


                    }
                binding?.rbSortByName?.id ->
                    if (checked) {
                        saveSetting("sortOrder", "placeName")
                    }
                binding?.rbSortByProximity?.id ->
                    if (checked) {
                        saveSetting("sortOrder", "proximity")
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

    private fun saveSetting(setting: String, value: String){
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,Context.MODE_PRIVATE)
        val editor:SharedPreferences.Editor =  sharedPreferences.edit()

        //TODO: fix this
        editor.putString(setting, "setting")
        editor.putString("value", value)
        editor.apply()

        val test = sharedPreferences.contains(setting)
        Log.d("debug", "$setting is saved: $test")
        //editor.commit()
    }

}