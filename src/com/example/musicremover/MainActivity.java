package com.example.musicremover;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener, MediaPipeline.ProgressCallback {

    private static final String TAG = "MainActivity";

    private EditText urlEditText;
    private Button processButton;
    private TextView progressText;
    private VideoView videoView;
    private Button playButton;
    private Button pauseButton;
    private Button seekButton;
    private TextView statusText;

    private File resultVideoFile;
    private boolean isProcessing = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlEditText = (EditText) findViewById(R.id.urlEditText);
        processButton = (Button) findViewById(R.id.processButton);
        progressText = (TextView) findViewById(R.id.progressText);
        videoView = (VideoView) findViewById(R.id.videoView);
        playButton = (Button) findViewById(R.id.playButton);
        pauseButton = (Button) findViewById(R.id.pauseButton);
        seekButton = (Button) findViewById(R.id.seekButton);
        statusText = (TextView) findViewById(R.id.statusText);

        processButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        seekButton.setOnClickListener(this);

        videoView.setOnPreparedListener(new VideoPreparedListener());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.processButton) {
            String inputUrl = urlEditText.getText().toString().trim();
            if (inputUrl.length() == 0) {
                Toast.makeText(this, "Please enter a video URL or local file path", Toast.LENGTH_SHORT).show();
                return;
            }
            startProcessing(inputUrl);
        } else if (id == R.id.playButton) {
            if (resultVideoFile != null && resultVideoFile.exists()) {
                videoView.start();
            } else {
                Toast.makeText(this, "No video processed yet", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.pauseButton) {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
        } else if (id == R.id.seekButton) {
            if (resultVideoFile != null && resultVideoFile.exists()) {
                int currentPos = videoView.getCurrentPosition();
                int duration = videoView.getDuration();
                int newPos = currentPos + 5000;
                if (duration > 0 && newPos > duration) {
                    newPos = 0;
                }
                videoView.seekTo(newPos);
            }
        }
    }

    @Override
    public void onProgress(int percent, String stage) {
        mainHandler.post(new ProgressUpdater(this, percent, stage));
    }

    public void updateProgress(int percent, String stage) {
        progressText.setText("Processing: " + percent + "%");
        statusText.setText(stage);
    }

    public void onProcessSuccess(File resultVideo, long startTime) {
        mainHandler.post(new SuccessRunnable(this, resultVideo, startTime));
    }

    public void handleSuccess(File resultVideo, long startTime) {
        isProcessing = false;
        processButton.setEnabled(true);
        if (resultVideo != null && resultVideo.exists()) {
            long totalTimeMs = System.currentTimeMillis() - startTime;
            progressText.setText("Processing: 100%");
            statusText.setText("Done in " + totalTimeMs + "ms. Size: " + (resultVideo.length() / 1024) + " KB");
            resultVideoFile = resultVideo;
            videoView.setVideoURI(Uri.fromFile(resultVideoFile));
            videoView.start();
        } else {
            progressText.setText("Processing: Failed");
            statusText.setText("Error producing result video");
        }
    }

    public void onProcessError(String errorMessage) {
        mainHandler.post(new ErrorRunnable(this, errorMessage));
    }

    public void handleError(String errorMessage) {
        isProcessing = false;
        processButton.setEnabled(true);
        progressText.setText("Processing: Failed");
        statusText.setText("Error: " + errorMessage);
        Toast.makeText(this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    private void startProcessing(String sourceUrl) {
        if (isProcessing) return;
        isProcessing = true;
        processButton.setEnabled(false);
        progressText.setText("Processing: 0%");
        statusText.setText("Starting process...");

        long startTime = System.currentTimeMillis();
        Thread processingThread = new Thread(new ProcessTaskRunnable(this, sourceUrl, startTime));
        processingThread.start();
    }
}
