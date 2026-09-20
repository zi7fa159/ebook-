package com.example.musicremover;

import java.io.File;

public class SuccessRunnable implements Runnable {
    private final MainActivity activity;
    private final File resultVideo;
    private final long startTime;

    public SuccessRunnable(MainActivity activity, File resultVideo, long startTime) {
        this.activity = activity;
        this.resultVideo = resultVideo;
        this.startTime = startTime;
    }

    @Override
    public void run() {
        activity.handleSuccess(resultVideo, startTime);
    }
}
