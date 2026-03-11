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
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements CameraController.FrameCallback {
    private CameraController cameraController;
    private StarDetector starDetector;
    private FrameAligner frameAligner;
    private StackEngine stackEngine;
    private RawStackEngine rawStackEngine;
    private Renderer renderer;
    private RawRenderer rawRenderer;

    private boolean isStacking = false;
    private boolean isCapturingDark = false;
    private List<StarDetector.Star> referenceStars;

    private TextView statusText, timerText, sensorInfoText;
    private Button btnStart, btnStop, btnSave, btnReset, btnApply, btnCloseMenu, btnDark;
    private ImageButton btnMenu;
    private ToggleButton toggleStretch;
    private View menuLayout;
    private EditText editExposure, editISO, editFocus;
    private Spinner spinnerAlgo;
    private CheckBox checkAlign, checkRaw;

    private int width, height;
    private byte[] yData, uData, vData;
    private short[] rawData;
    private byte[] rowBuffer;

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
        timerText = findViewById(R.id.timerText);
        sensorInfoText = findViewById(R.id.sensorInfoText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        btnMenu = findViewById(R.id.btnMenu);
        btnApply = findViewById(R.id.btnApply);
        btnCloseMenu = findViewById(R.id.btnCloseMenu);
        btnDark = findViewById(R.id.btnDark);
        toggleStretch = findViewById(R.id.toggleStretch);
        menuLayout = findViewById(R.id.menuLayout);

        editExposure = findViewById(R.id.editExposure);
        editISO = findViewById(R.id.editISO);
        editFocus = findViewById(R.id.editFocus);
        spinnerAlgo = findViewById(R.id.spinnerAlgo);
        checkAlign = findViewById(R.id.checkAlign);
        checkRaw = findViewById(R.id.checkRaw);

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
                cameraController.setRawMode(checkRaw.isChecked());

                if (stackEngine != null) stackEngine.setMode(StackEngine.Mode.valueOf(spinnerAlgo.getSelectedItem().toString()));

                sensorInfoText.setText(String.format("Mode: %s | Exposure: %.2fs | ISO: %d", checkRaw.isChecked() ? "RAW" : "YUV", expS, iso));

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
            if (cameraController.isRawMode()) {
                if (rawStackEngine != null) rawStackEngine.reset();
            } else {
                if (stackEngine != null) {
                    stackEngine.reset();
                    stackEngine.setMode(StackEngine.Mode.valueOf(spinnerAlgo.getSelectedItem().toString()));
                }
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
            if (rawStackEngine != null) rawStackEngine.reset();
            if (stackEngine != null) stackEngine.reset();
            referenceStars = null;
            startTime = System.currentTimeMillis();
            updateTimerText(0);
        });

        btnDark.setOnClickListener(v -> {
            if (!cameraController.isRawMode()) {
                Toast.makeText(this, "Dark frame only supported in RAW mode", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Cover lens and wait for one frame...", Toast.LENGTH_LONG).show();
            isCapturingDark = true;
            menuLayout.setVisibility(View.GONE);
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
                    if (isStacking) {
                        int count = cameraController.isRawMode() ? (rawStackEngine != null ? rawStackEngine.getFrameCount() : 0) : (stackEngine != null ? stackEngine.getFrameCount() : 0);
                        updateTimerText(count);
                    }
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
        try { image = reader.acquireLatestImage(); } catch (Exception e) { return; }
        if (image == null) return;

        if (width == 0) {
            width = image.getWidth();
            height = image.getHeight();
            yData = new byte[width * height];
            uData = new byte[(width / 2) * (height / 2)];
            vData = new byte[(width / 2) * (height / 2)];
            rowBuffer = new byte[image.getPlanes()[0].getRowStride()];
            stackEngine = new StackEngine(width, height);
            renderer = new Renderer(((SurfaceView)findViewById(R.id.surfaceView)).getHolder(), width, height);
        }

        if (cameraController.isRawMode()) { image.close(); return; }

        copyPlane(image.getPlanes()[0], yData, width, height);
        copyPlane(image.getPlanes()[1], uData, width / 2, height / 2);
        copyPlane(image.getPlanes()[2], vData, width / 2, height / 2);

        if (isStacking) {
            float dx = 0, dy = 0;
            if (checkAlign.isChecked()) {
                List<StarDetector.Star> currentStars = starDetector.detectStarsFromY(yData, width, height, width);
                if (referenceStars == null) {
                    if (currentStars.size() >= 8) referenceStars = currentStars;
                } else {
                    FrameAligner.Translation t = frameAligner.align(referenceStars, currentStars);
                    if (t != null) { dx = t.dx; dy = t.dy; } else { image.close(); return; }
                }
            }
            stackEngine.addFrame(yData, width, uData, width / 2, vData, width / 2, dx, dy);
            renderer.render(stackEngine.getStackY(), stackEngine.getStackU(), stackEngine.getStackV(), width, height, toggleStretch.isChecked(), stackEngine.getFrameCount(), stackEngine.getMode());
        } else {
            stackEngine.reset();
            stackEngine.addFrame(yData, width, uData, width / 2, vData, width / 2, 0, 0);
            renderer.render(stackEngine.getStackY(), stackEngine.getStackU(), stackEngine.getStackV(), width, height, toggleStretch.isChecked(), 1, StackEngine.Mode.AVERAGE);
        }
        image.close();
    }

    @Override
    public void onRawFrameReceived(ImageReader reader) {
        Image image = null;
        try { image = reader.acquireLatestImage(); } catch (Exception e) { return; }
        if (image == null) return;

        int w = image.getWidth();
        int h = image.getHeight();
        if (rawData == null || rawData.length != w * h) {
            rawData = new short[w * h];
            rawStackEngine = new RawStackEngine(w, h);
            rawRenderer = new RawRenderer(((SurfaceView)findViewById(R.id.surfaceView)).getHolder(), w, h);
        }

        ShortBuffer sb = image.getPlanes()[0].getBuffer().asShortBuffer();
        sb.get(rawData);

        if (isCapturingDark) {
            float[] dark = new float[rawData.length];
            for (int i = 0; i < rawData.length; i++) dark[i] = (float)(rawData[i] & 0xFFFF);
            rawStackEngine.setDarkFrame(dark);
            isCapturingDark = false;
            runOnUiThread(() -> Toast.makeText(this, "Dark Frame Captured", Toast.LENGTH_SHORT).show());
        } else if (isStacking) {
            float dx = 0, dy = 0;
            if (checkAlign.isChecked()) {
                List<StarDetector.Star> currentStars = starDetector.detectStarsFromRaw(rawData, w, h);
                if (referenceStars == null) {
                    if (currentStars.size() >= 8) referenceStars = currentStars;
                } else {
                    FrameAligner.Translation t = frameAligner.align(referenceStars, currentStars);
                    if (t != null) { dx = t.dx; dy = t.dy; } else { image.close(); return; }
                }
            }
            rawStackEngine.addFrame(rawData, dx, dy);
            rawRenderer.render(rawStackEngine.getStackBuffer(), w, h, toggleStretch.isChecked());
        } else {
            float[] preview = new float[rawData.length];
            for (int i = 0; i < rawData.length; i++) preview[i] = (float)(rawData[i] & 0xFFFF);
            rawRenderer.render(preview, w, h, toggleStretch.isChecked());
        }
        image.close();
    }

    private void copyPlane(Image.Plane plane, byte[] dest, int w, int h) {
        ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        buffer.rewind();
        for (int y = 0; y < h; y++) {
            buffer.position(y * rowStride);
            if (pixelStride == 1) {
                buffer.get(dest, y * w, w);
            } else {
                buffer.get(rowBuffer, 0, Math.min(rowStride, buffer.remaining()));
                for (int x = 0; x < w; x++) dest[y * w + x] = rowBuffer[x * pixelStride];
            }
        }
    }

    private void saveStackedImage() {
        if (cameraController.isRawMode()) {
            if (rawStackEngine == null || rawStackEngine.getFrameCount() == 0) return;
            saveRawStack();
        } else {
            if (stackEngine == null || stackEngine.getFrameCount() == 0) return;
            saveYuvStack();
        }
    }

    private void saveYuvStack() {
        float[] bufferY = stackEngine.getStackY().clone();
        int frames = stackEngine.getFrameCount();
        StackEngine.Mode mode = stackEngine.getMode();
        int w = width, h = height;
        new Thread(() -> {
            Uri uri = createExternalImageUri("AstroStack_YUV_" + System.currentTimeMillis() + ".tif");
            try {
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    BufferedOutputStream bout = new BufferedOutputStream(out);
                    write16BitTiff(bout, bufferY, w, h, frames, mode);
                    bout.flush();
                    bout.close();
                    runOnUiThread(() -> Toast.makeText(this, "Saved YUV stack", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void saveRawStack() {
        float[] buffer = rawStackEngine.getStackBuffer().clone();
        int rw = rawStackEngine.getWidth();
        int rh = rawStackEngine.getHeight();
        new Thread(() -> {
            Uri uri = createExternalImageUri("AstroStack_RAW_" + System.currentTimeMillis() + ".tif");
            try {
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    BufferedOutputStream bout = new BufferedOutputStream(out);
                    write16BitTiff(bout, buffer, rw, rh, 1, StackEngine.Mode.AVERAGE); // RAW is always averaged in this impl
                    bout.flush();
                    bout.close();
                    runOnUiThread(() -> Toast.makeText(this, "Saved RAW stack", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private Uri createExternalImageUri(String filename) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/tiff");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AstroStacker");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void write16BitTiff(OutputStream out, float[] buffer, int width, int height, int frames, StackEngine.Mode mode) throws Exception {
        out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});
        int pixelDataSize = width * height * 2;
        int ifdOffset = 8 + pixelDataSize;
        out.write((ifdOffset & 0xFF)); out.write(((ifdOffset >> 8) & 0xFF)); out.write(((ifdOffset >> 16) & 0xFF)); out.write(((ifdOffset >> 24) & 0xFF));

        float maxVal = 255;
        if (mode == StackEngine.Mode.ADDITIVE) {
            for (float f : buffer) if (f > maxVal) maxVal = f;
        }
        float scale = 65535.0f / maxVal;
        for (float val : buffer) {
            int v = (int)(val * scale);
            if (v > 65535) v = 65535; else if (v < 0) v = 0;
            out.write(v & 0xFF); out.write((v >> 8) & 0xFF);
        }

        short numEntries = 9;
        out.write(numEntries & 0xFF); out.write((numEntries >> 8) & 0xFF);
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
