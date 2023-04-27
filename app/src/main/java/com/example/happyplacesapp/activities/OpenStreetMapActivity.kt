package com.example.happyplacesapp.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplacesapp.Constants
import com.example.happyplacesapp.databinding.ActivityOpenStereetMapBinding
import com.example.happyplacesapp.models.LocationModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class OpenStreetMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpenStereetMapBinding
    private lateinit var map: MapView
    private var location: GeoPoint? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenStereetMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setUseDataConnection(true)
        map.setMultiTouchControls(true)
        map.isTilesScaledToDpi = true

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("HappyPlacesApp", MODE_PRIVATE)
        )

        if (intent.hasExtra(Constants.LOCATION)) {
            val locationFromIntent = if (SDK_INT >= 33) {
                intent.extras?.getParcelable(
                    Constants.LOCATION, LocationModel::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.extras?.getParcelable(Constants.LOCATION)
            }

            location = GeoPoint(locationFromIntent!!.latitude, locationFromIntent.longitude)

            val marker = Marker(map)
            marker.position = GeoPoint(
                location?.latitude ?: 0.0,
                location?.longitude ?: 0.0
            )
            map.overlays.add(marker)
        }

        val compassOverlay = CompassOverlay(
            this,
            InternalCompassOrientationProvider(this),
            map
        )
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)

        val dm = this.resources.displayMetrics
        val scaleBarOverlay = ScaleBarOverlay(map)
        scaleBarOverlay.setCentred(true)
        scaleBarOverlay.setMaxLength(1.5f)
        scaleBarOverlay.setTextSize(15 * dm.density)
//play around with these values to get the location on screen in the right place for your application
        scaleBarOverlay.setScaleBarOffset(170, dm.heightPixels - 110)
        map.overlays.add(scaleBarOverlay)

        getCurrentLocation()

        val mapEventsReceiver = MapEventsReceiverImpl()
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(mapEventsOverlay)

        binding.buttonGetLocation.setOnClickListener {
            if (location == null) {
                Toast.makeText(
                    this@OpenStreetMapActivity,
                    "Please select a location.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val resultIntent = Intent()
                val resultLocation = LocationModel(
                    location!!.latitude, location!!.longitude)
                resultIntent.putExtra(Constants.LOCATION, resultLocation)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun getCurrentLocation() {
        Dexter.withContext(this@OpenStreetMapActivity).withPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) {

                    val lm = getSystemService(LOCATION_SERVICE) as LocationManager

                    @SuppressLint("MissingPermission")
                    val lastKnownLoc: Location? = lm.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER)

                    val latitude = lastKnownLoc?.latitude ?: 51.110298
                    val longitude = lastKnownLoc?.longitude ?: 17.030990

                    val mapController = map.controller
                    mapController.animateTo(
                        if (location != null) {
                            GeoPoint(location!!.latitude, location!!.longitude)
                        } else {
                            GeoPoint(latitude, longitude)
                        },
                        8.0,
                        1000
                    )

                    val myLocation = GpsMyLocationProvider(this@OpenStreetMapActivity)
                    val locationOverlay = MyLocationNewOverlay(myLocation, map)
                    locationOverlay.enableMyLocation()
                    map.overlays.add(locationOverlay)
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

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this@OpenStreetMapActivity)
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

    inner class MapEventsReceiverImpl: MapEventsReceiver {
        override fun singleTapConfirmedHelper(geoPoint: GeoPoint?): Boolean {
            location = geoPoint

            map.overlays.forEach{
                if (it is Marker) map.overlays.remove(it)
            }

            val marker = Marker(map)
            marker.position = GeoPoint(
                location?.latitude ?: 0.0,
                location?.longitude ?: 0.0
            )
            map.overlays.add(marker)

            return true
        }

        override fun longPressHelper(p: GeoPoint?): Boolean {
            Log.d("longPressHelper", "${p?.latitude} - ${p?.longitude}")
            return false
        }
    }
}