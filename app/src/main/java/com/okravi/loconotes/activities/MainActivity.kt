package com.okravi.loconotes.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.okravi.loconotes.database.DatabaseHandler
import com.okravi.loconotes.databinding.ActivityMainBinding
import com.okravi.loconotes.models.LocationNoteModel
import com.okravi.loconotes.models.dbNoteModel
import pl.kitek.rvswipetodelete.SwipeToEditCallback
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

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
    private var locationPermissionsOK = false
    private var currentLocation: Location? = null
    private val maxNumberOfNearbyPlacesToShowUser = 30
    private var nearbyPlacesInRecyclerView: Boolean = false
    //creating empty list of places
    var listOfNearbyPlaces = ArrayList<LocationNoteModel>(5)
    //creating empty list of notes to load to from the db
    var listOfSavedNotes = ArrayList<dbNoteModel>(5)

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
        //displaying saved notes
        //getNotesListFromLocalDB()
    }


    //Checking if location service is enabled on the device
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //Checking whether user granted the location permissions for the app
    private fun checkLocationPermissionsWithDexter() {

        if (!isLocationEnabled()) {

            Toast.makeText(
                this,
                "The location service is disabled on the device. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This redirects to settings where user needs to turn on the location provider
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            //Checking location permissions for the app
            Dexter.withActivity(this).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        locationPermissionsOK = true
                        //getting user location
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
            .setMessage("It looks like the location permissions weren't granted.")
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

    //LocationCallback - Called when FusedLocationProviderClient has a new Location
    private val mLocationCallBack = object : LocationCallback(){
        var initialCameraZoomIn: Boolean = true

        override fun onLocationResult(locationResult: LocationResult){

            currentLocation = locationResult.lastLocation
            val mLatitude = currentLocation!!.latitude
            val mLongitude = currentLocation!!.longitude
            val position = LatLng(mLatitude, mLongitude)
            //Zooming in on user's location
            //animating camera only if the user did not change zoom level
            val zoom: Float = mMap.cameraPosition.zoom
            //not zooming in if user zoomed in/out manually
            if ((zoom == 18f) || initialCameraZoomIn){
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
                mMap.animateCamera(newLatLngZoom)
                initialCameraZoomIn = false
            }
        }
    }


    //Displaying users location on the map. Permission status saved to $locationPermissionsOK
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("markers", "were in onMapReady")
        if (locationPermissionsOK) {
            Log.d("markers", "were in onMapReady by the location perms are OK")
            Log.d("debug", "locationPermissions:$locationPermissionsOK")
            mMap = googleMap
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = false
            //TODO: only show markers here, don't read the whole DB
            getNotesListFromLocalDB()


            mMap.setOnMarkerClickListener { marker ->
                if (marker.isInfoWindowShown) {
                    marker.hideInfoWindow()
                    Toast.makeText(this, "clicked on a marker ${marker.tag}", Toast.LENGTH_SHORT)
                        .show()
                }else{
                    marker.showInfoWindow()
                    Toast.makeText(this, "clicked on a marker ${marker.tag}", Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }
        }else{
            Log.d("markers", "were in onMapReady by the location perms are off")
        }
    }

    override fun onResume() {
        //TODO check if this is really needed (when user gives the permission)
        super.onResume()
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        Log.d("debug", "resuming")
        //getNotesListFromLocalDB()
    }

    //Getting a list of locations closest to the user's current location. Checking permissions
    //in the onClick function
    @SuppressLint("MissingPermission")
    private fun getListOfLocationsForCurrentPosition(): List<LocationNoteModel>{
        Log.d("debug", "getListOfLocationsForCurrentPosition/start" )
        var placesWithPhotosCounter = 0
        var bitmapsSavedCounter = 0
        var cycleCounter = 0
        //Client that exposes the Places API methods
        val placesClient = Places.createClient(this)
        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.LAT_LNG,
            Place.Field.ID, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS)
        // Use the builder to create a FindCurrentPlaceRequest.
        val requestNearbyPlaces: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
            val placeResponse = placesClient.findCurrentPlace(requestNearbyPlaces)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val response = task.result
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods ?: emptyList()) {

                        //Going through $maxNumberOfNearbyPlacesToShowUser and saving if they have photos
                        when {
                            cycleCounter<maxNumberOfNearbyPlacesToShowUser -> {

                                val nearbyLocation = LocationNoteModel()
                                nearbyLocation.googlePlaceID = placeLikelihood.place.id!!.toString()
                                nearbyLocation.placeName = placeLikelihood.place.name!!
                                nearbyLocation.placeLatitude = placeLikelihood.place.latLng!!
                                    .latitude.toString()
                                nearbyLocation.placeLongitude = placeLikelihood.place.latLng!!
                                    .longitude.toString()
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
                Log.d("debug", "clicked on add note" )
                //making sure we don't display nearby items twice
                listOfNearbyPlaces.clear()

                if(isLocationEnabled()) {
                    Log.d("debug", "location is enabled, proceeding" )
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
                    //TODO fix the this logic, it's for testing only
                    getNotesListFromLocalDB()
                }
            }

            binding?.btnSettings?.id -> {
                Toast.makeText(this, "Settings button clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displaySavedNotesMarkersOnMap() {

        //displaying locations of saved notes on a map
        Log.d("markers frm database", listOfSavedNotes.size.toString())

        for (i in 0 until listOfSavedNotes.size) {
            Log.d("setting up marker #", i.toString())
            val markerPosition = LatLng(listOfSavedNotes[i].placeLatitude.toDouble(),
                listOfSavedNotes[i].placeLongitude.toDouble())
            Log.d("marker pos", "$markerPosition")
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(markerPosition)
                    .title(listOfSavedNotes[i].placeName)
            )
            marker?.tag = listOfSavedNotes[i].googlePlaceID
            marker?.snippet = listOfSavedNotes[i].textNote

        }


    }

    private fun setupNotesListRecyclerView(notesList: ArrayList<dbNoteModel>) {

        binding?.rvList?.layoutManager = LinearLayoutManager(this@MainActivity)
        val notesAdapter = NotesAdapter(items = notesList, context = this@MainActivity)
        binding?.rvList?.setHasFixedSize(true)
        binding?.rvList?.adapter = notesAdapter

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE

        notesAdapter.setOnClickListener(object : NotesAdapter.OnClickListener{
            override fun onClick(position: Int, model: dbNoteModel) {
                Toast.makeText(this@MainActivity, "clicked on a NOTE ${notesList[position].keyID}", Toast.LENGTH_SHORT).show()

            }
        })
        binding?.rvList?.scheduleLayoutAnimation()
        displaySavedNotesMarkersOnMap()

        val editSwipeHandler = object: SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                //TODO: simplify this
                val adapter = notesAdapter as NotesAdapter
                adapter.notifyEditItem(this@MainActivity, viewHolder.adapterPosition, NOTE_EDIT_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    //Making sure the location gets displayed on the map if user gives back the location permissions
    //TODO: check if this is actually working
    override fun onStart() {
        super.onStart()

        if(locationPermissionsOK){
            getFusedUserLocation()
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)
        }
    }

    private fun setupNearbyPlacesRecyclerView(nearbyPlaceList: ArrayList<LocationNoteModel>) {

        binding?.rvList?.layoutManager = StaggeredGridLayoutManager(2, 1)
        val nearbyPlacesAdapter = NearbyPlacesAdapter(items = nearbyPlaceList)

        binding?.rvList?.setHasFixedSize(true)
        binding?.rvList?.adapter = nearbyPlacesAdapter

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE

        nearbyPlacesAdapter.setOnClickListener(object : NearbyPlacesAdapter.OnClickListener{
            override fun onClick(position: Int, model: LocationNoteModel) {
                Toast.makeText(this@MainActivity, "clicked on a place", Toast.LENGTH_SHORT).show()

                val newNote = LocationNoteModel()

                newNote.textNote = ""
                newNote.placeName = listOfNearbyPlaces[position].placeName
                newNote.googlePlaceID = listOfNearbyPlaces[position].googlePlaceID
                newNote.placeLongitude = listOfNearbyPlaces[position].placeLongitude
                newNote.placeLatitude = listOfNearbyPlaces[position].placeLatitude

                val stream = ByteArrayOutputStream()
                listOfNearbyPlaces[position].photo?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray: ByteArray = stream.toByteArray()

                newNote.photoByteArray = byteArray

                val intent = Intent(this@MainActivity, NoteEditActivity::class.java)
                intent.putExtra(PLACE_DATA, newNote)
                startActivity(intent)
            }
        })
        nearbyPlacesInRecyclerView = true
        binding?.rvList?.scheduleLayoutAnimation()
    }

    private fun getNotesListFromLocalDB(){

        val dbHandler = DatabaseHandler(this)
        val notesListTester : ArrayList<dbNoteModel> = dbHandler.getNotesList()
        Log.d("RCVD database:", notesListTester.size.toString())
        // = dbHandler.getNotesList()

        if(notesListTester.size > 0){
            listOfSavedNotes = notesListTester
            setupNotesListRecyclerView(listOfSavedNotes)
        }else {
            return
        }
    }

    companion object {
        var PLACE_DATA = "place_data"
        var NOTE_EDIT_ACTIVITY_REQUEST_CODE = 1
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}