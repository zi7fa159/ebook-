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
import android.widget.Button;
import android.widget.SeekBar;
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

    private TextView statusText, infoText, sensorInfoText;
    private Button btnStart, btnStop, btnSave, btnReset;
    private ToggleButton toggleStretch;
    private int width, height;

    private Timer timer;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }

        statusText = findViewById(R.id.statusText);
        infoText = findViewById(R.id.infoText);
        sensorInfoText = findViewById(R.id.sensorInfoText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        toggleStretch = findViewById(R.id.toggleStretch);

        SeekBar exposureSeekBar = findViewById(R.id.exposureSeekBar);
        SeekBar isoSeekBar = findViewById(R.id.isoSeekBar);
        SeekBar focusSeekBar = findViewById(R.id.focusSeekBar);

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

        if (surfaceView.getHolder().getSurface().isValid()) {
            openCamera();
        }

        btnStart.setOnClickListener(v -> {
            isStacking = true;
            referenceStars = null;
            if (stackEngine != null) stackEngine.reset();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            startTime = System.currentTimeMillis();
            startTimer();
            cameraController.startCapture();
        });

        btnStop.setOnClickListener(v -> {
            isStacking = false;
            stopTimer();
            cameraController.stopCapture();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        btnReset.setOnClickListener(v -> {
            if (stackEngine != null) stackEngine.reset();
            referenceStars = null;
            startTime = System.currentTimeMillis();
            updateStatusText(0);
        });

        btnSave.setOnClickListener(v -> saveStackedImage());

        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long exposureNs = (progress + 1) * 1_000_000_000L;
                cameraController.setExposure(exposureNs);
                updateInfoText(progress + 1, isoSeekBar.getProgress() * 100 + 100, focusSeekBar.getProgress() / 10.0f);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        isoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int iso = progress * 100 + 100;
                cameraController.setIso(iso);
                updateInfoText(exposureSeekBar.getProgress() + 1, iso, focusSeekBar.getProgress() / 10.0f);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        focusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float dist = progress / 10.0f;
                cameraController.setFocus(dist);
                updateInfoText(exposureSeekBar.getProgress() + 1, isoSeekBar.getProgress() * 100 + 100, dist);
                if (!isStacking) cameraController.startCapture(); // Update focus live
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initial defaults
        exposureSeekBar.setProgress(0); // 1s
        isoSeekBar.setProgress(7); // ISO 800
        focusSeekBar.setProgress(0); // Inf
    }

    private void openCamera() {
        cameraController.openCamera(this, () -> {
            runOnUiThread(() -> {
                cameraController.startCapture();
                Toast.makeText(MainActivity.this, "Camera Ready", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateInfoText(long expS, int iso, float focus) {
        infoText.setText("Exp: " + expS + "s | ISO: " + iso + " | Focus: " + (focus == 0 ? "Inf" : focus));
    }

    private void updateStatusText(int frames) {
        long elapsed = isStacking ? (System.currentTimeMillis() - startTime) / 1000 : 0;
        String time = String.format("%02d:%02d", elapsed / 60, elapsed % 60);
        statusText.setText("Frames: " + frames + " | Time: " + time);
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (isStacking) updateStatusText(stackEngine != null ? stackEngine.getFrameCount() : 0);
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
    public void onFrameReceived(ImageReader reader) {
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
        byte[] yData = new byte[yBuffer.remaining()];
        yBuffer.get(yData);

        if (isStacking) {
            List<StarDetector.Star> currentStars = starDetector.detectStars(yData, width, height, rowStride);
            float dx = 0, dy = 0;

            if (referenceStars == null) {
                referenceStars = currentStars;
                stackEngine.addFrame(yData, rowStride, 0, 0);
            } else {
                FrameAligner.Translation t = frameAligner.align(referenceStars, currentStars);
                if (t != null) {
                    dx = t.dx;
                    dy = t.dy;
                    stackEngine.addFrame(yData, rowStride, dx, dy);
                } else {
                    // Could not align
                }
            }

            renderer.render(stackEngine.getStackBuffer(), width, height, toggleStretch.isChecked());
        } else {
            // Preview only
            float[] previewBuffer = new float[width * height];
            for (int i = 0; i < width * height; i++) {
                int yIdx = (i / width) * rowStride + (i % width);
                previewBuffer[i] = (yData[yIdx] & 0xFF);
            }
            renderer.render(previewBuffer, width, height, toggleStretch.isChecked());
        }

        image.close();
    }

    private void saveStackedImage() {
        if (stackEngine == null || stackEngine.getFrameCount() == 0 || width == 0) return;

        float[] buffer = stackEngine.getStackBuffer().clone();
        Toast.makeText(this, "Saving TIFF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            // Save as 16-bit Grayscale TIFF
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
                String msg = e.getMessage();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void write16BitTiff(OutputStream out, float[] buffer, int width, int height) throws Exception {
        // Simple Little-Endian TIFF Header for 16-bit grayscale
        // Header (8 bytes)
        out.write(new byte[]{0x49, 0x49, 0x2A, 0x00}); // II, 42
        int pixelDataSize = width * height * 2;
        int ifdOffset = 8 + pixelDataSize;
        out.write(new byte[]{(byte)(ifdOffset & 0xFF), (byte)((ifdOffset >> 8) & 0xFF),
                             (byte)((ifdOffset >> 16) & 0xFF), (byte)((ifdOffset >> 24) & 0xFF)});

        // Pixel Data
        for (float val : buffer) {
            int v = (int)(val * 257.01f); // Scale 0-255 to 0-65535
            if (v > 65535) v = 65535;
            if (v < 0) v = 0;
            out.write(v & 0xFF);
            out.write((v >> 8) & 0xFF);
        }

        // IFD (Image File Directory)
        short numEntries = 8;
        out.write(numEntries & 0xFF);
        out.write((numEntries >> 8) & 0xFF);

        writeTIFFEntry(out, (short)256, (short)4, 1, width);         // Width
        writeTIFFEntry(out, (short)257, (short)4, 1, height);        // Height
        writeTIFFEntry(out, (short)258, (short)3, 1, 16);            // BitsPerSample
        writeTIFFEntry(out, (short)259, (short)3, 1, 1);             // Compression (None)
        writeTIFFEntry(out, (short)262, (short)3, 1, 1);             // PhotometricInterpretation (BlackIsZero)
        writeTIFFEntry(out, (short)273, (short)4, 1, 8);             // StripOffsets
        writeTIFFEntry(out, (short)278, (short)4, 1, height);        // RowsPerStrip
        writeTIFFEntry(out, (short)279, (short)4, 1, pixelDataSize); // StripByteCounts

        out.write(new byte[]{0, 0, 0, 0}); // Next IFD offset
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraController.close();
    }
}
