package com.okravi.loconotes.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.okravi.loconotes.R
import com.okravi.loconotes.adapters.NearbyPlacesAdapter
import com.okravi.loconotes.adapters.NotesAdapter
import com.okravi.loconotes.databinding.ActivityMainBinding
import com.okravi.loconotes.models.LocationNoteModel
import java.util.*





private var binding : ActivityMainBinding? = null

class MainActivity : AppCompatActivity(), OnMapReadyCallback, View.OnClickListener {
    //GoogleMaps class for map manipulation
    private lateinit var mMap: GoogleMap
    // FusedLocationProviderClient - Main class for receiving location updates.

    //testing
    private lateinit var nearbyPlacesAdapter : NearbyPlacesAdapter
    //testing
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // LocationRequest - Requirements for the location updates, i.e.,
    // how often you should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest
    // This will store current location info

    private var currentLocation: Location? = null
    private val maxNumberOfNearbyPlacesToShowUser = 30
    private var nearbyPlacesInRecyclerView: Boolean = false
    var listOfNearbyPlaces = ArrayList<LocationNoteModel>(5)

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
        binding?.btnListNotes?.setOnClickListener(this)
        binding?.btnSettings?.setOnClickListener(this)

        //animating the buttons
        binding?.btnSettings?.translationX = 250F
        binding?.btnSettings?.
        animate()?.alpha(1f)?.translationXBy(-250F)?.setStartDelay(350)?.duration = 1100
        binding?.btnAddNote?.translationX = 250F
        binding?.btnAddNote?.
        animate()?.alpha(1f)?.translationXBy(-250F)?.setStartDelay(200)?.duration = 1100
        binding?.btnListNotes?.translationX = 250F
        binding?.btnListNotes?.
        animate()?.alpha(1f)?.translationXBy(-250F)?.setStartDelay(50)?.duration = 1100
    }

    var listOfSavedNotes = ArrayList<LocationNoteModel>(5)

    //Checking if location provider is enabled for all apps
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //Checking whether user granted the location permissions
    private fun checkLocationPermissionsWithDexter() {

        if (!isLocationEnabled()) {

            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {

            Dexter.withActivity(this).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        getFusedUserLocation()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()

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
    }

    //testing
    var previousUserLocation : LatLng =  LatLng(-0.0, 0.0)
    //testing

    //LocationCallback - Called when FusedLocationProviderClient has a new Location
    private val mLocationCallBack = object : LocationCallback(){
        //Zooming in only upon the app's start
        var alreadyZoomedIn: Boolean = false

        override fun onLocationResult(locationResult: LocationResult){
            //val mLastLocation: Location = locationResult.lastLocation
            currentLocation = locationResult.lastLocation
            val mLatitude = currentLocation!!.latitude
            val mLongitude = currentLocation!!.longitude
            //Zooming in on user's location
            val position = LatLng(mLatitude, mLongitude)

            //TODO: see if we should check the alreadyZoomed in here as well
            if ((position != previousUserLocation)){
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
                alreadyZoomedIn = true
                mMap.animateCamera(newLatLngZoom)
            }

            previousUserLocation = position
        }
    }


    //Displaying users location on the map. Checking permissions with isLocationEnabled()

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        if (isLocationEnabled()) {

            mMap = googleMap
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = false
        }

        mMap.setOnMarkerClickListener { marker ->
            if (marker.isInfoWindowShown) {

                marker.hideInfoWindow()
                Toast.makeText(this, "clicked on a marker ${marker.tag}", Toast.LENGTH_SHORT).show()
            } else {
                marker.showInfoWindow()
                Toast.makeText(this, "clicked on a marker ${marker.tag}", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    override fun onResume() {
        //TODO check if this is really needed (when user gives the permission)
        super.onResume()
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    //Getting a list of locations closest to the user's current location. Checking permissions
    //in the onClick function
    @SuppressLint("MissingPermission")
    private fun getListOfLocationsForCurrentPosition(): List<LocationNoteModel>{
        var placesWithPhotosCounter = 0
        var bitmapsSavedCounter = 0
        var cycleCounter = 0
        //Client that exposes the Places API methods
        val placesClient = Places.createClient(this)
        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ID, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS)
        // Use the builder to create a FindCurrentPlaceRequest.
        val requestNearbyPlaces: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
            val placeResponse = placesClient.findCurrentPlace(requestNearbyPlaces)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val response = task.result
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods ?: emptyList()) {

                        //Saving top probability places to the list if they have photos
                        when {
                            cycleCounter<maxNumberOfNearbyPlacesToShowUser -> {

                                val nearbyLocation = LocationNoteModel()
                                nearbyLocation.googlePlaceID = placeLikelihood.place.id!!.toString()
                                nearbyLocation.placeName = placeLikelihood.place.name!!
                                nearbyLocation.placeLatitude = placeLikelihood.place.latLng!!.latitude.toString()
                                nearbyLocation.placeLongitude = placeLikelihood.place.latLng!!.longitude.toString()
                                nearbyLocation.placeLikelyHood = placeLikelihood.likelihood

                                cycleCounter += 1

                                val photoMetadata = placeLikelihood.place
                                    .photoMetadatas?.first()
                                if (photoMetadata != null){
                                    placesWithPhotosCounter += 1
                                    Log.d("debug", "we have a photo")
                                    val photoRequest = FetchPhotoRequest
                                        .builder(photoMetadata)
                                        .setMaxWidth(500)
                                        .setMaxHeight(500)
                                        .build()

                                    placesClient.fetchPhoto(photoRequest)
                                        .addOnSuccessListener { fetchPhotoResponse ->
                                            val bitmap = fetchPhotoResponse.bitmap
                                            nearbyLocation.photo = bitmap
                                            bitmapsSavedCounter += 1
                                            //TODO: this is a temporary solution to photos loading late
                                            if (bitmapsSavedCounter == placesWithPhotosCounter){
                                                setupNearbyPlacesRecyclerView(listOfNearbyPlaces)
                                            }

                                        }.addOnFailureListener { exception ->
                                            if (exception is ApiException) {
                                                val statusCode = exception.statusCode
                                                Log.e(TAG,
                                                    "Place not found: " +
                                                            exception.message + ", " +
                                                            "statusCode: " + statusCode)
                                            }
                                        }
                                }
                                        if (photoMetadata != null) {
                                    listOfNearbyPlaces.add(nearbyLocation)
                                            Log.d("debug", "Saving a place to a list")
                                }
                            }
                        }
                    }
                } else {
                    val exception = task.exception
                    if (exception is ApiException) {
                        val statusCode = exception.statusCode
                        Log.e(TAG,
                            "Place not found: " +
                                    exception.message + ", " +
                                    "statusCode: " + statusCode)
                    }
                }
            }


        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        return listOfNearbyPlaces
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            binding?.btnAddNote?.id -> {

                //making sure we don't display nearby items twice
                listOfNearbyPlaces.clear()

                if(isLocationEnabled()) {
                    getListOfLocationsForCurrentPosition()

                }else{
                    Toast.makeText(this, "Please grant the location permission!", Toast.LENGTH_SHORT).show()
                }

            }

            binding?.btnListNotes?.id -> {
                Toast.makeText(this, "list button", Toast.LENGTH_SHORT).show()

                if (listOfSavedNotes.size > 0){
                    setupNotesListRecyclerView(listOfSavedNotes)
                }else{
                    Toast.makeText(this, "No notes saved!", Toast.LENGTH_SHORT).show()
                }
            }

            binding?.btnSettings?.id -> {
                Toast.makeText(this, "Settings button clicked", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun getSavedNotesFromDB(){
        //TODO

    }

    private fun displaySavedNotesMarkersOnMap() {

        //displaying locations of saved notes on a map
        for (i in 0 until listOfSavedNotes.size) {

            val markerPosition = LatLng(listOfNearbyPlaces[i].placeLatitude.toDouble(),
                listOfNearbyPlaces[i].placeLongitude.toDouble())
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(markerPosition)
                    .title(listOfSavedNotes[i].placeName)
            )
            //TODO: change the tag from Google Place ID to a unique DB identifier
            marker?.tag = listOfSavedNotes[i].googlePlaceID
            marker?.snippet = listOfSavedNotes[i].textNote
        }
    }

    private fun setupNotesListRecyclerView(notesList: ArrayList<LocationNoteModel>) {

        binding?.rvList?.layoutManager = LinearLayoutManager(this@MainActivity)
        val notesAdapter = NotesAdapter(items = notesList)
        binding?.rvList?.setHasFixedSize(true)
        binding?.rvList?.adapter = notesAdapter

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE

        notesAdapter.setOnClickListener(object : NotesAdapter.OnClickListener{
            override fun onClick(position: Int, model: LocationNoteModel) {
                Toast.makeText(this@MainActivity, "clicked on a NOTE", Toast.LENGTH_SHORT).show()

            }
        })
        binding?.rvList?.scheduleLayoutAnimation()
        displaySavedNotesMarkersOnMap()
    }

    //Making sure the location gets displayed on the map if user gives back the location permissions
    override fun onStart() {
        super.onStart()

        if(isLocationEnabled()){
            getFusedUserLocation()
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)
        }
    }

    private fun setupNearbyPlacesRecyclerView(nearbyPlaceList: ArrayList<LocationNoteModel>) {

        binding?.rvList?.layoutManager = StaggeredGridLayoutManager(2, 1)
        val nearbyPlacesAdapter = NearbyPlacesAdapter(items = nearbyPlaceList)

        Log.d("debug", "Adding places: ${nearbyPlaceList.size}, already in view ${nearbyPlacesAdapter.itemCount} ")

        binding?.rvList?.setHasFixedSize(true)
        binding?.rvList?.adapter = nearbyPlacesAdapter

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE


        nearbyPlacesAdapter.setOnClickListener(object : NearbyPlacesAdapter.OnClickListener{
            override fun onClick(position: Int, model: LocationNoteModel) {
                Toast.makeText(this@MainActivity, "clicked on a place", Toast.LENGTH_SHORT).show()

                /*
                val inputEditTextField = EditText(this@MainActivity)
                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("")
                    .setMessage("Please add a note for: ${listOfNearbyPlaces[position].placeName}")
                    .setView(inputEditTextField)
                    .setPositiveButton("OK") { _, _ ->
                        val newNote = LocationNoteModel()
                        val editTextInput = inputEditTextField.text.toString()
                        newNote.textNote = editTextInput
                        newNote.placeName = listOfNearbyPlaces[position].placeName
                        newNote.googlePlaceID = listOfNearbyPlaces[position].googlePlaceID
                        newNote.placeLongitude = listOfNearbyPlaces[position].placeLongitude
                        newNote.placeLatitude = listOfNearbyPlaces[position].placeLatitude
                        newNote.photo = listOfNearbyPlaces[position].photo
                        listOfSavedNotes.add(newNote)

                        Log.d("debug", "Size of listOfSavedNotes:${listOfSavedNotes[listOfSavedNotes.size-1]}, ${editTextInput}")

                        //displaying the whole list of notes after adding new one
                        setupNotesListRecyclerView(listOfSavedNotes)
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                dialog.show()

                 */
                val newNote = LocationNoteModel()

                newNote.textNote = ""
                newNote.placeName = listOfNearbyPlaces[position].placeName
                newNote.googlePlaceID = listOfNearbyPlaces[position].googlePlaceID
                newNote.placeLongitude = listOfNearbyPlaces[position].placeLongitude
                newNote.placeLatitude = listOfNearbyPlaces[position].placeLatitude
                //TODO: decode bitmap to make serializable work

                /*


I know where the problem is in your case and that is about the BitMap All you need to do is Decode your BitMap before sending into intent

Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.thumbsup);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        largeIcon.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

And then send the following Object in intent

intent.putExtras("remindermessage",object);

and if not about the Bitmap then you should look for other things which might be taking more space and decode them before sending into intent

                 */
                //newNote.photo = listOfNearbyPlaces[position].photo

                val intent = Intent(this@MainActivity, NoteEditActivity::class.java)
                intent.putExtra(PLACE_DATA, newNote)
                startActivity(intent)
            }
        })
        nearbyPlacesInRecyclerView = true
        binding?.rvList?.scheduleLayoutAnimation()

    }

    companion object {
        var PLACE_DATA = "place_data"
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}