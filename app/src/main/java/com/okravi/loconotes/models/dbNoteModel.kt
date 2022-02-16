package com.okravi.loconotes.models

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import com.google.android.gms.maps.model.Marker
import java.io.Serializable
import kotlin.math.sqrt

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


    fun populateProximity(note: dbNoteModel, currentLocation: Location) {

        val mLatitude = currentLocation.latitude.toFloat()
        val mLongitude = currentLocation.longitude.toFloat()

        val noteLatitude = note.placeLatitude.toFloat()
        val noteLongitude = note.placeLongitude.toFloat()

        val a = (mLatitude.minus(noteLatitude)).times(mLatitude.minus(noteLatitude))
        val b = (mLongitude.minus(noteLongitude)).times(mLongitude.minus(noteLongitude))
        val distance = sqrt(a.plus(b))

        note.proximity = distance
    }

