package com.notesapp.view.ui;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.note_team_android_android.service.database.NoteDatabase;
import com.example.note_team_android_android.service.model.Category;
import com.example.note_team_android_android.service.model.Note;
import com.example.note_team_android_android.service.utils.Constants;
import com.example.note_team_android_android.view.adapter.CategoryAdapter;
import com.notesapp.R;
import com.notesapp.service.database.NoteDatabase;
import com.notesapp.service.model.Category;
import com.notesapp.service.model.Note;
import com.notesapp.service.utils.Constants;
import com.notesapp.view.adapter.CategoryAdapter;
import com.notesapp.view.callback.CategoryListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteCategoryUpdate extends AppCompatActivity implements CategoryListener {

    private RecyclerView recyclerView;
    private List<Category> categoryList;
    private CategoryAdapter categoryAdapter;

    int selectedCategoryId = 0;
    boolean isCategorySelectedForUpdate = false;

    private Note alreadyAvailableNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_category_update);

        ImageView imageDone = findViewById(R.id.imageDone);
        imageDone.setOnClickListener(v -> updateCategoryProcess());

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            if (alreadyAvailableNote != null){
                selectedCategoryId = alreadyAvailableNote.getCategoryId();
            }
        }
        recyclerView = findViewById(R.id.categoryRecyclerView);
        recyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        );

        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(NoteCategoryUpdate.this,categoryList, this);
        recyclerView.setAdapter(categoryAdapter);

        getCategorys(Constants.REQUEST_CODE_SHOW_CATEGORY, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                categoryAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(categoryList.size() != 0) {
                    categoryAdapter.searchCategory(s.toString());
                }
            }
        });

    }

    private void updateCategoryProcess() {
        if(isCategorySelectedForUpdate){

            if (alreadyAvailableNote != null){
                final Note note = new Note();
                note.setTitle(alreadyAvailableNote.getTitle());
                note.setNoteText(alreadyAvailableNote.getNoteText());
                note.setDateTime(alreadyAvailableNote.getDateTime());
                note.setImagePath(alreadyAvailableNote.getImagePath());
                note.setCategoryId(selectedCategoryId);

                if (alreadyAvailableNote != null) {
                    note.setId(alreadyAvailableNote.getId());
                }

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        NoteDatabase.getNotesDatabase(getApplicationContext()).noteDao().insertNote(note);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent();
                                setResult(RESULT_OK, intent);
                                finish();
                            }
                        });
                    }
                });
            }else {
                Toast.makeText(this, "Something went wrong to update category", Toast.LENGTH_SHORT).show();

            }

        }else {
            Toast.makeText(this, "Please select/change category.", Toast.LENGTH_SHORT).show();
        }

    }

    private void getCategorys(final int requestCategory, final boolean isCategoryDeleted) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            List<Category> allCategorys = NoteDatabase.getNotesDatabase(getApplicationContext())
                    .noteDao().getAllCategory();

            handler.post(() -> {

                categoryList.clear();
                for (int i = 0; i < allCategorys.size(); i++) {
                    if(allCategorys.get(i).getId() == selectedCategoryId){
                        allCategorys.get(i).setSelected(true);
                    }else {
                        allCategorys.get(i).setSelected(false);
                    }
                    categoryList.add(allCategorys.get(i));
                }
                categoryAdapter.notifyDataSetChanged();

                /*if(requestCategory == Constants.REQUEST_CODE_SHOW_CATEGORY) {
                    categoryList.addAll(allCategorys);
                    categoryAdapter.notifyDataSetChanged();
                } else if(requestCategory == Constants.REQUEST_CODE_ADD_CATEGORY) {
                    categoryList.add(allCategorys.get(allCategorys.size() - 1));
                    categoryAdapter.notifyItemInserted(categoryList.size() - 1);
                    recyclerView.smoothScrollToPosition(categoryList.size() - 1);
                } else if(requestCategory == Constants.REQUEST_CODE_UPDATE_CATEGORY) {
                    categoryList.remove(noteClickedPosition);
                    if(isCategoryDeleted){
                        categoryAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        categoryList.add(noteClickedPosition, allCategorys.get(noteClickedPosition));
                        categoryAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }*/
            });
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQUEST_CODE_ADD_CATEGORY && resultCode == RESULT_OK) {
            getCategorys(Constants.REQUEST_CODE_ADD_CATEGORY, false);
        } else if( requestCode == Constants.REQUEST_CODE_UPDATE_CATEGORY && resultCode == RESULT_OK) {
            if(data != null) {
                getCategorys(Constants.REQUEST_CODE_UPDATE_CATEGORY, data.getBooleanExtra("isCategoryDeleted", false));
            }
        }
    }


    @Override
    public void onCategoryClicked(Category category, int position) {
       /* Intent intent = new Intent(getApplicationContext(), NotesActivity.class);
        intent.putExtra("catId", category.getId());
        intent.putExtra("catName",category.getCategoryName());
        startActivityForResult(intent, Constants.REQUEST_CODE_UPDATE_NOTE);*/

       if(selectedCategoryId != category.getId()){
           for (int i = 0; i < categoryList.size(); i++) {
               if(categoryList.get(i).getId() == category.getId()){
                   categoryList.get(i).setSelected(true);
                   selectedCategoryId = category.getId();
                   isCategorySelectedForUpdate = true;
               }else {
                   categoryList.get(i).setSelected(false);
               }
           }
           categoryAdapter.notifyDataSetChanged();
       }else {
           Toast.makeText(this, "Note already exist in this category!!", Toast.LENGTH_SHORT).show();
       }



    }

    @Override
    public void onCategoryLongClicked(Category category, int position) {

    }


}