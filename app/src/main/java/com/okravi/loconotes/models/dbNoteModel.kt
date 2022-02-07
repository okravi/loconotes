package com.okravi.loconotes.models

import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.maps.model.Marker
import java.io.Serializable

data class dbNoteModel(
    var keyID: String = "",
    var googlePlaceID: String = "",
    var placeName: String = "",
    var placeLatitude: String = "",
    var placeLongitude: String = "",
    var dateNoteLastModified: Long = 0,
    var textNote: String = "",
    var photo: String = "",
    var isSelected: Boolean = false,
    var proximity: Float = -1.0F,
    var marker: Marker? = null

    ): Serializable
