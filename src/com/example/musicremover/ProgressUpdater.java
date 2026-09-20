package com.example.musicremover;

public class ProgressUpdater implements Runnable {
    private final MainActivity activity;
    private final int percent;
    private final String stage;

    public ProgressUpdater(MainActivity activity, int percent, String stage) {
        this.activity = activity;
        this.percent = percent;
        this.stage = stage;
    }

    @Override
    public void run() {
        activity.updateProgress(percent, stage);
    }
}
