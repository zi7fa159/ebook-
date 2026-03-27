package com.example.astrostacker;

import android.media.Image;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FrameProcessor {
    private StackEngine stackEngine;
    private StarDetector starDetector = new StarDetector();
    private FrameAligner frameAligner = new FrameAligner();
    private Renderer renderer;
    private List<StarDetector.Star> referenceStars;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isStacking = false;

    public FrameProcessor(StackEngine stackEngine, Renderer renderer) {
        this.stackEngine = stackEngine;
        this.renderer = renderer;
    }

    public void startStacking() {
        isStacking = true;
        referenceStars = null;
        stackEngine.reset();
    }

    public void stopStacking() {
        isStacking = false;
    }

    public void processFrame(Image image) {
        if (!isStacking) {
            image.close();
            return;
        }

        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] yData = new byte[yBuffer.remaining()];
        yBuffer.get(yData);
        image.close();

        executor.execute(() -> {
            List<StarDetector.Star> currentStars = starDetector.detectStars(yData, width, height, 50);

            int dx = 0, dy = 0;
            if (referenceStars == null) {
                referenceStars = currentStars;
            } else {
                FrameAligner.Translation t = frameAligner.align(referenceStars, currentStars);
                dx = t.dx;
                dy = t.dy;
            }

            stackEngine.addFrame(yData, dx, dy);
            renderer.render(stackEngine);
        });
    }
}
