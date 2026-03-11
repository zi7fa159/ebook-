package com.example.astrostacker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StarDetector {
    public static class Star {
        public int x, y;
        public int brightness;

        public Star(int x, int y, int brightness) {
            this.x = x;
            this.y = y;
            this.brightness = brightness;
        }
    }

    public List<Star> detectStars(byte[] grayData, int width, int height, int maxStars) {
        List<Star> stars = new ArrayList<>();

        // Calculate threshold (e.g., mean + 3*stddev, or just percentile)
        // For simplicity and speed, let's use a robust percentile-based approach
        long sum = 0;
        for (int i = 0; i < grayData.length; i += 10) sum += (grayData[i] & 0xFF);
        int avg = (int) (sum / (grayData.length / 10));
        int threshold = Math.max(avg + 40, 150);

        int neighborhood = 5;

        for (int y = neighborhood; y < height - neighborhood; y += 2) {
            for (int x = neighborhood; x < width - neighborhood; x += 2) {
                int val = grayData[y * width + x] & 0xFF;
                if (val > threshold) {
                    // Simple local maxima check
                    boolean isMax = true;
                    for (int dy = -neighborhood; dy <= neighborhood; dy++) {
                        for (int dx = -neighborhood; dx <= neighborhood; dx++) {
                            if ((grayData[(y + dy) * width + (x + dx)] & 0xFF) > val) {
                                isMax = false;
                                break;
                            }
                        }
                        if (!isMax) break;
                    }

                    if (isMax) {
                        stars.add(new Star(x, y, val));
                    }
                }
            }
        }

        Collections.sort(stars, new Comparator<Star>() {
            @Override
            public int compare(Star s1, Star s2) {
                return Integer.compare(s2.brightness, s1.brightness);
            }
        });

        if (stars.size() > maxStars) {
            return stars.subList(0, maxStars);
        }
        return stars;
    }
}
