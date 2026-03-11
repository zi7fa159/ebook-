package com.alsclone.astrostacker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private CameraController cameraController;
    private FrameProcessor frameProcessor;
    private Renderer renderer;

    private TextView statusText;
    private TextView frameCounter;
    private android.widget.ProgressBar captureProgress;

    private TextView valExp, valIso, valFocus;
    private Button btnMainAction;

    private long currentShutterNs = 2000000000L; // 2s default
    private int currentIso = 1600;
    private float currentFocus = 0.0f;
    private boolean isStacking = false;
    private boolean isDestroyed = false;

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
        frameCounter = findViewById(R.id.frame_counter);
        captureProgress = findViewById(R.id.capture_progress);

        valExp = findViewById(R.id.val_exp);
        valIso = findViewById(R.id.val_iso);
        valFocus = findViewById(R.id.val_focus);
        btnMainAction = findViewById(R.id.btn_main_action);

        findViewById(R.id.ctrl_exp).setOnClickListener(v -> showEntryDialog("Exposure (seconds)", "2", s -> {
            try {
                float sec = Float.parseFloat(s);
                if (sec < 1) sec = 1; if (sec > 32) sec = 32;
                currentShutterNs = (long)(sec * 1000000000L);
                valExp.setText((int)sec + "s");
                updateCamera();
            } catch (Exception e) {}
        }));

        findViewById(R.id.ctrl_iso).setOnClickListener(v -> showEntryDialog("ISO", "1600", s -> {
            try {
                int iso = Integer.parseInt(s);
                if (iso < 100) iso = 100; if (iso > 6400) iso = 6400;
                currentIso = iso;
                valIso.setText(String.valueOf(iso));
                updateCamera();
            } catch (Exception e) {}
        }));

        findViewById(R.id.ctrl_focus).setOnClickListener(v -> showEntryDialog("Focus (0=INF, 10=Macro)", "0", s -> {
            try {
                float f = Float.parseFloat(s);
                if (f < 0) f = 0; if (f > 10) f = 10;
                currentFocus = f;
                valFocus.setText(f == 0 ? "INF" : String.format("%.1f", f));
                updateCamera();
            } catch (Exception e) {}
        }));

        btnMainAction.setOnClickListener(v -> {
            if (!isStacking) {
                startStacking();
            } else {
                stopStacking();
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> saveResult());

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.startStacking();
                frameCounter.setText("0 Frames");
                Toast.makeText(this, "Stack cleared", Toast.LENGTH_SHORT).show();
            }
        });

        initCamera();
    }

    private void showEntryDialog(String title, String hint, java.util.function.Consumer<String> callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(hint);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> callback.accept(input.getText().toString()));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void startStacking() {
        if (frameProcessor != null) {
            frameProcessor.startStacking();
            isStacking = true;
            btnMainAction.setText("STOP");
            btnMainAction.setBackgroundColor(0xFF444444);
            statusText.setText("STACKING...");
            startProgressThread();
        }
    }

    private void stopStacking() {
        if (frameProcessor != null) {
            frameProcessor.stopStacking();
            isStacking = false;
            btnMainAction.setText("START");
            btnMainAction.setBackgroundColor(0xFFFF4444);
            statusText.setText("PAUSED");
        }
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();
    }

    private void updateCamera() {
        if (cameraController != null) {
            cameraController.updateParams(currentShutterNs, currentIso, currentFocus);
        }
    }

    private void initCamera() {
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
                updateCamera(); // Apply initial params
            });
        }).start();
    }

    private void startProgressThread() {
        new Thread(() -> {
            int lastCount = -1;
            while (isStacking && !isDestroyed) {
                if (frameProcessor != null) {
                    int count = frameProcessor.getFrameCount();
                    if (count != lastCount) {
                        lastCount = count;
                        final int c = count;
                        runOnUiThread(() -> frameCounter.setText(c + " Frames"));
                    }
                }

                // Progress bar animation based on exposure time
                for (int i = 0; i <= 100; i += 2) {
                    if (!isStacking || isDestroyed) break;
                    final int p = i;
                    runOnUiThread(() -> captureProgress.setProgress(p));
                    try { Thread.sleep(currentShutterNs / 50000000); } catch (Exception e) {}
                }
            }
        }).start();
    }

    private void saveResult() {
        if (frameProcessor == null || cameraController == null) return;
        final float[] buffer = frameProcessor.getResultBuffer();
        final int w = cameraController.getRawSize().getWidth();
        final int h = cameraController.getRawSize().getHeight();

        Toast.makeText(this, "Saving 50MP lossless result...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            float maxValFound = 0;
            for (int i = 0; i < buffer.length; i += 1000) if (buffer[i] > maxValFound) maxValFound = buffer[i];
            if (maxValFound == 0) maxValFound = 1;
            final float maxVal = maxValFound;

            File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File path = new File(pictures, "ALS_Astro");
            if (!path.exists()) path.mkdirs();

            String ts = String.valueOf(System.currentTimeMillis());
            File pngFile = new File(path, "ALS_Stack_" + ts + ".png");
            File tiffFile = new File(path, "ALS_Stack_" + ts + ".tiff");

            try {
                // Save 16-bit TIFF (Natural Color Gains from metadata should be here, using manual for now)
                TiffWriter.saveTiff16Color(tiffFile.getAbsolutePath(), buffer, w, h, 2.0f, 2.0f);

                // Save 8-bit PNG (Downscaled preview for gallery compatibility)
                savePngOptimized(pngFile, buffer, w, h, maxVal);

                runOnUiThread(() -> Toast.makeText(this, "Saved to Pictures/ALS_Astro", Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void savePngOptimized(File file, float[] buffer, int w, int h, float maxVal) throws IOException {
        int sw = w / 2;
        int sh = h / 2;
        Bitmap bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        int[] rowPixels = new int[sw];

        for (int y = 0; y < sh; y++) {
            for (int x = 0; x < sw; x++) {
                int bx = (x * 2 / 2) * 2;
                int by = (y * 2 / 2) * 2;
                float r = buffer[by * w + bx] * 2.0f;
                float g = (buffer[by * w + (bx + 1)] + buffer[(by + 1) * w + bx]) / 2.0f;
                float b = buffer[(by + 1) * w + (bx + 1)] * 2.0f;

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
