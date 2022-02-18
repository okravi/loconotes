package com.okravi.loconotes.activities

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.okravi.loconotes.R
import com.okravi.loconotes.database.DatabaseHandler
import com.okravi.loconotes.databinding.ActivityNoteEditBinding
import com.okravi.loconotes.models.LocationNoteModel
import com.okravi.loconotes.models.dbNoteModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private var binding : ActivityNoteEditBinding? = null

class NoteEditActivity : AppCompatActivity(), View.OnClickListener {

    private var savedImagePath : Uri? = null
    private lateinit var noteData: dbNoteModel
    private lateinit var placeData: LocationNoteModel
    private lateinit var bmp: Bitmap
    private var creatingNewNote: Boolean = false
    private var customNote: Boolean = false
    private lateinit var noteFromDB : ArrayList<dbNoteModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.btnLoadPicture?.setOnClickListener(this)
        binding?.btnTakePhoto?.setOnClickListener(this)
        binding?.btnSaveNote?.setOnClickListener(this)

        //getting place data based on the clicked recyclerview position
        if(intent.hasExtra(MainActivity.PLACE_DATA)){
            creatingNewNote = true
            placeData = intent.getSerializableExtra(
                MainActivity.PLACE_DATA) as LocationNoteModel

            //displaying place data sent from MainActivity
            Log.d("debug", "placeData.placeName:${placeData.placeName}")
            binding?.etPlaceName?.setText(placeData.placeName)
            binding?.etLatitude?.setText("Lat:${placeData.placeLatitude}")
            binding?.etLongitude?.setText("Lon:${placeData.placeLongitude}")

            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val netDate = Calendar.getInstance().timeInMillis
            val date = sdf.format(netDate)

            binding?.etDateModified?.setText(date.toString())

            //if the note is not custom, showing the saved image
            if (placeData.photoByteArray!!.isNotEmpty()){
                customNote = false
                val byteArray = placeData.photoByteArray
                bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

                //testing
                val pictureWidth = bmp.getWidth()
                val pictureHeight = bmp.getHeight()
                Log.d("debug", "pictureHeight: $pictureHeight, pictureWidth:$pictureWidth")
                val pictureSidesRatio : Float = pictureHeight.toFloat() / pictureWidth.toFloat()
                val imageViewWidth = (pictureSidesRatio).toInt()
                Log.d("debug", "imageWidth:$imageViewWidth")

                Log.d("debug:", "250dp in px is ${250.toInt().toDP(this)}")

                val neededHeightInPx = 250.toInt().toDP(this)

                //binding?.placePhoto?.layoutParams?.width = imageViewWidth.toPx(this)
                binding?.photoWidget?.layoutParams?.width = (neededHeightInPx / pictureSidesRatio).roundToInt()
                binding?.photoWidget?.layoutParams?.height = neededHeightInPx




                binding?.placePhoto?.setImageBitmap(bmp)


            }else{
                //to allow saving a note with no photo
                customNote = true
            }
        }
        //getting note data based on the swiped recyclerview position
        if(intent.hasExtra(MainActivity.NOTE_DATA)) {
            noteData = intent.getSerializableExtra(
                MainActivity.NOTE_DATA
            ) as dbNoteModel

            //Reading note from DB based on keyID
            val dbHandler = DatabaseHandler(this)
            noteFromDB = dbHandler.getNote(noteData.keyID.toInt())

            //Displaying Note
            binding?.etPlaceName?.setText(noteFromDB[0].placeName)
            binding?.etLatitude?.setText(noteFromDB[0].placeLatitude)
            binding?.etLongitude?.setText(noteFromDB[0].placeLongitude)
            binding?.etNote?.setText(noteFromDB[0].textNote)

            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val netDate = Date(noteFromDB[0].dateNoteLastModified)
            val date = sdf.format(netDate)

            binding?.etDateModified?.setText(date.toString())

            val noteUri = noteFromDB[0].photo.toUri()

            //if there's a valid URI, show saved photo
            if (noteFromDB[0].photo.length > 5 ){
                binding?.placePhoto?.setImageURI(noteUri)
            //else show a placeholder
            }else{
                binding?.placePhoto?.setImageResource(
                        R.drawable.ic_custom_rv_note_image_placeholder)
            }
            //image URI from DB
            savedImagePath = noteFromDB[0].photo.toUri()
        }
    }

    //convert px tp dp
    private fun Int.toDP(context: Context) = this *
            context.resources.displayMetrics.densityDpi /
            DisplayMetrics.DENSITY_DEFAULT

    override fun onResume() {
        super.onResume()
        //graying out coordinates fields
        binding?.etLatitude?.isEnabled = false
        binding?.etLongitude?.isEnabled = false
    }

    override fun onClick(v: View?) {
        when (v!!.id){
            binding?.btnLoadPicture?.id ->
            {
                choosePhotoFromGallery()
            }

            binding?.btnTakePhoto?.id ->
            {
                takePhoto()
            }
            binding?.btnSaveNote?.id ->

            {
                //if some fields are not filled show toast
                if ((binding?.etPlaceName?.text.isNullOrEmpty() ||
                            binding?.etLatitude?.text.isNullOrEmpty() ||
                            binding?.etLongitude?.text.isNullOrEmpty() ||
                            binding?.etNote?.text.isNullOrEmpty())
                ) {
                    Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
                } else {
                    //saving default image if there's one
                    if((savedImagePath == null) && (!customNote)){

                        //saving Places image if user did not choose an alternative
                        savedImagePath = saveImageToInternalStorage(bmp)
                    }
                    //saving note to the db
                    val dbNoteModel = dbNoteModel(
                            (if(creatingNewNote) "0" else {noteFromDB[0].keyID}),
                    (if(creatingNewNote) placeData.googlePlaceID else noteFromDB[0].googlePlaceID),
                    binding?.etPlaceName?.text.toString(),
                    binding?.etLatitude?.text.toString(),
                    binding?.etLongitude?.text.toString(),
                    Calendar.getInstance().timeInMillis,
                    binding?.etNote?.text.toString(),
                    savedImagePath.toString(),
                    )

                    val dbHandler = DatabaseHandler(this)

                    when {
                        creatingNewNote -> {
                            val addNote = dbHandler.addNote(dbNoteModel)

                            if(addNote > 0){
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }

                        !creatingNewNote -> {
                            val updateNote = dbHandler.updateNote(dbNoteModel)
                            if(updateNote > 0){

                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }

                }
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when (requestCode) {
                //displaying locally stored image chosen by user
                GALLERY -> {
                    if(data != null){
                        val contentURI = data.data

                        try {
                            bmp = MediaStore.Images.Media
                                .getBitmap(this.contentResolver, contentURI)
                            savedImagePath = saveImageToInternalStorage(bmp)
                            binding?.placePhoto?.setImageBitmap(bmp)
                        }catch (e: IOException){
                            e.printStackTrace()
                            Toast.makeText(this, "Failed to load the image",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                //displaying photo taken by camera
                CAMERA -> {
                   bmp = data!!.extras!!.get("data") as Bitmap
                    savedImagePath = saveImageToInternalStorage(bmp)
                    binding?.placePhoto?.setImageBitmap(bmp)
                }
            }
        }
    }

    private fun choosePhotoFromGallery() {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object: MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {if (report!!.areAllPermissionsGranted()){
                val galleryIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, GALLERY)
            }
            }
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken)
            {
                showRationaleDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun takePhoto(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object: MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?)
            {if (report!!.areAllPermissionsGranted()){
                val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(galleryIntent, CAMERA)
            }
            }
            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken)
            {
                showRationaleDialogForPermissions()
            }
        }).onSameThread().check()
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

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    companion object {
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "LoconotesImages"
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}