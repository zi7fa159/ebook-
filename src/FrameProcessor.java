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

    public enum State { IDLE, LIVE, STACKING, PAUSED, CALIBRATING_DARK }
    private State currentState = State.IDLE;
    private boolean showStack = false;
    private int width, height;
    private int frameLimit = 0;
    private int currentMethod = 0;

    // Using two buffers for efficient handover from Camera thread
    private short[][] rawBufferPool = new short[2][];
    private int poolIdx = 0;
    private byte[] grayBuffer;
    private volatile Image lastImage;

    // Professional Hot Pixel tracking
    private byte[] hotPixelMap; // 0: clear, 255: confirmed hot
    private byte[] outlierVotes;
    private int hotAggression = 50;

    private float[] masterDark;
    private int darkCount = 0;
    private int rejectedCount = 0;

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

        this.hotPixelMap = new byte[width * height];
        this.outlierVotes = new byte[width * height];
    }

    public void setState(State state) {
        this.currentState = state;
        if (state == State.STACKING) {
            this.showStack = true;
        } else if (state == State.LIVE) {
            this.showStack = false;
        }
        if (state == State.IDLE) {
            processingHandler.removeCallbacksAndMessages(null);
        }
    }

    public void setShowStack(boolean show) {
        this.showStack = show;
    }

    public void setFrameLimit(int limit) {
        this.frameLimit = limit;
    }

    public void setStackMethod(int method) {
        this.currentMethod = method;
        stackEngine.setStackMethod(method);
    }

    public void setCfaPattern(int cfa) {
        stackEngine.setCfaPattern(cfa);
    }

    public void setHotAggression(int aggression) {
        this.hotAggression = aggression;
    }

    public void resetStack() {
        processingHandler.removeCallbacksAndMessages(null);
        processingHandler.post(() -> {
            stackEngine.reset();
            java.util.Arrays.fill(outlierVotes, (byte)0);
            java.util.Arrays.fill(hotPixelMap, (byte)0);
            rejectedCount = 0;
        });
    }

    public void startDarkCalibration() {
        processingHandler.post(() -> {
            masterDark = new float[width * height];
            darkCount = 0;
            currentState = State.CALIBRATING_DARK;
        });
    }

    public synchronized void processFrame(Image image) {
        if (currentState == State.IDLE) {
            image.close();
            return;
        }

        try {
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

            // Keep a copy of the metadata but close the actual image as soon as we can
            if (lastImage != null) lastImage.close();
            lastImage = image;
            // Actually, we must NOT close it if we want to save DNG later,
            // but we can only hold ONE image if we want to keep the reader happy.

            submitToProcessing(currentRaw);
        } catch (Exception e) {
            android.util.Log.e("FrameProcessor", "Error copying image", e);
            image.close();
        }
    }

    private void submitToProcessing(final short[] currentRaw) {
        processingHandler.post(() -> {
            if (currentState == State.PAUSED) {
                if (showStack) {
                    renderer.updateStack(stackEngine.getStackBuffer(), width, height, stackEngine.getFrameCount(), currentMethod == 1);
                } else {
                    renderer.updateLive(currentRaw, width, height);
                }
                return;
            }

            if (currentState == State.LIVE) {
                renderer.updateLive(currentRaw, width, height);
                return;
            }

            if (frameLimit > 0 && stackEngine.getFrameCount() >= frameLimit) {
                currentState = State.PAUSED; // Auto-switch to paused when limit reached
                return;
            }

            if (currentState == State.CALIBRATING_DARK) {
                for (int i = 0; i < width * height; i++) {
                    masterDark[i] = (masterDark[i] * darkCount + (currentRaw[i] & 0xFFFF)) / (darkCount + 1);
                }
                darkCount++;
                if (darkCount >= 10) currentState = State.PAUSED; // Done after 10 frames
                return;
            }

            if (masterDark != null) {
                for (int i = 0; i < width * height; i++) {
                    int val = (currentRaw[i] & 0xFFFF) - (int)masterDark[i];
                    currentRaw[i] = (short) Math.max(0, val);
                }
            }

            // Downscale for star detection speed (4x if 50MP, else 2x)
            int step = (width > 6000) ? 4 : 2;
            int dw = width / step;
            int dh = height / step;
            if (grayBuffer == null || grayBuffer.length != dw * dh) grayBuffer = new byte[dw * dh];

            for (int y = 0; y < dh; y++) {
                for (int x = 0; x < dw; x++) {
                    grayBuffer[y * dw + x] = (byte) ((currentRaw[(y * step) * width + (x * step)] & 0xFFFF) >> 8);
                }
            }

            // Use the pre-allocated star detector or create one for the correct size
            StarDetector sd = new StarDetector(dw, dh);
            List<float[]> stars = sd.detectStarsCentroid(grayBuffer);

            // Frame Quality Rejection (FWHM estimate)
            if (stars.size() < 5) {
                rejectedCount++;
                return;
            }

            FrameAligner.Alignment alignment = new FrameAligner.Alignment(0,0,0);
            if (stackEngine.getFrameCount() == 0) {
                frameAligner.setReferenceStars(stars, dw, dh);
            } else {
                alignment = frameAligner.computeAlignment(stars);
                // Scale back to full resolution
                alignment.dx *= step;
                alignment.dy *= step;
            }

            removeHotPixels(currentRaw, width, height);
            stackEngine.addFrame(currentRaw, alignment);

            if (showStack) {
                renderer.updateStack(stackEngine.getStackBuffer(), width, height, stackEngine.getFrameCount(), currentMethod == 1);
            } else {
                renderer.updateLive(currentRaw, width, height);
            }
        });
    }

    public int getFrameCount() {
        return stackEngine.getFrameCount();
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public float[] getResultBuffer() {
        return stackEngine.getStackBuffer();
    }

    public Image getLastImage() {
        return lastImage;
    }

    private void removeHotPixels(short[] data, int w, int h) {
        int step = 2;
        // 0 to 100 range. 0=disabled, 100=most aggressive
        if (hotAggression <= 0) return;

        float thresholdMultiplier = 4.0f - (hotAggression / 100.0f) * 3.0f; // 4.0 to 1.0
        int minDiff = 500 - (hotAggression * 4); // 500 to 100

        for (int y = step; y < h - step; y++) {
            for (int x = step; x < w - step; x++) {
                int idx = y * w + x;

                if (hotPixelMap[idx] == (byte)255) {
                    data[idx] = (short) (( (data[idx-step]&0xFFFF) + (data[idx+step]&0xFFFF) ) / 2);
                    continue;
                }

                int val = data[idx] & 0xFFFF;
                if (val > 100) {
                    int v1 = data[idx - step] & 0xFFFF;
                    int v2 = data[idx + step] & 0xFFFF;
                    int v3 = data[idx - step * w] & 0xFFFF;
                    int v4 = data[idx + step * w] & 0xFFFF;

                    int maxN = Math.max(Math.max(v1, v2), Math.max(v3, v4));
                    if (val > maxN * thresholdMultiplier && val > maxN + minDiff) {
                        if (outlierVotes[idx] < 127) outlierVotes[idx]++;
                        if (outlierVotes[idx] > 3) {
                            hotPixelMap[idx] = (byte)255;
                        }
                        data[idx] = (short) maxN;
                    }
                }
            }
        }
    }
}
