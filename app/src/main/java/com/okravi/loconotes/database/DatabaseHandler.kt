package com.okravi.loconotes.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.LiveData
import com.okravi.loconotes.models.dbNoteModel

open class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LocationNotesDatabase"
        private const val TABLE_NOTES = "NotesTable"
        private const val KEY_ID = "_id"
        private const val KEY_PLACE_NAME = "place_name"
        private const val KEY_PLACE_PHOTO = "place_photo"
        private const val KEY_NOTE = "note_text"
        private const val KEY_DATE_MODIFIED = "last_modified_date"
        private const val KEY_PLACE_ID = "google_place_id"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {

        val CREATE_NOTES_TABLE = ("CREATE TABLE " + TABLE_NOTES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PLACE_NAME + " TEXT,"
                + KEY_PLACE_PHOTO + " TEXT,"
                + KEY_NOTE + " TEXT,"
                + KEY_DATE_MODIFIED + " TEXT,"
                + KEY_PLACE_ID + " TEXT,"
                + KEY_LATITUDE + " TEXT,"
                + KEY_LONGITUDE + " TEXT)")
        db?.execSQL(CREATE_NOTES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun addNote(note: dbNoteModel): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        //contentValues.put(KEY_ID, null)
        contentValues.put(KEY_PLACE_NAME, note.placeName)
        contentValues.put(KEY_PLACE_PHOTO, note.photo)
        contentValues.put(KEY_NOTE, note.textNote)
        contentValues.put(KEY_DATE_MODIFIED, note.dateNoteLastModified)
        contentValues.put(KEY_PLACE_ID, note.googlePlaceID)
        contentValues.put(KEY_LATITUDE, note.placeLatitude)
        contentValues.put(KEY_LONGITUDE, note.placeLongitude)

        val result = db.insert(TABLE_NOTES, null, contentValues)

        db.close()
        return result
    }

    fun updateNote(note: dbNoteModel): Int {

        val db = this.writableDatabase
        val contentValues = ContentValues()
        //contentValues.put(KEY_ID, note.keyID)
        contentValues.put(KEY_PLACE_NAME, note.placeName)
        contentValues.put(KEY_PLACE_PHOTO, note.photo)
        contentValues.put(KEY_NOTE, note.textNote)
        contentValues.put(KEY_DATE_MODIFIED, note.dateNoteLastModified)
        contentValues.put(KEY_PLACE_ID, note.googlePlaceID)
        contentValues.put(KEY_LATITUDE, note.placeLatitude)
        contentValues.put(KEY_LONGITUDE, note.placeLongitude)

        val success = db.update(
            TABLE_NOTES,
            contentValues,
            KEY_ID + "=" + note.keyID, null)

        db.close()
        return success
    }

    fun deleteNote(note: dbNoteModel): Int {
        val db = this.writableDatabase

        val success = db.delete(
            TABLE_NOTES,
            KEY_ID + "=" + note.keyID, null)

        db.close()
        return success
    }



    @SuppressLint("Range")
    fun getNote(keyID: Int): ArrayList<dbNoteModel>{
        val notesList = ArrayList<dbNoteModel>()
        val selectQuery = "SELECT * FROM $TABLE_NOTES WHERE $KEY_ID = $keyID"
        val db = this.readableDatabase

        try{
            val cursor  : Cursor = db.rawQuery(selectQuery, null)

            if(cursor.moveToFirst()) do {

                val note = dbNoteModel(
                    cursor.getString(cursor.getColumnIndex(KEY_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_NAME)),
                    cursor.getString(cursor.getColumnIndex(KEY_LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(KEY_LONGITUDE)),
                    cursor.getLong(cursor.getColumnIndex(KEY_DATE_MODIFIED)),
                    cursor.getString(cursor.getColumnIndex(KEY_NOTE)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_PHOTO)),
                )
                notesList.add(note)

            }while (cursor.moveToNext())
            cursor.close()

        }catch (e:SQLiteException){
            db.execSQL(selectQuery)
            Log.d("excp database:", "caught exception")
            return ArrayList()
        }
        db.close()
        return notesList
    }




    @SuppressLint("Range")
    fun getNotesList(): ArrayList<dbNoteModel>{
        val notesList = ArrayList<dbNoteModel>()
        val selectQuery = "SELECT * FROM $TABLE_NOTES"
        val db = this.readableDatabase

        try{
            val cursor  : Cursor = db.rawQuery(selectQuery, null)

            if(cursor.moveToFirst()) do {

                val note = dbNoteModel(
                    cursor.getString(cursor.getColumnIndex(KEY_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_NAME)),
                    cursor.getString(cursor.getColumnIndex(KEY_LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(KEY_LONGITUDE)),
                    cursor.getLong(cursor.getColumnIndex(KEY_DATE_MODIFIED)),
                    cursor.getString(cursor.getColumnIndex(KEY_NOTE)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_PHOTO)),
                )
                notesList.add(note)

            }while (cursor.moveToNext())
            cursor.close()

        }catch (e:SQLiteException){
            db.execSQL(selectQuery)
            Log.d("excp database:", "caught exception")
            return ArrayList()
        }

        db.close()
        return notesList
    }

}