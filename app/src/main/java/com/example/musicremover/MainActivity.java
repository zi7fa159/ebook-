package com.example.musicremover;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends Activity implements View.OnClickListener, VideoProcessor.ProgressCallback {

    private EditText etUrl;
    private Button btnProcess;
    private TextView tvStatus;
    private VideoView videoView;
    private Button btnPlay;
    private Button btnPause;
    private Button btnSeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUrl = findViewById(R.id.etUrl);
        btnProcess = findViewById(R.id.btnProcess);
        tvStatus = findViewById(R.id.tvStatus);
        videoView = findViewById(R.id.videoView);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnSeek = findViewById(R.id.btnSeek);

        btnProcess.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnSeek.setOnClickListener(this);

        etUrl.setText("https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-480p-30s/master/video.mp4");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnProcess) {
            String inputUrlOrPath = etUrl.getText().toString().trim();
            if (inputUrlOrPath.isEmpty()) {
                Toast.makeText(this, "Please enter a video URL or file path", Toast.LENGTH_SHORT).show();
                return;
            }
            btnProcess.setEnabled(false);
            tvStatus.setText("Processing: 0%");
            VideoProcessor.processVideo(inputUrlOrPath, getCacheDir(), this);

        } else if (id == R.id.btnPlay) {
            if (videoView != null) {
                videoView.start();
            }
        } else if (id == R.id.btnPause) {
            if (videoView != null && videoView.isPlaying()) {
                videoView.pause();
            }
        } else if (id == R.id.btnSeek) {
            if (videoView != null) {
                int currentPos = videoView.getCurrentPosition();
                int duration = videoView.getDuration();
                if (duration > 0) {
                    int newPos = (currentPos + 5000) % duration;
                    videoView.seekTo(newPos);
                }
            }
        }
    }

    private static class ProgressRunnable implements Runnable {
        private final TextView tvStatus;
        private final int percentage;
        private final String stage;

        public ProgressRunnable(TextView tvStatus, int percentage, String stage) {
            this.tvStatus = tvStatus;
            this.percentage = percentage;
            this.stage = stage;
        }

        @Override
        public void run() {
            tvStatus.setText("Processing: " + percentage + "% (" + stage + ")");
        }
    }

    private static class ErrorRunnable implements Runnable {
        private final MainActivity activity;
        private final String error;

        public ErrorRunnable(MainActivity activity, String error) {
            this.activity = activity;
            this.error = error;
        }

        @Override
        public void run() {
            activity.btnProcess.setEnabled(true);
            activity.tvStatus.setText("Error: " + error);
            Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
        }
    }

    private static class CompleteRunnable implements Runnable {
        private final MainActivity activity;
        private final String outputVideoPath;
        private final long totalTimeMs;
        private final double rtf;

        public CompleteRunnable(MainActivity activity, String outputVideoPath, long totalTimeMs, double rtf) {
            this.activity = activity;
            this.outputVideoPath = outputVideoPath;
            this.totalTimeMs = totalTimeMs;
            this.rtf = rtf;
        }

        @Override
        public void run() {
            activity.btnProcess.setEnabled(true);
            activity.tvStatus.setText("Processing: 100% | Time: " + totalTimeMs + "ms | RTF: " + Math.round(rtf * 100.0) / 100.0);
            activity.playVideo(outputVideoPath);
        }
    }

    @Override
    public void onProgress(int percentage, String stage) {
        runOnUiThread(new ProgressRunnable(tvStatus, percentage, stage));
    }

    @Override
    public void onError(String error) {
        runOnUiThread(new ErrorRunnable(this, error));
    }

    @Override
    public void onComplete(String outputVideoPath, long totalTimeMs, double rtf, long videoDurationMs) {
        runOnUiThread(new CompleteRunnable(this, outputVideoPath, totalTimeMs, rtf));
    }

    private static class PreparedListener implements MediaPlayer.OnPreparedListener {
        private final VideoView videoView;

        public PreparedListener(VideoView videoView) {
            this.videoView = videoView;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.setLooping(true);
            videoView.start();
        }
    }

    private void playVideo(String videoPath) {
        videoView.setVideoPath(videoPath);
        videoView.setOnPreparedListener(new PreparedListener(videoView));
    }
}
