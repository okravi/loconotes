package com.okravi.loconotes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.okravi.loconotes.databinding.ItemNearbyPlaceBinding
import com.okravi.loconotes.models.LocationNoteModel



class NearbyPlacesAdapter(
    private val items: ArrayList<LocationNoteModel>
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onClickRecyclerListener: View.OnClickListener? = null

    //inflate items
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    //binding every item to a view
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = items[position]

        if (holder is MyViewHolder) {
            holder.tvNearbyPlaceName.text = model.placeName

            holder.itemView.setOnClickListener{
                if(onClickRecyclerListener != null){

                    //TODO: (it) might not be right here
                    onClickRecyclerListener!!.onClick(it)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
    //describe item view and metadata about its place within the RecyclerView
    private class MyViewHolder(binding: ItemNearbyPlaceBinding):
        RecyclerView.ViewHolder(binding.root){
        var tvNearbyPlaceName = binding.tvNearbyPlaceName
    }
}