package com.example.note_team_android_android.view.ui;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.note_team_android_android.R;
import com.example.note_team_android_android.service.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity  extends AppCompatActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        private RecyclerView recyclerView;
        private List<Category> categoryList;
        private CategoryAdapter categoryAdapter;

        private int noteClickedPosition = -1;

        private AlertDialog dialogAddCategory;
        private AlertDialog dialogUpdateDeleteCategory;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            ImageView imageAddCategory = findViewById(R.id.imageAddCategory);
            imageAddCategory.setOnClickListener(v -> showAddCategoryDialog(null));

            recyclerView = findViewById(R.id.categoryRecyclerView);
            recyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            );

            categoryList = new ArrayList<>();
            categoryAdapter = new CategoryAdapter(MainActivity.this,categoryList, this);
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

        private void showAddCategoryDialog(Category category) {
            dialogAddCategory = null;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.layout_add_category, findViewById(R.id.layoutAddURLContainer));
            builder.setView(view);

            dialogAddCategory = builder.create();
            if(dialogAddCategory.getWindow() != null) {
                dialogAddCategory.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputCategory = view.findViewById(R.id.inputCategory);
            inputCategory.requestFocus();

            if(category!= null){
                inputCategory.setText(category.getCategoryName());
            }

            view.findViewById(R.id.textAdd).setOnClickListener(v -> {
                final String inputCatStr = inputCategory.getText().toString().trim();

                if(inputCatStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter Category", Toast.LENGTH_SHORT).show();
                } else {
                    dialogAddCategory.dismiss();
                    dialogAddCategory = null;

                    if(category != null){
                        saveCategory(inputCatStr,category.getId(), true);
                    }else {
                        saveCategory(inputCatStr,0, false);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogAddCategory.dismiss());
            dialogAddCategory.show();
        }

        private void saveCategory(String categoryName,int id, boolean isUpdate) {

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Category name can't be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            final Category category = new Category();
            category.setCategoryName(categoryName);
            if (isUpdate) {
                category.setId(id);
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    NoteDatabase.getNotesDatabase(getApplicationContext()).noteDao().insertCategory(category);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getCategorys(Constants.REQUEST_CODE_ADD_CATEGORY, false);
                        }
                    });
                }
            });
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
                        allCategorys.get(i).setSelected(false);
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
            } else if(requestCode == Constants.REQUEST_CODE_UPDATE_CATEGORY && resultCode == RESULT_OK) {
                if(data != null) {
                    getCategorys(Constants.REQUEST_CODE_UPDATE_CATEGORY, data.getBooleanExtra("isCategoryDeleted", false));
                }
            }else if(requestCode == Constants.REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
                getCategorys(Constants.REQUEST_CODE_ADD_CATEGORY, false);
            }
        }


        @Override
        public void onCategoryClicked(Category category, int position) {
            noteClickedPosition = position;
            Intent intent = new Intent(getApplicationContext(), NotesActivity.class);
            intent.putExtra("catId", category.getId());
            intent.putExtra("catName",category.getCategoryName());
            startActivityForResult(intent, Constants.REQUEST_CODE_UPDATE_NOTE);
        }

        @Override
        public void onCategoryLongClicked(Category category, int position) {
            showUpdateDeleteCategoryDialog(category);
        }


        private void showUpdateDeleteCategoryDialog(Category category) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.layout_update_category_dialog, findViewById(R.id.layoutUpdateDeleteNoteContainer));
            builder.setView(view);

            dialogUpdateDeleteCategory = builder.create();
            if(dialogUpdateDeleteCategory.getWindow() != null) {
                dialogUpdateDeleteCategory.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.deleteCategoryLL).setOnClickListener(v -> {
                dialogUpdateDeleteCategory.dismiss();
                showConfirmDeleteDialog(category);
            });
            view.findViewById(R.id.updateCategoryLL).setOnClickListener(v -> {
                dialogUpdateDeleteCategory.dismiss();
                showAddCategoryDialog(category);

            });
            dialogUpdateDeleteCategory.show();
        }

        private void showConfirmDeleteDialog(Category category) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.layout_delete_category,
                            (ViewGroup) findViewById(R.id.layoutDeleteCategoryContainer));

            builder.setView(view);
            AlertDialog dialogDeleteNote = builder.create();

            if(dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteCategory).setOnClickListener(v -> {
                dialogDeleteNote.dismiss();
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        NoteDatabase.getNotesDatabase(getApplicationContext()).noteDao().deleteCategory(category);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                getCategorys(Constants.REQUEST_CODE_ADD_CATEGORY, false);
                            }
                        });
                    }
                });
            });
            view.findViewById(R.id.textDeleteCancel).setOnClickListener(v -> dialogDeleteNote.dismiss());
            dialogDeleteNote.show();
        }
}
