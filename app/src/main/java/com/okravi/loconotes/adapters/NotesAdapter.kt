package com.okravi.loconotes.adapters

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.okravi.loconotes.activities.NoteEditActivity
import com.okravi.loconotes.databinding.ItemNoteBinding
import com.okravi.loconotes.models.dbNoteModel
import android.content.Context
import com.okravi.loconotes.activities.MainActivity


open class NotesAdapter(
    private val context: Context,
    private val items: ArrayList<dbNoteModel>
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickRecyclerListener: OnClickListener? = null

    //inflate items
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.e("debug", "we're in onCreateViewHolder")
        return MyViewHolder(ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    //binding each item to a view
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = items[position]
        Log.e("debug", "we're in onBindViewHolder")
        if (holder is MyViewHolder) {
            holder.tvNoteTitle.text = model.placeName
            holder.tvTextNote.text = model.textNote
            holder.ivNoteImage.setImageURI(model.photo.toUri())

            holder.itemView.setOnClickListener{
                if(onClickRecyclerListener != null){

                    onClickRecyclerListener!!.onClick(position, model)
                }
            }
        }
    }

    fun setOnClickListener(onClickRecyclerListener: OnClickListener){
        this.onClickRecyclerListener = onClickRecyclerListener
    }

    //notify adapter that this item is going to be changed
    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int){
        val intent = Intent(context, NoteEditActivity::class.java)
        intent.putExtra(MainActivity.NOTE_DATA, items[position])
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface OnClickListener{
        fun onClick(position: Int, model: dbNoteModel)
    }
    //describe item view and metadata about its place within the RecyclerView
    private class MyViewHolder(binding: ItemNoteBinding):
        RecyclerView.ViewHolder(binding.root){

        var tvNoteTitle = binding.tvNoteTitle
        var tvTextNote = binding.tvTextNote
        var ivNoteImage = binding.ivItemNote
    }
}


