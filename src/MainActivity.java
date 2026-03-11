package com.alsclone.astrostacker;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
    private TextView exposureText;
    private TextView isoText;
    private SeekBar exposureSeekBar;
    private SeekBar isoSeekBar;

    private long currentExposureNs = 1000000000L;
    private int currentIso = 800;
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

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }

        TextureView preview = findViewById(R.id.preview);
        renderer = new Renderer(preview);

        statusText = findViewById(R.id.status_text);
        exposureText = findViewById(R.id.exposure_text);
        isoText = findViewById(R.id.iso_text);

        exposureSeekBar = findViewById(R.id.exposure_seekbar);
        isoSeekBar = findViewById(R.id.iso_seekbar);

        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 1;
                currentExposureNs = seconds * 1000000000L;
                exposureText.setText(String.format(Locale.US, "Exp: %ds", seconds));
                if (cameraController != null) cameraController.updateParams(currentExposureNs, currentIso);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        isoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentIso = 100 + (progress * 100);
                isoText.setText(String.format(Locale.US, "ISO: %d", currentIso));
                if (cameraController != null) cameraController.updateParams(currentExposureNs, currentIso);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.startStacking();
                startStatusUpdate();
            }
        });

        Button btnStop = findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.stopStacking();
            }
        });

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveResult());

        initCamera();
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

    private void startStatusUpdate() {
        new Thread(() -> {
            while (!isDestroyed) {
                runOnUiThread(() -> {
                    if (frameProcessor != null) {
                        statusText.setText("Frames: " + frameProcessor.getFrameCount());
                    }
                });
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            }
        }).start();
    }

    private void saveResult() {
        if (frameProcessor == null) return;
        float[] buffer = frameProcessor.getResultBuffer();
        int w = cameraController.getRawSize().getWidth();
        int h = cameraController.getRawSize().getHeight();

        // Save as 16-bit PNG (using Bitmap for simplicity in this 1:1 clone,
        // though Android Bitmaps are 8-bit per channel usually.
        // For true 16-bit, we'd need a custom PNG encoder or TIFF.
        // Requirements say 16-bit PNG/TIFF.

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[w * h];
        float maxVal = 0;
        for (float f : buffer) if (f > maxVal) maxVal = f;
        if (maxVal == 0) maxVal = 1;

        for (int i = 0; i < buffer.length; i++) {
            int v = (int) ((buffer[i] / maxVal) * 255);
            pixels[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File pngFile = new File(path, "ALS_Stack_" + System.currentTimeMillis() + ".png");
        File tiffFile = new File(path, "ALS_Stack_" + System.currentTimeMillis() + ".tiff");

        try {
            // Save 8-bit preview PNG
            try (FileOutputStream out = new FileOutputStream(pngFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            // Save 16-bit TIFF
            TiffWriter.saveTiff16(tiffFile.getAbsolutePath(), buffer, w, h);

            Toast.makeText(this, "Saved to Pictures folder (PNG & 16-bit TIFF)", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
