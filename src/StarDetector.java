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

    private float sensitivityK = 4.0f;
    private byte[] proxyBuffer;

    public void setSensitivity(float k) {
        this.sensitivityK = k;
    }

    public List<Star> detectStarsFromY(byte[] yData, int width, int height, int rowStride) {
        return detectStarsInternal(yData, width, height, rowStride);
    }

    public List<Star> detectStarsFromRaw(short[] rawData, int width, int height) {
        int proxyW = width / 2;
        int proxyH = height / 2;
        if (proxyBuffer == null || proxyBuffer.length != proxyW * proxyH) {
            proxyBuffer = new byte[proxyW * proxyH];
        }

        for (int y = 0; y < proxyH; y++) {
            for (int x = 0; x < proxyW; x++) {
                int sum = (rawData[(y*2)*width + (x*2)] & 0xFFFF) +
                          (rawData[(y*2)*width + (x*2+1)] & 0xFFFF) +
                          (rawData[(y*2+1)*width + (x*2)] & 0xFFFF) +
                          (rawData[(y*2+1)*width + (x*2+1)] & 0xFFFF);
                proxyBuffer[y * proxyW + x] = (byte)((sum / 4) >> 2);
            }
        }

        List<Star> stars = detectStarsInternal(proxyBuffer, proxyW, proxyH, proxyW);
        for (Star s : stars) {
            s.x *= 2;
            s.y *= 2;
        }
        return stars;
    }

    private List<Star> detectStarsInternal(byte[] data, int width, int height, int stride) {
        long sum = 0;
        long sumSq = 0;
        int count = 0;
        int step = 4;

        for (int y = 0; y < height; y += step) {
            int rowOffset = y * stride;
            for (int x = 0; x < width; x += step) {
                int val = data[rowOffset + x] & 0xFF;
                sum += val;
                sumSq += (val * val);
                count++;
            }
        }

        float mean = (float) sum / count;
        float variance = ((float) sumSq / count) - (mean * mean);
        float stdDev = (float) Math.sqrt(Math.max(0, variance));
        float threshold = mean + sensitivityK * stdDev;

        List<Star> stars = new ArrayList<>();
        for (int y = 2; y < height - 2; y += 2) {
            int rowOffset = y * stride;
            for (int x = 2; x < width - 2; x += 2) {
                int val = data[rowOffset + x] & 0xFF;
                if (val > threshold) {
                    boolean isMax = true;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int neighbor = data[(y + dy) * stride + (x + dx)] & 0xFF;
                            if (neighbor > val) {
                                isMax = false;
                                break;
                            }
                        }
                        if (!isMax) break;
                    }

                    if (isMax) {
                        float m00 = 0, m10 = 0, m01 = 0;
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                int pixel = data[(y + dy) * stride + (x + dx)] & 0xFF;
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

        Collections.sort(stars);
        if (stars.size() > 50) {
            return new ArrayList<>(stars.subList(0, 50));
        }
        return stars;
    }
}
