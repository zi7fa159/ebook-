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
    private android.view.View captureProgress;

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
    private int currentBlackLevel = 64;
    private float stretchBlack = 0.0f;
    private float stretchMid = 0.5f;
    private float stretchWhite = 1.0f;
    private int currentHotAggression = 50;
    private int startTimerSec = 0;
    private boolean isStacking = false;
    private boolean isDestroyed = false;

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

    private void initUI() {
        TextureView preview = findViewById(R.id.preview);
        renderer = new Renderer(preview);

        statusText = findViewById(R.id.status_text);
        statusText.setText("LIVE VIEW");
        frameCounter = findViewById(R.id.frame_counter);
        captureProgress = findViewById(R.id.capture_progress);

        valExp = findViewById(R.id.val_exp);
        valIso = findViewById(R.id.val_iso);
        valFocus = findViewById(R.id.val_focus);
        valLimit = findViewById(R.id.val_limit);
        valTimer = findViewById(R.id.val_timer);
        btnMainAction = findViewById(R.id.btn_main_action);
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
            String hint = "0.0";
            if (minFocus != null) hint = "0.0 to " + minFocus;
            final Float fMinFocus = minFocus;
            showEntryDialog("Focus (0=INF, Higher=Macro)", hint, s -> {
                try {
                    float f = Float.parseFloat(s);
                    if (f < 0) f = 0;
                    if (fMinFocus != null && f > fMinFocus) f = fMinFocus;
                    currentFocus = f;
                    valFocus.setText(f == 0 ? "INF" : String.format("%.1f", f));
                    updateCamera();
                } catch (Exception e) {}
            });
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
            final android.widget.CheckBox cbDng = new android.widget.CheckBox(this); cbDng.setText("RAW DNG (Reference Frame)"); cbDng.setChecked(false);
            final android.widget.CheckBox cbPng = new android.widget.CheckBox(this); cbPng.setText("High-res PNG (Preview Style)"); cbPng.setChecked(true);

            layout.addView(cbLinear);
            layout.addView(cbStretch);
            layout.addView(cbDng);
            layout.addView(cbPng);

            builder.setView(layout);
            builder.setPositiveButton("SAVE SELECTED", (dialog, which) -> {
                if (cbLinear.isChecked()) saveResult(false);
                if (cbStretch.isChecked()) saveResult(true);
                if (cbDng.isChecked()) {
                    File pictures = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                    File path = new File(pictures, "A2LS_Astro");
                    if (!path.exists()) path.mkdirs();
                    saveDng(new File(path, "A2LS_Raw_" + System.currentTimeMillis() + ".dng"));
                }
                if (cbPng.isChecked() && !cbLinear.isChecked()) {
                    savePngOnly();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            if (frameProcessor != null) {
                frameProcessor.resetStack();
                frameProcessor.setState(FrameProcessor.State.LIVE);
                isStacking = false;
                runOnUiThread(() -> {
                    btnMainAction.setText("START");
                    btnMainAction.setBackgroundColor(0xFFFF4444);
                    statusText.setText("LIVE VIEW");
                    frameCounter.setText("0 Frames");
                });
                addLog("Stack Cleared & Live Reset");
                Toast.makeText(this, "Stack cleared", Toast.LENGTH_SHORT).show();
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
        mainLayout.addView(contentFrame);

        // Sections
        android.widget.LinearLayout stretchLayout = new android.widget.LinearLayout(this);
        stretchLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        stretchLayout.setPadding(30, 10, 30, 10);

        android.widget.LinearLayout colorLayout = new android.widget.LinearLayout(this);
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

        // Color Content
        colorLayout.addView(createLabel("Red Gain:"));
        final EditText rGain = new EditText(this); rGain.setText(String.valueOf(currentRGain)); rGain.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        colorLayout.addView(rGain);

        colorLayout.addView(createLabel("Green Gain:"));
        final EditText gGain = new EditText(this); gGain.setText(String.valueOf(currentGGain)); gGain.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        colorLayout.addView(gGain);

        colorLayout.addView(createLabel("Blue Gain:"));
        final EditText bGain = new EditText(this); bGain.setText(String.valueOf(currentBGain)); bGain.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        colorLayout.addView(bGain);

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

        builder.setView(mainLayout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                currentRGain = Float.parseFloat(rGain.getText().toString());
                currentGGain = Float.parseFloat(gGain.getText().toString());
                currentBGain = Float.parseFloat(bGain.getText().toString());
                currentBlackLevel = Integer.parseInt(bLevel.getText().toString());
                stretchBlack = blackBar.getProgress() / 100.0f;
                stretchMid = midBar.getProgress() / 100.0f;
                stretchWhite = whiteBar.getProgress() / 100.0f;
                if (renderer != null) {
                    renderer.setWbGains(currentRGain, currentGGain, currentBGain);
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
        builder.setNeutralButton("Auto", (dialog, which) -> {
            if (renderer != null) renderer.setAutoStretch(true);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(0, 10, 0, 0);
        tv.setTextColor(0xFFCCCCCC);
        return tv;
    }


    private void addLog(String msg) {
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
            startProgressThread();
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
                        frameProcessor.setState(FrameProcessor.State.LIVE);
                        cameraController.setFrameProcessor(frameProcessor);
                        updateCamera();
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
            while (isStacking && !isDestroyed) {
                if (frameProcessor != null) {
                    int count = frameProcessor.getFrameCount();
                    if (count != lastCount) {
                        lastCount = count;
                        final int c = count;
                        runOnUiThread(() -> {
                            frameCounter.setText(c + " Frames");
                            if (frameLimit > 0 && c >= frameLimit) {
                                stopStacking();
                            }
                        });
                        addLog("Frame " + c + " stacked");
                    }
                }

                for (int i = 0; i <= 100; i += 2) {
                    if (!isStacking || isDestroyed) break;
                    final int p = i;
                    runOnUiThread(() -> {
                        android.view.ViewGroup.LayoutParams lp = captureProgress.getLayoutParams();
                        if (lp != null) {
                            lp.width = (captureProgress.getRootView().getWidth() * p) / 100;
                            captureProgress.setLayoutParams(lp);
                        }
                    });
                    try { Thread.sleep(Math.max(10, currentShutterNs / 50000000)); } catch (Exception e) {}
                }
            }
            // Reset progress bar on exit
            runOnUiThread(() -> {
                android.view.ViewGroup.LayoutParams lp = captureProgress.getLayoutParams();
                if (lp != null) {
                    lp.width = 0;
                    captureProgress.setLayoutParams(lp);
                }
            });
        }).start();
    }

    private void savePngOnly() {
        if (frameProcessor == null || cameraController == null || renderer == null) return;
        final float[] buffer = frameProcessor.getResultBuffer();
        if (buffer == null) return;
        final int w = cameraController.getRawSize().getWidth();
        final int h = cameraController.getRawSize().getHeight();
        final boolean isSum = spinMethod.getSelectedItemPosition() == 1;
        final int frameCount = frameProcessor.getFrameCount();
        final StretchParams params = renderer.getCurrentParams();

        new Thread(() -> {
            try {
                File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File path = new File(pictures, "A2LS_Astro");
                if (!path.exists()) path.mkdirs();
                String ts = String.valueOf(System.currentTimeMillis());
                File pngFile = new File(path, "A2LS_Stack_" + ts + ".png");
                savePngOptimized(pngFile, buffer, w, h, frameCount, isSum, params);
                addLog("Saved: " + pngFile.getName());
            } catch (Exception e) {}
        }).start();
    }

    private void saveResult(boolean stretched) {
        if (frameProcessor == null || cameraController == null || renderer == null) return;
        final float[] buffer = frameProcessor.getResultBuffer();
        if (buffer == null) return;

        final int w = cameraController.getRawSize().getWidth();
        final int h = cameraController.getRawSize().getHeight();
        final boolean isSum = spinMethod.getSelectedItemPosition() == 1;
        final int frameCount = frameProcessor.getFrameCount();
        final StretchParams params = renderer.getCurrentParams();

        addLog("Saving 16-bit TIFF (" + (stretched ? "Stretched" : "Linear") + ")...");

        new Thread(() -> {
            try {
                File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File path = new File(pictures, "A2LS_Astro");
                if (!path.exists()) path.mkdirs();

                String ts = String.valueOf(System.currentTimeMillis());
                File tiffFile = new File(path, "A2LS_Stack_" + (stretched ? "Stretched_" : "") + ts + ".tiff");

                float effectiveBlack = (currentBlackLevel + stretchBlack * 1024) * (isSum ? frameCount : 1);

                if (stretched) {
                    TiffWriter.saveTiff16Stretched(tiffFile.getAbsolutePath(), buffer, w, h, currentRGain, currentGGain, currentBGain, effectiveBlack, params);
                } else {
                    TiffWriter.saveTiff16Color(tiffFile.getAbsolutePath(), buffer, w, h, currentRGain, currentGGain, currentBGain, currentBlackLevel, frameCount, isSum);
                }

                addLog("Saved: " + tiffFile.getName());
                runOnUiThread(() -> Toast.makeText(this, "Saved to Pictures/A2LS_Astro", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                addLog("Save failed: " + e.getMessage());
                Log.e(TAG, "Save failed", e);
            }
        }).start();
    }

    private void saveDng(File file) {
        if (frameProcessor == null || cameraController == null) return;
        Image img = frameProcessor.getLastImage();
        android.hardware.camera2.TotalCaptureResult res = cameraController.getLastCaptureResult();
        android.hardware.camera2.CameraCharacteristics charac = cameraController.getCharacteristics();
        if (img == null || res == null || charac == null) {
            addLog("DNG Save: Metadata or Image missing");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(file);
             android.hardware.camera2.DngCreator dngCreator = new android.hardware.camera2.DngCreator(charac, res)) {
            dngCreator.writeImage(out, img);
        } catch (IOException e) {
            Log.e(TAG, "DNG save failed", e);
        }
    }

    private void savePngOptimized(File file, float[] buffer, int w, int h, int frameCount, boolean isSum, StretchParams params) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] rowPixels = new int[w];
        float effectiveBlack = isSum ? (currentBlackLevel * frameCount) : currentBlackLevel;
        float whiteClip = (1023.0f - currentBlackLevel) * (isSum ? frameCount : 1.0f) * 0.95f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int bx = (x / 2) * 2;
                int by = (y / 2) * 2;
                float r = (buffer[by * w + bx] - effectiveBlack) * currentRGain;
                float g = ((buffer[by * w + (bx + 1)] + buffer[(by + 1) * w + bx]) / 2.0f - effectiveBlack) * currentGGain;
                float b = (buffer[(by + 1) * w + (bx + 1)] - effectiveBlack) * currentBGain;

                // Purple Highlight Fix
                if (r > whiteClip || g > whiteClip || b > whiteClip) {
                    float m = Math.max(r, Math.max(g, b));
                    if (m > whiteClip) { r = g = b = m; }
                }

                // Apply Stretch
                r = (r - params.blackOffset) * params.scale;
                g = (g - params.blackOffset) * params.scale;
                b = (b - params.blackOffset) * params.scale;

                int ri, gi, bi;
                if (params.useAuto) {
                    ri = (int) (Math.sqrt(Math.max(0, r) / 255.0) * 255.0);
                    gi = (int) (Math.sqrt(Math.max(0, g) / 255.0) * 255.0);
                    bi = (int) (Math.sqrt(Math.max(0, b) / 255.0) * 255.0);
                } else {
                    ri = (int) (Math.pow(Math.max(0, r/255f), params.midFactor) * 255);
                    gi = (int) (Math.pow(Math.max(0, g/255f), params.midFactor) * 255);
                    bi = (int) (Math.pow(Math.max(0, b/255f), params.midFactor) * 255);
                }

                ri = Math.min(255, Math.max(0, ri));
                gi = Math.min(255, Math.max(0, gi));
                bi = Math.min(255, Math.max(0, bi));
                rowPixels[x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
            bitmap.setPixels(rowPixels, 0, w, 0, y, w, 1);
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out);
        }
        bitmap.recycle();
    }
}
