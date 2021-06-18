package com.notesapp.service.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.notesapp.service.dao.NoteDao;
import com.notesapp.service.model.Category;
import com.notesapp.service.model.Note;
import com.notesapp.service.utils.Constants;


@Database(entities = {Category.class, Note.class}, exportSchema = false, version = 1)
public abstract class NoteDatabase extends RoomDatabase {
    private static NoteDatabase notesDatabase;

    public static synchronized NoteDatabase getNotesDatabase(Context context) {
        if (notesDatabase == null) {
            notesDatabase = Room.databaseBuilder(
                    context,
                    NoteDatabase.class,
                    Constants.DATABASE_NAME
            ).build();
        }
        return notesDatabase;
    }
    public abstract NoteDao noteDao();
}
