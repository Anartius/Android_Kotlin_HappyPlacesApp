package com.example.happyplacesapp.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplacesapp.R
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.models.HappyPlaceModel

open class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
): RecyclerView.Adapter<HappyPlacesAdapter.MyViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int): HappyPlacesAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_happy_place,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(viewHolder: HappyPlacesAdapter.MyViewHolder, position: Int) {
        val item = list[position]

        viewHolder.itemView.apply {

            viewHolder.ivPlaceCircular.setImageURI(Uri.parse(item.image))
            viewHolder.tvTitle.text = item.title
            viewHolder.tvDescription.text = item.description

            viewHolder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position, item)
                }
            }
        }
    }

    fun itemHasAdded() {
        val dbHandler = DatabaseHandler(context)
        list = dbHandler.getHappyPlacesList()
        notifyItemInserted(list.size)
    }

    fun removeAt(position: Int) {
        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deletePlace(list[position])
        if (isDeleted > 0) {
            list.removeAt(position)
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick(position: Int, model: HappyPlaceModel)
    }

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val ivPlaceCircular: ImageView = itemView.findViewById(R.id.iv_place_circular)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
    }
}