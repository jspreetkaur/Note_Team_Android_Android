package com.notesapp.view.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.makeramen.roundedimageview.RoundedImageView;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.notesapp.R;
import com.notesapp.service.database.NoteDatabase;
import com.notesapp.service.model.Note;
import com.notesapp.service.utils.Constants;
import com.notesapp.service.utils.TimeStamp;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CreateNoteActivity extends AppCompatActivity {
    private EditText inputNoteTitle, inputNoteText;
    private TextView textDateTime;
    private RoundedImageView imageNote;

    private LinearLayout updateCategoryLL;

    private String selectedAudioPath;
    private String selectedImagePath;

    private AlertDialog dialogDeleteNote;

    private Note alreadyAvailableNote;
    boolean isForUpdate = false;
    int categoryIdIntent = 0;

    private Activity context = CreateNoteActivity.this;

    FusedLocationProviderClient mFusedLocationClient;

    int PERMISSION_ID = 44;
    double selectedLatitude = 0.0;
    double selectedLongitude = 0.0;
    LinearLayout LocationLL;
    TextView addressTv;

    BottomSheetBehavior<LinearLayout> audioBottomSheetBehavior;

    MediaPlayer mediaPlayer;
    TextView audioTimeTv;
    ImageView audioPlayPauseImg;

    Handler handlerAudioTime;

    private static final int REQUEST_RECORD_AUDIO = 0;
    private static final String AUDIO_FILE_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/recorded_audio.wav";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        updateCategoryLL = findViewById(R.id.updateCategoryLL);
        LocationLL = findViewById(R.id.LocationLL);
        addressTv = findViewById(R.id.addressTv);
        audioTimeTv = findViewById(R.id.audioTimeTv);
        audioPlayPauseImg= findViewById(R.id.audioPlayPauseImg);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // method to get the location
        getLastLocation();
        if(getIntent() != null){
            categoryIdIntent = getIntent().getIntExtra("catId",0);
            //Toast.makeText(this, ""+categoryIdIntent, Toast.LENGTH_SHORT).show();
        }

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());
        initializeViews();

        textDateTime.setText(TimeStamp.getTimeStamp());

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(v -> saveNote());
        selectedAudioPath = "";
        selectedImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveImage).setOnClickListener(v -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        findViewById(R.id.deleteAudioImg).setOnClickListener(v -> {
            if(mediaPlayer != null){
                if(mediaPlayer.isPlaying()){
                    Toast.makeText(context, "Please stop audio first", Toast.LENGTH_SHORT).show();
                }else{
                    findViewById(R.id.audioCardLL).setVisibility(View.GONE);
                    selectedAudioPath = "";
                }
            }else{
                findViewById(R.id.audioCardLL).setVisibility(View.GONE);
                selectedAudioPath = "";
            }
        });

        updateCategoryLL.setOnClickListener(view -> {
            if(isForUpdate){
                Intent intent = new Intent(getApplicationContext(), NoteCategoryUpdate.class);
                intent.putExtra("isViewOrUpdate", true);
                intent.putExtra("note",alreadyAvailableNote);
                startActivityForResult(intent, Constants.REQUEST_CODE_UPDATE_CATEGORY);
            }
        });

        LocationLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                intent.putExtra("latitude", selectedLatitude);
                intent.putExtra("longitude",selectedLongitude);
                startActivityForResult(intent, Constants.REQUEST_CODE_PLACE_PICK);
            }
        });
        initMiscellaneous();
        initAudio();

        audioPlayPauseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mediaPlayer != null){
                    if (mediaPlayer.isPlaying()) {
                        audioPlayPauseImg.setImageResource(R.drawable.ic_play);
                        mediaPlayer.pause();
                    } else {
                        audioPlayPauseImg.setImageResource(R.drawable.ic_pause);
                        playAudioTime();
                        mediaPlayer.start();
                    }
                }
            }
        });
    }

    private void playAudioTime() {
        if(handlerAudioTime != null){
            handlerAudioTime.removeCallbacksAndMessages(null);
        }

        handlerAudioTime = new Handler();
        handlerAudioTime.post(new Runnable() {
            @Override
            public void run() {
                int duration= mediaPlayer.getCurrentPosition();
                //Log.e("playerDuration", "duration "+duration);
                //and update your seekbar from handler
                //change your int to time format...
                String time = String.format("%02d:%02d ", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
                audioTimeTv.setText(time);
                handlerAudioTime.postDelayed(this,1000);
            }
        });
    }

    private void initializeViews() {
        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteText = findViewById(R.id.inputNoteText);
        textDateTime = findViewById(R.id.textDateTime);
        imageNote = findViewById(R.id.imageNote);
    }

    private void setViewOrUpdateNote() {
        updateCategoryLL.setVisibility(View.VISIBLE);
        isForUpdate = true;
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());

        selectedLatitude = alreadyAvailableNote.getLatitude();
        selectedLongitude = alreadyAvailableNote.getLongitude();
        setSelectedAddressProcess();

        final String imagePathStr = alreadyAvailableNote.getImagePath();
        if (imagePathStr != null && !imagePathStr.trim().isEmpty() && imagePathStr != "") {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(imagePathStr));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = imagePathStr;
        }

        final String audioPathStr = alreadyAvailableNote.getAudioPath();
        if (audioPathStr != null && !audioPathStr.trim().isEmpty() && audioPathStr != "") {
            selectedAudioPath = audioPathStr;
            initMediaPlayer();
        }
    }

    private void saveNote() {
        final String noteTitle = inputNoteTitle.getText().toString().trim();
        final String noteText = inputNoteText.getText().toString().trim();
        final String dateTimeStr = textDateTime.getText().toString().trim();

        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(noteTitle);
        note.setNoteText(noteText);
        note.setDateTime(dateTimeStr);
        note.setImagePath(selectedImagePath);
        note.setAudioPath(selectedAudioPath);
        note.setCategoryId(categoryIdIntent);
        note.setLatitude(selectedLatitude);
        note.setLongitude(selectedLongitude);

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
    }

    public void initMediaPlayer(){
        //set up MediaPlayer

        findViewById(R.id.audioCardLL).setVisibility(View.VISIBLE);
        audioTimeTv.setText("00:00");
        audioPlayPauseImg.setImageResource(R.drawable.ic_play);

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(context, Uri.parse(selectedAudioPath));
            mediaPlayer.prepare();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mPlayer) {
                    if(mediaPlayer != null){
                        if(handlerAudioTime != null){
                            handlerAudioTime.removeCallbacksAndMessages(null);
                        }
                        mediaPlayer.reset();
                        initMediaPlayer();
                    }

                }
            });
            //mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initMiscellaneous() {
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);

        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(v -> {
            if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });


        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);


            String[] permissions1 = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            Permissions.check(context, permissions1, null, null, new PermissionHandler() {
                @Override
                public void onGranted() {
                    CropImage.activity(null).setGuidelines(CropImageView.Guidelines.ON).start(context);
                }
            });
        });

        layoutMiscellaneous.findViewById(R.id.layoutAddAudio).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            //showAddURLDialog();

            if(audioBottomSheetBehavior != null){
                if(audioBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    audioBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    audioBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        if(alreadyAvailableNote != null) {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    private void initAudio() {
        final LinearLayout layoutAudioBottomSheet = findViewById(R.id.layoutAudioBottomSheet);
        audioBottomSheetBehavior = BottomSheetBehavior.from(layoutAudioBottomSheet);

        layoutAudioBottomSheet.findViewById(R.id.textAudio).setOnClickListener(v -> {
            if(audioBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                audioBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                audioBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });


        layoutAudioBottomSheet.findViewById(R.id.layoutRecordBtn).setOnClickListener(v -> {
            audioBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            String[] permissions1 = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,};
            Permissions.check(context, permissions1, null, null, new PermissionHandler() {
                @Override
                public void onGranted() {
                    Intent intent = new Intent(getApplicationContext(), RecordAudioActivity.class);
                    startActivityForResult(intent, Constants.REQUEST_CODE_RECORD_AUDIO);
                }
            });
        });

        layoutAudioBottomSheet.findViewById(R.id.layoutAudioGalleryBtn).setOnClickListener(v -> {
            audioBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            //showAddURLDialog();

            String[] permissions1 = { Manifest.permission.READ_EXTERNAL_STORAGE};
            Permissions.check(context, permissions1, null, null, new PermissionHandler() {
                @Override
                public void onGranted() {
                    Intent intent_upload = new Intent();
                    intent_upload.setType("audio/*");
                    intent_upload.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent_upload,Constants.REQUEST_CODE_AUDIO_GALLERY);
                }
            });

        });

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*if(requestCode == Constants.REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if(data != null) {
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null) {
                    try {
                        Glide.with(CreateNoteActivity.this)
                                .load(selectedImageUri)
                                .into(imageNote);

                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                        selectedImagePath = getPathFromUri(selectedImageUri);
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }*/
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK && result != null) {
                Uri selectedImageUri = result.getUri();
                String profileImagePath = getPathFromUri(selectedImageUri);
                //Toast.makeText(context, ""+profileImagePath, Toast.LENGTH_SHORT).show();
                if (!profileImagePath.equals("")){
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                    selectedImagePath = profileImagePath;
                    Glide.with(context).load(profileImagePath).into(imageNote);
                   // uploadAFile(new File(profileImagePath));
                }
            }
        }
        else if(requestCode == Constants.REQUEST_CODE_UPDATE_CATEGORY && resultCode == RESULT_OK){
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
        else if(requestCode == Constants.REQUEST_CODE_PLACE_PICK && resultCode == RESULT_OK){
            if(data != null){
                selectedLatitude = data.getDoubleExtra("latitude",0.0);
                selectedLongitude = data.getDoubleExtra("longitude",0.0);
                setSelectedAddressProcess();
            }else {
                Toast.makeText(context, "Location not selected!", Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == Constants.REQUEST_CODE_AUDIO_GALLERY && resultCode == RESULT_OK){
            if(data != null){
                //the selected audio.
                Uri uri = data.getData();
                String audioPath = getPathFromUri(uri);
                if (!audioPath.equals("")){
                    selectedAudioPath = audioPath;
                    initMediaPlayer();
                }

                Log.i("SelectedAudio","gallery>> "+audioPath);

            }else {
                Toast.makeText(context, "Audio not picked.", Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode ==  Constants.REQUEST_CODE_RECORD_AUDIO && resultCode == RESULT_OK) {
            if (data != null) {
               String recordedFile = data.getStringExtra("filePath");
                //Toast.makeText(this, "Audio recorded successfully!  " + recordedFile, Toast.LENGTH_SHORT).show();
                Log.i("SelectedAudio","gallery>> "+recordedFile);
                if (recordedFile != null && !recordedFile.equals("")){
                    selectedAudioPath = recordedFile;
                    initMediaPlayer();
                }

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Audio was not recorded", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri,null, null, null, null);
        if(cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }



    private void showDeleteNoteDialog() {
        if(dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.layout_delete_note,
                            (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer));

            builder.setView(view);
            dialogDeleteNote = builder.create();

            if(dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(v -> {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        NoteDatabase.getNotesDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent();
                                intent.putExtra("isNoteDeleted", true);
                                setResult(RESULT_OK, intent);
                                dialogDeleteNote.dismiss();
                                finish();
                            }
                        });
                    }
                });
            });
            view.findViewById(R.id.textDeleteCancel).setOnClickListener(v -> dialogDeleteNote.dismiss());
        }
        dialogDeleteNote.show();
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if(!isForUpdate){
                            if (location == null) {
                                requestNewLocationData();
                            } else {
                                selectedLatitude = location.getLatitude();
                                selectedLongitude = location.getLongitude();
                                setSelectedAddressProcess();
                            }
                        }

                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    private void setSelectedAddressProcess() {
        if(selectedLatitude != 0.0 && selectedLongitude != 0.0){

            try {
                LocationLL.setVisibility(View.VISIBLE);
                addressTv.setText(getAddress(new LatLng(selectedLatitude,selectedLongitude)));
            } catch (Exception e) {
                e.printStackTrace();
                addressTv.setText("-");

            }

        }
    }

    private String getAddress(LatLng latLng){
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            Log.e("SelectedAddress"," address>  "+address + "  Lat  "+latLng.latitude + "  Long  "+latLng.longitude);
            return address;
        } catch (IOException e) {
            e.printStackTrace();
            return "No Address Found";
        }


    }


    @Override
    protected void onPause() {
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                audioPlayPauseImg.setImageResource(R.drawable.ic_play);
            }
        }

        if(handlerAudioTime != null){
            handlerAudioTime.removeCallbacksAndMessages(null);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.reset();
            }
        }
        if(handlerAudioTime != null){
            handlerAudioTime.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            selectedLatitude = mLastLocation.getLatitude();
            selectedLongitude = mLastLocation.getLongitude();
            setSelectedAddressProcess();
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /*if (checkPermissions()) {
            getLastLocation();
        }*/
    }

}