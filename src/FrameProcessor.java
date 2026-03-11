package com.alsclone.astrostacker;

import android.graphics.Point;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
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

    private short[][] rawBufferPool = new short[2][];
    private int poolIdx = 0;
    private byte[] grayBuffer;

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
        stackEngine.reset();
        isStacking = true;
    }

    public void stopStacking() {
        isStacking = false;
    }

    public void processFrame(Image image) {
        if (!isStacking) {
            image.close();
            return;
        }

        // Handle RAW_SENSOR (usually 16-bit)
        // For Realme 8i 50MP, check plane count and format
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();

        // Buffer reuse
        if (rawBufferPool[0] == null) rawBufferPool[0] = new short[width * height];
        if (rawBufferPool[1] == null) rawBufferPool[1] = new short[width * height];
        if (grayBuffer == null) grayBuffer = new byte[width * height];

        final short[] currentRaw = rawBufferPool[poolIdx];
        poolIdx = (poolIdx + 1) % 2;

        buffer.order(java.nio.ByteOrder.nativeOrder());

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
            // Processing happens on currentRaw which is now reserved for this task
            for (int i = 0; i < currentRaw.length; i++) {
                grayBuffer[i] = (byte) ((currentRaw[i] & 0xFFFF) >> 8);
            }

            List<Point> stars = starDetector.detectStars(grayBuffer);

            int dx = 0, dy = 0;
            if (stackEngine.getFrameCount() == 0) {
                frameAligner.setReferenceStars(stars);
            } else {
                Point shift = frameAligner.computeShift(stars);
                dx = shift.x;
                dy = shift.y;
            }

            stackEngine.addFrame(currentRaw, dx, dy);

            // Update preview
            renderer.updateStack(stackEngine.getStackBuffer(), width, height);
        });
    }

    public int getFrameCount() {
        return stackEngine.getFrameCount();
    }

    public float[] getResultBuffer() {
        return stackEngine.getStackBuffer();
    }
}
