package com.alsclone.astrostacker;

import android.graphics.Point;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import java.nio.ByteBuffer;
import java.util.List;

public class FrameProcessor {
    private final StackEngine stackEngine;
    private final StarDetector starDetector;
    private final FrameAligner frameAligner;
    private final Renderer renderer;

    private final HandlerThread processingThread;
    private final Handler processingHandler;

    private boolean isStacking = false;
    private int width, height;

    // Using two buffers for efficient handover from Camera thread
    private short[][] rawBufferPool = new short[2][];
    private int poolIdx = 0;
    private byte[] grayBuffer;
    private Image latestRawImage;

    public FrameProcessor(int width, int height, Renderer renderer) {
        this.width = width;
        this.height = height;
        this.renderer = renderer;
        this.stackEngine = new StackEngine(width, height);
        this.starDetector = new StarDetector(width, height);
        this.frameAligner = new FrameAligner();

        this.processingThread = new HandlerThread("FrameProcessor");
        this.processingThread.start();
        this.processingHandler = new Handler(processingThread.getLooper());
    }

    public void startStacking() {
        isStacking = true;
    }

    public void stopStacking() {
        isStacking = false;
        processingHandler.removeCallbacksAndMessages(null);
    }

    public void resetStack() {
        processingHandler.post(() -> {
            stackEngine.reset();
        });
    }

    public synchronized void processFrame(Image image) {
        if (!isStacking) {
            image.close();
            return;
        }

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.order(java.nio.ByteOrder.nativeOrder());

        if (rawBufferPool[0] == null) rawBufferPool[0] = new short[width * height];
        if (rawBufferPool[1] == null) rawBufferPool[1] = new short[width * height];
        if (grayBuffer == null) grayBuffer = new byte[width * height];

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

        image.close();

        processingHandler.post(() -> {
            // Downscale for star detection speed (4x)
            int dw = width / 2;
            int dh = height / 2;
            if (grayBuffer.length < dw * dh) grayBuffer = new byte[dw * dh];

            for (int y = 0; y < dh; y++) {
                for (int x = 0; x < dw; x++) {
                    grayBuffer[y * dw + x] = (byte) ((currentRaw[(y * 2) * width + (x * 2)] & 0xFFFF) >> 8);
                }
            }

            // Create a star detector that knows about downscaling
            StarDetector sd = new StarDetector(dw, dh);
            List<Point> stars = sd.detectStars(grayBuffer);

            int dx = 0, dy = 0;
            if (stackEngine.getFrameCount() == 0) {
                frameAligner.setReferenceStars(stars);
            } else {
                Point shift = frameAligner.computeShift(stars);
                // Shift must be even to keep Bayer pattern aligned, and account for 2x downscale
                dx = (shift.x * 2 / 2) * 2;
                dy = (shift.y * 2 / 2) * 2;
            }

            removeHotPixels(currentRaw, width, height);
            stackEngine.addFrame(currentRaw, dx, dy);
            renderer.updateStack(stackEngine.getStackBuffer(), width, height);
        });
    }

    public int getFrameCount() {
        return stackEngine.getFrameCount();
    }

    public float[] getResultBuffer() {
        return stackEngine.getStackBuffer();
    }

    private void removeHotPixels(short[] data, int w, int h) {
        int threshold = 8000;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int idx = y * w + x;
                int val = data[idx] & 0xFFFF;
                if (val > threshold) {
                    int maxNeighbor = 0;
                    maxNeighbor = Math.max(maxNeighbor, data[idx - 1] & 0xFFFF);
                    maxNeighbor = Math.max(maxNeighbor, data[idx + 1] & 0xFFFF);
                    maxNeighbor = Math.max(maxNeighbor, data[idx - w] & 0xFFFF);
                    maxNeighbor = Math.max(maxNeighbor, data[idx + w] & 0xFFFF);
                    if (val > maxNeighbor * 2) data[idx] = (short) maxNeighbor;
                }
            }
        }
    }
}
