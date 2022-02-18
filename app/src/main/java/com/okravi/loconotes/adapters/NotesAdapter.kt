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
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.okravi.loconotes.R
import com.okravi.loconotes.activities.MainActivity

import com.okravi.loconotes.database.DatabaseHandler
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

open class NotesAdapter(
    private val context: Context,
    private val items: ArrayList<dbNoteModel>
):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickRecyclerListener: OnClickListener? = null
    //inflate items
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    //binding each item to a view
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = items[position]

        if (holder is MyViewHolder) {
            holder.tvNoteTitle.text = model.placeName
            holder.tvTextNote.text = model.textNote
            holder.ivNoteImage.setImageURI(model.photo.toUri())

            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val netDate = Date(model.dateNoteLastModified)
            val date = sdf.format(netDate)

            holder.tvDateModified.text = date

            //changing background color of selected/deselected rv
            if (items[position].isSelected){
                //holder.cvItemNote.background.setTint(Color.LTGRAY)
                //holder.tvNoteTitle?.background?.setTint(Color.LTGRAY)
                holder.cvItemNote.setBackgroundResource(R.drawable.element_note_highlighted)
                holder.ivNoteImage.setBackgroundResource(R.color.main_accent)
                Log.d("debug", "onBindViewHolder, highlighting note:$position")
            }else{
                //holder.cvItemNote.background.setTint(Color.WHITE)
                holder.cvItemNote.setBackgroundResource(R.drawable.element_note_default)
                holder.ivNoteImage.setBackgroundResource(R.color.main_foreground)
                Log.d("debug", "onBindViewHolder, highlighting note:$position")
            }

            holder.itemView.setOnClickListener{
                if(onClickRecyclerListener != null){
                    Log.d("debug", "holder.itemView.setOnClickListener{: clicked on $position")
                    onClickRecyclerListener!!.onClick(position, model)
                }
            }
        }
    }

    fun setOnClickListener(onClickRecyclerListener: OnClickListener){
        this.onClickRecyclerListener = onClickRecyclerListener
    }

    fun removeAt(adapterPosition: Int) {
        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deleteNote(items[adapterPosition])
        if(isDeleted > 0){
            items.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)

            Log.d("debug", "fun removeAt(adapterPosition: Int):removed at $adapterPosition")
        }
    }

    //notify adapter that this item is going to be changed
    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int){
        val intent = Intent(context, NoteEditActivity::class.java)
        //clearing marker since it's not serializable
        items[position].marker = null

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
        var cvItemNote = binding.cvItemNote
        var tvDateModified = binding.tvDateNote
    }
}


