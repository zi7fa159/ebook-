package com.musicremover.app;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ProcessTaskRunnable implements Runnable {

    private static final String TAG = "ProcessTaskRunnable";

    private final Context context;
    private final String inputUrl;
    private final File outputFile;
    private final VideoPipeline.ProgressListener listener;
    private final Handler mainHandler;
    private final Button btnProcess;
    private final TextView tvProgress;
    private final MainActivity activity;

    public ProcessTaskRunnable(Context context, String inputUrl, File outputFile,
                                VideoPipeline.ProgressListener listener, Handler mainHandler,
                                Button btnProcess, TextView tvProgress, MainActivity activity) {
        this.context = context;
        this.inputUrl = inputUrl;
        this.outputFile = outputFile;
        this.listener = listener;
        this.mainHandler = mainHandler;
        this.btnProcess = btnProcess;
        this.tvProgress = tvProgress;
        this.activity = activity;
    }

    @Override
    public void run() {
        try {
            VideoPipeline.PipelineResult result = VideoPipeline.processVideo(
                    context,
                    inputUrl,
                    outputFile,
                    listener
            );

            Log.i(TAG, "Processing succeeded: Dur=" + result.videoDurationSec + "s, Time=" +
                    result.totalTimeMs + "ms, RTF=" + String.format("%.3f", result.rtf) +
                    ", RAM=" + String.format("%.2f", result.peakRamMb) + "MB");

            mainHandler.post(new SuccessRunnable(btnProcess, tvProgress, activity, outputFile));

        } catch (Exception e) {
            Log.e(TAG, "Error processing video", e);
            mainHandler.post(new ErrorRunnable(btnProcess, tvProgress, context, e.getMessage()));
        }
    }
}

class SuccessRunnable implements Runnable {
    private final Button btnProcess;
    private final TextView tvProgress;
    private final MainActivity activity;
    private final File outputFile;

    public SuccessRunnable(Button btnProcess, TextView tvProgress, MainActivity activity, File outputFile) {
        this.btnProcess = btnProcess;
        this.tvProgress = tvProgress;
        this.activity = activity;
        this.outputFile = outputFile;
    }

    @Override
    public void run() {
        btnProcess.setEnabled(true);
        tvProgress.setText("Processing: 100%");
        activity.playProcessedVideo(outputFile);
    }
}

class ErrorRunnable implements Runnable {
    private final Button btnProcess;
    private final TextView tvProgress;
    private final Context context;
    private final String errorMessage;

    public ErrorRunnable(Button btnProcess, TextView tvProgress, Context context, String errorMessage) {
        this.btnProcess = btnProcess;
        this.tvProgress = tvProgress;
        this.context = context;
        this.errorMessage = errorMessage;
    }

    @Override
    public void run() {
        btnProcess.setEnabled(true);
        tvProgress.setText("Error: " + errorMessage);
        Toast.makeText(context, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
    }
}
