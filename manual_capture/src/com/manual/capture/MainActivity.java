package com.manual.capture;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
    private Renderer renderer;
    private FrameProcessor frameProcessor;
    private ColorEngine colorEngine;

    private TextureView preview;
    private TextView statusText, storageInfo;
    private ProgressBar progress;
    private Button btnShoot;
    private ToggleButton btnMode;

    private SeekBar seekShutter, seekIso, seekFocus, seekWb;
    private TextView valShutter, valIso, valFocus, valWb;

    private float minShutter = 0.0001f, maxShutter = 32.0f;
    private int minIso = 100, maxIso = 6400;
    private float minFocus = 0.0f, maxFocus = 10.0f;
    private int minWb = 2000, maxWb = 10000;

    private Uri saveFolderUri;
    private boolean isRunning = false;
    private int captureCount = 0;
    private int targetFrames = 1;

    private SharedPreferences prefs;
    private HandlerThread saveThread;
    private Handler saveHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.manual.capture.R.layout.activity_main);

        saveThread = new HandlerThread("SaveThread");
        saveThread.start();
        saveHandler = new Handler(saveThread.getLooper());

        prefs = getSharedPreferences("manual_raw_prefs", Context.MODE_PRIVATE);
        loadPrefs();
        initUI();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            initCamera();
        }
    }

    private void loadPrefs() {
        minShutter = prefs.getFloat("minShutter", 0.0001f);
        maxShutter = prefs.getFloat("maxShutter", 32.0f);
        minIso = prefs.getInt("minIso", 100);
        maxIso = prefs.getInt("maxIso", 6400);
        minFocus = prefs.getFloat("minFocus", 0.0f);
        maxFocus = prefs.getFloat("maxFocus", 10.0f);
        minWb = prefs.getInt("minWb", 2000);
        maxWb = prefs.getInt("maxWb", 10000);
        String uriStr = prefs.getString("saveFolderUri", null);
        if (uriStr != null) {
            try { saveFolderUri = Uri.parse(uriStr); } catch (Exception e) {}
        }
    }

    private void savePrefs() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putFloat("minShutter", minShutter);
        ed.putFloat("maxShutter", maxShutter);
        ed.putInt("minIso", minIso);
        ed.putInt("maxIso", maxIso);
        ed.putFloat("minFocus", minFocus);
        ed.putFloat("maxFocus", maxFocus);
        ed.putInt("minWb", minWb);
        ed.putInt("maxWb", maxWb);
        if (saveFolderUri != null) ed.putString("saveFolderUri", saveFolderUri.toString());
        ed.apply();
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
            float val = minShutter + (maxShutter - minShutter) * (p / 10000.0f);
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

        btnMode.setOnCheckedChangeListener((b, checked) -> {
            if (cameraController != null) cameraController.setAdjustmentMode(!checked);
            if (checked) {
                showFrameCountDialog();
            }
        });

        setLimitAction(valShutter, "Shutter Range", (min, max) -> { minShutter = min; maxShutter = max; savePrefs(); });
        setLimitAction(valIso, "ISO Range", (min, max) -> { minIso = (int)min; maxIso = (int)max; savePrefs(); });
        setLimitAction(valFocus, "Focus Range", (min, max) -> { minFocus = min; maxFocus = max; savePrefs(); });
        setLimitAction(valWb, "WB Range", (min, max) -> { minWb = (int)min; maxWb = (int)max; savePrefs(); });

        if (saveFolderUri != null) storageInfo.setText("FOLDER: OK");
    }

    private void showFrameCountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Continuous Frames");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(targetFrames));
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                targetFrames = Integer.parseInt(input.getText().toString());
            } catch (Exception e) {}
        });
        builder.show();
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

        valTxt.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Manual Value");
            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                try {
                    float val = Float.parseFloat(input.getText().toString());
                    updateCamParamsManual(valTxt.getId(), val);
                } catch (Exception e) {}
            });
            builder.show();
        });
    }

    private void updateCamParamsManual(int viewId, float val) {
        if (cameraController == null) return;
        long exp = (long) (Float.parseFloat(valShutter.getText().toString().replace("s", "")) * 1e9);
        int iso = Integer.parseInt(valIso.getText().toString());
        float focus = Float.parseFloat(valFocus.getText().toString().replace("INF", "0"));
        int wb = Integer.parseInt(valWb.getText().toString().replace("K", ""));

        if (viewId == valShutter.getId()) exp = (long)(val * 1e9);
        else if (viewId == valIso.getId()) iso = (int)val;
        else if (viewId == valFocus.getId()) focus = val;
        else if (viewId == valWb.getId()) wb = (int)val;

        cameraController.updateParams(exp, iso, focus, wb);
        if (colorEngine != null) colorEngine.setTemperature(wb);

        if (viewId == valShutter.getId()) valShutter.setText(String.format("%.4fs", val));
        else if (viewId == valIso.getId()) valIso.setText(String.valueOf((int)val));
        else if (viewId == valFocus.getId()) valFocus.setText(val == 0 ? "INF" : String.format("%.2f", val));
        else if (viewId == valWb.getId()) valWb.setText((int)val + "K");
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
        float s = minShutter + (maxShutter - minShutter) * (seekShutter.getProgress() / 10000.0f);
        int i = minIso + (int)((maxIso - minIso) * (seekIso.getProgress() / 1000.0f));
        float f = minFocus + (maxFocus - minFocus) * (seekFocus.getProgress() / 1000.0f);
        int w = minWb + (int)((maxWb - minWb) * (seekWb.getProgress() / 1000.0f));
        cameraController.updateParams((long)(s * 1e9), i, f, w);
        if (colorEngine != null) colorEngine.setTemperature(w);
    }

    private void startCapture() {
        if (saveFolderUri == null) {
            Toast.makeText(this, "Select folder first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!btnMode.isChecked()) {
            Toast.makeText(this, "Adjustment mode active. Switch to CONT for saving.", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;
        captureCount = 0;
        btnShoot.setText("STOP");
        progress.setVisibility(View.VISIBLE);
        progress.setMax(targetFrames);
        progress.setProgress(0);
        cameraController.takeStill();
    }

    private void stopCapture() {
        isRunning = false;
        runOnUiThread(() -> {
            btnShoot.setText("CAPTURE");
            progress.setVisibility(View.GONE);
            statusText.setText("DONE: " + captureCount + " frames");
        });
    }

    @Override
    public void onRawFrame(Image img, TotalCaptureResult result, boolean isPreview) {
        if (isPreview) {
            if (frameProcessor != null) frameProcessor.processFrame(img);
        } else {
            if (isRunning) {
                captureCount++;
                final int currentCount = captureCount;
                // Move save logic to background thread and wait for completion to avoid OOM
                saveHandler.post(() -> {
                    try {
                        saveDng(img, result);
                    } finally {
                        img.close(); // Explicitly close here
                    }

                    runOnUiThread(() -> {
                        progress.setProgress(currentCount);
                        statusText.setText("SAVED: " + currentCount + "/" + targetFrames);

                        // Trigger next frame ONLY after saving is done to avoid memory spikes
                        if (currentCount < targetFrames && isRunning) {
                            cameraController.takeStill();
                        } else if (currentCount >= targetFrames) {
                            stopCapture();
                        }
                    });
                });
            } else {
                img.close();
            }
        }
    }

    private void saveDng(Image img, TotalCaptureResult result) {
        try {
            String filename = "RAW_" + System.currentTimeMillis() + ".dng";
            Uri fileUri = DocumentsContract.createDocument(getContentResolver(), saveFolderUri, "image/x-adobe-dng", filename);
            if (fileUri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(fileUri)) {
                    DngCreator creator = new DngCreator(cameraController.getCharacteristics(), result);
                    creator.setOrientation(android.media.ExifInterface.ORIENTATION_NORMAL);
                    creator.writeImage(out, img);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Save failed", e);
        }
    }

    private void initCamera() {
        renderer = new Renderer(preview);
        colorEngine = new ColorEngine();
        renderer.setColorEngine(colorEngine);

        cameraController = new CameraController(this, preview, this);
        cameraController.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                CameraCharacteristics chars = cameraController.getCharacteristics();
                android.util.Size size = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                frameProcessor = new FrameProcessor(size.getWidth(), size.getHeight(), renderer);
                frameProcessor.setSensorLevels(64, 1023);
                updateCamParams();
            } catch (Exception e) {}
        }, 2000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();
            saveFolderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            storageInfo.setText("FOLDER: OK");
            savePrefs();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (cameraController != null) cameraController.stop();
        if (frameProcessor != null) frameProcessor.stop();
        if (saveThread != null) saveThread.quitSafely();
    }

    interface LimitCallback {
        void onLimitSet(float min, float max);
    }
}
