package com.example.musicremover;

public class ErrorRunnable implements Runnable {
    private final MainActivity activity;
    private final String errorMessage;

    public ErrorRunnable(MainActivity activity, String errorMessage) {
        this.activity = activity;
        this.errorMessage = errorMessage;
    }

    @Override
    public void run() {
        activity.handleError(errorMessage);
    }
}
