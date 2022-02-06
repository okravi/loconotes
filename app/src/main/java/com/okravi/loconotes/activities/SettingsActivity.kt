package com.okravi.loconotes.activities

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import com.google.android.gms.common.internal.Constants
import com.okravi.loconotes.Constants.SORT_METHOD
import com.okravi.loconotes.databinding.ActivitySettingsBinding

private var binding: ActivitySettingsBinding? = null
private lateinit var mSharedPreferences: SharedPreferences

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mSharedPreferences = getSharedPreferences(Constants., Context.MODE_PRIVATE)
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
                        Toast.makeText(this, "rbSortByDateModified", Toast.LENGTH_SHORT).show()
                    }
                binding?.rbSortByName?.id ->
                    if (checked) {
                        Toast.makeText(this, "rbSortByName", Toast.LENGTH_SHORT).show()
                    }
                binding?.rbSortByProximity?.id ->
                    if (checked) {
                        Toast.makeText(this, "rbSortProximity", Toast.LENGTH_SHORT).show()
                    }
                binding?.rbUpdateNotesListAutomatically?.id ->
                    if (checked) {
                        Toast.makeText(this, "rbUpdateNotesListAutomatically", Toast.LENGTH_SHORT).show()
                    }
                binding?.rbUpdateNotesListManually?.id ->
                    if (checked) {
                        Toast.makeText(this, "rbUpdateNotesListManually", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        val editor = mSharedPreferences.edit()
        editor.putString(Constants.SORT_METHOD, "test")
        editor.apply()
    }

}