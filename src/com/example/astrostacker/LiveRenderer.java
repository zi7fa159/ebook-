package com.example.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import java.util.Arrays;

public class LiveRenderer {
    private final SurfaceHolder holder;
    private Bitmap bitmap;
    private int[] pixels;
    private float[] sample = new float[10000];

    public LiveRenderer(SurfaceHolder holder, int width, int height) {
        this.holder = holder;
        // Half-size bitmap for fast preview performance
        this.bitmap = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888);
        this.pixels = new int[(width / 2) * (height / 2)];
    }

    public void renderRaw(float[] rawStack, int width, int height) {
        int outW = width / 2;
        int outH = height / 2;

        // Adaptive stretch for preview
        int sampleStep = Math.max(1, rawStack.length / sample.length);
        for (int i = 0; i < sample.length; i++) {
            sample[i] = rawStack[Math.min(i * sampleStep, rawStack.length - 1)];
        }
        Arrays.sort(sample);
        float black = sample[(int)(sample.length * 0.1)];
        float white = sample[(int)(sample.length * 0.95)];
        if (white <= black) white = black + 1.0f;
        float invRange = 255.0f / (white - black);

        // Fast 2x2 debayering to RGB
        for (int y = 0; y < outH; y++) {
            for (int x = 0; x < outW; x++) {
                int rIdx = (y * 2) * width + (x * 2);
                int g1Idx = (y * 2) * width + (x * 2 + 1);
                int g2Idx = (y * 2 + 1) * width + (x * 2);
                int bIdx = (y * 2 + 1) * width + (x * 2 + 1);

                int r = (int)((rawStack[rIdx] - black) * invRange);
                int g = (int)(((rawStack[g1Idx] + rawStack[g2Idx]) / 2.0f - black) * invRange);
                int b = (int)((rawStack[bIdx] - black) * invRange);

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                pixels[y * outW + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        bitmap.setPixels(pixels, 0, outW, 0, 0, outW, outH);
        drawToSurface();
    }

    public void renderYuv(byte[] yData, int width, int height) {
        int outW = width / 2;
        int outH = height / 2;
        for (int y = 0; y < outH; y++) {
            for (int x = 0; x < outW; x++) {
                int val = yData[(y * 2) * width + (x * 2)] & 0xFF;
                pixels[y * outW + x] = 0xFF000000 | (val << 16) | (val << 8) | val;
            }
        }
        bitmap.setPixels(pixels, 0, outW, 0, 0, outW, outH);
        drawToSurface();
    }

    private void drawToSurface() {
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            try {
                int cw = canvas.getWidth();
                int ch = canvas.getHeight();
                float scale = Math.min((float)cw / bitmap.getWidth(), (float)ch / bitmap.getHeight());
                int dw = (int)(bitmap.getWidth() * scale);
                int dh = (int)(bitmap.getHeight() * scale);
                Rect dest = new Rect((cw - dw) / 2, (ch - dh) / 2, (cw + dw) / 2, (ch + dh) / 2);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(bitmap, null, dest, null);
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
