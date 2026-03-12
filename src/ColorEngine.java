package com.alsclone.astrostacker;

import android.graphics.Color;

/**
 * Standardized RAW Color Pipeline for A2LS.
 * Handles Black Level subtraction, WB gains, Gamma, and Clipping.
 */
public class ColorEngine {

    public static class Params {
        public float temperature = 5000f;
        public float tint = 0f;
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

    /**
     * Converts Color Temperature (Kelvin) and Tint to RGB gains.
     * Based on approximate inverse of Planckian locus.
     */
    public static float[] getGainsFromTempTint(float temp, float tint) {
        float r, g, b;
        float t = temp / 100.0f;

        // Calculate color of light source
        if (t <= 66) {
            r = 255;
            g = (float) (99.4708025861 * Math.log(t) - 161.1195681661);
            b = (t <= 19) ? 0 : (float) (138.5177312231 * Math.log(t - 10) - 305.0447927307);
        } else {
            r = (float) (329.698727446 * Math.pow(t - 60, -0.1332047592));
            g = (float) (288.1221695283 * Math.pow(t - 60, -0.0755148492));
            b = 255;
        }

        // Normalize light color
        float max = Math.max(r, Math.max(g, b));
        r /= max; g /= max; b /= max;

        // Inverse for gains
        float gr = 1.0f / Math.max(0.01f, r);
        float gg = 1.0f / Math.max(0.01f, g);
        float gb = 1.0f / Math.max(0.01f, b);

        // Apply Tint (Green-Magenta axis)
        // Positive tint = more magenta = less green = lower green gain
        gg *= (float) Math.pow(1.001, -tint);

        // Normalize so G is 1.0 (typical for RAW)
        gr /= gg;
        gb /= gg;
        gg = 1.0f;

        return new float[]{gr, gg, gb};
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

        // 3. Highlight Handling
        // Calculate dynamic white level to avoid ISO clipping
        float dynamicWhite = (p.whiteLevel - p.blackLevel);
        if (r > dynamicWhite || g > dynamicWhite || b > dynamicWhite) {
            float max = Math.max(r, Math.max(g, b));
            // Desaturate highlights to avoid purple/funky tints in stars
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
