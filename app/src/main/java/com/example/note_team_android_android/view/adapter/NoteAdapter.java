package com.example.note_team_android_android.view.adapter;

import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.logging.LogRecord;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note_team_android_android.callback.NoteListener;
import com.example.note_team_android_android.service.model.Note;
import com.makeramen.roundedimageview.RoundedImageView;
import com.notesapp.R;
import com.notesapp.service.model.Note;
import com.notesapp.view.callback.NoteListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> notes;
    private List<Note> notesSource;
    private NoteListener noteListener;

    private Timer timer;

    public NoteAdapter(List<Note> notes, NoteListener noteListener) {
        this.notes = notes;
        this.noteListener = noteListener;
        notesSource = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_container_note, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(notes.get(position));
        holder.layoutNote.setOnClickListener(v -> noteListener.onNoteClicked(notes.get(position),position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDateTime,textNoteTxt;
        LinearLayout layoutNote;
        RoundedImageView imageNote;
        ImageView audioImg;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textNoteTxt = itemView.findViewById(R.id.textNoteTxt);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            imageNote = itemView.findViewById(R.id.imageNote);
            audioImg = itemView.findViewById(R.id.imageAudio);
        }

        void setNote(Note note) {
            textTitle.setText(note.getTitle());

            textDateTime.setText(note.getDateTime());
            if(note.getNoteText() != null && !note.getNoteText().equals("")){
                textNoteTxt.setVisibility(View.VISIBLE);
                textNoteTxt.setText(note.getNoteText());
            }else {
                textNoteTxt.setVisibility(View.GONE);
            }

            if(note.getImagePath() != null && !note.getImagePath().equals("")) {
                imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                imageNote.setVisibility(View.VISIBLE);
            } else {
                imageNote.setVisibility(View.GONE);
            }

            if(note.getAudioPath() != null && !note.getAudioPath().equals("")){
                audioImg.setVisibility(View.VISIBLE);
            }else {
                audioImg.setVisibility(View.GONE);
            }
        }
    }

    public void searchNote(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(searchKeyword.trim().isEmpty()) {
                    notes = notesSource;
                } else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for(Note note : notesSource) {
                        if(note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                         /*note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||*/
                         note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        }, 250);
    }
    public void cancelTimer() {
        if(timer != null) {
            timer.cancel();
        }
    }
}
