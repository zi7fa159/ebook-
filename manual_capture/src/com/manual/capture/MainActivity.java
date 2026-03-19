package com.manual.capture;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.*;
import java.io.OutputStream;

public class MainActivity extends Activity implements CameraController.CaptureListener {
    private static final String TAG = "ManualRaw_Main";
    private CameraController cameraController;
    private TextureView preview;
    private TextView statusText, storageInfo;
    private ProgressBar progress;
    private Button btnShoot;
    private ToggleButton btnMode;

    private SeekBar seekShutter, seekIso, seekFocus, seekWb;
    private TextView valShutter, valIso, valFocus, valWb;

    private float minShutter = 0.01f, maxShutter = 2.0f;
    private int minIso = 100, maxIso = 3200;
    private float minFocus = 0.0f, maxFocus = 10.0f;
    private int minWb = 3000, maxWb = 7000;

    private Uri saveFolderUri;
    private boolean isContinuous = false;
    private boolean isRunning = false;
    private long startTime = 0;
    private int captureCount = 0;

    interface LimitCallback {
        void onLimitSet(float min, float max);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.manual.capture.R.layout.activity_main);
        initUI();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            initCamera();
        }
    }

    private void initUI() {
        preview = findViewById(com.manual.capture.R.id.preview);
        statusText = findViewById(com.manual.capture.R.id.status_text);
        storageInfo = findViewById(com.manual.capture.R.id.storage_info);
        progress = findViewById(com.manual.capture.R.id.capture_progress);
        btnShoot = findViewById(com.manual.capture.R.id.btn_shoot);
        btnMode = findViewById(com.manual.capture.R.id.btn_mode);

        seekShutter = findViewById(com.manual.capture.R.id.seek_shutter); valShutter = findViewById(com.manual.capture.R.id.val_shutter);
        seekIso = findViewById(com.manual.capture.R.id.seek_iso); valIso = findViewById(com.manual.capture.R.id.val_iso);
        seekFocus = findViewById(com.manual.capture.R.id.seek_focus); valFocus = findViewById(com.manual.capture.R.id.val_focus);
        seekWb = findViewById(com.manual.capture.R.id.seek_wb); valWb = findViewById(com.manual.capture.R.id.val_wb);

        setupSlider(seekShutter, valShutter, "s", p -> {
            float val = minShutter + (maxShutter - minShutter) * (p / 1000.0f);
            return String.format("%.4f", val);
        });
        setupSlider(seekIso, valIso, "", p -> String.valueOf(minIso + (int)((maxIso - minIso) * (p / 1000.0f))));
        setupSlider(seekFocus, valFocus, "", p -> {
            float val = minFocus + (maxFocus - minFocus) * (p / 1000.0f);
            return val == 0 ? "INF" : String.format("%.2f", val);
        });
        setupSlider(seekWb, valWb, "K", p -> String.valueOf(minWb + (int)((maxWb - minWb) * (p / 1000.0f))));

        findViewById(com.manual.capture.R.id.btn_folder).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 2);
        });

        btnShoot.setOnClickListener(v -> {
            if (isRunning) stopCapture();
            else startCapture();
        });

        setLimitAction(valShutter, "Shutter Range", (min, max) -> { minShutter = min; maxShutter = max; });
        setLimitAction(valIso, "ISO Range", (min, max) -> { minIso = (int)min; maxIso = (int)max; });
        setLimitAction(valFocus, "Focus Range", (min, max) -> { minFocus = min; maxFocus = max; });
        setLimitAction(valWb, "WB Range", (min, max) -> { minWb = (int)min; maxWb = (int)max; });
    }

    private void setupSlider(SeekBar seek, TextView valTxt, String suffix, java.util.function.Function<Integer, String> mapper) {
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valTxt.setText(mapper.apply(progress) + suffix);
                if (fromUser) updateCamParams();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setLimitAction(View view, String title, final LimitCallback callback) {
        view.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText minInput = new EditText(this); minInput.setHint("Min"); layout.addView(minInput);
            final EditText maxInput = new EditText(this); maxInput.setHint("Max"); layout.addView(maxInput);
            builder.setView(layout);
            builder.setPositiveButton("SET", (dialog, which) -> {
                try {
                    callback.onLimitSet(Float.parseFloat(minInput.getText().toString()), Float.parseFloat(maxInput.getText().toString()));
                    Toast.makeText(this, "Range Updated", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {}
            });
            builder.show();
            return true;
        });
    }

    private void updateCamParams() {
        if (cameraController == null) return;
        float s = minShutter + (maxShutter - minShutter) * (seekShutter.getProgress() / 1000.0f);
        int i = minIso + (int)((maxIso - minIso) * (seekIso.getProgress() / 1000.0f));
        float f = minFocus + (maxFocus - minFocus) * (seekFocus.getProgress() / 1000.0f);
        int w = minWb + (int)((maxWb - minWb) * (seekWb.getProgress() / 1000.0f));
        cameraController.updateParams((long)(s * 1e9), i, f, w);
    }

    private void startCapture() {
        if (saveFolderUri == null) {
            Toast.makeText(this, "Select folder first!", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;
        isContinuous = btnMode.isChecked();
        startTime = System.currentTimeMillis();
        captureCount = 0;
        btnShoot.setText("STOP");
        if (isContinuous) {
            progress.setVisibility(View.VISIBLE);
            progress.setIndeterminate(true);
        }
        captureCycle();
    }

    private void stopCapture() {
        isRunning = false;
        runOnUiThread(() -> {
            btnShoot.setText("CAPTURE");
            progress.setVisibility(View.GONE);
            statusText.setText("DONE: " + captureCount + " frames");
        });
    }

    private void captureCycle() {
        if (!isRunning) return;
        cameraController.takeRaw();
    }

    @Override
    public void onRawAvailable(Image img, TotalCaptureResult result) {
        captureCount++;
        saveDng(img, result);
        final long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        runOnUiThread(() -> statusText.setText("SAVED: " + captureCount + " (" + elapsed + "s)"));

        if (isContinuous && isRunning) {
            new Handler(Looper.getMainLooper()).postDelayed(this::captureCycle, 100);
        } else {
            stopCapture();
        }
    }

    private void saveDng(Image img, TotalCaptureResult result) {
        try {
            String filename = "IMG_" + System.currentTimeMillis() + ".dng";
            Uri fileUri = DocumentsContract.createDocument(getContentResolver(), saveFolderUri, "image/x-adobe-dng", filename);

            try (OutputStream out = getContentResolver().openOutputStream(fileUri)) {
                DngCreator creator = new DngCreator(cameraController.getCharacteristics(), result);
                creator.writeImage(out, img);
            }
            img.close();
        } catch (Exception e) {
            Log.e(TAG, "Save failed", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();
            saveFolderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            storageInfo.setText("FOLDER: " + treeUri.getLastPathSegment());
        }
    }

    private void initCamera() {
        cameraController = new CameraController(this, preview, this);
        cameraController.start();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (cameraController != null) cameraController.stop();
    }
}
