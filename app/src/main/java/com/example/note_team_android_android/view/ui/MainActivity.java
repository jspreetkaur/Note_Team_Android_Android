package com.example.note_team_android_android.view.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.note_team_android_android.R;

public class MainActivity  extends AppCompatActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageAddCategory = findViewById(R.id.imageAddCategory);
        imageAddCategory.setOnClickListener(v -> showAddCategoryDialog(null));

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

    }
}
