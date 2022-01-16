package com.okravi.loconotes.activities

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.okravi.loconotes.databinding.ActivityMainBinding
import com.okravi.loconotes.R

private var binding : ActivityMainBinding? = null

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        checkLocationPermissionsWithDexter()







    }

    //Checking whether user granted the location permissions

    private fun checkLocationPermissionsWithDexter(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                Toast.makeText(this@MainActivity, "Location permissions granted", Toast.LENGTH_SHORT).show()
                getFusedUserLocation()
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                //TODO implement intent and show the rationale dialog
                Toast.makeText(this@MainActivity, "WE HAVE A PROBLEM with LOCATION permissions", Toast.LENGTH_SHORT).show()
            }

        }).onSameThread().check()
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
            // This interval is inexact.
            interval = 60000

            // Sets the fastest rate for active location updates.
            // This interval is exact, and your application will never
            // receive updates more frequently than this value
            fastestInterval = 1000

            // Sets the maximum time when batched location
            // updates are delivered. Updates may be
            // delivered sooner than this interval
            maxWaitTime = 500

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, mLocationCallBack,
            Looper.myLooper()!!
        )

    }
    //LocationCallback - Called when FusedLocationProviderClient has a new Location
    private val mLocationCallBack = object : LocationCallback(){

        var initiallyZoomedIn: Boolean = false

        override fun onLocationResult(locationResult: LocationResult){
            //val mLastLocation: Location = locationResult.lastLocation
            currentLocation = locationResult.lastLocation
            var mLatitude = currentLocation!!.latitude
            Log.i("Latitude", "$mLatitude")
            var mLongitude = currentLocation!!.longitude
            Log.i("Longitude", "$mLongitude")

            //Zooming in on user's location

            val position = LatLng(mLatitude, mLongitude)

            if (!initiallyZoomedIn){
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
                initiallyZoomedIn = true
                mMap.animateCamera(newLatLngZoom)
            }




        }
    }


//Displaying users location on map. We've already checked the permissions with Dexter.
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.e("debug", "onMapReady")
        mMap = googleMap

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = false


        //getCurrentLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}