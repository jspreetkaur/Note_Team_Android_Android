package com.notesapp.view.callback;

import com.notesapp.service.model.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
}
