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
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends Activity {
    private static final String TAG = "A2LS_MainActivity";
    private CameraController cameraController;
    private FrameProcessor frameProcessor;
    private Renderer renderer;

    private TextView statusText;
    private TextView frameCounter;

    private TextView valExp, valIso, valFocus, valLimit, valTimer, logText;
    private Button btnMainAction;
    private android.widget.ScrollView logScroll;
    private android.widget.Spinner spinMethod;

    private long currentShutterNs = 1000000000L; // Default 1s for live view
    private int currentIso = 1600;
    private float currentFocus = 0.0f;
    private long currentFrameDurationNs = 1100000000L;
    private int frameLimit = 0; // 0 = INF
    private float currentRGain = 1.6f;
    private float currentGGain = 1.0f;
    private float currentBGain = 2.1f;
    private float currentTemp = 5000f;
    private float currentTint = 0f;
    private int currentBlackLevel = 64;
    private float stretchBlack = 0.0f;
    private float stretchMid = 0.5f;
    private float stretchWhite = 1.0f;
    private int currentHotAggression = 50;
    private int startTimerSec = 0;
    private boolean isStacking = false;
    private boolean isDestroyed = false;
    private boolean isZoomed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Global Exception Handler to catch and log crashes
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            Log.e(TAG, "CRASH: " + sw.toString());
            saveCrashLog(sw.toString());
            System.exit(1);
        });

        try {
            setContentView(R.layout.activity_main);
            initUI();
            checkPermissions();
        } catch (Throwable e) {
            Log.e(TAG, "CRITICAL INIT FAILURE", e);
            showDebugScreen(e);
        }
    }

    private void showDebugScreen(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText("CRITICAL ERROR DURING INITIALIZATION:\n\n" + stackTrace);
        tv.setTextColor(0xFFFF0000);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(12);
        sv.addView(tv);
        setContentView(sv);

        saveCrashLog(stackTrace);
    }

    private void saveCrashLog(String log) {
        try {
            File dir = getExternalFilesDir(null);
            File file = new File(dir, "crash_log.txt");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(log.getBytes());
            fos.close();
        } catch (IOException ignored) {}
    }

    public void applyFocus(float f) {
        currentFocus = f;
        runOnUiThread(() -> {
            valFocus.setText(f == 0 ? "INF" : String.format("%.2f", f));
            addLog("Focus Applied: " + String.format("%.2f", f));
        });
        updateCamera();
    }

    public void setFocusInternal(float f) {
        currentFocus = f;
        updateCamera();
        runOnUiThread(() -> {
             android.widget.SeekBar focusSlider = findViewById(R.id.focus_slider);
             if (cameraController != null) {
                 Float minFocus = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                 float maxFocus = (minFocus != null) ? minFocus : 10.0f;
                 focusSlider.setProgress((int)(f / maxFocus * 1000));
             }
        });
    }


    private void initUI() {
        TextureView preview = findViewById(R.id.preview);
        renderer = new Renderer(preview);

        statusText = findViewById(R.id.status_text);
        statusText.setText("LIVE VIEW");
        frameCounter = findViewById(R.id.frame_counter);

        valExp = findViewById(R.id.val_exp);
        valIso = findViewById(R.id.val_iso);
        valFocus = findViewById(R.id.val_focus);
        valLimit = findViewById(R.id.val_limit);
        valTimer = findViewById(R.id.val_timer);
        btnMainAction = findViewById(R.id.btn_main_action);

        android.widget.SeekBar focusSlider = findViewById(R.id.focus_slider);
        focusSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (cameraController == null) return;
                Float minFocus = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                float maxFocus = (minFocus != null) ? minFocus : 10.0f;
                float f = (progress / 1000.0f) * maxFocus;
                currentFocus = f;
                valFocus.setText(f == 0 ? "INF" : String.format("%.2f", f));
                updateCamera();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        focusSlider.setMax(1000);

        Button btnZoom = findViewById(R.id.btn_zoom);
        btnZoom.setOnClickListener(v -> {
            isZoomed = !isZoomed;
            if (renderer != null) renderer.setZoom(isZoomed);
            btnZoom.setBackgroundColor(isZoomed ? 0xAAFF0000 : 0x44FFFFFF);
            addLog("Focus Zoom: " + (isZoomed ? "ON" : "OFF"));
        });
        logText = findViewById(R.id.log_text);

        spinMethod = findViewById(R.id.spin_method);
        String[] methods = {"Mean Stacking", "Sum Stacking", "Sigma Clipping"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, methods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinMethod.setAdapter(adapter);
        spinMethod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (frameProcessor != null) frameProcessor.setStackMethod(position);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        logScroll = findViewById(R.id.log_scroll);
        if (logScroll != null) logScroll.setVisibility(View.VISIBLE);

        addLog("Application Initialized");
        addLog("Ready. Select target and Exposure.");

        findViewById(R.id.ctrl_exp).setOnClickListener(v -> {
            android.util.Range<Long> range = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            String hint = "1.0";
            if (range != null) hint = String.format("%.1f to %.1f", range.getLower()/1e9, range.getUpper()/1e9);
            final android.util.Range<Long> fRange = range;
            showEntryDialog("Exposure (seconds)", hint, s -> {
                try {
                    float sec = Float.parseFloat(s);
                    long ns = (long)(sec * 1000000000L);
                    if (fRange != null) {
                        if (ns < fRange.getLower()) ns = fRange.getLower();
                        if (ns > fRange.getUpper()) ns = fRange.getUpper();
                    }
                    currentShutterNs = ns;
                    valExp.setText(String.format("%.1fs", currentShutterNs / 1e9));
                    updateCamera();
                } catch (Exception e) {}
            });
        });

        findViewById(R.id.ctrl_iso).setOnClickListener(v -> {
            android.util.Range<Integer> range = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            String hint = "1600";
            if (range != null) hint = range.getLower() + " to " + range.getUpper();
            final android.util.Range<Integer> fRange = range;
            showEntryDialog("ISO", hint, s -> {
                try {
                    int iso = Integer.parseInt(s);
                    if (fRange != null) {
                        if (iso < fRange.getLower()) iso = fRange.getLower();
                        if (iso > fRange.getUpper()) iso = fRange.getUpper();
                    }
                    currentIso = iso;
                    valIso.setText(String.valueOf(currentIso));
                    updateCamera();
                } catch (Exception e) {}
            });
        });

        findViewById(R.id.ctrl_focus).setOnClickListener(v -> {
            Float minFocus = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            float maxFocus = (minFocus != null) ? minFocus : 10.0f;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Manual Focus");

            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);

            final TextView focusLbl = new TextView(this);
            focusLbl.setText("Current: " + (currentFocus == 0 ? "INF" : String.format("%.2f", currentFocus)));
            layout.addView(focusLbl);

            final android.widget.SeekBar focusBar = new android.widget.SeekBar(this);
            focusBar.setMax(1000);
            focusBar.setProgress((int)(currentFocus / maxFocus * 1000));
            focusBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(android.widget.SeekBar s, int p, boolean b) {
                    float f = (p / 1000.0f) * maxFocus;
                    currentFocus = f;
                    focusLbl.setText("Current: " + (f == 0 ? "INF" : String.format("%.2f", f)));
                    valFocus.setText(f == 0 ? "INF" : String.format("%.2f", f));
                    updateCamera();
                }
                public void onStartTrackingTouch(android.widget.SeekBar s) {}
                public void onStopTrackingTouch(android.widget.SeekBar s) {}
            });
            layout.addView(focusBar);

            builder.setView(layout);
            builder.setPositiveButton("OK", null);
            builder.show();
        });

        findViewById(R.id.ctrl_limit).setOnClickListener(v -> showEntryDialog("Frame Limit (0 for INF)", "20", s -> {
            try {
                int l = Integer.parseInt(s);
                frameLimit = Math.max(0, l);
                valLimit.setText(frameLimit == 0 ? "INF" : String.valueOf(frameLimit));
                if (frameProcessor != null) frameProcessor.setFrameLimit(frameLimit);
            } catch (Exception e) {}
        }));

        findViewById(R.id.ctrl_timer).setOnClickListener(v -> showEntryDialog("Start Timer (seconds)", "5", s -> {
            try {
                int l = Integer.parseInt(s);
                startTimerSec = Math.max(0, l);
                valTimer.setText(startTimerSec + "s");
            } catch (Exception e) {}
        }));

        findViewById(R.id.btn_tune).setOnClickListener(v -> showTuneDialog());


        findViewById(R.id.btn_dark).setOnClickListener(v -> {
            if (frameProcessor != null) {
                addLog("Capturing 30 Dark Frames...");
                frameProcessor.startDarkCalibration();
            }
        });

        btnMainAction.setOnClickListener(v -> {
            if (!isStacking) {
                if (startTimerSec > 0) {
                    startCountdown();
                } else {
                    startStacking();
                }
            } else {
                stopStacking();
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Export Options");

            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);

            final android.widget.CheckBox cbLinear = new android.widget.CheckBox(this); cbLinear.setText("Linear TIFF (16-bit)"); cbLinear.setChecked(true);
            final android.widget.CheckBox cbStretch = new android.widget.CheckBox(this); cbStretch.setText("Stretched TIFF (16-bit)"); cbStretch.setChecked(true);
            final android.widget.CheckBox cbPng = new android.widget.CheckBox(this); cbPng.setText("High-res PNG (Preview Style)"); cbPng.setChecked(true);
            final android.widget.CheckBox cbDng = new android.widget.CheckBox(this); cbDng.setText("RAW DNG (Single Frame)"); cbDng.setChecked(false);

            layout.addView(cbLinear);
            layout.addView(cbStretch);
            layout.addView(cbPng);
            layout.addView(cbDng);

            builder.setView(layout);
            builder.setPositiveButton("SAVE SELECTED", (dialog, which) -> {
                new Thread(() -> {
                    try {
                        if (cbLinear.isChecked()) saveResultSync(false);
                        if (cbStretch.isChecked()) saveResultSync(true);
                        if (cbPng.isChecked()) savePngOnlySync();
                        if (cbDng.isChecked()) saveDngSync();
                        runOnUiThread(() -> Toast.makeText(this, "Export Complete", Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        Log.e(TAG, "Sequential Export Failed", e);
                        addLog("Export Error: " + e.getMessage());
                    }
                }).start();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            if (frameProcessor != null) {
                android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
                b.setTitle("Clear Data");
                b.setItems(new String[]{"Clear Stack Only", "Clear Master Dark Only", "Clear Everything"}, (dialog, which) -> {
                    if (which == 0 || which == 2) {
                        frameProcessor.resetStack();
                        frameProcessor.setState(FrameProcessor.State.LIVE);
                        isStacking = false;
                        runOnUiThread(() -> {
                            btnMainAction.setText("START");
                            btnMainAction.setBackgroundColor(0xFFFF4444);
                            statusText.setText("LIVE VIEW");
                            frameCounter.setText("0 Frames");
                        });
                        addLog("Stack Cleared");
                    }
                    if (which == 1 || which == 2) {
                        frameProcessor.clearMasterDark();
                        addLog("Master Dark Cleared");
                    }
                    Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show();
                });
                b.show();
            }
        });
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        };

        boolean granted = true;
        for (String p : permissions) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        if (!granted) {
            requestPermissions(permissions, 1);
        } else {
            initCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
        }
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

    private void showTuneDialog() {
        if (isStacking) {
            Toast.makeText(this, "Pause stacking to tune", Toast.LENGTH_SHORT).show();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tune Studio");

        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(this);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);

        android.widget.HorizontalScrollView tabScroll = new android.widget.HorizontalScrollView(this);
        android.widget.LinearLayout tabLayout = new android.widget.LinearLayout(this);
        tabLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        tabScroll.addView(tabLayout);
        mainLayout.addView(tabScroll);

        android.widget.FrameLayout contentFrame = new android.widget.FrameLayout(this);
        contentFrame.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        mainLayout.addView(contentFrame);

        // Sections
        android.widget.LinearLayout stretchLayout = new android.widget.LinearLayout(this);
        stretchLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        stretchLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        stretchLayout.setPadding(30, 10, 30, 10);

        android.widget.LinearLayout colorLayout = new android.widget.LinearLayout(this);
        colorLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        colorLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        colorLayout.setPadding(30, 10, 30, 10);
        colorLayout.setVisibility(View.GONE);

        contentFrame.addView(stretchLayout);
        contentFrame.addView(colorLayout);

        // Tab Buttons
        Button btnStretch = new Button(this); btnStretch.setText("STRETCH"); tabLayout.addView(btnStretch);
        Button btnColor = new Button(this); btnColor.setText("COLOR"); tabLayout.addView(btnColor);

        btnStretch.setOnClickListener(v -> { stretchLayout.setVisibility(View.VISIBLE); colorLayout.setVisibility(View.GONE); });
        btnColor.setOnClickListener(v -> { stretchLayout.setVisibility(View.GONE); colorLayout.setVisibility(View.VISIBLE); });

        // Stretch Content
        HistogramView histView = new HistogramView(this);
        histView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 200));
        if (renderer != null) histView.setData(renderer.getHistogram());
        histView.setMarkers(stretchBlack, stretchMid, stretchWhite);
        stretchLayout.addView(histView);

        final TextView blackLbl = createLabel("Black: " + stretchBlack); stretchLayout.addView(blackLbl);
        final android.widget.SeekBar blackBar = new android.widget.SeekBar(this); blackBar.setMax(100); blackBar.setProgress((int)(stretchBlack*100)); stretchLayout.addView(blackBar);

        final TextView midLbl = createLabel("Mid (Gamma): " + stretchMid); stretchLayout.addView(midLbl);
        final android.widget.SeekBar midBar = new android.widget.SeekBar(this); midBar.setMax(100); midBar.setProgress((int)(stretchMid*100)); stretchLayout.addView(midBar);

        final TextView whiteLbl = createLabel("White: " + stretchWhite); stretchLayout.addView(whiteLbl);
        final android.widget.SeekBar whiteBar = new android.widget.SeekBar(this); whiteBar.setMax(100); whiteBar.setProgress((int)(stretchWhite*100)); stretchLayout.addView(whiteBar);

        final TextView hotLbl = createLabel("Hot Pixel Aggression: " + currentHotAggression); stretchLayout.addView(hotLbl);
        final android.widget.SeekBar hotBar = new android.widget.SeekBar(this); hotBar.setMax(100); hotBar.setProgress(currentHotAggression); stretchLayout.addView(hotBar);
        hotBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int p, boolean b) {
                hotLbl.setText("Hot Pixel Aggression: " + p);
                currentHotAggression = p;
                if (frameProcessor != null) frameProcessor.setHotAggression(p);
            }
            public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });

        // Color Content
        final TextView tempLbl = createLabel("Temp: " + (int)currentTemp + "K"); colorLayout.addView(tempLbl);
        final android.widget.SeekBar tempBar = new android.widget.SeekBar(this); tempBar.setMax(100); tempBar.setProgress((int)((currentTemp-2000)/80)); colorLayout.addView(tempBar);

        final TextView tintLbl = createLabel("Tint: " + (int)currentTint); colorLayout.addView(tintLbl);
        final android.widget.SeekBar tintBar = new android.widget.SeekBar(this); tintBar.setMax(200); tintBar.setProgress((int)currentTint + 100); colorLayout.addView(tintBar);

        colorLayout.addView(createLabel("Manual Gains (Optional):"));
        android.widget.LinearLayout gainsRow = new android.widget.LinearLayout(this);
        final EditText rGain = new EditText(this); rGain.setText(String.format("%.2f", currentRGain)); rGain.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1));
        final EditText gGain = new EditText(this); gGain.setText(String.format("%.2f", currentGGain)); gGain.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1));
        final EditText bGain = new EditText(this); bGain.setText(String.format("%.2f", currentBGain)); bGain.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1));
        gainsRow.addView(rGain); gainsRow.addView(gGain); gainsRow.addView(bGain);
        colorLayout.addView(gainsRow);

        colorLayout.addView(createLabel("Black Level:"));
        final EditText bLevel = new EditText(this); bLevel.setText(String.valueOf(currentBlackLevel)); bLevel.setInputType(InputType.TYPE_CLASS_NUMBER);
        colorLayout.addView(bLevel);

        // Preview interaction
        android.widget.SeekBar.OnSeekBarChangeListener listener = new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int p, boolean b) {
                float bl = blackBar.getProgress()/100f;
                float mi = midBar.getProgress()/100f;
                float wh = whiteBar.getProgress()/100f;
                blackLbl.setText("Black: " + String.format("%.2f", bl));
                midLbl.setText("Mid: " + String.format("%.2f", mi));
                whiteLbl.setText("White: " + String.format("%.2f", wh));
                histView.setMarkers(bl, mi, wh);
                if (renderer != null) renderer.setStretch(bl, mi, wh);
            }
            public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {}
        };
        blackBar.setOnSeekBarChangeListener(listener);
        midBar.setOnSeekBarChangeListener(listener);
        whiteBar.setOnSeekBarChangeListener(listener);

        tempBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int p, boolean b) {
                float t = 2000 + p * 80;
                tempLbl.setText("Temp: " + (int)t + "K");
                if (renderer != null) renderer.setTempTint(t, tintBar.getProgress() - 100);
            }
            public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });

        tintBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int p, boolean b) {
                int t = p - 100;
                tintLbl.setText("Tint: " + t);
                if (renderer != null) renderer.setTempTint(2000 + tempBar.getProgress() * 80, t);
            }
            public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });

        builder.setView(mainLayout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                currentTemp = 2000 + tempBar.getProgress() * 80;
                currentTint = tintBar.getProgress() - 100;
                currentRGain = Float.parseFloat(rGain.getText().toString());
                currentGGain = Float.parseFloat(gGain.getText().toString());
                currentBGain = Float.parseFloat(bGain.getText().toString());
                currentBlackLevel = Integer.parseInt(bLevel.getText().toString());
                stretchBlack = blackBar.getProgress() / 100.0f;
                stretchMid = midBar.getProgress() / 100.0f;
                stretchWhite = whiteBar.getProgress() / 100.0f;
                if (renderer != null) {
                    renderer.setTempTint(currentTemp, currentTint);
                    // If gains were manually edited, they take priority
                    float rg = Float.parseFloat(rGain.getText().toString());
                    float gg = Float.parseFloat(gGain.getText().toString());
                    float bg = Float.parseFloat(bGain.getText().toString());
                    if (Math.abs(rg - currentRGain) > 0.01 || Math.abs(gg - currentGGain) > 0.01 || Math.abs(bg - currentBGain) > 0.01) {
                         renderer.setWbGains(rg, gg, bg);
                         currentRGain = rg; currentGGain = gg; currentBGain = bg;
                    } else {
                         ColorEngine.Params cp = renderer.getColorParams();
                         currentRGain = cp.rGain; currentGGain = cp.gGain; currentBGain = cp.bGain;
                    }

                    renderer.setBlackLevel(currentBlackLevel);
                    renderer.setStretch(stretchBlack, stretchMid, stretchWhite);
                }
            } catch (Exception e) {}
        });
        builder.setNegativeButton("Reset", (dialog, which) -> {
            stretchBlack = 0.0f; stretchMid = 0.5f; stretchWhite = 1.0f;
            if (renderer != null) {
                renderer.setAutoStretch(true);
            }
        });
        builder.setNeutralButton("Auto WB", (dialog, which) -> {
            autoWb();
        });
        builder.setNegativeButton("Auto Stretch", (dialog, which) -> {
            if (renderer != null) renderer.setAutoStretch(true);
        });
        builder.show();
    }

    private void autoWb() {
        if (cameraController == null || renderer == null) return;
        android.hardware.camera2.TotalCaptureResult res = cameraController.getLastCaptureResult();
        if (res != null) {
            android.hardware.camera2.params.RggbChannelVector gains = res.get(android.hardware.camera2.CaptureResult.COLOR_CORRECTION_GAINS);
            if (gains != null) {
                currentRGain = gains.getRed();
                currentGGain = (gains.getGreenEven() + gains.getGreenOdd()) / 2.0f;
                currentBGain = gains.getBlue();
                // Normalize to G=1.0
                currentRGain /= currentGGain;
                currentBGain /= currentGGain;
                currentGGain = 1.0f;
                renderer.setWbGains(currentRGain, currentGGain, currentBGain);
                addLog("Auto WB Applied: R=" + String.format("%.2f", currentRGain) + " B=" + String.format("%.2f", currentBGain));
                Toast.makeText(this, "Auto WB Applied", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No capture result yet", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(0, 10, 0, 0);
        tv.setTextColor(0xFFCCCCCC);
        return tv;
    }


    public void addLog(String msg) {
        runOnUiThread(() -> {
            if (logText != null) {
                logText.append("\n" + msg);
                if (logScroll != null) {
                    logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    private void startCountdown() {
        btnMainAction.setEnabled(false);
        new Thread(() -> {
            for (int i = startTimerSec; i > 0; i--) {
                final int remaining = i;
                runOnUiThread(() -> {
                    statusText.setText("STARTING IN " + remaining + "s");
                    addLog("Timer: " + remaining + "...");
                });
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            }
            runOnUiThread(() -> {
                btnMainAction.setEnabled(true);
                startStacking();
            });
        }).start();
    }

    private void startStacking() {
        if (frameProcessor != null) {
            frameProcessor.setStackMethod(spinMethod.getSelectedItemPosition());
            frameProcessor.setFrameLimit(frameLimit);
            frameProcessor.setState(FrameProcessor.State.STACKING);
            isStacking = true;
            runOnUiThread(() -> {
                btnMainAction.setText("STOP");
                btnMainAction.setBackgroundColor(0xFF444444);
                statusText.setText("STACKING...");
                addLog("Stacking Started");
            });
        }
    }

    private void stopStacking() {
        if (frameProcessor != null) {
            frameProcessor.setState(FrameProcessor.State.PAUSED);
            isStacking = false;
            btnMainAction.setText("START");
            btnMainAction.setBackgroundColor(0xFFFF4444);
            statusText.setText("PAUSED (STACK)");
            addLog("Stacking Paused");
        }
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        if (cameraController != null) cameraController.close();
        super.onDestroy();
    }

    private void updateCamera() {
        if (cameraController != null) {
            cameraController.updateParams(currentShutterNs, currentIso, currentFocus, currentFrameDurationNs);
        }
    }

    private void initCamera() {
        new Thread(() -> {
            try {
                cameraController = new CameraController(this, null);
                cameraController.start();
                long waitStart = System.currentTimeMillis();
                while (cameraController.getRawSize() == null && System.currentTimeMillis() - waitStart < 5000) {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }

                final android.util.Size size = cameraController.getRawSize();
                final android.hardware.camera2.CameraCharacteristics chars = cameraController.getCharacteristics();
                if (size != null && chars != null) {
                    android.graphics.Rect activeRect = chars.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    addLog("RAW Size: " + size.getWidth() + "x" + size.getHeight());
                    if (activeRect != null) {
                        addLog("Active Array: " + activeRect.width() + "x" + activeRect.height());
                    }

                    runOnUiThread(() -> {
                        // Extract sensor black/white levels
                        android.hardware.camera2.params.BlackLevelPattern blp = chars.get(android.hardware.camera2.CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);
                        if (blp != null) {
                            currentBlackLevel = blp.getOffsetForIndex(0, 0);
                            if (renderer != null) renderer.setBlackLevel(currentBlackLevel);
                        }
                        Integer wl = chars.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);
                        if (wl != null) {
                            if (renderer != null) renderer.setWhiteLevel(wl.floatValue());
                        }
                        Integer orientation = chars.get(android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION);
                        if (orientation != null) {
                            if (renderer != null) renderer.setSensorOrientation(orientation);
                        }
                        Integer cfa = chars.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
                        if (cfa != null) {
                            if (renderer != null) renderer.setCfaPattern(cfa);
                        }

                        frameProcessor = new FrameProcessor(size.getWidth(), size.getHeight(), renderer);
                        if (cfa != null) frameProcessor.setCfaPattern(cfa);
                        frameProcessor.setState(FrameProcessor.State.LIVE);
                        cameraController.setFrameProcessor(frameProcessor);
                        updateCamera();
                        startProgressThread();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Camera init timeout", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "initCamera background thread failed", e);
            }
        }).start();
    }

    private void startProgressThread() {
        new Thread(() -> {
            int lastCount = -1;
            int lastDarkCount = -1;
            FrameProcessor.State lastState = null;

            while (!isDestroyed) {
                if (frameProcessor != null) {
                    final int count = frameProcessor.getFrameCount();
                    final int darkCount = frameProcessor.getDarkCount();
                    final FrameProcessor.State state = frameProcessor.getState();

                    if (count != lastCount || darkCount != lastDarkCount || state != lastState) {
                        lastCount = count;
                        lastDarkCount = darkCount;
                        lastState = state;

                        final double nr = Math.sqrt(count);
                        runOnUiThread(() -> {
                            frameCounter.setText(count + " Frames (NR: " + String.format("%.1fx", nr) + ")");

                            if (state == FrameProcessor.State.CALIBRATING_DARK) {
                                statusText.setText("CALIBRATING DARKS (" + frameProcessor.getDarkCount() + "/30)");
                            } else if (state == FrameProcessor.State.STACKING) {
                                statusText.setText("STACKING");
                                if (frameLimit > 0 && count >= frameLimit) {
                                    stopStacking();
                                }
                            } else if (state == FrameProcessor.State.PAUSED) {
                                statusText.setText("PAUSED");
                            } else if (state == FrameProcessor.State.LIVE) {
                                statusText.setText("LIVE VIEW");
                            }
                        });

                        if (count > 0 && state == FrameProcessor.State.STACKING) {
                            addLog("Frame " + count + " stacked");
                        }
                    }
                }

                for (int i = 0; i <= 100; i += 2) {
                    if (!isStacking || isDestroyed) break;
                    try { Thread.sleep(Math.max(10, currentShutterNs / 50000000)); } catch (Exception e) {}
                }
            }
        }).start();
    }

    private void savePngOnlySync() throws IOException {
        if (frameProcessor == null || cameraController == null || renderer == null) return;
        final float[] buffer = frameProcessor.getResultBuffer();
        if (buffer == null) return;
        final int w = cameraController.getRawSize().getWidth();
        final int h = cameraController.getRawSize().getHeight();
        final boolean isSum = spinMethod.getSelectedItemPosition() == 1;
        final int frameCount = frameProcessor.getFrameCount();
        final ColorEngine.Params params = renderer.getColorParams();

        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File path = new File(pictures, "A2LS_Astro");
        if (!path.exists()) path.mkdirs();
        String ts = String.valueOf(System.currentTimeMillis());
        File pngFile = new File(path, "A2LS_Stack_" + ts + ".png");

        int orientation = 90;
        Integer ori = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION);
        if (ori != null) orientation = ori;

        savePngOptimized(pngFile, buffer, w, h, frameCount, isSum, params, orientation);
        addLog("Saved: " + pngFile.getName());
    }

    private void saveResultSync(boolean stretched) throws IOException {
        if (frameProcessor == null || cameraController == null || renderer == null) return;
        final float[] buffer = frameProcessor.getResultBuffer();
        if (buffer == null) return;

        final int w = cameraController.getRawSize().getWidth();
        final int h = cameraController.getRawSize().getHeight();
        final boolean isSum = spinMethod.getSelectedItemPosition() == 1;
        final int frameCount = frameProcessor.getFrameCount();

        final ColorEngine.Params params = renderer.getColorParams();

        int cfa = 0;
        Integer cfaInt = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
        if (cfaInt != null) cfa = cfaInt;

        int orientation = 90;
        Integer ori = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION);
        if (ori != null) orientation = ori;

        addLog("Saving 16-bit TIFF (" + (stretched ? "Stretched" : "Linear") + ")...");

        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File path = new File(pictures, "A2LS_Astro");
        if (!path.exists()) path.mkdirs();

        String ts = String.valueOf(System.currentTimeMillis());
        File tiffFile = new File(path, "A2LS_Stack_" + (stretched ? "Stretched_" : "") + ts + ".tiff");

        boolean usingDarks = frameProcessor.hasMasterDark();
        float effectiveBlack = usingDarks ? 0 : (currentBlackLevel + stretchBlack * 1024) * (isSum ? frameCount : 1);
        int tiffBlack = usingDarks ? 0 : currentBlackLevel;

        if (stretched) {
            TiffWriter.saveTiff16Stretched(tiffFile.getAbsolutePath(), buffer, w, h, currentRGain, currentGGain, currentBGain, effectiveBlack, params, cfa, orientation);
        } else {
            TiffWriter.saveTiff16Color(tiffFile.getAbsolutePath(), buffer, w, h, currentRGain, currentGGain, currentBGain, tiffBlack, frameCount, isSum, cfa, orientation);
        }

        addLog("Saved: " + tiffFile.getName());
    }

    private void saveDngSync() throws IOException {
        if (frameProcessor == null || cameraController == null) return;
        short[] raw = frameProcessor.getRawBuffer();
        if (raw == null) return;

        android.util.Size size = cameraController.getRawSize();
        android.hardware.camera2.CameraCharacteristics chars = cameraController.getCharacteristics();
        android.hardware.camera2.TotalCaptureResult result = cameraController.getLastCaptureResult();

        if (size == null || chars == null || result == null) {
            addLog("DNG Export Error: Missing metadata");
            return;
        }

        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File path = new File(pictures, "A2LS_Astro");
        if (!path.exists()) path.mkdirs();
        File dngFile = new File(path, "A2LS_Capture_" + System.currentTimeMillis() + ".dng");

        try (android.hardware.camera2.DngCreator dngCreator = new android.hardware.camera2.DngCreator(chars, result)) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(raw.length * 2);
            buffer.order(java.nio.ByteOrder.nativeOrder());
            buffer.asShortBuffer().put(raw);
            dngCreator.writeByteBuffer(new FileOutputStream(dngFile), size, buffer, 0);
            addLog("Saved: " + dngFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "DNG Write Failed", e);
            addLog("DNG Error: " + e.getMessage());
        }
    }

    private void savePngOptimized(File file, float[] buffer, int w, int h, int frameCount, boolean isSum, ColorEngine.Params params, int orientation) throws IOException {
        // Handle orientation by potentially swapping dimensions
        int outW = (orientation == 90 || orientation == 270) ? h : w;
        int outH = (orientation == 90 || orientation == 270) ? w : h;

        Bitmap bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        int[] argbArray = new int[outW * outH];
        boolean usingDarks = frameProcessor.hasMasterDark();
        float effectiveBlack = usingDarks ? 0 : (isSum ? (currentBlackLevel * frameCount) : currentBlackLevel);

        int cfa = 0;
        if (cameraController != null) {
            Integer cfaInt = cameraController.getCharacteristics().get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
            if (cfaInt != null) cfa = cfaInt;
        }

        // Process in 2x2 blocks from Bayer buffer for speed and correctness
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int idx = y * w + x;
                float v00 = buffer[idx];
                float v01 = buffer[idx + 1];
                float v10 = buffer[idx + w];
                float v11 = buffer[idx + w + 1];

                float r, g, b;
                switch(cfa) {
                    case 1: r=v01; g=(v00+v11)/2f; b=v10; break;
                    case 2: r=v10; g=(v00+v11)/2f; b=v01; break;
                    case 3: r=v11; g=(v01+v10)/2f; b=v00; break;
                    default: r=v00; g=(v01+v10)/2f; b=v11; break;
                }

                int argb = ColorEngine.processPixel(r, g, g, b, params, effectiveBlack);

                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int srcX = x + dx;
                        int srcY = y + dy;

                        int destX, destY;
                        if (orientation == 90) {
                            destX = (h - 1) - srcY;
                            destY = srcX;
                        } else if (orientation == 270) {
                            destX = srcY;
                            destY = (w - 1) - srcX;
                        } else if (orientation == 180) {
                            destX = (w - 1) - srcX;
                            destY = (h - 1) - srcY;
                        } else {
                            destX = srcX;
                            destY = srcY;
                        }

                        if (destX >= 0 && destX < outW && destY >= 0 && destY < outH) {
                            argbArray[destY * outW + destX] = argb;
                        }
                    }
                }
            }
        }
        bitmap.setPixels(argbArray, 0, outW, 0, 0, outW, outH);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
}
