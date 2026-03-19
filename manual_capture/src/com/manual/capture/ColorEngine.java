package com.manual.capture;

public class ColorEngine {
    public static class Params {
        public float temperature = 5000;
        public float tint = 0;
        public float rGain = 1.6f;
        public float gGain = 1.0f;
        public float bGain = 2.1f;
        public int blackLevel = 64;
        public float whiteLevel = 1023.0f;
        public float stretchScale = 1.0f;
        public float gamma = 1.0f;
        public float manualBlackOffset = 0;
        public boolean useAutoStretch = true;
    }

    private float temp = 5000;

    public void setTemperature(float t) {
        this.temp = t;
    }

    public static int processPixel(float r, float g1, float g2, float b, Params p, float effectiveBlack) {
        float ravg = (r - effectiveBlack) * p.rGain;
        float gavg = ((g1 + g2) / 2f - effectiveBlack) * p.gGain;
        float bavg = (b - effectiveBlack) * p.bGain;

        ravg = Math.max(0, ravg - p.manualBlackOffset) * p.stretchScale;
        gavg = Math.max(0, gavg - p.manualBlackOffset) * p.stretchScale;
        bavg = Math.max(0, bavg - p.manualBlackOffset) * p.stretchScale;

        if (p.gamma != 1.0f) {
            ravg = (float) (255.0 * Math.pow(ravg / 255.0, p.gamma));
            gavg = (float) (255.0 * Math.pow(gavg / 255.0, p.gamma));
            bavg = (float) (255.0 * Math.pow(bavg / 255.0, p.gamma));
        }

        int ri = Math.min(255, (int) ravg);
        int gi = Math.min(255, (int) gavg);
        int bi = Math.min(255, (int) bavg);

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    public static float[] getGainsFromTempTint(float temp, float tint) {
        float r, g, b;
        if (temp <= 6600) {
            r = 255;
            g = (float) (99.4708025861 * Math.log(temp / 100.0) - 161.1195681661);
            if (temp <= 1900) b = 0;
            else b = (float) (138.5177312231 * Math.log(temp / 100.0 - 10) - 305.0447927307);
        } else {
            r = (float) (329.698727446 * Math.pow(temp / 100.0 - 60, -0.1332047592));
            g = (float) (288.1221695283 * Math.pow(temp / 100.0 - 60, -0.0755148492));
            b = 255;
        }
        float max = Math.max(r, Math.max(g, b));
        return new float[]{max / r, max / g, max / b};
    }
}
