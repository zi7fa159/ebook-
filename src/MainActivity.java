package com.example.astrostacker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements CameraController.FrameCallback {
    private CameraController cameraController;
    private StarDetector starDetector;
    private FrameAligner frameAligner;
    private StackEngine stackEngine;
    private Renderer renderer;

    private boolean isStacking = false;
    private List<StarDetector.Star> referenceStars;

    private TextView statusText, timerText, sensorInfoText;
    private Button btnStart, btnStop, btnSave, btnReset, btnApply, btnCloseMenu;
    private ImageButton btnMenu;
    private ToggleButton toggleStretch;
    private View menuLayout;
    private EditText editExposure, editISO, editFocus;
    private Spinner spinnerAlgo;
    private CheckBox checkAlign;

    private int width, height;

    private Timer timer;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        sensorInfoText = findViewById(R.id.sensorInfoText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        btnMenu = findViewById(R.id.btnMenu);
        btnApply = findViewById(R.id.btnApply);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        toggleStretch = findViewById(R.id.toggleStretch);
        menuLayout = findViewById(R.id.menuLayout);

        editExposure = findViewById(R.id.editExposure);
        editISO = findViewById(R.id.editISO);
        editFocus = findViewById(R.id.editFocus);
        spinnerAlgo = findViewById(R.id.spinnerAlgo);
        checkAlign = findViewById(R.id.checkAlign);

        String[] algos = {"AVERAGE", "ADDITIVE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, algos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlgo.setAdapter(adapter);

        cameraController = new CameraController(this);
        starDetector = new StarDetector();
        frameAligner = new FrameAligner();

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });

        btnMenu.setOnClickListener(v -> menuLayout.setVisibility(View.VISIBLE));
        btnCloseMenu.setOnClickListener(v -> menuLayout.setVisibility(View.GONE));

        btnApply.setOnClickListener(v -> {
            try {
                float expS = Float.parseFloat(editExposure.getText().toString());
                int iso = Integer.parseInt(editISO.getText().toString());
                float focus = Float.parseFloat(editFocus.getText().toString());

                cameraController.setExposure((long)(expS * 1_000_000_000L));
                cameraController.setIso(iso);
                cameraController.setFocus(focus);

                if (stackEngine != null) {
                    stackEngine.setMode(StackEngine.Mode.valueOf(spinnerAlgo.getSelectedItem().toString()));
                }

                sensorInfoText.setText(String.format("Exposure: %.2fs | ISO: %d | Focus: %.1f", expS, iso, focus));

                cameraController.startCapture();
                Toast.makeText(this, "Settings Applied", Toast.LENGTH_SHORT).show();
                menuLayout.setVisibility(View.GONE);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show();
            }
        });

        btnStart.setOnClickListener(v -> {
            isStacking = true;
            referenceStars = null;
            if (stackEngine != null) {
                stackEngine.reset();
                stackEngine.setMode(StackEngine.Mode.valueOf(spinnerAlgo.getSelectedItem().toString()));
            }
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            startTime = System.currentTimeMillis();
            startTimer();
            statusText.setText("Status: Stacking...");
        });

        btnStop.setOnClickListener(v -> {
            isStacking = false;
            stopTimer();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            statusText.setText("Status: Stopped");
        });

        btnReset.setOnClickListener(v -> {
            if (stackEngine != null) stackEngine.reset();
            referenceStars = null;
            startTime = System.currentTimeMillis();
            updateTimerText(0);
        });

        btnSave.setOnClickListener(v -> saveStackedImage());
    }

    private void openCamera() {
        cameraController.openCamera(this, () -> {
            runOnUiThread(() -> {
                cameraController.startCapture();
                Toast.makeText(MainActivity.this, "Camera Ready", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateTimerText(int frames) {
        long elapsed = isStacking ? (System.currentTimeMillis() - startTime) / 1000 : 0;
        String time = String.format("%02d:%02d", elapsed / 60, elapsed % 60);
        timerText.setText("Stacked: " + frames + " | Time: " + time);
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (isStacking) updateTimerText(stackEngine != null ? stackEngine.getFrameCount() : 0);
                });
            }
        }, 1000, 1000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onYuvFrameReceived(ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
        } catch (Exception e) {
            return;
        }
        if (image == null) return;

        if (width == 0) {
            width = image.getWidth();
            height = image.getHeight();
            stackEngine = new StackEngine(width, height);
            renderer = new Renderer(((SurfaceView)findViewById(R.id.surfaceView)).getHolder(), width, height);
        }

        Image.Plane yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        int rowStride = yPlane.getRowStride();

        // Efficiently copy Y data
        byte[] yData = new byte[yBuffer.remaining()];
        yBuffer.get(yData);

        if (isStacking) {
            float dx = 0, dy = 0;
            if (checkAlign.isChecked()) {
                List<StarDetector.Star> currentStars = starDetector.detectStars(yData, width, height, rowStride);
                if (referenceStars == null) {
                    if (currentStars.size() >= 8) {
                        referenceStars = currentStars;
                    }
                } else {
                    FrameAligner.Translation t = frameAligner.align(referenceStars, currentStars);
                    if (t != null) {
                        dx = t.dx;
                        dy = t.dy;
                    } else {
                        // Discard frame or keep last alignment? Requirement says discard.
                        image.close();
                        runOnUiThread(() -> statusText.setText("Status: Align Failed (Skipped)"));
                        return;
                    }
                }
            }

            stackEngine.addFrame(yData, rowStride, dx, dy);
            runOnUiThread(() -> statusText.setText("Status: Stacking..."));
            renderer.render(stackEngine.getStackBuffer(), width, height, toggleStretch.isChecked());
        } else {
            // Preview only
            float[] previewBuffer = new float[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    previewBuffer[y * width + x] = (yData[y * rowStride + x] & 0xFF);
                }
            }
            renderer.render(previewBuffer, width, height, toggleStretch.isChecked());
        }

        image.close();
    }

    @Override
    public void onRawFrameReceived(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            // Save RAW DNG if requested (Not implemented in live pipeline as per instructions)
            image.close();
        }
    }

    private void saveStackedImage() {
        if (stackEngine == null || stackEngine.getFrameCount() == 0) return;

        float[] buffer = stackEngine.getStackBuffer().clone();
        Toast.makeText(this, "Saving 16-bit TIFF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            ContentValues values = new ContentValues();
            String filename = "AstroStack_" + System.currentTimeMillis() + ".tif";
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/tiff");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AstroStacker");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            try {
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    BufferedOutputStream bout = new BufferedOutputStream(out);
                    write16BitTiff(bout, buffer, width, height);
                    bout.flush();
                    bout.close();
                    runOnUiThread(() -> Toast.makeText(this, "Saved: " + filename, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Save Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void write16BitTiff(OutputStream out, float[] buffer, int width, int height) throws Exception {
        // Little-Endian TIFF Header
        out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});
        int pixelDataSize = width * height * 2;
        int ifdOffset = 8 + pixelDataSize;

        out.write((ifdOffset & 0xFF));
        out.write(((ifdOffset >> 8) & 0xFF));
        out.write(((ifdOffset >> 16) & 0xFF));
        out.write(((ifdOffset >> 24) & 0xFF));

        // Pixel Data
        for (float val : buffer) {
            int v = (int)(val * 257.0f); // Scale 0-255 to 0-65535
            if (v > 65535) v = 65535;
            if (v < 0) v = 0;
            out.write(v & 0xFF);
            out.write((v >> 8) & 0xFF);
        }

        // IFD
        short numEntries = 9;
        out.write(numEntries & 0xFF);
        out.write((numEntries >> 8) & 0xFF);

        writeTIFFEntry(out, (short)256, (short)4, 1, width);
        writeTIFFEntry(out, (short)257, (short)4, 1, height);
        writeTIFFEntry(out, (short)258, (short)3, 1, 16);
        writeTIFFEntry(out, (short)259, (short)3, 1, 1);
        writeTIFFEntry(out, (short)262, (short)3, 1, 1);
        writeTIFFEntry(out, (short)273, (short)4, 1, 8);
        writeTIFFEntry(out, (short)277, (short)3, 1, 1);
        writeTIFFEntry(out, (short)278, (short)4, 1, height);
        writeTIFFEntry(out, (short)279, (short)4, 1, pixelDataSize);

        out.write(new byte[]{0, 0, 0, 0});
    }

    private void writeTIFFEntry(OutputStream out, short tag, short type, int count, int val) throws Exception {
        out.write(tag & 0xFF); out.write((tag >> 8) & 0xFF);
        out.write(type & 0xFF); out.write((type >> 8) & 0xFF);
        out.write(count & 0xFF); out.write((count >> 8) & 0xFF);
        out.write((count >> 16) & 0xFF); out.write((count >> 24) & 0xFF);
        out.write(val & 0xFF); out.write((val >> 8) & 0xFF);
        out.write((val >> 16) & 0xFF); out.write((val >> 24) & 0xFF);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraController.close();
    }
}
