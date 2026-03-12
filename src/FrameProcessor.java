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

    public enum State { IDLE, LIVE, STACKING, PAUSED }
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
    private int[] outlierVotes;

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
        this.outlierVotes = new int[width * height];
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

    public void resetStack() {
        processingHandler.removeCallbacksAndMessages(null);
        processingHandler.post(() -> {
            stackEngine.reset();
            java.util.Arrays.fill(outlierVotes, 0);
            java.util.Arrays.fill(hotPixelMap, (byte)0);
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
            List<Point> stars = sd.detectStars(grayBuffer);

            int dx = 0, dy = 0;
            if (stackEngine.getFrameCount() == 0) {
                frameAligner.setReferenceStars(stars);
            } else {
                Point shift = frameAligner.computeShift(stars);
                // Shift must be even to keep Bayer pattern aligned, and account for downscale
                dx = (shift.x * step / 2) * 2;
                dy = (shift.y * step / 2) * 2;
            }

            removeHotPixels(currentRaw, width, height);
            stackEngine.addFrame(currentRaw, dx, dy);

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

    public float[] getResultBuffer() {
        return stackEngine.getStackBuffer();
    }

    public Image getLastImage() {
        return lastImage;
    }

    private void removeHotPixels(short[] data, int w, int h) {
        int step = 2;
        int frameIdx = stackEngine.getFrameCount();

        for (int y = step; y < h - step; y++) {
            for (int x = step; x < w - step; x++) {
                int idx = y * w + x;

                // If confirmed hot from previous frames, fix immediately
                if (hotPixelMap[idx] == (byte)255) {
                    data[idx] = (short) (( (data[idx-step]&0xFFFF) + (data[idx+step]&0xFFFF) ) / 2);
                    continue;
                }

                int val = data[idx] & 0xFFFF;
                if (val > 300) {
                    int v1 = data[idx - step] & 0xFFFF;
                    int v2 = data[idx + step] & 0xFFFF;
                    int v3 = data[idx - step * w] & 0xFFFF;
                    int v4 = data[idx + step * w] & 0xFFFF;

                    int maxN = Math.max(Math.max(v1, v2), Math.max(v3, v4));
                    int medianN = (v1 + v2 + v3 + v4) / 4;

                    // Outlier detection: significantly brighter than neighbors
                    if (val > maxN * 2.0 && val > medianN + 200) {
                        // Temporal verification: star would move due to drift/alignment,
                        // but a hot pixel stays at the same sensor coordinate.
                        outlierVotes[idx]++;
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
