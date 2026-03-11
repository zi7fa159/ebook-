package com.example.astrostacker;

import android.app.Activity;
import android.content.ContentValues;
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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends Activity implements CameraController.FrameCallback {
    private CameraController cameraController;
    private StarDetector starDetector;
    private FrameAligner frameAligner;
    private StackEngine stackEngine;
    private Renderer renderer;

    private boolean isStacking = false;
    private List<StarDetector.Star> referenceStars;

    private TextView statusText, infoText;
    private Button btnStart, btnStop, btnSave;
    private int width, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        infoText = findViewById(R.id.infoText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSave = findViewById(R.id.btnSave);

        SeekBar exposureSeekBar = findViewById(R.id.exposureSeekBar);
        SeekBar isoSeekBar = findViewById(R.id.isoSeekBar);

        cameraController = new CameraController(this);
        starDetector = new StarDetector();
        frameAligner = new FrameAligner();

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                cameraController.openCamera(MainActivity.this);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraController.close();
            }
        });

        btnStart.setOnClickListener(v -> {
            isStacking = true;
            referenceStars = null;
            if (stackEngine != null) stackEngine.reset();
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            cameraController.startCapture();
        });

        btnStop.setOnClickListener(v -> {
            isStacking = false;
            cameraController.stopCapture();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        btnSave.setOnClickListener(v -> saveStackedImage());

        exposureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long exposureNs = (progress + 1) * 1_000_000_000L;
                cameraController.setExposure(exposureNs);
                updateInfoText(progress + 1, isoSeekBar.getProgress() * 100 + 100);
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
                updateInfoText(exposureSeekBar.getProgress() + 1, iso);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initial defaults
        exposureSeekBar.setProgress(0); // 1s
        isoSeekBar.setProgress(7); // ISO 800
    }

    private void updateInfoText(long expS, int iso) {
        infoText.setText("Exp: " + expS + "s | ISO: " + iso);
    }

    @Override
    public void onFrameReceived(ImageReader reader) {
        Image image = reader.acquireLatestImage();
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
                    // Could not align, maybe skip frame
                }
            }

            runOnUiThread(() -> statusText.setText("Frames: " + stackEngine.getFrameCount()));
            renderer.render(stackEngine.getStackBuffer(), width, height);
        }

        image.close();
    }

    private void saveStackedImage() {
        if (stackEngine == null || stackEngine.getFrameCount() == 0) return;

        float[] buffer = stackEngine.getStackBuffer();

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
                write16BitTiff(out, buffer, width, height);
                out.close();
                Toast.makeText(this, "Saved 16-bit TIFF to Pictures/AstroStacker", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save 16-bit TIFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void write16BitTiff(OutputStream out, float[] buffer, int width, int height) throws Exception {
        // Simple Little-Endian TIFF Header for 16-bit grayscale
        // Header (8 bytes)
        out.write(new byte[]{0x49, 0x49, 0x2A, 0x00}); // II, 42
        int ifdOffset = 8 + width * height * 2;
        out.write(new byte[]{(byte)(ifdOffset & 0xFF), (byte)((ifdOffset >> 8) & 0xFF),
                             (byte)((ifdOffset >> 16) & 0xFF), (byte)((ifdOffset >> 24) & 0xFF)});

        // Pixel Data
        for (float val : buffer) {
            int v = (int)(val * 257); // Scale 0-255 to 0-65535
            if (v > 65535) v = 65535;
            out.write(v & 0xFF);
            out.write((v >> 8) & 0xFF);
        }

        // IFD (Image File Directory)
        short numEntries = 7;
        out.write(numEntries & 0xFF);
        out.write((numEntries >> 8) & 0xFF);

        writeTIFFEntry(out, (short)256, (short)4, 1, width);  // Width
        writeTIFFEntry(out, (short)257, (short)4, 1, height); // Height
        writeTIFFEntry(out, (short)258, (short)3, 1, 16);     // BitsPerSample
        writeTIFFEntry(out, (short)259, (short)3, 1, 1);      // Compression (None)
        writeTIFFEntry(out, (short)262, (short)3, 1, 1);      // PhotometricInterpretation (BlackIsZero)
        writeTIFFEntry(out, (short)273, (short)4, 1, 8);      // StripOffsets
        writeTIFFEntry(out, (short)278, (short)4, 1, height); // RowsPerStrip

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
}
