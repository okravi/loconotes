package com.okravi.loconotes.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
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
import com.okravi.loconotes.Constants
import com.okravi.loconotes.R
import com.okravi.loconotes.adapters.NearbyPlacesAdapter
import com.okravi.loconotes.adapters.NotesAdapter
import com.okravi.loconotes.database.DatabaseHandler
import com.okravi.loconotes.databinding.ActivityMainBinding
import com.okravi.loconotes.models.LocationNoteModel
import com.okravi.loconotes.models.dbNoteModel
import kotlinx.coroutines.*
import pl.kitek.rvswipetodelete.SwipeToDeleteCallback
import pl.kitek.rvswipetodelete.SwipeToEditCallback
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.sqrt

private var binding : ActivityMainBinding? = null
private const val sharedPrefFile = "loconotesSettings"

class MainActivity : AppCompatActivity(), OnMapReadyCallback, View.OnClickListener {
    private lateinit var notesAdapter : NotesAdapter
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
    //number of highlighted Maker in markers array
    private var highlightedMarker: Int = -1
    //creating empty list of places
    private var selectedNotesRV : Int = -1
    var listOfNearbyPlaces = ArrayList<LocationNoteModel>(5)
    //creating empty list of notes to load to from the db
    var listOfSavedNotes = ArrayList<dbNoteModel>(5)

    private var updatePlacesListMethod: String? = "default"
    private var updateNoteListMethod: String? = "default"
    private var sortOrder: String? = "default"
    private var placesPreloaded = false

    private lateinit var lastPositionListUpdatedAt : LatLng
    private var lastTimeListAutoUpdatedBasedOnUserLocation: Long = 0
    private val distanceUserHasToMoveForTheListToAutoUpdate = 0.0014854462
    //minimum time to pass to refresh list with autoupdate turned on
    private val minimumListUpdateDelay = 6000

    private var notesListInView = true
    private var placesListInView = false

    var initialCameraZoomIn: Boolean = true
    var initialLocationResult = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //displaying splashscreen
        installSplashScreen().apply {
            this.setKeepOnScreenCondition {
                runBlocking {
                    delay(1200)
                }
                false
                 }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        checkLocationPermissionsWithDexter()

        //Initializing Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key), Locale.US)
        }

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

        override fun onLocationResult(locationResult: LocationResult){

            //TODO: this may be possible to simplify
            currentLocation = locationResult.lastLocation
            val mLatitude = currentLocation!!.latitude
            val mLongitude = currentLocation!!.longitude
            val position = LatLng(mLatitude, mLongitude)

            if ((initialLocationResult) &&
                (sortOrder == "proximity") &&
                notesListInView){

                lastPositionListUpdatedAt = position
                getNotesListFromLocalDB()
                initialLocationResult = false

                kickOffPlaceListSetupProcess("preloadPlaces")
            }
            //updating notes list if enough time has passed and user moved far enough
            if ((sortOrder == "proximity") &&
                (!initialLocationResult) &&
                (updateNoteListMethod == "automatic") &&
                notesListInView){
                //this condition has to be checked separately!
                if(isItTimeToAutoUpdateList()){
                    lastPositionListUpdatedAt = position
                    calculateNoteProximityToCurrentLocation()
                }
            }

            //updating places list if enough time has passed and user moved far enough
            if ((updatePlacesListMethod == "automatic") &&
                placesListInView){

                //this condition has to be checked separately!
                if(isItTimeToAutoUpdateList()){
                    lastPositionListUpdatedAt = position
                    kickOffPlaceListSetupProcess("showPlaces")
                }
            }

            //Zooming in on user's location
            //animating camera only if the user did not change zoom level
            val zoom: Float = mMap.cameraPosition.zoom
            //not zooming in if user zoomed in/out manually or clicked on a marker
            if (((zoom == 18f) || initialCameraZoomIn) && (highlightedMarker == -1)){
                val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 18f)
                mMap.animateCamera(newLatLngZoom)
                initialCameraZoomIn = false
            }
        }
    }

    //TODO: switch to registerForActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("debug", "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NOTE_EDIT_ACTIVITY_REQUEST_CODE) {
            Log.d("debug", "onActivityResult, resultCode: $requestCode")
            if (resultCode == Activity.RESULT_OK) {

                getNotesListFromLocalDB()
            } else {
                Log.e("Activity", "Cancelled or Back Pressed")
            }
        }
    }

    //Displaying users location on the map. Permission status saved to $locationPermissionsOK
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        if (locationPermissionsOK) {
            mMap = googleMap
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = false

            //reading the db and setting up the notes rv
            if(sortOrder != "proximity"){
                getNotesListFromLocalDB()
            }

            //if clicked on My Location button, setup markers once again and center on user's loc
            mMap.setOnMyLocationButtonClickListener() {
                highlightedMarker = -1
                displaySavedNotesMarkersOnMap()
                true
            }

            //when clicked on a marker show details and change marker's color
            mMap.setOnMarkerClickListener { marker ->

                marker.showInfoWindow()
                //looking up marker in listOfSavedNotes and highlighting it
                for (i in listOfSavedNotes.indices){
                    if (listOfSavedNotes[i].marker?.id == marker.id){
                        highlightClickedNoteMarkerOnMap(i)
                    }
                }
                true
            }
/*
            //testing preloading places
            lifecycleScope.launch {
                kickOffPlaceListSetupProcess("preloadPlaces")
            }

 */

        }else{
            checkLocationPermissionsWithDexter()
        }
    }

    override fun onResume() {

        super.onResume()
        //reading settings to see if lists should be auto updated when user moves
        updatePlacesListMethod = readSetting("updatePlacesListMethod")
        updateNoteListMethod = readSetting("updateNoteListMethod")
        sortOrder = readSetting("sortOrder")

        if (locationPermissionsOK) {
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)

        }
        //to let the same item be selected again when user gets back to the MainActivity
        selectedNotesRV = -1


    }

    //Getting a list of locations closest to the user's current location. Permissions already checked.
    @SuppressLint("MissingPermission")
    private suspend fun getListOfLocationsForCurrentPosition(parameter: String): List<LocationNoteModel> = withContext(Dispatchers.IO){

        var placesWithPhotosCounter = 0
        var bitmapsSavedCounter = 0
        var cycleCounter = 0
        //Client that exposes the Places API methods
        val placesClient = Places.createClient(this@MainActivity)
        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.LAT_LNG,
            Place.Field.ID, Place.Field.ADDRESS, Place.Field.PHOTO_METADATAS)
        // Use the builder to create a FindCurrentPlaceRequest.
        val requestNearbyPlaces: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
        val placeResponse = placesClient.findCurrentPlace(requestNearbyPlaces)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {

                //making sure we don't display nearby items twice
                listOfNearbyPlaces.clear()

                //adding empty place
                val emptyPlace = LocationNoteModel()
                listOfNearbyPlaces.add(emptyPlace)

                listOfNearbyPlaces[0].placeLatitude = currentLocation!!.latitude.toString()
                listOfNearbyPlaces[0].placeLongitude = currentLocation!!.longitude.toString()
                listOfNearbyPlaces[0].googlePlaceID = "none"

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

                                        if (bitmapsSavedCounter == placesWithPhotosCounter){
                                            //testing
                                            placesPreloaded = true

                                            if (parameter == "showPlaces"){
                                                placesPreloaded = false
                                                setupNearbyPlacesRecyclerView(listOfNearbyPlaces)
                                            }
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
                            }
                        }
                    }
                }
            } else {
                Log.d("debug", "Place not found")
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

        return@withContext listOfNearbyPlaces
    }


    override fun onClick(v: View?) {
        when (v!!.id) {
            binding?.btnAddNote?.id -> {

                kickOffPlaceListSetupProcess("showPlaces")
            }

            binding?.btnListNotes?.id -> {

                getNotesListFromLocalDB()
                kickOffPlaceListSetupProcess("preloadPlaces")
            }

            binding?.btnSettings?.id -> {

                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun kickOffPlaceListSetupProcess(parameter: String){

        if ((!placesPreloaded) && (parameter == "preloadPlaces")){

            if(isLocationEnabled() && Constants.isNetworkAvailable(this)) {

                lifecycleScope.launch {
                    getListOfLocationsForCurrentPosition("preloadPlaces")
                }

            }else{
                Toast.makeText(this,
                    "Please grant the location permissions and switch on the network access!",
                    Toast.LENGTH_SHORT).show()
            }
        }

        if((placesPreloaded) && (parameter == "showPlaces")){

            setupNearbyPlacesRecyclerView(listOfNearbyPlaces)
        }

        if((!placesPreloaded) && (parameter == "showPlaces")){

            if((isLocationEnabled()) && Constants.isNetworkAvailable(this)){

                lifecycleScope.launch {
                    getListOfLocationsForCurrentPosition("showPlaces")
                }
            }else{
                Toast.makeText(this,
                    "Please grant the location permissions and switch on the network access!",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    //return highlighted marker back to original state
    private fun setSelectedMarkerBackToDefault(position: Int){
        if (highlightedMarker != -1){

            listOfSavedNotes[highlightedMarker].marker?.setIcon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

            listOfSavedNotes[position].marker?.hideInfoWindow()

        }
        highlightedMarker = -1
    }

    private fun highlightClickedNoteMarkerOnMap(position: Int) {

        //making sure there's no more than 1 highlighted marker
        if (highlightedMarker != -1){
            listOfSavedNotes[highlightedMarker].marker?.setIcon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        }
        //highlighting marker
        listOfSavedNotes[position].marker?.showInfoWindow()
        listOfSavedNotes[position].marker?.setIcon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        //saving highlighted marker position for future use
        highlightedMarker = position

        //zooming in on a highlighted marker
        val highlightedMarkerPosition = listOfSavedNotes[position].marker?.position
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(highlightedMarkerPosition!!, 18f)
        mMap.animateCamera(newLatLngZoom)
    }

    //displaying locations of saved notes on a map
    private fun displaySavedNotesMarkersOnMap() {

        //deleting all already shown markers
        mMap.clear()
        for (note in listOfSavedNotes){
            note.marker = null
        }

        //resetting highlighted marker id
        highlightedMarker = -1

        if (listOfSavedNotes.size >0){

            for (i in 0 until listOfSavedNotes.size) {

                val markerPosition = LatLng(listOfSavedNotes[i].placeLatitude.toDouble(),
                    listOfSavedNotes[i].placeLongitude.toDouble())

                val newMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(markerPosition)
                        .title(listOfSavedNotes[i].placeName)
                )
                //saving marker
                listOfSavedNotes[i].marker = newMarker
                listOfSavedNotes[i].marker?.tag = listOfSavedNotes[i].googlePlaceID
                listOfSavedNotes[i].marker?.snippet = listOfSavedNotes[i].textNote
            }
        }
    }

    private fun setupNotesListRecyclerView(notesList: ArrayList<dbNoteModel>) {

        binding?.rvList?.layoutManager = LinearLayoutManager(this@MainActivity)
        notesAdapter = NotesAdapter(items = notesList, context = this@MainActivity)

        notesListInView = true
        placesListInView = false

        binding?.rvList?.setHasFixedSize(true)
        binding?.rvList?.adapter = notesAdapter

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE

        notesAdapter.setOnClickListener(object : NotesAdapter.OnClickListener{
            override fun onClick(position: Int, model: dbNoteModel) {
                Log.d("string", "notesAdapter.setOnClickListener, clicked on $position")
                //highlighting clicked on and relevant marker
                if (!notesList[position].isSelected){

                    highlightClickedNoteMarkerOnMap(position)
                    notesList[position].isSelected = true
                    notesAdapter.notifyItemChanged(position)

                }else{
                    setSelectedMarkerBackToDefault(position)
                    notesList[position].isSelected = false
                    notesAdapter.notifyItemChanged(position)
                }

                //deselecting previously selected note rv
                if ((selectedNotesRV != -1) && (selectedNotesRV != position)){
                    notesList[selectedNotesRV].isSelected = false
                    notesAdapter.notifyItemChanged(selectedNotesRV)
                }
                selectedNotesRV = position
            }
        })
        binding?.rvList?.scheduleLayoutAnimation()

        displaySavedNotesMarkersOnMap()

        val editSwipeHandler = object: SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvList?.adapter as NotesAdapter
                adapter.notifyEditItem(this@MainActivity, viewHolder.adapterPosition, NOTE_EDIT_ACTIVITY_REQUEST_CODE)
            }
        }

        val editItemTouchHandler = ItemTouchHelper(editSwipeHandler)
        editItemTouchHandler.attachToRecyclerView(binding?.rvList)


        val deleteSwipeHandler = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvList?.adapter as NotesAdapter

                //removing marker of the deleted note
                listOfSavedNotes[viewHolder.adapterPosition].marker?.remove()

                val adapterPosition = viewHolder.adapterPosition
                //removing rv
                adapter.removeAt(adapterPosition)
                //commented out to avoid unintended animation of the last rv, delete this if all ok
                //adapter.notifyItemRemoved(adapterPosition+1)
                adapter.notifyItemRangeChanged(adapterPosition+1, adapter.itemCount)

                //in case previously selected RV was deleted
                if (selectedNotesRV == adapterPosition){
                    selectedNotesRV = -1
                }
                //in case previously highlighted marker was deleted
                if (highlightedMarker == adapterPosition){
                    highlightedMarker = -1
                }
                //correcting selected notes rv position
                if ((selectedNotesRV > adapterPosition) && (selectedNotesRV != -1)){
                    selectedNotesRV -= 1
                }
                //if no items left, show tvNoRecordsAvailable message
                if (adapter.itemCount < 1 ){
                    binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
                    binding?.rvList?.visibility = View.GONE
                }
            }
        }

        val deleteItemTouchHandler = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHandler.attachToRecyclerView(binding?.rvList)

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE
    }

    //Making sure the location gets displayed on the map if user gives back the location permissions
    override fun onStart() {
        super.onStart()
        /*
        if(locationPermissionsOK){
            getFusedUserLocation()
            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)
        }
         */
    }

    private fun setupNearbyPlacesRecyclerView(nearbyPlaceList: ArrayList<LocationNoteModel>) {

        binding?.rvList?.layoutManager = StaggeredGridLayoutManager(2, 1)
        val nearbyPlacesAdapter = NearbyPlacesAdapter(items = nearbyPlaceList)

        notesListInView = false
        placesListInView = true

        binding?.rvList?.setHasFixedSize(true)
        binding?.rvList?.adapter = nearbyPlacesAdapter

        binding?.tvNoRecordsAvailable?.visibility = View.GONE
        binding?.rvList?.visibility = View.VISIBLE

        nearbyPlacesAdapter.setOnClickListener(object : NearbyPlacesAdapter.OnClickListener{
            override fun onClick(position: Int, model: LocationNoteModel) {

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
                startActivityForResult(intent, NOTE_EDIT_ACTIVITY_REQUEST_CODE)

            }
        })
        nearbyPlacesInRecyclerView = true
        binding?.rvList?.scheduleLayoutAnimation()
    }

    //reading the whole DB to listOfSavedNotes
    private fun getNotesListFromLocalDB(){

        val dbHandler = DatabaseHandler(this)
        listOfSavedNotes.clear()
        listOfSavedNotes = dbHandler.getNotesList()

        if(listOfSavedNotes.size > 0){
            binding?.tvNoRecordsAvailable?.visibility = View.GONE
            binding?.rvList?.visibility = View.VISIBLE
            sortNotes()
        }else{
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
            binding?.rvList?.visibility = View.GONE
        }
    }

    private fun calculateNoteProximityToCurrentLocation(){
        //TODO: FIX, current loc is not available yet when this executes

        if ((listOfSavedNotes.size > 0) && (currentLocation != null)){

            val mLatitude = currentLocation!!.latitude.toFloat()
            val mLongitude = currentLocation!!.longitude.toFloat()
            for (note in listOfSavedNotes){

                val noteLatitude = note.placeLatitude.toFloat()
                val noteLongitude = note.placeLongitude.toFloat()

                val a = (mLatitude.minus(noteLatitude)).times(mLatitude.minus(noteLatitude))
                val b = (mLongitude.minus(noteLongitude)).times(mLongitude.minus(noteLongitude))
                val distance = sqrt(a.plus(b))

                note.proximity = distance
            }

            listOfSavedNotes.sortWith(compareBy { it.proximity })

            setupNotesListRecyclerView(listOfSavedNotes)

        }else{
            return
        }
    }

    //sorting notes based on saved settings
    private fun sortNotes(){
        when (readSetting("sortOrder")) {
            "default" -> {
                calculateNoteProximityToCurrentLocation()
            }
            "proximity" -> {

                calculateNoteProximityToCurrentLocation()
            }
            "placeName" -> {
                listOfSavedNotes.sortWith(
                    compareBy { it.placeName })
                setupNotesListRecyclerView(listOfSavedNotes)
            }
            "dateModified" -> {
                listOfSavedNotes.sortWith(
                    compareBy<dbNoteModel> { it.dateNoteLastModified }.reversed())

                setupNotesListRecyclerView(listOfSavedNotes)
            }
        }
    }

    private fun readSetting(setting: String): String? {
        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)

        return sharedPreferences.getString(setting, "default")
    }

    //checking if it is OK to auto update list (by time & distance)
    private fun isItTimeToAutoUpdateList(): Boolean{

        val timePassedSinceLastAutoUpdate = Calendar.getInstance().timeInMillis - lastTimeListAutoUpdatedBasedOnUserLocation
        //if enough time has passed
        if (timePassedSinceLastAutoUpdate > minimumListUpdateDelay){

            val cLatitude = currentLocation!!.latitude
            val cLongitude = currentLocation!!.longitude
            val lLatitude = lastPositionListUpdatedAt.latitude
            val lLongitude = lastPositionListUpdatedAt.longitude

            val a = (cLatitude.minus(lLatitude)).times(cLatitude.minus(lLatitude))
            val b = (cLongitude.minus(cLongitude)).times(cLongitude.minus(lLongitude))
            val distanceUserMovedSinceLastListUpdate = sqrt(a.plus(b))
            //if user moved far enough too
            if (distanceUserMovedSinceLastListUpdate > distanceUserHasToMoveForTheListToAutoUpdate) {

                //saving current time as the moment the list was updated on
                lastTimeListAutoUpdatedBasedOnUserLocation = Calendar.getInstance().timeInMillis
                //saving current location as the last position the list was updated at
                lastPositionListUpdatedAt = LatLng(cLatitude, cLongitude)

                return true
            }
        }

        return false
    }

    companion object {
        var PLACE_DATA = "place_data"
        var NOTE_DATA = "note_data"
        var NOTE_EDIT_ACTIVITY_REQUEST_CODE = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}