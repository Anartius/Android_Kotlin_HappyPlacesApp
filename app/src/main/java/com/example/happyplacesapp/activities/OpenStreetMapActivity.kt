package com.example.happyplacesapp.activities

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplacesapp.databinding.ActivityOpenStereetMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class OpenStreetMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpenStereetMapBinding
    private lateinit var map: MapView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenStereetMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val lastKnownLoc: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        val latitude = lastKnownLoc?.latitude ?: 51.110298
        val longitude = lastKnownLoc?.longitude ?: 17.030990

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("HappyPlacesApp", MODE_PRIVATE)
        )

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setUseDataConnection(true)
        map.setMultiTouchControls(true)
        map.isTilesScaledToDpi = true

        val myLocation = GpsMyLocationProvider(this)
        val locationOverlay = MyLocationNewOverlay(myLocation, map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

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
//play around with these values to get the location on screen in the right place for your application
        scaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, dm.heightPixels - 110)
        map.overlays.add(scaleBarOverlay)

        val mapEventsReceiver = MapEventsReceiverImpl()
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(mapEventsOverlay)

        val mapController = map.controller
        mapController.animateTo(GeoPoint(latitude, longitude), 12.0, 2000)

    }

    inner class MapEventsReceiverImpl: MapEventsReceiver {

        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
            Toast.makeText(
                this@OpenStreetMapActivity,
                "${p?.latitude} - ${p?.longitude}",
                Toast.LENGTH_LONG
            ).show()

            Log.d("singleTapConfirmedHelper", "${p?.latitude} - ${p?.longitude}")
            return true
        }

        override fun longPressHelper(p: GeoPoint?): Boolean {
            Log.d("longPressHelper", "${p?.latitude} - ${p?.longitude}")
            return false
        }
    }
}