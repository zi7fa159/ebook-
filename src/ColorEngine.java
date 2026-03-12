package com.alsclone.astrostacker;

import android.graphics.Color;

/**
 * Standardized RAW Color Pipeline for A2LS.
 * Handles Black Level subtraction, WB gains, Gamma, and Clipping.
 */
public class ColorEngine {

    public static class Params {
        public float rGain = 1.6f;
        public float gGain = 1.0f;
        public float bGain = 2.1f;
        public int blackLevel = 64;
        public float whiteLevel = 1023.0f;
        public float stretchScale = 1.0f;
        public float gamma = 1.0f; // 1.0 = Linear
        public float manualBlackOffset = 0.0f;
        public boolean useAutoStretch = true;
    }

    public static int processPixel(float raw, float g1, float g2, float b, Params p, float effectiveBlack, float whiteClip) {
        // 1. Black Level Subtraction
        float r = raw - effectiveBlack;
        float g = ((g1 + g2) / 2.0f) - effectiveBlack;
        float bl = b - effectiveBlack;

        // 2. White Balance
        r *= p.rGain;
        g *= p.gGain;
        bl *= p.bGain;

        // 3. Purple Highlight Fix (Clamping near saturation)
        if (r > whiteClip || g > whiteClip || bl > whiteClip) {
            float max = Math.max(r, Math.max(g, bl));
            if (max > whiteClip) { r = g = bl = max; }
        }

        // 4. Stretching & Gamma
        // Map to [0, 1] range based on current scale
        r = (r - p.manualBlackOffset) * p.stretchScale / 255.0f;
        g = (g - p.manualBlackOffset) * p.stretchScale / 255.0f;
        bl = (bl - p.manualBlackOffset) * p.stretchScale / 255.0f;

        if (p.gamma != 1.0f) {
            r = (float) Math.pow(Math.max(0, r), p.gamma);
            g = (float) Math.pow(Math.max(0, g), p.gamma);
            bl = (float) Math.pow(Math.max(0, bl), p.gamma);
        }

        // 5. Final Scaling to 8-bit
        int ri = Math.max(0, Math.min(255, (int)(r * 255)));
        int gi = Math.max(0, Math.min(255, (int)(g * 255)));
        int bi = Math.max(0, Math.min(255, (int)(bl * 255)));

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }
}
