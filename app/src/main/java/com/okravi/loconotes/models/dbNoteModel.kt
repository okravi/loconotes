package com.okravi.loconotes.models

import android.graphics.Bitmap
import android.net.Uri
import java.io.Serializable

data class dbNoteModel (
    var keyID: String = "",
    var googlePlaceID: String = "",
    var placeName: String = "",
    var placeLatitude: String = "",
    var placeLongitude: String = "",
    var dateNoteLastModified: String = "",
    var textNote: String = "",
    var photo: String = "",

    ): Serializable