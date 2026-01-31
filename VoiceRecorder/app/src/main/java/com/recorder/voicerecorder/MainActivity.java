package com.recorder.voicerecorder;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 100;
    private Button btnRecord, btnStop, btnPlay;
    private TextView tvStatus, tvTimer;
    private Switch swCallRecording;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;

    private boolean isRecording = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;

    private SharedPreferences sharedPreferences;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);
        btnPlay = findViewById(R.id.btnPlay);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);
        swCallRecording = findViewById(R.id.switch_call_recording);

        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";

        sharedPreferences = getSharedPreferences("VoiceRecorderPrefs", MODE_PRIVATE);
        swCallRecording.setChecked(sharedPreferences.getBoolean("call_recording_enabled", false));

        swCallRecording.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (checkPermissions()) {
                        enableCallRecording(true);
                    } else {
                        requestPermissions();
                    }
                } else {
                    enableCallRecording(false);
                }
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    startRecording();
                } else {
                    requestPermissions();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecording();
            }
        });
    }

    private void enableCallRecording(boolean enable) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("call_recording_enabled", enable);
        editor.apply();
        if (enable) {
            Toast.makeText(this, "Call Recording Enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Call Recording Disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();

            startTime = System.currentTimeMillis();
            handler.postDelayed(timerRunnable, 0);

            isRecording = true;
            tvStatus.setText("Recording...");
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
            btnPlay.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;

            handler.removeCallbacks(timerRunnable);

            isRecording = false;
            tvStatus.setText("Recording Stopped");
            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(true);
        }
    }

    private void playRecording() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            tvStatus.setText("Playing Recording");
            btnRecord.setEnabled(false);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(false);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;

            tvStatus.setText("Press Record to start recording");
            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(true);
        }
    }

    private boolean checkPermissions() {
        int recordAudio = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        int writeStorage = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPhoneState = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
        return recordAudio == PackageManager.PERMISSION_GRANTED && writeStorage == PackageManager.PERMISSION_GRANTED && readPhoneState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean recordAudioGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean writeStorageGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean readPhoneStateGranted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                if (recordAudioGranted && writeStorageGranted && readPhoneStateGranted) {
                    Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                    enableCallRecording(true);
                    swCallRecording.setChecked(true);
                } else {
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                    enableCallRecording(false);
                    swCallRecording.setChecked(false);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRecording) {
            stopRecording();
        }
        if (mediaPlayer != null) {
            stopPlaying();
        }
    }
}
