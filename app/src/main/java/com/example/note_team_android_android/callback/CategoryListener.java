package com.example.note_team_android_android.callback;

import com.example.note_team_android_android.service.model.Category;

public interface CategoryListener {
    void onCategoryClicked(Category category, int position);
    void onCategoryLongClicked(Category category, int position);
}
