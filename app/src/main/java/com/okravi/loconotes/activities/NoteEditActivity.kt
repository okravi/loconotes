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
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.okravi.loconotes.database.DatabaseHandler
import com.okravi.loconotes.databinding.ActivityNoteEditBinding
import com.okravi.loconotes.models.LocationNoteModel
import com.okravi.loconotes.models.dbNoteModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*


private var binding : ActivityNoteEditBinding? = null

class NoteEditActivity : AppCompatActivity(), View.OnClickListener {

    private var savedImagePath : Uri? = null
    private lateinit var placeData: LocationNoteModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        //getting place data based on the clicked recyclerview position


        if(intent.hasExtra(MainActivity.PLACE_DATA)){
            placeData = intent.getSerializableExtra(
                MainActivity.PLACE_DATA) as LocationNoteModel

            binding?.etPlaceName?.setText(placeData.placeName)
            binding?.etLatitude?.setText(placeData.placeLatitude)
            binding?.etLongitude?.setText(placeData.placeLongitude)

            val byteArray = placeData.photoByteArray
            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

            binding?.placePhoto?.setImageBitmap(bmp)
            binding?.btnLoadPicture?.setOnClickListener(this)
            binding?.btnTakePhoto?.setOnClickListener(this)
            binding?.btnSaveNote?.setOnClickListener(this)


        }
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
                Toast.makeText(this, "SUBMIT button clicked", Toast.LENGTH_SHORT).show()
//testing
                when{
                    (binding?.etPlaceName?.text.isNullOrEmpty() ||
                            binding?.etLatitude?.text.isNullOrEmpty() ||
                            binding?.etLongitude?.text.isNullOrEmpty() ||
                            binding?.etNote?.text.isNullOrEmpty()) -> {
                        Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
                    }
                    //TODO: there's still a case when image is loaded from Places
                    savedImagePath == null -> {
                        Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                    }else ->{
                    Log.d("debug", "starting to save to db, title is going to be ${binding?.etPlaceName.toString()}")
                    val dbNoteModel = dbNoteModel(
                        "0",
                        placeData.googlePlaceID,
                        binding?.etPlaceName?.text.toString(),
                        binding?.etLatitude?.text.toString(),
                        binding?.etLongitude?.text.toString(),
                        Calendar.getInstance().time.toString(),
                        binding?.etNote?.text.toString(),
                        savedImagePath.toString(),
                    )
                    val dbHandler = DatabaseHandler(this)

//testing this
                    if (null == null){

                        val addNote = dbHandler.addNote(dbNoteModel)
                        Log.d("debug", "just saved to the db, save result is $addNote")
                        if(addNote > 0){
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }else{
                        setResult(Activity.RESULT_CANCELED)
                        val updateHappyPlace = dbHandler.updateNote(dbNoteModel)

                        if(updateHappyPlace > 0){

                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }



                }

                }
                //testing
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when (requestCode) {
                //displaying the selected image
                GALLERY -> {
                    if(data != null){
                        val contentURI = data.data
                        val testURI = data.extras
                        try {
                            val selectedImageBitmap = MediaStore.Images.Media
                                .getBitmap(this.contentResolver, contentURI)
                            savedImagePath = saveImageToInternalStorage(selectedImageBitmap)
                            Log.d("debug: ", "PAth :: $savedImagePath")
                            binding?.placePhoto?.setImageBitmap(selectedImageBitmap)
                        }catch (e: IOException){
                            e.printStackTrace()
                            Toast.makeText(this, "Failed to load the image",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                CAMERA -> {
                    val thumbnail : Bitmap = data!!.extras!!.get("data") as Bitmap

                    savedImagePath = saveImageToInternalStorage(thumbnail)
                    Log.d("Saved image: ", "PAth :: $savedImagePath")

                    binding?.placePhoto?.setImageBitmap(thumbnail)
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