package com.okravi.loconotes.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.okravi.loconotes.databinding.ActivityMainBinding
import com.okravi.loconotes.databinding.ActivityNoteEditBinding
import com.okravi.loconotes.models.LocationNoteModel

private var binding : ActivityNoteEditBinding? = null

class NoteEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        //getting place data based on the clicked recyclerview position


        if(intent.hasExtra(MainActivity.PLACE_DATA)){
            var placeData = intent.getSerializableExtra(
                MainActivity.PLACE_DATA) as LocationNoteModel

           binding?.etPlaceName?.setText(placeData.placeName)


        }




    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}