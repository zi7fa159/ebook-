package com.alsclone.astrostacker;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends Activity {
    private CameraController cameraController;
    private FrameProcessor frameProcessor;
    private Renderer renderer;

    private TextView statusText;
    private TextView logText;
    private android.widget.ProgressBar captureProgress;
    private android.widget.ScrollView logScroll;

    private SeekBar shutterSeekBar, isoSeekBar, focusSeekBar, wbRedSeekBar, wbBlueSeekBar;

    private long currentShutterNs = 1000000000L;
    private int currentIso = 800;
    private float currentFocus = 0.0f;
    private float currentWbRed = 2.0f;
    private float currentWbBlue = 2.0f;
    private boolean isDestroyed = false;

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            "android.permission.MANAGE_EXTERNAL_STORAGE"
        }, 1);

        TextureView preview = findViewById(R.id.preview);
        renderer = new Renderer(preview);

        statusText = findViewById(R.id.status_text);
        logText = findViewById(R.id.log_text);
        captureProgress = findViewById(R.id.capture_progress);
        logScroll = findViewById(R.id.log_scroll);

        shutterSeekBar = findViewById(R.id.shutter_seekbar);
        isoSeekBar = findViewById(R.id.iso_seekbar);
        focusSeekBar = findViewById(R.id.focus_seekbar);
        wbRedSeekBar = findViewById(R.id.wb_red_seekbar);
        wbBlueSeekBar = findViewById(R.id.wb_blue_seekbar);

        shutterSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                currentShutterNs = (p + 1) * 1000000000L;
                addLog("Shutter: " + (p + 1) + "s");
                updateCamera();
            }
        });

        isoSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                currentIso = 100 + (p * 100);
                addLog("ISO: " + currentIso);
                updateCamera();
            }
        });

        focusSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                currentFocus = p / 10.0f; // 0.0 to 10.0
                addLog("Focus: " + currentFocus);
                updateCamera();
            }
        });

        wbRedSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                currentWbRed = p / 100.0f;
                renderer.setWbGains(currentWbRed, currentWbBlue);
            }
        });

        wbBlueSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                currentWbBlue = p / 100.0f;
                renderer.setWbGains(currentWbRed, currentWbBlue);
            }
        });

        findViewById(R.id.btn_start).setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.startStacking();
                startStackingUI();
                addLog("Stacking Started");
            }
        });

        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.stopStacking();
                addLog("Stacking Stopped");
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> saveResult());

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.startStacking(); // Re-init
                addLog("Stack Cleared");
            }
        });

        initCamera();
    }

    private void updateCamera() {
        if (cameraController != null) {
            cameraController.updateParams(currentShutterNs, currentIso, currentFocus);
        }
    }

    private void addLog(String msg) {
        runOnUiThread(() -> {
            logText.append("\n" + msg);
            logScroll.post(() -> logScroll.fullScroll(android.view.View.FOCUS_DOWN));
        });
    }

    private abstract class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public abstract void onProgressChanged(SeekBar s, int p, boolean f);
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}
    }

    private void initCamera() {
        // We need to wait for cameraController to get the raw size to init frameProcessor
        // For simplicity, we initialize it with a placeholder and let it adjust or re-init
        // On Realme 8i, 50MP is roughly 8192x6144 or similar.
        // In a real app, we'd wait for CameraCharacteristics.

        new Thread(() -> {
            cameraController = new CameraController(this, null);
            cameraController.start();
            while (cameraController.getRawSize() == null) {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            }

            final android.util.Size size = cameraController.getRawSize();
            runOnUiThread(() -> {
                frameProcessor = new FrameProcessor(size.getWidth(), size.getHeight(), renderer);
                cameraController.setFrameProcessor(frameProcessor);
            });
        }).start();
    }

    private void startStackingUI() {
        new Thread(() -> {
            int lastCount = -1;
            while (!isDestroyed) {
                if (frameProcessor != null) {
                    int count = frameProcessor.getFrameCount();
                    if (count != lastCount) {
                        lastCount = count;
                        final int c = count;
                        runOnUiThread(() -> statusText.setText("Stacked: " + c + " frames"));
                        addLog("Frame " + c + " added to stack");
                    }
                }

                // Simulated progress bar for capture
                for (int i = 0; i <= 100; i += 5) {
                    final int p = i;
                    runOnUiThread(() -> captureProgress.setProgress(p));
                    try { Thread.sleep(currentShutterNs / 20000000); } catch (Exception e) {}
                }
            }
        }).start();
    }

    private void saveResult() {
        if (frameProcessor == null || cameraController == null) return;
        final float[] buffer = frameProcessor.getResultBuffer();
        final int w = cameraController.getRawSize().getWidth();
        final int h = cameraController.getRawSize().getHeight();

        Toast.makeText(this, "Saving... please wait", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            float maxValFound = 0;
            for (int i = 0; i < buffer.length; i += 100) if (buffer[i] > maxValFound) maxValFound = buffer[i];
            if (maxValFound == 0) maxValFound = 1;
            final float maxVal = maxValFound;

            File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File path = new File(pictures, "ALS_Astro");
            if (!path.exists()) path.mkdirs();

            String ts = String.valueOf(System.currentTimeMillis());
            File pngFile = new File(path, "ALS_Stack_" + ts + ".png");
            File tiffFile = new File(path, "ALS_Stack_" + ts + ".tiff");
            File dngFile = new File(path, "ALS_Frame_" + ts + ".dng");

            try {
                // Save 16-bit TIFF (Color) - already optimized to process row by row
                TiffWriter.saveTiff16Color(tiffFile.getAbsolutePath(), buffer, w, h, currentWbRed, currentWbBlue);

                // Save 8-bit PNG - do it row by row to save memory
                savePngOptimized(pngFile, buffer, w, h, maxVal);

                // Save latest RAW (Simplified to TIFF for stability in this version)
                Image rawImg = frameProcessor.getLatestRawImage();
                if (rawImg != null) {
                    rawImg.close(); // Just close it for now as TIFF is the main lossless format
                }

                runOnUiThread(() -> Toast.makeText(this, "Saved to Pictures/ALS_Astro", Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void savePngOptimized(File file, float[] buffer, int w, int h, float maxVal) throws IOException {
        // Since we can't easily write PNG row-by-row with Bitmap.compress,
        // and a full 50MP Bitmap + pixels array is too much memory (~400MB total),
        // we'll use a smaller version for the PNG preview if memory is an issue,
        // or just try to be very careful.
        // Better: implement a simple 8-bit PPM or similar if PNG is too memory-heavy,
        // but let's try to use a smaller bitmap for the PNG preview to ensure stability.

        int sampleSize = 2; // Downscale PNG preview by 2x to save 4x memory
        int sw = w / sampleSize;
        int sh = h / sampleSize;
        Bitmap bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        int[] rowPixels = new int[sw];

        for (int y = 0; y < sh; y++) {
            for (int x = 0; x < sw; x++) {
                int origX = x * sampleSize;
                int origY = y * sampleSize;

                int bx = (origX / 2) * 2;
                int by = (origY / 2) * 2;
                float r = buffer[by * w + bx] * currentWbRed;
                float g = (buffer[by * w + (bx + 1)] + buffer[(by + 1) * w + bx]) / 2.0f;
                float b = buffer[(by + 1) * w + (bx + 1)] * currentWbBlue;

                int ri = Math.min(255, (int) ((r / maxVal) * 255));
                int gi = Math.min(255, (int) ((g / maxVal) * 255));
                int bi = Math.min(255, (int) ((b / maxVal) * 255));
                rowPixels[x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
            bitmap.setPixels(rowPixels, 0, sw, 0, y, sw, 1);
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        }
        bitmap.recycle();
    }
}
