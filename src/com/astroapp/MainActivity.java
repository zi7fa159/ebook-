package com.astroapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import java.util.Collections;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private CameraController cameraController;
    private StorageManager storageManager;
    private CaptureLoop captureLoop;
    private ImageReader rawReader;
    private ImageReader jpegReader;

    private TextureView textureView;
    private ImageView previewOverlay;
    private PrecisionWheelView shutterWheel;
    private PrecisionWheelView isoWheel;
    private TextView shutterValueText;
    private TextView isoValueText;
    private TextView statusText;
    private Button captureButton;

    private Handler mainHandler;
    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private int currentIso = 800;
    private double currentShutterS = 1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.astroapp.R.layout.activity_main);

        mainHandler = new Handler(getMainLooper());
        cameraThread = new HandlerThread("CameraBackground");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        initUI();
        checkPermissions();
    }

    private void initUI() {
        textureView = findViewById(com.astroapp.R.id.textureView);
        previewOverlay = findViewById(com.astroapp.R.id.previewOverlay);
        shutterWheel = findViewById(com.astroapp.R.id.shutterWheel);
        isoWheel = findViewById(com.astroapp.R.id.isoWheel);
        shutterValueText = findViewById(com.astroapp.R.id.shutterValueText);
        isoValueText = findViewById(com.astroapp.R.id.isoValueText);
        statusText = findViewById(com.astroapp.R.id.statusText);
        captureButton = findViewById(com.astroapp.R.id.captureButton);

        shutterWheel.setRange(0.001, 30.0, 0.333);
        shutterWheel.setValue(currentShutterS);
        shutterWheel.setLabel("SS");
        shutterWheel.setOnValueChangeListener(v -> {
            currentShutterS = v;
            shutterValueText.setText(String.format("Shutter: %.3fs", v));
            if (cameraController != null) cameraController.updateSettings(currentIso, (long)(currentShutterS * 1_000_000_000L));
        });

        isoWheel.setRange(100, 6400, 500);
        isoWheel.setValue(currentIso);
        isoWheel.setLabel("ISO");
        isoWheel.setOnValueChangeListener(v -> {
            currentIso = (int) v;
            isoValueText.setText(String.format("ISO: %d", currentIso));
            if (cameraController != null) cameraController.updateSettings(currentIso, (long)(currentShutterS * 1_000_000_000L));
        });

        captureButton.setOnClickListener(v -> startCapture());

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setupCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    private void setupCamera() {
        cameraController = new CameraController(this);
        storageManager = new StorageManager();

        try {
            cameraController.openCamera(new CameraController.CameraStateCallback() {
                @Override
                public void onOpened() {
                    startPreview();
                }

                @Override
                public void onDisconnected() {
                    finish();
                }

                @Override
                public void onError(int error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Camera error: " + error, Toast.LENGTH_SHORT).show());
                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot open camera", e);
        }
    }

    private void startPreview() {
        CameraCharacteristics characteristics = cameraController.getCharacteristics();
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size rawSize = map.getOutputSizes(android.graphics.ImageFormat.RAW_SENSOR)[0];
        Size jpegSize = map.getOutputSizes(android.graphics.ImageFormat.JPEG)[0];

        rawReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(), android.graphics.ImageFormat.RAW_SENSOR, 5);
        jpegReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(), android.graphics.ImageFormat.JPEG, 5);

        captureLoop = new CaptureLoop(cameraController, storageManager, rawReader, jpegReader);

        try {
            Surface surface = new Surface(textureView.getSurfaceTexture());
            cameraController.startPreview(surface, rawReader, jpegReader, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot start preview", e);
        }
    }

    private void startCapture() {
        statusText.setText("Capturing...");
        captureButton.setEnabled(false);
        previewOverlay.setVisibility(View.GONE);

        captureLoop.start(100, currentIso, (long)(currentShutterS * 1_000_000_000L), new CaptureLoop.ProgressListener() {
            @Override
            public void onProgress(int current, int total) {
                runOnUiThread(() -> statusText.setText(String.format("Capturing %d/%d", current, total)));
            }

            @Override
            public void onJpegAvailable(Image image) {
                final Bitmap bitmap = PreviewProcessor.processJpeg(image);
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        previewOverlay.setImageBitmap(bitmap);
                        previewOverlay.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    statusText.setText("Ready");
                    captureButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Capture finished", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraController != null) cameraController.close();
        if (captureLoop != null) captureLoop.quit();
        cameraThread.quitSafely();
    }
}
