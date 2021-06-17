package com.example.note_team_android_android.callback;

import com.example.note_team_android_android.service.model.Note;
import com.notesapp.service.model.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
}
