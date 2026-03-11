package com.example.astrostacker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StarDetector {
    public static class Star implements Comparable<Star> {
        public float x, y, brightness;
        public Star(float x, float y, float brightness) {
            this.x = x;
            this.y = y;
            this.brightness = brightness;
        }
        @Override
        public int compareTo(Star o) {
            return Float.compare(o.brightness, this.brightness);
        }
    }

    private float sensitivityK = 4.0f; // Mean + K * StdDev

    public void setSensitivity(float k) {
        this.sensitivityK = k;
    }

    public List<Star> detectStars(byte[] yData, int width, int height, int rowStride) {
        // Step 1: Calculate Mean and StdDev for Adaptive Thresholding
        // We use a subset of pixels to speed up calculation
        long sum = 0;
        long sumSq = 0;
        int count = 0;
        int step = 4; // Sample every 4th pixel

        for (int y = 0; y < height; y += step) {
            int rowOffset = y * rowStride;
            for (int x = 0; x < width; x += step) {
                int val = yData[rowOffset + x] & 0xFF;
                sum += val;
                sumSq += (val * val);
                count++;
            }
        }

        float mean = (float) sum / count;
        float variance = ((float) sumSq / count) - (mean * mean);
        float stdDev = (float) Math.sqrt(Math.max(0, variance));
        float threshold = mean + sensitivityK * stdDev;

        // Step 2: Detect Local Maxima
        List<Star> stars = new ArrayList<>();
        // Avoid edges
        for (int y = 2; y < height - 2; y += 2) {
            int rowOffset = y * rowStride;
            for (int x = 2; x < width - 2; x += 2) {
                int val = yData[rowOffset + x] & 0xFF;
                if (val > threshold) {
                    // Check 3x3 neighborhood for local maximum
                    boolean isMax = true;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int neighbor = yData[(y + dy) * rowStride + (x + dx)] & 0xFF;
                            if (neighbor > val) {
                                isMax = false;
                                break;
                            }
                        }
                        if (!isMax) break;
                    }

                    if (isMax) {
                        // Step 3: Sub-pixel Centroid Refinement (3x3)
                        float m00 = 0, m10 = 0, m01 = 0;
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                int pixel = yData[(y + dy) * rowStride + (x + dx)] & 0xFF;
                                m00 += pixel;
                                m10 += (x + dx) * pixel;
                                m01 += (y + dy) * pixel;
                            }
                        }
                        if (m00 > 0) {
                            stars.add(new Star(m10 / m00, m01 / m00, val));
                        }
                    }
                }
            }
        }

        // Step 4: Limit to top 50 brightest stars
        Collections.sort(stars);
        if (stars.size() > 50) {
            return new ArrayList<>(stars.subList(0, 50));
        }
        return stars;
    }
}
