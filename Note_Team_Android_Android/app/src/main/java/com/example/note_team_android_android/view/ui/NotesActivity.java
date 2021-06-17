package com.notesapp.view.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.notesapp.R;
import com.notesapp.service.database.NoteDatabase;
import com.notesapp.service.model.Note;
import com.notesapp.service.utils.Constants;
import com.notesapp.view.adapter.NoteAdapter;
import com.notesapp.view.callback.NoteListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesActivity extends AppCompatActivity implements NoteListener {

    private RecyclerView recyclerView;
    private List<Note> noteList;
    private NoteAdapter noteAdapter;

    private int noteClickedPosition = -1;

    int categoryIdIntent = 0;
    private AlertDialog dialogAddURL;
    BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    int sortType = 0;

    boolean isListUpdated = false;


    @Override
    public void onBackPressed() {
        if(isListUpdated){
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        TextView textNoteName = findViewById(R.id.textMyNotes);

        if(getIntent() != null){
            try {
                textNoteName.setText("My Notes("+getIntent().getStringExtra("catName")+")");
            }catch (Exception e){
                textNoteName.setText("My Notes");
            }
            categoryIdIntent = getIntent().getIntExtra("catId",0);
        }

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(v -> {
                    Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                    intent.putExtra("catId",categoryIdIntent);
                    startActivityForResult(intent, Constants.REQUEST_CODE_ADD_NOTE);
                }
        );

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());



        recyclerView = findViewById(R.id.notesRecyclerView);
        recyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList, this);
        recyclerView.setAdapter(noteAdapter);

        getNotes(Constants.REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                noteAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(noteList.size() != 0) {
                    noteAdapter.searchNote(s.toString());
                }
            }
        });
        initSort();
        findViewById(R.id.textSortBy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bottomSheetBehavior != null){
                    if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    } else {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            }
        });
    }

    private void initSort() {
        final LinearLayout layoutSort = findViewById(R.id.layoutSort);
        bottomSheetBehavior = BottomSheetBehavior.from(layoutSort);

        layoutSort.findViewById(R.id.textMiscellaneous).setOnClickListener(v -> {
            if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        ImageView tickImgTitleAscending = layoutSort.findViewById(R.id.tickImgTitleAscending);
        ImageView tickImgTitleDescending = layoutSort.findViewById(R.id.tickImgTitleDescending);
        ImageView tickImgTimeAscending = layoutSort.findViewById(R.id.tickImgTimeAscending);
        ImageView tickImgTimeDescending = layoutSort.findViewById(R.id.tickImgTimeDescending);


        layoutSort.findViewById(R.id.layoutTitleAscending).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            tickImgTitleAscending.setVisibility(View.VISIBLE);
            tickImgTitleDescending.setVisibility(View.GONE);
            tickImgTimeAscending.setVisibility(View.GONE);
            tickImgTimeDescending.setVisibility(View.GONE);
            if(sortType != 1){
                sortType = 1;
                sortListFunc();
            }

        });

        layoutSort.findViewById(R.id.layoutTitleDescending).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            tickImgTitleAscending.setVisibility(View.GONE);
            tickImgTitleDescending.setVisibility(View.VISIBLE);
            tickImgTimeAscending.setVisibility(View.GONE);
            tickImgTimeDescending.setVisibility(View.GONE);
            if(sortType != 2){
                sortType = 2;
                sortListFunc();
            }

        });
        layoutSort.findViewById(R.id.layoutTimeAscending).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            tickImgTitleAscending.setVisibility(View.GONE);
            tickImgTitleDescending.setVisibility(View.GONE);
            tickImgTimeAscending.setVisibility(View.VISIBLE);
            tickImgTimeDescending.setVisibility(View.GONE);
            if(sortType != 3){
                sortType = 3;
                sortListFunc();
            }

        });
        layoutSort.findViewById(R.id.layoutTimeDescending).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            tickImgTitleAscending.setVisibility(View.GONE);
            tickImgTitleDescending.setVisibility(View.GONE);
            tickImgTimeAscending.setVisibility(View.GONE);
            tickImgTimeDescending.setVisibility(View.VISIBLE);
            if(sortType != 4){
                sortType = 4;
                sortListFunc();
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            isListUpdated = true;
            getNotes(Constants.REQUEST_CODE_ADD_NOTE, false);
        } else if( requestCode == Constants.REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if(data != null) {
                isListUpdated = true;
                getNotes(Constants.REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        }
    }


    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("catId",categoryIdIntent);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note",note);
        startActivityForResult(intent, Constants.REQUEST_CODE_UPDATE_NOTE);
    }


}