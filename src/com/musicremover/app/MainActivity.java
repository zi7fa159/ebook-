package com.musicremover.app;

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

public class MainActivity extends Activity implements View.OnClickListener, MediaPlayer.OnPreparedListener, VideoPipeline.ProgressListener {

    private static final String TAG = "MainActivity";

    private EditText etVideoUrl;
    private Button btnProcess;
    private TextView tvProgress;
    private VideoView videoView;
    private Button btnPlay;
    private Button btnPause;
    private Button btnSeek;

    private Handler mainHandler;
    private File processedVideoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        etVideoUrl = (EditText) findViewById(R.id.etVideoUrl);
        btnProcess = (Button) findViewById(R.id.btnProcess);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        videoView = (VideoView) findViewById(R.id.videoView);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnPause = (Button) findViewById(R.id.btnPause);
        btnSeek = (Button) findViewById(R.id.btnSeek);

        btnProcess.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnSeek.setOnClickListener(this);

        videoView.setOnPreparedListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnProcess) {
            startProcessing();
        } else if (id == R.id.btnPlay) {
            if (videoView != null && !videoView.isPlaying()) {
                videoView.start();
            }
        } else if (id == R.id.btnPause) {
            if (videoView != null && videoView.isPlaying()) {
                videoView.pause();
            }
        } else if (id == R.id.btnSeek) {
            if (videoView != null) {
                int duration = videoView.getDuration();
                if (duration > 0) {
                    int current = videoView.getCurrentPosition();
                    int target = (current + 5000) % duration;
                    videoView.seekTo(target);
                }
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setLooping(true);
    }

    @Override
    public void onProgress(int percent, String stage) {
        mainHandler.post(new ProgressUpdateRunnable(tvProgress, percent));
    }

    private void startProcessing() {
        String inputUrl = etVideoUrl.getText().toString().trim();
        if (inputUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a valid video URL or local file path", Toast.LENGTH_SHORT).show();
            return;
        }

        btnProcess.setEnabled(false);
        tvProgress.setText("Processing: 0%");

        processedVideoFile = new File(getExternalFilesDir(null), "result.mp4");

        ProcessTaskRunnable task = new ProcessTaskRunnable(
                this,
                inputUrl,
                processedVideoFile,
                this,
                mainHandler,
                btnProcess,
                tvProgress,
                this
        );

        new Thread(task).start();
    }

    public void playProcessedVideo(File file) {
        if (file.exists()) {
            videoView.setVideoURI(Uri.fromFile(file));
            videoView.start();
        }
    }
}

class ProgressUpdateRunnable implements Runnable {
    private final TextView tvProgress;
    private final int percent;

    public ProgressUpdateRunnable(TextView tvProgress, int percent) {
        this.tvProgress = tvProgress;
        this.percent = percent;
    }

    @Override
    public void run() {
        tvProgress.setText("Processing: " + percent + "%");
    }
}
