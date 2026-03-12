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

    public static int processPixel(float rIn, float g1In, float g2In, float bIn, Params p, float effectiveBlack) {
        // 1. Black Level Subtraction
        float r = rIn - effectiveBlack;
        float g = ((g1In + g2In) / 2.0f) - effectiveBlack;
        float b = bIn - effectiveBlack;

        // 2. White Balance
        r *= p.rGain;
        g *= p.gGain;
        b *= p.bGain;

        // 3. Simple Highlight Recovery (Desaturate if any channel clips)
        float clip = (p.whiteLevel - p.blackLevel) * (effectiveBlack / p.blackLevel); // Scaled white level
        if (r > clip || g > clip || b > clip) {
            float max = Math.max(r, Math.max(g, b));
            r = g = b = max;
        }

        // 4. Stretching & Gamma
        // Map to [0, 1] range
        r = (r - p.manualBlackOffset) * p.stretchScale / 255.0f;
        g = (g - p.manualBlackOffset) * p.stretchScale / 255.0f;
        b = (b - p.manualBlackOffset) * p.stretchScale / 255.0f;

        if (p.useAutoStretch) {
            // S-Curve like stretch for auto mode
            r = (float) Math.sqrt(Math.max(0, r));
            g = (float) Math.sqrt(Math.max(0, g));
            b = (float) Math.sqrt(Math.max(0, b));
        } else if (p.gamma != 1.0f) {
            r = (float) Math.pow(Math.max(0, r), p.gamma);
            g = (float) Math.pow(Math.max(0, g), p.gamma);
            b = (float) Math.pow(Math.max(0, b), p.gamma);
        }

        // 5. Final Scaling to 8-bit
        int ri = Math.max(0, Math.min(255, (int)(r * 255)));
        int gi = Math.max(0, Math.min(255, (int)(g * 255)));
        int bi = Math.max(0, Math.min(255, (int)(b * 255)));

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }
}
