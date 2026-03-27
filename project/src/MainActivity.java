package com.example.astrostacker;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private CameraController cameraController;
    private FrameProcessor frameProcessor;
    private StackEngine stackEngine;
    private Renderer renderer;

    private TextView exposureLabel, isoLabel, statusLabel;
    private SeekBar exposureSeekBar, isoSeekBar;
    private Button btnStart, btnStop, btnSave;

    private int currentIso = 800;
    private long currentExposureTimeNs = 1000000000L; // 1s

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
        }

        SurfaceView surfaceView = findViewById(getResources().getIdentifier("surfaceView", "id", getPackageName()));
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initApp(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraController.closeCamera();
            }
        });

        exposureLabel = findViewById(getResources().getIdentifier("exposureLabel", "id", getPackageName()));
        isoLabel = findViewById(getResources().getIdentifier("isoLabel", "id", getPackageName()));
        statusLabel = findViewById(getResources().getIdentifier("statusLabel", "id", getPackageName()));
        exposureSeekBar = findViewById(getResources().getIdentifier("exposureSeekBar", "id", getPackageName()));
        isoSeekBar = findViewById(getResources().getIdentifier("isoSeekBar", "id", getPackageName()));
        btnStart = findViewById(getResources().getIdentifier("btnStart", "id", getPackageName()));
        btnStop = findViewById(getResources().getIdentifier("btnStop", "id", getPackageName()));
        btnSave = findViewById(getResources().getIdentifier("btnSave", "id", getPackageName()));

        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = progress + 1;
                currentExposureTimeNs = seconds * 1000000000L;
                exposureLabel.setText("Exposure: " + seconds + "s");
                cameraController.updateParameters(currentExposureTimeNs, currentIso);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        isoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentIso = progress;
                isoLabel.setText("ISO: " + currentIso);
                cameraController.updateParameters(currentExposureTimeNs, currentIso);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnStart.setOnClickListener(v -> {
            frameProcessor.startStacking();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            startStatusUpdate();
        });

        btnStop.setOnClickListener(v -> {
            frameProcessor.stopStacking();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        btnSave.setOnClickListener(v -> saveStack());
    }

    private void initApp(SurfaceHolder holder) {
        stackEngine = new StackEngine(1280, 720); // Standard resolution
        renderer = new Renderer(holder);
        frameProcessor = new FrameProcessor(stackEngine, renderer);
        cameraController = new CameraController(this, frameProcessor);
        cameraController.startBackgroundThread();
        cameraController.openCamera(1280, 720);
    }

    private void startStatusUpdate() {
        new Thread(() -> {
            while (btnStop.isEnabled()) {
                runOnUiThread(() -> statusLabel.setText("Frames: " + stackEngine.getFrameCount()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
        }).start();
    }

    private void saveStack() {
        float[] buffer = stackEngine.getStackBuffer();
        int width = stackEngine.getWidth();
        int height = stackEngine.getHeight();

        // To save as 16-bit without external libraries like OpenCV, we'll write a simple PAM (Portable Arbitrary Map)
        // or a Netpbm format (PGM) which supports 16-bit and is easily readable by many astrophotography tools.
        // PGM Format: P5\nWIDTH HEIGHT\nMAXVAL\nDATA

        File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(path, "astro_stack_" + System.currentTimeMillis() + ".pgm");

        try (FileOutputStream out = new FileOutputStream(file)) {
            String header = "P5\n" + width + " " + height + "\n65535\n";
            out.write(header.getBytes());

            // Assuming the float buffer contains luminance values that we want to scale to 16-bit.
            // If the buffer already represents real luminance, we might need to find max and scale.
            float maxVal = 1.0f;
            for (float v : buffer) if (v > maxVal) maxVal = v;

            for (float v : buffer) {
                // Scale to 0-65535
                int val16 = (int) ((v / maxVal) * 65535);
                if (val16 > 65535) val16 = 65535;
                if (val16 < 0) val16 = 0;

                // Write Big-Endian 16-bit
                out.write((val16 >> 8) & 0xFF);
                out.write(val16 & 0xFF);
            }
            Toast.makeText(this, "Saved 16-bit PGM to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraController != null) {
            cameraController.stopBackgroundThread();
        }
    }
}
