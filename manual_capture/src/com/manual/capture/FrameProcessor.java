package com.manual.capture;

import android.content.Context;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import java.nio.ByteBuffer;

public class FrameProcessor {
    private final Renderer renderer;
    private int width, height;
    private final short[][] rawBufferPool = new short[2][];
    private int poolIdx = 0;
    private volatile boolean isProcessing = false;
    private int defaultBlackLevel = 64;
    private float whiteLevel = 1023f;

    private final HandlerThread processingThread;
    private final Handler processingHandler;

    public FrameProcessor(int width, int height, Renderer renderer) {
        this.width = width;
        this.height = height;
        this.renderer = renderer;
        this.processingThread = new HandlerThread("FrameProcessor");
        this.processingThread.start();
        this.processingHandler = new Handler(processingThread.getLooper());
    }

    public void setSensorLevels(int bl, float wl) {
        this.defaultBlackLevel = bl;
        this.whiteLevel = wl;
    }

    public synchronized void processFrame(Image image) {
        if (isProcessing) {
            image.close();
            return;
        }

        try {
            isProcessing = true;
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            buffer.order(java.nio.ByteOrder.nativeOrder());

            if (rawBufferPool[0] == null) rawBufferPool[0] = new short[width * height];
            if (rawBufferPool[1] == null) rawBufferPool[1] = new short[width * height];

            final short[] currentRaw = rawBufferPool[poolIdx];
            poolIdx = (poolIdx + 1) % 2;

            int rowStride = plane.getRowStride();
            if (rowStride == width * 2) {
                buffer.asShortBuffer().get(currentRaw);
            } else {
                for (int y = 0; y < height; y++) {
                    buffer.position(y * rowStride);
                    buffer.asShortBuffer().get(currentRaw, y * width, width);
                }
            }

            submitToProcessing(currentRaw);
            image.close();
        } catch (Exception e) {
            image.close();
            isProcessing = false;
        }
    }

    private void submitToProcessing(final short[] currentRaw) {
        processingHandler.post(() -> {
            try {
                renderer.setBlackLevel(defaultBlackLevel);
                renderer.setWhiteLevel(whiteLevel);
                renderer.updateLive(currentRaw, width, height);
            } finally {
                isProcessing = false;
            }
        });
    }

    public void stop() {
        processingThread.quitSafely();
    }
}
