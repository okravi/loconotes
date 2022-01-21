package com.okravi.loconotes.adapters

import android.util.Log
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

    var onClickRecyclerListener: OnClickListener? = null

    //inflate items
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.e("debug", "we're in onCreateViewHolder")
        return MyViewHolder(ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    //binding each item to a view
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = items[position]
        Log.e("debug", "we're in onBindViewHolder")
        if (holder is MyViewHolder) {
            holder.tvNearbyPlaceName.text = model.placeName
            Log.e("debug", "we're binding each item to a view")

            if(model.photo != null){
                holder.ivNearbyPlacePhoto.setImageBitmap(model.photo)
            }

            holder.itemView.setOnClickListener{
                if(onClickRecyclerListener != null){

                    //TODO: (it) might not be right here
                    onClickRecyclerListener!!.onClick(position, model)
                }
            }
        }
    }

    fun setOnClickListener(onClickRecyclerListener: OnClickListener){
        this.onClickRecyclerListener = onClickRecyclerListener
    }

    fun clearView(){
        items.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface OnClickListener{
        fun onClick(position: Int, model: LocationNoteModel)
    }
    //describe item view and metadata about its place within the RecyclerView
    private class MyViewHolder(binding: ItemNearbyPlaceBinding):
        RecyclerView.ViewHolder(binding.root){

        var tvNearbyPlaceName = binding.tvNearbyPlaceName
        var ivNearbyPlacePhoto = binding.ivNearbyPlacePhoto

    }
}


