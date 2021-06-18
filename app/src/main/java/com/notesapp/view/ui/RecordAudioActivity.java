package com.notesapp.view.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.notesapp.R;
import com.notesapp.service.utils.VisualizerView;

import java.io.File;
import java.io.IOException;

public class RecordAudioActivity extends AppCompatActivity {

    //Recording controls
    private FloatingActionButton mRecordButton = null;

    private TextView mRecordingPrompt;
    private int mRecordPromptCount = 0;

    private boolean mStartRecording = true;
    private boolean mPauseRecording = true;

    private Chronometer mChronometer = null;
    long timeWhenPaused = 0;


    private String mFileName = null;
    private String mFilePath = null;

    private MediaRecorder mRecorder = null;

    VisualizerView visualizerView;

    ImageView imageSave;
    boolean isRecording = true;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);
        visualizerView = (VisualizerView) findViewById(R.id.visualizer);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        //update recording prompt text
        mRecordingPrompt = (TextView) findViewById(R.id.recording_status_text);
        imageSave = findViewById(R.id.imageSave);
        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> onBackPressed());

        mRecordButton = findViewById(R.id.btnRecord);
        mRecordButton.setBackgroundColor(getResources().getColor(R.color.colorBlue));
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
            }
        });

        imageSave.setOnClickListener(view -> {
            if(mFilePath != null && !mFilePath.equals("")){
                Intent intent = new Intent();
                intent.putExtra("filePath",mFilePath);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        // create the Handler for visualizer update
        handler = new Handler();

    }

    @Override
    public void onDestroy() {
        if (mRecorder != null) {
            stopRecording();
        }

        super.onDestroy();
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();

        mRecorder = null;
        isRecording = false;
        handler.removeCallbacks(updateVisualizer);

    }

    public void startRecording() {
        setFileNameAndPath();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(192000);

        try {
            mRecorder.prepare();
            mRecorder.start();
            isRecording = true;
            handler.post(updateVisualizer);
        } catch (IOException e) {
            Log.e("LOG_TAG", "prepare() failed");
        }
    }

    // updates the visualizer every 50 milliseconds
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isRecording) // if we are already recording
            {
                if(mRecorder != null){
                    // get the current amplitude
                    int x = mRecorder.getMaxAmplitude();
                    visualizerView.addAmplitude(x); // update the VisualizeView
                    visualizerView.invalidate(); // refresh the VisualizerView

                    // update in 40 milliseconds
                    handler.postDelayed(this, 40);
                }

            }
        }
    };

    public void setFileNameAndPath(){
        File f;
        do{

            mFileName =  "My Recording_" + (System.currentTimeMillis()) + ".mp3";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/SoundRecorder/" + mFileName;

            f = new File(mFilePath);
        }while (f.exists() && !f.isDirectory());
    }
    // Recording Start/Stop
    //TODO: recording pause
    private void onRecord(boolean start){

        if (start) {
            // start recording
            mRecordButton.setImageResource(R.drawable.ic_stop);
            //mPauseButton.setVisibility(View.VISIBLE);
            Toast.makeText(this,R.string.toast_recording_start,Toast.LENGTH_SHORT).show();
            File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
            if (!folder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                folder.mkdir();
            }

            //start Chronometer
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();
            mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    if (mRecordPromptCount == 0) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
                    } else if (mRecordPromptCount == 1) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + "..");
                    } else if (mRecordPromptCount == 2) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
                        mRecordPromptCount = -1;
                    }

                    mRecordPromptCount++;
                }
            });

            startRecording();

            mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
            mRecordPromptCount++;

        } else {
            //stop recording
            mRecordButton.setImageResource(R.drawable.ic_mic);
            //mPauseButton.setVisibility(View.GONE);
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPaused = 0;
            mRecordingPrompt.setText(getString(R.string.record_prompt));
            imageSave.setVisibility(View.VISIBLE);
            stopRecording();

        }
    }

}