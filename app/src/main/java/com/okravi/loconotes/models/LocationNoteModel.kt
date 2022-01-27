package com.okravi.loconotes.models

import android.graphics.Bitmap
import java.io.Serializable

data class LocationNoteModel (
    var keyID: String = "",
    var googlePlaceID: String = "",
    var placeName: String = "",
    var placeLatitude: String = "",
    var placeLongitude: String = "",
    var placeLikelyHood: Double = 0.0,
    var dateNoteLastModified: String = "",
    var textNote: String = "",
    var isSelected: Boolean = false,
    var photoMetadata: String ="",
    var photo: Bitmap? = null,
    var photoByteArray: ByteArray? = null

    ): Serializable
