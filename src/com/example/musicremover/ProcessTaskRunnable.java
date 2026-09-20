package com.example.musicremover;

import java.io.File;

public class ProcessTaskRunnable implements Runnable {
    private final MainActivity activity;
    private final String sourceUrl;
    private final long startTime;

    public ProcessTaskRunnable(MainActivity activity, String sourceUrl, long startTime) {
        this.activity = activity;
        this.sourceUrl = sourceUrl;
        this.startTime = startTime;
    }

    @Override
    public void run() {
        try {
            File inputVideo = MediaPipeline.obtainInputVideo(activity, sourceUrl, activity);
            File resultVideo = MediaPipeline.processVideo(activity, inputVideo, activity);
            activity.onProcessSuccess(resultVideo, startTime);
        } catch (Exception e) {
            activity.onProcessError(e.getMessage());
        }
    }
}
