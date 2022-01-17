package com.okravi.loconotes.models

import java.io.Serializable

data class LocationNoteModel(
    var googlePlaceID: String = "",
    var placeName: String = "",
    var placeLatitude: String = "",
    var placeLongitude: String = "",
    var placeLikelyHood: Double = 0.0,
    var dateNoteLastModified: String = "",
    var textNote: String = "",
    var isSelected: Boolean = false

    ): Serializable
