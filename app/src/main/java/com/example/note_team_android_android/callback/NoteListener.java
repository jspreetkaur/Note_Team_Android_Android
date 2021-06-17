package com.example.note_team_android_android.callback;

import com.notesapp.service.model.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
}
