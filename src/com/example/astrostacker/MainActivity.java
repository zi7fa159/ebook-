package com.example.astrostacker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.List;

public class MainActivity extends Activity implements CameraController.FrameCallback {
    private CameraController cameraController;
    private StarDetector starDetector;
    private FrameAligner frameAligner;
    private RawStackEngine rawStackEngine;
    private LiveRenderer renderer;

    private boolean isStacking = false;
    private List<StarDetector.Star> refStars;
    private int widthYuv, heightYuv;
    private int widthRaw, heightRaw;
    private byte[] yData;
    private short[] rawData;
    private byte[] rowBuffer;

    private TextView statusText, counterText, valExp, valIso;
    private Button btnCapture, btnSave, btnSettings, btnApply;
    private View settingsOverlay;
    private EditText setExp, setIso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        counterText = findViewById(R.id.counterText);
        valExp = findViewById(R.id.valExp);
        valIso = findViewById(R.id.valIso);
        btnCapture = findViewById(R.id.btnCapture);
        btnSave = findViewById(R.id.btnSave);
        btnSettings = findViewById(R.id.btnSettings);
        btnApply = findViewById(R.id.btnApply);
        settingsOverlay = findViewById(R.id.settingsOverlay);
        setExp = findViewById(R.id.setExp);
        setIso = findViewById(R.id.setIso);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] perms = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE} :
                    new String[]{Manifest.permission.CAMERA};
            requestPermissions(perms, 101);
        } else {
            initApp();
        }
    }

    private void initApp() {
        cameraController = new CameraController(this);
        starDetector = new StarDetector();
        frameAligner = new FrameAligner();

        SurfaceView sv = findViewById(R.id.surfaceView);
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder h) {
                cameraController.openCamera(MainActivity.this, () -> {
                    runOnUiThread(() -> {
                        cameraController.startStreaming();
                        statusText.setText("READY");
                    });
                });
            }
            @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int h1) {}
            @Override public void surfaceDestroyed(SurfaceHolder h) {}
        });

        btnCapture.setOnClickListener(v -> {
            if (!isStacking) {
                isStacking = true;
                refStars = null;
                if (rawStackEngine != null) rawStackEngine.reset();
                btnCapture.setText("STOP");
                statusText.setText("STACKING...");
            } else {
                isStacking = false;
                btnCapture.setText("START");
                statusText.setText("FINISHED");
            }
        });

        btnSettings.setOnClickListener(v -> settingsOverlay.setVisibility(View.VISIBLE));
        btnApply.setOnClickListener(v -> {
            try {
                float expS = Float.parseFloat(setExp.getText().toString());
                int iso = Integer.parseInt(setIso.getText().toString());
                cameraController.setExposure((long)(expS * 1_000_000_000L));
                cameraController.setIso(iso);
                cameraController.startStreaming();
                valExp.setText(expS + "s");
                valIso.setText(String.valueOf(iso));
                settingsOverlay.setVisibility(View.GONE);
            } catch (Exception e) { Toast.makeText(this, "Invalid settings", Toast.LENGTH_SHORT).show(); }
        });

        btnSave.setOnClickListener(v -> saveTiff());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initApp();
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onYuvFrameReceived(ImageReader reader) {
        Image img = null;
        try {
            img = reader.acquireLatestImage();
            if (img == null) return;

            if (yData == null) {
                widthYuv = img.getWidth();
                heightYuv = img.getHeight();
                yData = new byte[widthYuv * heightYuv];
                rowBuffer = new byte[img.getPlanes()[0].getRowStride()];
                renderer = new LiveRenderer(((SurfaceView)findViewById(R.id.surfaceView)).getHolder(), widthYuv, heightYuv);
            }

            Image.Plane plane = img.getPlanes()[0];
            ByteBuffer buf = plane.getBuffer();
            int rowStride = plane.getRowStride();

            for (int y = 0; y < heightYuv; y++) {
                buf.position(y * rowStride);
                buf.get(yData, y * widthYuv, widthYuv);
            }

            if (!isStacking && renderer != null) {
                runOnUiThread(() -> renderer.renderYuv(yData, widthYuv, heightYuv));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (img != null) img.close();
        }
    }

    @Override
    public void onRawFrameReceived(ImageReader reader) {
        Image img = null;
        try {
            img = reader.acquireLatestImage();
            if (img == null) return;

            if (rawData == null) {
                widthRaw = img.getWidth();
                heightRaw = img.getHeight();
                rawData = new short[widthRaw * heightRaw];
                rawStackEngine = new RawStackEngine(widthRaw, heightRaw);
            }

            ShortBuffer buf = img.getPlanes()[0].getBuffer().asShortBuffer();
            buf.get(rawData);

            if (isStacking && yData != null) {
                float dx = 0, dy = 0;
                List<StarDetector.Star> stars = starDetector.detectStars(yData, widthYuv, heightYuv);
                if (refStars == null) {
                    if (stars.size() >= 5) refStars = stars;
                } else {
                    FrameAligner.Translation t = frameAligner.align(refStars, stars, (float)widthRaw/widthYuv, (float)heightRaw/heightYuv);
                    if (t != null) { dx = t.dx; dy = t.dy; } else { return; }
                }
                if (rawStackEngine != null) {
                    rawStackEngine.addFrame(rawData, dx, dy);
                    runOnUiThread(() -> {
                        if (renderer != null) renderer.renderRaw(rawStackEngine.getStack(), widthRaw, heightRaw);
                        counterText.setText(rawStackEngine.getFrameCount() + " frames");
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (img != null) img.close();
        }
    }

    private void saveTiff() {
        if (rawStackEngine == null || rawStackEngine.getFrameCount() == 0) return;
        float[] stack = rawStackEngine.getStack().clone();
        int w = rawStackEngine.getWidth();
        int h = rawStackEngine.getHeight();
        new Thread(() -> {
            try {
                String name = "ProAstro_" + System.currentTimeMillis() + ".tif";
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/tiff");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProAstro");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    BufferedOutputStream bout = new BufferedOutputStream(out);
                    TiffWriter.write16BitGrayscale(bout, stack, w, h);
                    bout.flush(); bout.close();
                    runOnUiThread(() -> Toast.makeText(this, "Saved: " + name, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (cameraController != null) cameraController.close();
    }
}
