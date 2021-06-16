package com.example.note_team_android_android.service.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.note_team_android_android.service.model.Category;
import com.example.note_team_android_android.service.model.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM category_table ORDER BY id ASC")
    List<Category> getAllCategory();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(Category category);

    @Delete
    void deleteCategory(Category category);

    @Query("SELECT * FROM note_table WHERE category_id = :catgoryId ORDER BY id ASC ")
    List<Note> getNotesFromCategory(Integer catgoryId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);
}
