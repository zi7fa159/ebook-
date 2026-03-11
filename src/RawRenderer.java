package com.example.astrostacker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import java.util.Arrays;

public class RawRenderer {
    private final SurfaceHolder holder;
    private Bitmap bitmap;
    private int[] pixels;
    private float[] sample = new float[10000];

    public RawRenderer(SurfaceHolder holder, int width, int height) {
        this.holder = holder;
        // Output bitmap is half size for performance during debayering
        this.bitmap = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888);
        this.pixels = new int[(width / 2) * (height / 2)];
    }

    public void render(float[] rawStack, int width, int height, boolean autoStretch) {
        int outW = width / 2;
        int outH = height / 2;

        float blackPoint = 0;
        float whitePoint = 1023; // Assuming 10-bit raw for now

        if (autoStretch) {
            int step = Math.max(1, rawStack.length / sample.length);
            for (int i = 0; i < sample.length; i++) {
                sample[i] = rawStack[Math.min(i * step, rawStack.length - 1)];
            }
            Arrays.sort(sample);
            blackPoint = sample[(int)(sample.length * 0.05)];
            whitePoint = sample[(int)(sample.length * 0.95)];
            if (whitePoint <= blackPoint) whitePoint = blackPoint + 1.0f;
        }

        float invRange = 255.0f / (whitePoint - blackPoint);

        // Simple debayering by averaging 2x2 cells to 1 RGB pixel
        // RGGB Pattern assumed
        for (int y = 0; y < outH; y++) {
            for (int x = 0; x < outW; x++) {
                int rIdx = (y * 2) * width + (x * 2);
                int g1Idx = (y * 2) * width + (x * 2 + 1);
                int g2Idx = (y * 2 + 1) * width + (x * 2);
                int bIdx = (y * 2 + 1) * width + (x * 2 + 1);

                float r = (rawStack[rIdx] - blackPoint) * invRange;
                float g = ((rawStack[g1Idx] + rawStack[g2Idx]) / 2.0f - blackPoint) * invRange;
                float b = (rawStack[bIdx] - blackPoint) * invRange;

                int ri = Math.max(0, Math.min(255, (int)r));
                int gi = Math.max(0, Math.min(255, (int)g));
                int bi = Math.max(0, Math.min(255, (int)b));

                pixels[y * outW + x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
        }

        bitmap.setPixels(pixels, 0, outW, 0, 0, outW, outH);

        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            try {
                int canvasW = canvas.getWidth();
                int canvasH = canvas.getHeight();
                float scale = Math.min((float)canvasW / outW, (float)canvasH / outH);
                int drawW = (int)(outW * scale);
                int drawH = (int)(outH * scale);
                int left = (canvasW - drawW) / 2;
                int top = (canvasH - drawH) / 2;

                Rect dest = new Rect(left, top, left + drawW, top + drawH);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(bitmap, null, dest, null);
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
