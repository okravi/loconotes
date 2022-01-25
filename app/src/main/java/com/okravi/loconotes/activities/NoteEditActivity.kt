package com.okravi.loconotes.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
            val placeData = intent.getSerializableExtra(
                MainActivity.PLACE_DATA) as LocationNoteModel

            binding?.etPlaceName?.setText(placeData.placeName)
            binding?.etLatitude?.setText(placeData.placeLatitude)
            binding?.etLongitude?.setText(placeData.placeLongitude)

            val byteArray = placeData.photoByteArray
            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

            binding?.placePhoto?.setImageBitmap(bmp)


        }




    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}