package com.example.note_team_android_android.service.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.note_team_android_android.service.dao.NoteDao;
import com.example.note_team_android_android.service.model.Category;
import com.example.note_team_android_android.service.model.Note;
import com.example.note_team_android_android.service.utils.Constants;


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
