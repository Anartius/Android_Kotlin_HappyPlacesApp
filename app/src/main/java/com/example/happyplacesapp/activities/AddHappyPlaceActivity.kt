package com.example.happyplacesapp.activities


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplacesapp.Constants
import com.example.happyplacesapp.R
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.models.LocationModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location: LocationModel? = null
    private var savedImageUri: Uri? = null
    private var latitude = 0.0
    private var longitude = 0.0

    private var placeDetails: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            placeDetails = if (SDK_INT >= 33) {
                intent.getParcelableExtra(
                    MainActivity.EXTRA_PLACE_DETAILS, HappyPlaceModel::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
            }
        }

        dateSetListener = DatePickerDialog.OnDateSetListener {
                _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }

        updateDateInView()

        if (placeDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            binding.etTitle.setText(placeDetails!!.title)
            binding.etDescription.setText(placeDetails!!.description)
            binding.etDate.setText(placeDetails!!.date)
            binding.etLocation.setText(placeDetails!!.location)
            latitude = placeDetails!!.latitude
            longitude = placeDetails!!.longitude

            savedImageUri = Uri.parse(placeDetails!!.image)
            binding.ivImage.setImageURI(savedImageUri)

            binding.btnSave.text = getString(R.string.btn_update)
        }

        binding.etDate.setOnClickListener(this@AddHappyPlaceActivity)

        binding.tvAddImage.setOnClickListener(this@AddHappyPlaceActivity)

        binding.btnSave.setOnClickListener(this@AddHappyPlaceActivity)

        binding.etLocation.setOnClickListener(this@AddHappyPlaceActivity)

        binding.btnSelectCurrentLocation.setOnClickListener(this@AddHappyPlaceActivity)
    }

    override fun onClick(v: View?) {
        when(v) {
            binding.etDate -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            binding.tvAddImage -> {
                val pictureDialog = AlertDialog.Builder(this@AddHappyPlaceActivity)
                pictureDialog.setTitle("Select Action")

                val pictureDialogItems = arrayOf(
                    "Select photo from gallery",
                    "Capture photo from camera"
                )

                pictureDialog.setItems(pictureDialogItems) {
                        _, itemPosition ->
                    when(itemPosition) {
                        0 -> pickImageFromGallery()
                        1 -> getPhotoFromCamera()
                    }
                }

                pictureDialog.show()
            }

            binding.btnSelectCurrentLocation -> {
                getLocation(true)
            }

            binding.btnSave -> {
                when {
                    binding.etTitle.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this, "Please enter title", Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.etDescription.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this, "Please enter a description", Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.etLocation.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this, "Please enter a location", Toast.LENGTH_SHORT
                        ).show()
                    }
                    savedImageUri == null -> {
                        Toast.makeText(
                            this, "Please select an image", Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val happyPlaceModel = HappyPlaceModel(
                            if (placeDetails == null) 0 else placeDetails!!.id,
                            binding.etTitle.text.toString(),
                            savedImageUri.toString(),
                            binding.etDescription.text.toString(),
                            binding.etDate.text.toString(),
                            binding.etLocation.text.toString(),
                            latitude,
                            longitude
                        )

                        val dbHandler = DatabaseHandler(this)

                        if (placeDetails == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)

                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)

                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }

            binding.etLocation -> {
                try {
                    getLocation(false)
                } catch (e:Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateDateInView() {
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.etDate.setText(sdf.format(cal.time)).toString()
    }

    private val permissions = if (SDK_INT >= 33) {
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        listOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        )
    }

    val pickImageFromGalleryForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val imageUri = intent?.data

            try {
                val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            contentResolver, imageUri!!
                        )
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }
                savedImageUri = saveImageToInternalStorage(bitmap)
                Log.e("Saved image:", "Path:: $savedImageUri")
                binding.ivImage.setImageBitmap(bitmap)

            } catch (e:Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pickImageFromGallery() {
        Dexter.withContext(this)
            .withPermissions(permissions)
            .withListener(object: MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {

                        val pickIntent = Intent(Intent.ACTION_PICK)
                        pickIntent.setDataAndType(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            "image/*"
                        )
                        pickImageFromGalleryForResult.launch(pickIntent)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()
    }

    private val getLocationForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            location = if (SDK_INT >= 33) {
                result.data?.getParcelableExtra(
                    Constants.LOCATION, LocationModel::class.java)
            } else {

                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(Constants.LOCATION)
            }
            if (location != null) {
                latitude = location!!.latitude
                longitude = location!!.longitude
            }
        }

        getAddressFromLocation()
    }

    private fun getLocation(currentLocation: Boolean) {
        Dexter.withContext(this@AddHappyPlaceActivity).withPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET
        ).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) {

                    @SuppressLint("MissingPermission")
                    if (currentLocation) {
                        fusedLocationClient = LocationServices
                            .getFusedLocationProviderClient(this@AddHappyPlaceActivity)

                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            object: CancellationToken() {
                                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                                    CancellationTokenSource().token

                                override fun isCancellationRequested() = false
                            }
                        ).addOnSuccessListener {currentLocation ->
                            if (currentLocation == null) {
                                Toast.makeText(
                                    this@AddHappyPlaceActivity,
                                    "Cannot get location.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                latitude = currentLocation.latitude
                                longitude = currentLocation.longitude
                                val locationModel = LocationModel(latitude, longitude)
                                location = locationModel
                                getAddressFromLocation()
                            }
                        }

                    } else {
                        val intent = Intent(
                            this@AddHappyPlaceActivity,
                            OpenStreetMapActivity::class.java
                        )
                        if (location != null) {
                            intent.putExtra(
                                Constants.LOCATION,
                                location
                            )
                        }
                        getLocationForResult.launch(intent)
                    }
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

    private fun getAddressFromLocation() {
        if (location != null) {
            val geocoder = Geocoder(this, Locale.getDefault())
            val listAddress = mutableListOf<Address>()

            if (SDK_INT >= 33) {
                val geocodeListener = Geocoder.GeocodeListener { addresses ->
                    listAddress.addAll(addresses)
                    binding.etLocation.setText(
                        if (listAddress.isNotEmpty()) {
                            listAddress.first().getAddressLine(0)
                        } else "${location!!.latitude}, ${location!!.longitude}"
                    )
                }
                geocoder.getFromLocation(
                    location!!.latitude,
                    location!!.longitude,
                    1,
                    geocodeListener
                )
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(
                    location!!.latitude,
                    location!!.longitude,
                    1
                )?.let {
                    listAddress.addAll(it)
                    binding.etLocation.setText(
                        if (listAddress.isNotEmpty()) {
                            listAddress.first().getAddressLine(0)
                        } else "${location?.latitude}, ${location?.longitude}"
                    )
                }
            }
        }
    }

    private val takePhotoForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result?.data?.extras?.get("data") as Bitmap
            savedImageUri = saveImageToInternalStorage(bitmap)
            binding.ivImage.setImageBitmap(bitmap)
        }
    }


    private fun getPhotoFromCamera() {

        Dexter.withContext(this@AddHappyPlaceActivity)
            .withPermissions(permissions)
            .withListener(object: MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {

                        takePhotoForResult.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(Constants.IMAGE_DIRECTORY, Context.MODE_APPEND)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()

        } catch (e:IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this@AddHappyPlaceActivity)
            .setMessage("It looks like you have turned off permission required for" +
                    " this feature. It can be enabled in the apps settings.")
            .setPositiveButton("GO TO SETTINGS") {
                    _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CANCEL") {
                    dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}