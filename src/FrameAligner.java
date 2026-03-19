package com.alsclone.astrostacker;

import android.graphics.Point;
import java.util.ArrayList;
import java.util.List;

public class FrameAligner {
    private List<float[]> referenceStars;
    private int width, height;
    private int[] hist;
    private static final int LIMIT = 200;
    private static final int HIST_SIZE = LIMIT * 2 + 1;

    public static class Alignment {
        public float dx, dy, angle;
        public Alignment(float dx, float dy, float angle) {
            this.dx = dx; this.dy = dy; this.angle = angle;
        }
    }

    public void setReferenceStars(List<float[]> stars, int w, int h) {
        this.referenceStars = stars;
        this.width = w;
        this.height = h;
    }

    public Alignment computeAlignment(List<float[]> currentStars) {
        if (referenceStars == null || referenceStars.isEmpty() || currentStars == null || currentStars.isEmpty()) {
            return new Alignment(0, 0, 0);
        }

        // 1. Initial translation guess using histogram (translation only)
        if (hist == null) {
            hist = new int[HIST_SIZE * HIST_SIZE];
        } else {
            java.util.Arrays.fill(hist, 0);
        }

        for (float[] r : referenceStars) {
            for (float[] c : currentStars) {
                int dx = Math.round(r[0] - c[0]);
                int dy = Math.round(r[1] - c[1]);
                if (Math.abs(dx) <= LIMIT && Math.abs(dy) <= LIMIT) {
                    hist[(dy + LIMIT) * HIST_SIZE + (dx + LIMIT)]++;
                }
            }
        }

        int maxVotes = 0;
        int bestDx = 0;
        int bestDy = 0;
        for (int y = 1; y < HIST_SIZE - 1; y++) {
            for (int x = 1; x < HIST_SIZE - 1; x++) {
                int votes = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        votes += hist[(y + dy) * HIST_SIZE + (x + dx)];
                    }
                }
                if (votes > maxVotes) {
                    maxVotes = votes;
                    bestDx = x - LIMIT;
                    bestDy = y - LIMIT;
                }
            }
        }

        if (maxVotes < 1) {
            return new Alignment(0, 0, 0);
        }

        // Sub-pixel centroid estimation on the histogram peak
        float sumX = 0, sumY = 0, sumV = 0;
        int bx = bestDx + LIMIT;
        int by = bestDy + LIMIT;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int v = hist[(by + dy) * HIST_SIZE + (bx + dx)];
                sumX += (bestDx + dx) * v;
                sumY += (bestDy + dy) * v;
                sumV += v;
            }
        }

        float subDx = bestDx;
        float subDy = bestDy;
        if (sumV > 0) {
            subDx = sumX / sumV;
            subDy = sumY / sumV;
        }

        // 2. Rotation estimation
        // For rotation, we need at least 2 matching pairs.
        // We'll find pairs that match the translation guess.
        List<float[][]> pairs = new ArrayList<>();
        float tol = 5.0f;
        for (float[] r : referenceStars) {
            for (float[] c : currentStars) {
                float dx = r[0] - c[0];
                float dy = r[1] - c[1];
                if (Math.abs(dx - subDx) < tol && Math.abs(dy - subDy) < tol) {
                    pairs.add(new float[][]{r, c});
                }
            }
        }

        float bestAngle = 0;
        if (pairs.size() >= 2) {
            float totalWeight = 0;
            float sumAngle = 0;
            float cx = width / 2.0f;
            float cy = height / 2.0f;

            for (int i = 0; i < pairs.size(); i++) {
                for (int j = i + 1; j < pairs.size(); j++) {
                    float[] r1 = pairs.get(i)[0];
                    float[] c1 = pairs.get(i)[1];
                    float[] r2 = pairs.get(j)[0];
                    float[] c2 = pairs.get(j)[1];

                    double angleR = Math.atan2(r2[1] - r1[1], r2[0] - r1[0]);
                    double angleC = Math.atan2(c2[1] - c1[1], c2[0] - c1[0]);
                    float dAngle = (float) Math.toDegrees(angleR - angleC);

                    if (dAngle > 180) dAngle -= 360;
                    if (dAngle < -180) dAngle += 360;

                    if (Math.abs(dAngle) < 5.0f) { // Reject extreme rotations
                        float dist = (float) Math.sqrt(Math.pow(r2[0]-r1[0],2) + Math.pow(r2[1]-r1[1],2));
                        sumAngle += dAngle * dist;
                        totalWeight += dist;
                    }
                }
            }
            if (totalWeight > 0) {
                bestAngle = sumAngle / totalWeight;
            }
        }

        return new Alignment(subDx, subDy, bestAngle);
    }

    public Point computeShift(List<Point> currentStars) {
        List<float[]> centroids = new ArrayList<>();
        for (Point p : currentStars) centroids.add(new float[]{(float)p.x, (float)p.y, 1.0f});
        Alignment a = computeAlignment(centroids);
        return new Point(Math.round(a.dx), Math.round(a.dy));
    }
}
