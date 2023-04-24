package com.example.happyplacesapp.activities

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplacesapp.databinding.ActivityHappyPlaceDetailsBinding
import com.example.happyplacesapp.models.HappyPlaceModel

class HappyPlaceDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHappyPlaceDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHappyPlaceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var placeDetails: HappyPlaceModel? = null

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            placeDetails = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(
                    MainActivity.EXTRA_PLACE_DETAILS, HappyPlaceModel::class.java)
            } else {
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
            }
        }

        if(placeDetails != null) {
            setSupportActionBar(binding.toolbarPlaceDetails)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.title = placeDetails.title

            binding.toolbarPlaceDetails.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.ivPlaceDetailImage.setImageURI(Uri.parse(placeDetails.image))
            binding.tvPlaceDetailsDescription.text = placeDetails.description
            binding.tvPlaceDetailsLocation.text = placeDetails.location

        }
    }
}