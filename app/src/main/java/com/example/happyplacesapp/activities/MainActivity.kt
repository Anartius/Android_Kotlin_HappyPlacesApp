package com.example.happyplacesapp.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplacesapp.Constants
import com.example.happyplacesapp.adapters.HappyPlacesAdapter
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ActivityMainBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.SwipeToDeleteCallback
import com.example.happyplacesapp.utils.SwipeToEditCallback

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var placesList: ArrayList<HappyPlaceModel>
    private lateinit var adapter: HappyPlacesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.title = Constants.APP_NAME

        getHappyPlacesListFromLocalDB()

        adapter = HappyPlacesAdapter(this, placesList)
        binding.rvPlacesList.adapter = adapter

        setupHappyPlacesRecyclerView()
    }

    fun startActivityForResult(intent: Intent) {
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {result ->

        if(result.resultCode == Activity.RESULT_OK) {
            adapter.itemHasAdded()
            setupHappyPlacesRecyclerView()
        }
    }

    private fun getHappyPlacesListFromLocalDB() {
        val dbHandler = DatabaseHandler(this)
        placesList = dbHandler.getHappyPlacesList()
    }

    private fun setupHappyPlacesRecyclerView() {

        getHappyPlacesListFromLocalDB()

        binding.fabAddPlace.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            resultLauncher.launch(intent)
        }

        if (placesList.isNotEmpty()) {
            binding.toolbarMain.visibility = View.VISIBLE
            binding.rvPlacesList.visibility = View.VISIBLE
            binding.tvNoPlacesFound.visibility = View.GONE

            binding.rvPlacesList.layoutManager = LinearLayoutManager(this)
            binding.rvPlacesList.setHasFixedSize(false)

            adapter.setOnClickListener(object: HappyPlacesAdapter.OnClickListener{
                override fun onClick(position: Int, model: HappyPlaceModel) {
                    val intent = Intent(
                        this@MainActivity, HappyPlaceDetailsActivity::class.java)
                    intent.putExtra(EXTRA_PLACE_DETAILS, model)
                    startActivityForResult(intent)
                }
            })

            val editSwipeHandler = object: SwipeToEditCallback(this) {

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    val position = viewHolder.absoluteAdapterPosition
                    val model = placesList[position]

                    val intent = Intent(
                        this@MainActivity, AddHappyPlaceActivity::class.java)
                    intent.putExtra(EXTRA_PLACE_DETAILS, model)
                    startActivityForResult(intent)

                    adapter.notifyItemChanged(position)
                }
            }

            val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
            editItemTouchHelper.attachToRecyclerView(binding.rvPlacesList)

            val deleteSwipeHandler = object: SwipeToDeleteCallback(this) {

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.absoluteAdapterPosition
                    adapter.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    setupHappyPlacesRecyclerView()
                }
            }

            val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
            deleteItemTouchHelper.attachToRecyclerView(binding.rvPlacesList)

        } else{
            binding.toolbarMain.visibility = View.GONE
            binding.rvPlacesList.visibility = View.GONE
            binding.tvNoPlacesFound.visibility = View.VISIBLE
        }
    }

    companion object {
        var EXTRA_PLACE_DETAILS = "extra place details"
    }
}