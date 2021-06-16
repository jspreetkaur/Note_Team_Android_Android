package com.example.note_team_android_android.view.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note_team_android_android.service.model.Category;
import com.notesapp.R;
import com.notesapp.service.database.NoteDatabase;
import com.notesapp.service.model.Category;
import com.notesapp.service.model.Note;
import com.notesapp.view.callback.CategoryListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> notes;
    private List<Category> notesSource;
    private Cate noteListener;
    private Context mContext;

    private Timer timer;

    public CategoryAdapter(Context context, List<Category> notes, CategoryListener noteListener) {
        this.mContext = context;
        this.notes = notes;
        this.noteListener = noteListener;
        notesSource = notes;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoryViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_container_category, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.setCategory(notes.get(position));
        holder.layoutCategory.setOnClickListener(v -> noteListener.onCategoryClicked(notes.get(position),position));
        holder.layoutCategory.setOnLongClickListener(view -> {
            noteListener.onCategoryLongClicked(notes.get(position),position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, notesCountTv;
        LinearLayout layoutCategory;
        ImageView tickImg;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            layoutCategory = itemView.findViewById(R.id.layoutCategory);
            tickImg = itemView.findViewById(R.id.tickImg);
            notesCountTv = itemView.findViewById(R.id.notesCountTv);
        }

        void setCategory(Category category) {
            textTitle.setText(category.getCategoryName());

            if(category.isSelected()){
                tickImg.setVisibility(View.VISIBLE);
            }else {
                tickImg.setVisibility(View.GONE);
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executorService.execute(() -> {
                List<Note> allNotes = NoteDatabase.getNotesDatabase(mContext)
                        .noteDao().getNotesFromCategory(category.getId());
                handler.post(() -> {
                    notesCountTv.setText(allNotes.size() > 0 ? allNotes.size()+"" : "");
                });
            });
        }
    }

    public void searchCategory(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(searchKeyword.trim().isEmpty()) {
                    notes = notesSource;
                } else {
                    ArrayList<Category> temp = new ArrayList<>();
                    for(Category note : notesSource) {
                        if(note.getCategoryName().toLowerCase().contains(searchKeyword.toLowerCase()) ) {
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
