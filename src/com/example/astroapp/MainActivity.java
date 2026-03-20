package com.example.astroapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.text.InputType;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private CameraController cameraController;
    private CaptureLoop captureLoop;
    private StorageManager storageManager;
    private PreviewProcessor previewProcessor;
    private PrecisionWheelView shutterWheel, isoWheel;
    private TextView statusText, exposureInfo, sessionInfo, frameCounter;
    private Button captureButton;
    private boolean isCapturing = false;

    private float currentShutter = 1.0f;
    private int currentISO = 800;
    private int frameCount = 100;
    private float frameDelay = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextureView textureView = findViewById(R.id.camera_preview);
        ImageView previewImage = findViewById(R.id.last_capture_preview);
        statusText = findViewById(R.id.status_text);
        exposureInfo = findViewById(R.id.exposure_info);
        sessionInfo = findViewById(R.id.session_settings_info);
        frameCounter = findViewById(R.id.frame_counter);
        captureButton = findViewById(R.id.capture_button);
        shutterWheel = findViewById(R.id.shutter_wheel);
        isoWheel = findViewById(R.id.iso_wheel);

        cameraController = new CameraController(this, textureView);
        storageManager = new StorageManager(this);
        previewProcessor = new PreviewProcessor(previewImage);
        captureLoop = new CaptureLoop(cameraController, storageManager, previewProcessor);

        shutterWheel.setRange(0.001f, 30.0f, 1.0f, false);
        isoWheel.setRange(100f, 3200f, 1.0f, true);
        shutterWheel.setValue(currentShutter);
        isoWheel.setValue(currentISO);

        shutterWheel.setOnValueChangeListener(val -> {
            currentShutter = val;
            updateExposureInfo();
            if (!isCapturing) cameraController.startPreview((long)(currentShutter * 1e9), currentISO);
        });

        isoWheel.setOnValueChangeListener(val -> {
            currentISO = (int) val;
            updateExposureInfo();
            if (!isCapturing) cameraController.startPreview((long)(currentShutter * 1e9), currentISO);
        });

        exposureInfo.setOnClickListener(v -> showNumericOverrideDialog());
        sessionInfo.setOnClickListener(v -> showSessionSettingsDialog());
        captureButton.setOnClickListener(v -> toggleCapture());

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                } else {
                    cameraController.openCamera();
                }
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });

        cameraController.startBackgroundThread();
        updateSessionInfo();
    }

    private void updateExposureInfo() {
        exposureInfo.setText(String.format("Shutter: %.4fs | ISO: %d", currentShutter, currentISO));
    }

    private void updateSessionInfo() {
        sessionInfo.setText(String.format("Frames: %d | Delay: %.1fs", frameCount, frameDelay));
    }

    private void showNumericOverrideDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manual Override");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 10, 30, 10);

        final EditText shutterInput = new EditText(this);
        shutterInput.setHint("Shutter (s)");
        shutterInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        shutterInput.setText(String.valueOf(currentShutter));
        layout.addView(shutterInput);

        final EditText isoInput = new EditText(this);
        isoInput.setHint("ISO");
        isoInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        isoInput.setText(String.valueOf(currentISO));
        layout.addView(isoInput);

        builder.setView(layout);
        builder.setPositiveButton("Set", (dialog, which) -> {
            try {
                currentShutter = Float.parseFloat(shutterInput.getText().toString());
                currentISO = Integer.parseInt(isoInput.getText().toString());
                shutterWheel.setValue(currentShutter);
                isoWheel.setValue(currentISO);
                updateExposureInfo();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSessionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Session Settings");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 10, 30, 10);

        final EditText framesInput = new EditText(this);
        framesInput.setHint("Frame Count");
        framesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        framesInput.setText(String.valueOf(frameCount));
        layout.addView(framesInput);

        final EditText delayInput = new EditText(this);
        delayInput.setHint("Delay (s)");
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        delayInput.setText(String.valueOf(frameDelay));
        layout.addView(delayInput);

        builder.setView(layout);
        builder.setPositiveButton("Set", (dialog, which) -> {
            try {
                frameCount = Integer.parseInt(framesInput.getText().toString());
                frameDelay = Float.parseFloat(delayInput.getText().toString());
                updateSessionInfo();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void toggleCapture() {
        if (isCapturing) {
            captureLoop.stop();
            isCapturing = false;
            captureButton.setText("START CAPTURE");
            frameCounter.setVisibility(View.GONE);
        } else {
            isCapturing = true;
            captureButton.setText("STOP CAPTURE");
            frameCounter.setVisibility(View.VISIBLE);

            captureLoop.setupImageReaderListeners();
            captureLoop.start(frameCount, (long)(frameDelay * 1000), (long)(currentShutter * 1e9), currentISO, new CaptureLoop.Listener() {
                @Override
                public void onProgress(int current, int total) {
                    runOnUiThread(() -> frameCounter.setText(String.format("Frames: %d/%d", current, total)));
                }

                @Override
                public void onFinish() {
                    runOnUiThread(() -> {
                        isCapturing = false;
                        captureButton.setText("START CAPTURE");
                        frameCounter.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Session finished", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraController.stopBackgroundThread();
    }
}
