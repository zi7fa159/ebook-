package com.alsclone.astrostacker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import java.util.Arrays;

public class CameraController {
    private static final String TAG = "CameraController";
    private final Context context;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader rawImageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private long exposureTimeNs = 1000000000L;
    private int iso = 800;
    private float focusDistance = 0.0f;
    private long frameDurationNs = 1100000000L;
    private String cameraId;
    private Size rawSize;
    private Size pixelArraySize;
    private android.graphics.Rect activeArraySize;
    private CameraCharacteristics characteristics;
    private TotalCaptureResult lastCaptureResult;

    private volatile FrameProcessor frameProcessor;

    public CameraController(Context context, FrameProcessor frameProcessor) {
        this.context = context;
        this.frameProcessor = frameProcessor;
    }

    public void setFrameProcessor(FrameProcessor frameProcessor) {
        this.frameProcessor = frameProcessor;
    }

    public void start() {
        if (backgroundThread != null) return;
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        openCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] ids = manager.getCameraIdList();
            if (ids.length == 0) {
                Log.e(TAG, "No cameras found");
                return;
            }
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    this.cameraId = id;
                    this.characteristics = characteristics;

                    this.pixelArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    this.activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                    // Log sensor info for color calibration
                    Integer cfa = characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
                    BlackLevelPattern blp = characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);
                    Integer wl = characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);
                    Log.i(TAG, "Sensor CFA: " + cfa); // 0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR
                    Log.i(TAG, "Sensor Black Level Pattern: " + (blp != null ? blp.toString() : "null"));
                    Log.i(TAG, "Sensor White Level: " + wl);
                    Log.i(TAG, "Sensor Pixel Array: " + (pixelArraySize != null ? pixelArraySize.toString() : "null"));
                    Log.i(TAG, "Sensor Active Array: " + (activeArraySize != null ? activeArraySize.toString() : "null"));

                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        Size[] rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR);
                        if (rawSizes != null && rawSizes.length > 0) {
                            // Find the largest RAW size, ideally matching the full sensor resolution (50MP)
                            Size bestRaw = rawSizes[0];
                            for (Size s : rawSizes) {
                                Log.i(TAG, "Available RAW Size: " + s.getWidth() + "x" + s.getHeight());
                                if (s.getWidth() * s.getHeight() > bestRaw.getWidth() * bestRaw.getHeight()) {
                                    bestRaw = s;
                                }
                            }
                            // Prefer size that matches pixel array exactly to ensure FOV parity with DNG
                            if (pixelArraySize != null) {
                                for (Size s : rawSizes) {
                                    if (s.getWidth() == pixelArraySize.getWidth() && s.getHeight() == pixelArraySize.getHeight()) {
                                        bestRaw = s;
                                        Log.i(TAG, "Found RAW size matching Pixel Array: " + bestRaw);
                                        break;
                                    }
                                }
                            }
                            rawSize = bestRaw;
                            Log.i(TAG, "Selected RAW Size: " + rawSize.getWidth() + "x" + rawSize.getHeight());
                        }
                    }
                    break;
                }
            }

            if (cameraId != null && rawSize != null) {
                // Reduced to 2 buffers to save memory on 50MP
                rawImageReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(), ImageFormat.RAW_SENSOR, 2);
                rawImageReader.setOnImageAvailableListener(reader -> {
                    Image img = null;
                    try {
                        img = reader.acquireLatestImage();
                        if (img != null) {
                            FrameProcessor fp = frameProcessor;
                            if (fp != null) fp.processFrame(img);
                            else img.close();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error acquiring image", e);
                        if (img != null) img.close();
                    }
                }, backgroundHandler);

                manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        Log.i(TAG, "Camera opened: " + cameraId);
                        cameraDevice = camera;
                        createCaptureSession();
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        Log.w(TAG, "Camera disconnected: " + cameraId);
                        closeCamera();
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                        Log.e(TAG, "Camera error on " + cameraId + ": " + error);
                        closeCamera();
                    }
                }, backgroundHandler);
            } else {
                Log.e(TAG, "No suitable back camera or RAW support found");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Access exception", e);
        }
    }

    private void createCaptureSession() {
        if (cameraDevice == null || rawImageReader == null) return;
        try {
            Surface surface = rawImageReader.getSurface();
            if (surface == null || !surface.isValid()) {
                Log.e(TAG, "Invalid surface");
                return;
            }
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    startCapture();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Configure failed");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Access exception", e);
        }
    }

    private void startCapture() {
        applyParams();
    }

    public synchronized void updateParams(long exposureTimeNs, int iso, float focusDistance) {
        this.exposureTimeNs = exposureTimeNs;
        this.iso = iso;
        this.focusDistance = focusDistance;
        if (captureSession != null) applyParams();
    }

    public synchronized void updateParams(long exposureTimeNs, int iso, float focusDistance, long frameDurationNs) {
        this.exposureTimeNs = exposureTimeNs;
        this.iso = iso;
        this.focusDistance = focusDistance;
        this.frameDurationNs = frameDurationNs;
        if (captureSession != null) applyParams();
    }

    private void applyParams() {
        if (cameraDevice == null || captureSession == null) return;
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            builder.addTarget(rawImageReader.getSurface());

            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);

            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance);
            builder.set(CaptureRequest.SENSOR_FRAME_DURATION, Math.max(exposureTimeNs + 100000000L, frameDurationNs));

            if (pixelArraySize != null) {
                builder.set(CaptureRequest.SCALER_CROP_REGION, new android.graphics.Rect(0, 0, pixelArraySize.getWidth(), pixelArraySize.getHeight()));
            } else if (activeArraySize != null) {
                builder.set(CaptureRequest.SCALER_CROP_REGION, activeArraySize);
            }

            captureSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    lastCaptureResult = result;
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to apply parameters", e);
        }
    }

    public void close() {
        closeCamera();
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try { backgroundThread.join(); } catch (InterruptedException e) {}
            backgroundThread = null;
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            try { captureSession.stopRepeating(); } catch (Exception e) {}
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (rawImageReader != null) {
            rawImageReader.close();
            rawImageReader = null;
        }
    }

    public Size getRawSize() {
        return rawSize;
    }

    public CameraCharacteristics getCharacteristics() {
        return characteristics;
    }

    public Size getPixelArraySize() {
        return pixelArraySize;
    }

    public android.graphics.Rect getActiveArraySize() {
        return activeArraySize;
    }

    public TotalCaptureResult getLastCaptureResult() {
        return lastCaptureResult;
    }
}
