package com.example.astroapp;

import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CaptureLoop {
    private static final String TAG = "AstroCaptureLoop";
    private CameraController cameraController;
    private StorageManager storageManager;
    private PreviewProcessor previewProcessor;
    private boolean isCapturing = false;
    private int frameCountGoal = 100;
    private long interFrameDelayMs = 500;
    private AtomicInteger frameCounter = new AtomicInteger(0);
    private long exposureTimeNs;
    private int iso;
    private Listener listener;

    private ConcurrentHashMap<Long, TotalCaptureResult> resultsMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, Image> imagesMap = new ConcurrentHashMap<>();

    public interface Listener {
        void onProgress(int current, int total);
        void onFinish();
    }

    public CaptureLoop(CameraController cameraController, StorageManager storageManager, PreviewProcessor previewProcessor) {
        this.cameraController = cameraController;
        this.storageManager = storageManager;
        this.previewProcessor = previewProcessor;
    }

    public void start(int frames, long delayMs, long exposureTimeNs, int iso, Listener listener) {
        this.frameCountGoal = frames;
        this.interFrameDelayMs = delayMs;
        this.exposureTimeNs = exposureTimeNs;
        this.iso = iso;
        this.listener = listener;
        this.frameCounter.set(0);
        this.isCapturing = true;
        this.resultsMap.clear();
        this.imagesMap.clear();

        storageManager.newSession();
        captureNext();
    }

    private void captureNext() {
        if (!isCapturing || frameCounter.get() >= frameCountGoal) {
            isCapturing = false;
            if (listener != null) listener.onFinish();
            return;
        }

        try {
            CaptureRequest.Builder builder = cameraController.getCameraDevice().createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(cameraController.getRawImageReader().getSurface());
            builder.addTarget(cameraController.getJpegImageReader().getSurface());

            cameraController.setupManualRequest(builder, exposureTimeNs, iso);
            int idx = frameCounter.get();
            builder.setTag(idx);

            cameraController.getCaptureSession().capture(builder.build(), captureCallback, cameraController.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Long timestamp = result.get(TotalCaptureResult.SENSOR_TIMESTAMP);
            if (timestamp != null) {
                resultsMap.put(timestamp, result);
                matchAndSave(timestamp);
            }

            int currentIdx = frameCounter.incrementAndGet();
            if (listener != null) listener.onProgress(currentIdx, frameCountGoal);

            if (isCapturing && currentIdx < frameCountGoal) {
                if (interFrameDelayMs > 0) {
                    cameraController.getBackgroundHandler().postDelayed(() -> captureNext(), interFrameDelayMs);
                } else {
                    captureNext();
                }
            }
        }
    };

    private void matchAndSave(long timestamp) {
        TotalCaptureResult result = resultsMap.get(timestamp);
        Image image = imagesMap.get(timestamp);

        if (result != null && image != null) {
            resultsMap.remove(timestamp);
            imagesMap.remove(timestamp);

            // For filename indexing, we can use the tag from the result if available,
            // or just rely on a separate counter for saved files.
            Integer frameIdx = (Integer) result.getRequest().getTag();
            storageManager.saveDng(image, cameraController.getCharacteristics(), result, frameIdx != null ? frameIdx : 0, exposureTimeNs, iso);
            image.close();
        }
    }

    public void stop() {
        isCapturing = false;
    }

    public void setupImageReaderListeners() {
        cameraController.getRawImageReader().setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            if (image != null) {
                long timestamp = image.getTimestamp();
                imagesMap.put(timestamp, image);
                matchAndSave(timestamp);
            }
        }, cameraController.getBackgroundHandler());

        cameraController.getJpegImageReader().setOnImageAvailableListener(reader -> {
            Image image = reader.acquireNextImage();
            if (image != null) {
                previewProcessor.processJpeg(image);
                image.close();
            }
        }, cameraController.getBackgroundHandler());
    }
}
