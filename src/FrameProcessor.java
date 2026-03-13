package com.alsclone.astrostacker;

import android.content.Context;
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
    private int defaultBlackLevel = 64;
    private StarDetector downscaledStarDetector;

    private final HandlerThread processingThread;
    private final Handler processingHandler;

    public enum State { IDLE, LIVE, STACKING, PAUSED, CALIBRATING_DARK }
    private volatile State currentState = State.IDLE;
    private boolean showStack = false;
    private final int width, height;
    private int frameLimit = 0;
    private int currentMethod = 0;

    // Using two buffers for efficient handover from Camera thread
    private final short[][] rawBufferPool = new short[2][];
    private int poolIdx = 0;
    private byte[] grayBuffer;
    // Professional Hot Pixel tracking
    private byte[] hotPixelData; // 0-254: votes, 255: confirmed hot
    private int hotAggression = 50;

    private float[] masterDark;
    private volatile int darkCount = 0;
    private volatile boolean isProcessing = false;

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

        this.hotPixelData = new byte[width * height];
    }

    public void setState(State state) {
        android.util.Log.i("FrameProcessor", "State Change: " + state);
        this.currentState = state;
        if (state == State.STACKING) {
            this.showStack = true;
        } else if (state == State.LIVE) {
            this.showStack = false;
        }
        if (state == State.IDLE) {
            processingHandler.removeCallbacksAndMessages(null);
            isProcessing = false;
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
        // stackEngine.setCfaPattern(cfa);
    }

    public void setDefaultBlackLevel(int bl) {
        this.defaultBlackLevel = bl;
    }

    public void setHotAggression(int aggression) {
        this.hotAggression = aggression;
    }

    public void resetStack() {
        processingHandler.post(() -> {
            stackEngine.reset();
            if (hotPixelData != null) java.util.Arrays.fill(hotPixelData, (byte)0);
            isProcessing = false;
            android.util.Log.i("FrameProcessor", "Stack Reset Complete");
        });
    }

    public void startDarkCalibration() {
        processingHandler.post(() -> {
            if (masterDark == null) {
                try {
                    masterDark = new float[width * height];
                } catch (OutOfMemoryError e) {
                    android.util.Log.e("FrameProcessor", "OOM Allocating Dark Buffer");
                    return;
                }
            }
            java.util.Arrays.fill(masterDark, 0.0f);
            darkCount = 0;
            currentState = State.CALIBRATING_DARK;
            android.util.Log.i("FrameProcessor", "Dark Calibration Started");
        });
    }

    public void clearMasterDark() {
        processingHandler.post(() -> {
            masterDark = null;
            darkCount = 0;
            isProcessing = false;
            currentState = State.LIVE;
            android.util.Log.i("FrameProcessor", "Master Dark Cleared");
        });
    }

    public synchronized void processFrame(Image image) {
        if (currentState == State.IDLE || isProcessing) {
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
            android.util.Log.e("FrameProcessor", "Error copying image", e);
            image.close();
            isProcessing = false;
        }
    }

    private void submitToProcessing(final short[] currentRaw) {
        processingHandler.post(() -> {
            try {
                processInternal(currentRaw);
            } catch (Exception e) {
                android.util.Log.e("FrameProcessor", "Error in processInternal", e);
            } finally {
                isProcessing = false;
            }

            if (System.currentTimeMillis() % 10000 < 1000) {
                Runtime r = Runtime.getRuntime();
                long used = (r.totalMemory() - r.freeMemory()) / 1048576;
                android.util.Log.i("FrameProcessor", "Stability Check - Memory Used: " + used + "MB");
            }
        });
    }

    private void processInternal(final short[] currentRaw) {
        State state = currentState;

        if (state == State.PAUSED) {
            if (showStack) {
                if (masterDark != null) renderer.setBlackLevel(0);
                else renderer.setBlackLevel(defaultBlackLevel);
                renderer.updateStack(stackEngine.getStackBuffer(), width, height, stackEngine.getFrameCount(), currentMethod == 1);
            } else {
                renderer.setBlackLevel(defaultBlackLevel);
                renderer.updateLive(currentRaw, width, height);
            }
            return;
        }

        if (state == State.LIVE) {
            renderer.setBlackLevel(defaultBlackLevel);
            renderer.updateLive(currentRaw, width, height);
            return;
        }

        if (state == State.CALIBRATING_DARK) {
            if (masterDark != null) {
                for (int i = 0; i < width * height; i++) {
                    masterDark[i] += (currentRaw[i] & 0xFFFF);
                }
                darkCount++;
                final int currentCount = darkCount;
                new Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Context ctx = renderer.getContext();
                    if (ctx instanceof MainActivity) {
                        if (currentCount % 5 == 0) {
                            ((MainActivity)ctx).addLog("Dark Frame " + currentCount + "/30 captured");
                        }
                    }
                });

                renderer.setDebugInfo("Dark: " + currentCount + "/30");
                if (currentCount >= 30) {
                    for (int i = 0; i < width * height; i++) {
                        masterDark[i] /= 30.0f;
                    }
                    currentState = State.LIVE;
                    renderer.setDebugInfo("Darks Ready");
                    new Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Context ctx = renderer.getContext();
                        if (ctx instanceof MainActivity) {
                            ((MainActivity)ctx).addLog("Master Dark Ready");
                        }
                    });
                }
            }
            return;
        }

        if (state == State.STACKING) {
            if (frameLimit > 0 && stackEngine.getFrameCount() >= frameLimit) {
                currentState = State.PAUSED;
                return;
            }

            if (masterDark != null) {
                for (int i = 0; i < width * height; i++) {
                    int val = (currentRaw[i] & 0xFFFF) - (int)masterDark[i];
                    currentRaw[i] = (short) Math.max(0, Math.min(65535, val));
                }
                renderer.setBlackLevel(0);
            } else {
                renderer.setBlackLevel(defaultBlackLevel);
            }

            int step = (width > 6000) ? 4 : 2;
            int dw = width / step;
            int dh = height / step;
            if (grayBuffer == null || grayBuffer.length != dw * dh) grayBuffer = new byte[dw * dh];

            for (int y = 0; y < dh; y++) {
                for (int x = 0; x < dw; x++) {
                    grayBuffer[y * dw + x] = (byte) ((currentRaw[(y * step) * width + (x * step)] & 0xFFFF) >> 8);
                }
            }

            if (downscaledStarDetector == null || downscaledStarDetector.getWidth() != dw) {
                downscaledStarDetector = new StarDetector(dw, dh);
            }
            List<float[]> stars = downscaledStarDetector.detectStarsCentroid(grayBuffer);

            FrameAligner.Alignment alignment = new FrameAligner.Alignment(0,0,0);
            if (stackEngine.getFrameCount() == 0) {
                frameAligner.setReferenceStars(stars, dw, dh);
            } else {
                alignment = frameAligner.computeAlignment(stars);
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
        }
    }

    public int getFrameCount() {
        return stackEngine.getFrameCount();
    }

    public State getState() {
        return currentState;
    }

    public int getDarkCount() {
        return darkCount;
    }

    public boolean hasMasterDark() {
        return masterDark != null;
    }

    public float[] getResultBuffer() {
        return stackEngine.getStackBuffer();
    }

    public short[] getRawBuffer() {
        return rawBufferPool[(poolIdx + 1) % 2];
    }

    private void removeHotPixels(short[] data, int w, int h) {
        int step = 2;
        if (hotAggression <= 0) return;

        float thresholdMultiplier = 5.0f - (hotAggression / 100.0f) * 4.2f; // 5.0 to 0.8
        int minDiff = 600 - (hotAggression * 5); // 600 to 100

        for (int y = step; y < h - step; y++) {
            for (int x = step; x < w - step; x++) {
                int idx = y * w + x;

                if (hotPixelData[idx] == (byte)255) {
                    // Interpolate from 4 immediate neighbors of same color
                    int sum = (data[idx-step]&0xFFFF) + (data[idx+step]&0xFFFF) +
                              (data[idx-step*w]&0xFFFF) + (data[idx+step*w]&0xFFFF);
                    data[idx] = (short) (sum / 4);
                    continue;
                }

                int val = data[idx] & 0xFFFF;
                if (val > 50) {
                    // Robust 8-neighbor max check in Bayer domain
                    int v1 = data[idx - step] & 0xFFFF;
                    int v2 = data[idx + step] & 0xFFFF;
                    int v3 = data[idx - step * w] & 0xFFFF;
                    int v4 = data[idx + step * w] & 0xFFFF;
                    int v5 = data[idx - step * w - step] & 0xFFFF;
                    int v6 = data[idx - step * w + step] & 0xFFFF;
                    int v7 = data[idx + step * w - step] & 0xFFFF;
                    int v8 = data[idx + step * w + step] & 0xFFFF;

                    int maxN = Math.max(Math.max(Math.max(v1, v2), Math.max(v3, v4)),
                                        Math.max(Math.max(v5, v6), Math.max(v7, v8)));

                    if (val > maxN * thresholdMultiplier && val > maxN + minDiff) {
                        int v = (hotPixelData[idx] & 0xFF);
                        if (v < 254) {
                            v++;
                            hotPixelData[idx] = (byte)v;
                        }
                        if (v > 2) {
                            hotPixelData[idx] = (byte)255;
                        }
                        data[idx] = (short) maxN;
                    }
                }
            }
        }
    }
}
