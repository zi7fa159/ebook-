package com.example.astrostacker;

import java.util.List;

public class FrameAligner {
    public static class Translation {
        public float dx, dy;
    }

    public Translation align(List<StarDetector.Star> ref, List<StarDetector.Star> current, float scaleX, float scaleY) {
        if (ref.isEmpty() || current.isEmpty()) return null;

        float bestDx = 0, bestDy = 0;
        int maxVotes = 0;

        // Simplified voting-based translation alignment
        int limit = Math.min(10, Math.min(ref.size(), current.size()));
        for (int i = 0; i < limit; i++) {
            for (int j = 0; j < limit; j++) {
                float dx = (current.get(i).x * scaleX) - (ref.get(j).x * scaleX);
                float dy = (current.get(i).y * scaleY) - (ref.get(j).y * scaleY);

                int votes = 0;
                for (StarDetector.Star rs : ref) {
                    float tx = rs.x * scaleX + dx;
                    float ty = rs.y * scaleY + dy;
                    for (StarDetector.Star cs : current) {
                        float cx = cs.x * scaleX;
                        float cy = cs.y * scaleY;
                        float distSq = (cx - tx) * (cx - tx) + (cy - ty) * (cy - ty);
                        if (distSq < 16) { votes++; break; }
                    }
                }

                if (votes > maxVotes) {
                    maxVotes = votes;
                    bestDx = dx;
                    bestDy = dy;
                }
            }
        }

        if (maxVotes < 4) return null;
        Translation t = new Translation();
        t.dx = bestDx; t.dy = bestDy;
        return t;
    }
}
