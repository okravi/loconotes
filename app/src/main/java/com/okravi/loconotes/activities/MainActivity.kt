package com.okravi.loconotes.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.okravi.loconotes.R
import com.okravi.loconotes.databinding.ActivityMainBinding
import java.util.*

private var binding : ActivityMainBinding? = null

class MainActivity : AppCompatActivity(), OnMapReadyCallback, View.OnClickListener {
    //GoogleMaps class for map manipulation
    private lateinit var mMap: GoogleMap
    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // LocationRequest - Requirements for the location updates, i.e.,
    // how often you should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest
    // This will store current location info
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        checkLocationPermissionsWithDexter()

        //Initializing Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key), Locale.US)
        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        binding?.btnAddNote?.setOnClickListener(this)
    }

    //Checking if location permissions are granted
    private fun isLocationEnabled(): Boolean {
        return !(ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    }

    //Checking whether user granted the location permissions
    private fun checkLocationPermissionsWithDexter() {

        if (!isLocationEnabled()) {
            Log.e("debug", "checkLocationPermissionsWithDexter/permissions enabled: false")
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {

            Log.e("debug", "checking permissions with Dexter")
            Dexter.withActivity(this).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    Log.e("debug", "Dexter checked the permissions and they are fine")

                    getFusedUserLocation()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    Log.e("debug", "we should show the rationale")
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()
            Log.e("debug", "we're in the end of checkLocationPermissionsWithDexter")
        }
    }

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like the permissions weren't granted. That's unfortunate.")
            .setPositiveButton("GO TO SETTINGS")
            { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
        Log.e("debug", "the user could've granted the access by now")

    }

    //Getting user location, we've already checked the permissions with Dexter
    @SuppressLint("MissingPermission")
    private fun getFusedUserLocation() {
        //Initialize fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        //Initialize locationRequest.
        locationRequest = LocationRequest().apply {
            // Sets the desired interval for
            // active location updates.
            interval = 60000

            // Sets the fastest rate for active location updates.
            fastestInterval = 2000

            // Sets the maximum time when batched location
            // updates are delivered.
            maxWaitTime = 500

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallBack,
            Looper.myLooper()!!
        )
        Log.e("debug", "we're in the end of GetFusedLocation")
    }

    //LocationCallback - Called when FusedLocationProviderClient has a new Location
    private val mLocationCallBack = object : LocationCallback(){
        //Zooming in only upon the app's start
        var alreadyZoomedIn: Boolean = false

        override fun onLocationResult(locationResult: LocationResult){
            //val mLastLocation: Location = locationResult.lastLocation
            currentLocation = locationResult.lastLocation
            var mLatitude = currentLocation!!.latitude
            Log.i("Latitude", "$mLatitude")
            var mLongitude = currentLocation!!.longitude
            Log.i("Longitude", "$mLongitude")

            //Zooming in on user's location
            val position = LatLng(mLatitude, mLongitude)
            if (!alreadyZoomedIn){

                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
                alreadyZoomedIn = true
                mMap.animateCamera(newLatLngZoom)
            }
        }
    }


    //Displaying users location on the map. Checking permissions with isLocationEnabled()
    //@SuppressLint("MissingPermission")
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        if (isLocationEnabled()) {

            mMap = googleMap
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = false
        }
    }

    //Getting a list of locations closest to the user's current location. Checking permissions
    //in the onClick function
    @SuppressLint("MissingPermission")
    private fun getListOfLocationsForCurrentPosition(){
        //Client that exposes the Places API methods
        var placesClient = Places.createClient(this)
        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ID, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS)
        val placeFieldsLatLng: List<Place.Field> = listOf(Place.Field.LAT_LNG)
        // Use the builder to create a FindCurrentPlaceRequest.
        val requestNearbyPlaces: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
            val placeResponse = placesClient.findCurrentPlace(requestNearbyPlaces)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods ?: emptyList()) {
                        Log.e(
                            "debug",
                            "Place '${placeLikelihood.place.name}' " +
                                    "has likelihood: ${placeLikelihood.likelihood} " +
                                    "and is located at ${placeLikelihood.place.latLng}" +
                                    " ${placeLikelihood.place.id}" +
                                    "${placeLikelihood.place.address}" +
                                    "${placeLikelihood.place.photoMetadatas}"
                        )
                    }
                } else {
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.e("debug", "Place not found: ${exception.statusCode}")
                    }
                }
            }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            binding?.btnAddNote?.id -> {
                Log.e("debug", "Add button clicked")

                if(isLocationEnabled()) {
                    getListOfLocationsForCurrentPosition()
                }else{
                    Toast.makeText(this, "Please grant the location permission!", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    //Making sure the location gets displayed on the map if user gives back the location permissions
    override fun onStart() {
        super.onStart()
        Log.e("debug", "We're back in the game!")

        if(isLocationEnabled()){

            getFusedUserLocation()
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}