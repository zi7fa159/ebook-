package com.astroapp;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CaptureLoop {
    private static final String TAG = "CaptureLoop";
    private final CameraController controller;
    private final StorageManager storageManager;
    private final ImageReader rawReader;
    private final ImageReader jpegReader;
    private final HandlerThread captureThread;
    private final Handler captureHandler;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private int frameCount = 100;
    private int currentFrame = 0;
    private int iso;
    private long exposureNs;

    private final TreeMap<Long, TotalCaptureResult> resultQueue = new TreeMap<>();
    private final TreeMap<Long, Image> rawQueue = new TreeMap<>();
    private final TreeMap<Long, Image> jpegQueue = new TreeMap<>();

    public interface ProgressListener {
        void onProgress(int current, int total);
        void onJpegAvailable(Image image);
        void onFinished();
    }

    private ProgressListener listener;

    public CaptureLoop(CameraController controller, StorageManager storageManager, ImageReader rawReader, ImageReader jpegReader) {
        this.controller = controller;
        this.storageManager = storageManager;
        this.rawReader = rawReader;
        this.jpegReader = jpegReader;
        this.captureThread = new HandlerThread("CaptureThread");
        this.captureThread.start();
        this.captureHandler = new Handler(captureThread.getLooper());

        setupReaders();
    }

    private void setupReaders() {
        rawReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            if (image != null) {
                synchronized (this) {
                    rawQueue.put(image.getTimestamp(), image);
                    checkAndSave();
                }
            }
        }, captureHandler);

        jpegReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            if (image != null) {
                if (listener != null) listener.onJpegAvailable(image);
                // Also optionally match JPEG to result/timestamp if needed for storage
                // For now, let's just close it if not used for storage
                image.close();
            }
        }, captureHandler);
    }

    private synchronized void checkAndSave() {
        while (!rawQueue.isEmpty() && !resultQueue.isEmpty()) {
            Long firstRawTime = rawQueue.firstKey();
            Long firstResultTime = resultQueue.firstKey();

            if (firstRawTime.equals(firstResultTime)) {
                Image rawImage = rawQueue.remove(firstRawTime);
                TotalCaptureResult result = resultQueue.remove(firstResultTime);

                int frameIdx = ++currentFrame;
                storageManager.saveFrame(rawImage, frameIdx, result, controller.getCharacteristics(), iso, exposureNs);

                if (listener != null) listener.onProgress(frameIdx, frameCount);

                if (frameIdx >= frameCount) {
                    stop();
                } else {
                    captureHandler.post(this::captureNext);
                }
            } else if (firstRawTime < firstResultTime) {
                rawQueue.remove(firstRawTime).close();
            } else {
                resultQueue.remove(firstResultTime);
            }
        }
    }

    public void start(int frames, int iso, long exposureNs, ProgressListener listener) {
        if (isRunning.get()) return;
        this.frameCount = frames;
        this.iso = iso;
        this.exposureNs = exposureNs;
        this.currentFrame = 0;
        this.listener = listener;
        this.isRunning.set(true);

        synchronized (this) {
            resultQueue.clear();
            rawQueue.clear();
            jpegQueue.clear();
        }

        captureHandler.post(this::captureNext);
    }

    private void captureNext() {
        if (!isRunning.get() || currentFrame >= frameCount) return;

        try {
            CaptureRequest request = controller.createCaptureRequest(rawReader.getSurface(), jpegReader.getSurface(), iso, exposureNs);
            controller.getCaptureSession().capture(request, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    synchronized (CaptureLoop.this) {
                        resultQueue.put(result.get(TotalCaptureResult.SENSOR_TIMESTAMP), result);
                        checkAndSave();
                    }
                }
            }, captureHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Capture failed", e);
            stop();
        }
    }

    public void stop() {
        if (isRunning.getAndSet(false)) {
            storageManager.finalizeSession();
            if (listener != null) listener.onFinished();
        }
    }

    public void quit() {
        stop();
        captureThread.quitSafely();
    }
}
