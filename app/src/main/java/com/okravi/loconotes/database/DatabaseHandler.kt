package com.okravi.loconotes.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.telephony.TelephonyCallback
import com.okravi.loconotes.models.LocationNoteModel


class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LocationNotesDatabase"
        private const val TABLE_NOTES = "NotesTable"

        private const val KEY_ID = "_id"
        private const val KEY_PLACE_NAME = "place name"
        private const val KEY_PLACE_PHOTO = "place photo"
        private const val KEY_NOTE = "note text"
        private const val KEY_DATE_MODIFIED = "last modified date"
        private const val KEY_PLACE_ID = "google place id"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {

        val CREATE_NOTES_TABLE = ("CREATE TABLE " + TABLE_NOTES + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
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

    fun addNote(note: LocationNoteModel): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, note.keyID)
        contentValues.put(KEY_PLACE_NAME, note.placeName)
        contentValues.put(KEY_PLACE_PHOTO, note.photoMetadata)
        contentValues.put(KEY_NOTE, note.textNote)
        contentValues.put(KEY_DATE_MODIFIED, note.dateNoteLastModified)
        contentValues.put(KEY_PLACE_ID, note.googlePlaceID)
        contentValues.put(KEY_LATITUDE, note.placeLatitude)
        contentValues.put(KEY_LONGITUDE, note.placeLongitude)

        val result = db.insert(TABLE_NOTES, null, contentValues)

        db.close()
        return result
    }

    fun updateNote(note: LocationNoteModel): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_ID, note.keyID)
        contentValues.put(KEY_PLACE_NAME, note.placeName)
        contentValues.put(KEY_PLACE_PHOTO, note.photoMetadata)
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

    fun deleteNote(note: LocationNoteModel): Int {
        val db = this.writableDatabase

        val success = db.delete(
            TABLE_NOTES,
            KEY_ID + "=" + note.keyID, null)

        db.close()
        return success
    }

    @SuppressLint("Range")
    fun getNotesList(): ArrayList<LocationNoteModel>{
        val notesList = ArrayList<LocationNoteModel>()
        val selectQuery = "SELECT * FROM $TABLE_NOTES"
        val db = this.readableDatabase

        try{
            val cursor  : Cursor = db.rawQuery(selectQuery, null)


            if(cursor.moveToFirst()) do {
                val place = LocationNoteModel(
                    cursor.getString(cursor.getColumnIndex(KEY_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_NAME)),
                    cursor.getString(cursor.getColumnIndex(KEY_LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(KEY_LONGITUDE)),
                    0.0,
                    cursor.getString(cursor.getColumnIndex(KEY_DATE_MODIFIED)),
                    cursor.getString(cursor.getColumnIndex(KEY_NOTE)),
                    false,
                    cursor.getString(cursor.getColumnIndex(KEY_PLACE_PHOTO)),
                    null,
                    null

                )
                notesList.add(place)

            }while (cursor.moveToNext())
            cursor.close()

        }catch (e:SQLiteException){
            db.execSQL(selectQuery)
            return ArrayList()
        }

        return notesList
    }

}