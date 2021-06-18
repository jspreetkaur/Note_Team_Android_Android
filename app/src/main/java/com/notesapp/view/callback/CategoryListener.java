package com.notesapp.view.callback;

import com.notesapp.service.model.Category;

public interface CategoryListener {
    void onCategoryClicked(Category category, int position);
    void onCategoryLongClicked(Category category, int position);
}
