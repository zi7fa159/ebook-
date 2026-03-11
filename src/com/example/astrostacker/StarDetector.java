package com.example.astrostacker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StarDetector {
    public static class Star implements Comparable<Star> {
        public float x, y, brightness;
        public Star(float x, float y, float brightness) {
            this.x = x; this.y = y; this.brightness = brightness;
        }
        @Override public int compareTo(Star o) { return Float.compare(o.brightness, this.brightness); }
    }

    public List<Star> detectStars(byte[] yData, int width, int height) {
        // Simple adaptive thresholding
        long sum = 0;
        for (int i = 0; i < yData.length; i += 8) sum += (yData[i] & 0xFF);
        float mean = (float) sum / (yData.length / 8);
        float threshold = mean + 40; // Base threshold over mean

        List<Star> stars = new ArrayList<>();
        for (int y = 5; y < height - 5; y += 3) {
            for (int x = 5; x < width - 5; x += 3) {
                int val = yData[y * width + x] & 0xFF;
                if (val > threshold) {
                    // Check if local maximum
                    boolean isMax = true;
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            if ((yData[(y + dy) * width + (x + dx)] & 0xFF) > val) {
                                isMax = false; break;
                            }
                        }
                        if (!isMax) break;
                    }
                    if (isMax) {
                        // Refine centroid
                        float m00 = 0, m10 = 0, m01 = 0;
                        for (int dy = -2; dy <= 2; dy++) {
                            for (int dx = -2; dx <= 2; dx++) {
                                int p = yData[(y + dy) * width + (x + dx)] & 0xFF;
                                m00 += p; m10 += (x + dx) * p; m01 += (y + dy) * p;
                            }
                        }
                        stars.add(new Star(m10 / m00, m01 / m00, val));
                    }
                }
            }
        }
        Collections.sort(stars);
        return stars.size() > 40 ? new ArrayList<>(stars.subList(0, 40)) : stars;
    }
}
