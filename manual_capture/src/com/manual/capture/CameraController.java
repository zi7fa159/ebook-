package com.manual.capture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import java.util.Arrays;
import java.util.Collections;

public class CameraController {
    private static final String TAG = "ManualCam_Controller";
    private final Context context;
    private final TextureView textureView;
    private final CaptureListener listener;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader rawReader;
    private HandlerThread camThread;
    private Handler camHandler;

    private long exposureNs = 1000000000L;
    private int iso = 800;
    private float focusDist = 0.0f;
    private int wbKelvin = 5000;

    private boolean isAdjustmentMode = true;
    private String cameraId;

    public interface CaptureListener {
        void onRawFrame(Image img, TotalCaptureResult result, boolean isPreview);
    }

    public CameraController(Context context, TextureView textureView, CaptureListener listener) {
        this.context = context;
        this.textureView = textureView;
        this.listener = listener;
    }

    public void start() {
        camThread = new HandlerThread("CameraThread");
        camThread.start();
        camHandler = new Handler(camThread.getLooper());
        openCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            if (cameraId == null) return;

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size bestRaw = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)),
                (a, b) -> Long.compare(a.getWidth() * (long)a.getHeight(), b.getWidth() * (long)b.getHeight()));

            rawReader = ImageReader.newInstance(bestRaw.getWidth(), bestRaw.getHeight(), ImageFormat.RAW_SENSOR, 3);
            rawReader.setOnImageAvailableListener(reader -> {
                // We'll acquire in the capture callback to sync with metadata
            }, camHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createSession();
                }
                @Override public void onDisconnected(CameraDevice camera) { stop(); }
                @Override public void onError(CameraDevice camera, int error) { stop(); }
            }, camHandler);
        } catch (Exception e) { Log.e(TAG, "Error opening camera", e); }
    }

    private void createSession() {
        try {
            Surface rawSurface = rawReader.getSurface();
            // We don't necessarily need a preview surface if we render RAW manually,
            // but Camera2 often prefers having one.
            Surface dummySurface = new Surface(textureView.getSurfaceTexture());

            cameraDevice.createCaptureSession(Arrays.asList(dummySurface, rawSurface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    updateRepeatingRequest();
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {}
            }, camHandler);
        } catch (Exception e) { Log.e(TAG, "Session creation failed", e); }
    }

    public void setAdjustmentMode(boolean active) {
        this.isAdjustmentMode = active;
        updateRepeatingRequest();
    }

    private void updateRepeatingRequest() {
        if (captureSession == null) return;
        try {
            captureSession.stopRepeating();
            if (isAdjustmentMode) {
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(rawReader.getSurface());
                // For preview, we "cap" the exposure to 500ms to keep UI responsive
                long previewExp = Math.min(exposureNs, 500_000_000L);
                applyManualParams(builder, previewExp, iso, focusDist);
                captureSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        Image img = rawReader.acquireLatestImage();
                        if (img != null) listener.onRawFrame(img, result, true);
                    }
                }, camHandler);
            }
        } catch (Exception e) { Log.e(TAG, "Update repeating failed", e); }
    }

    public void takeStill() {
        if (captureSession == null) return;
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(rawReader.getSurface());
            applyManualParams(builder, exposureNs, iso, focusDist);
            captureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Image img = rawReader.acquireLatestImage();
                    if (img != null) listener.onRawFrame(img, result, false);
                }
            }, camHandler);
        } catch (Exception e) { Log.e(TAG, "Still capture failed", e); }
    }

    public void updateParams(long exp, int isoVal, float focus, int wb) {
        this.exposureNs = exp;
        this.iso = isoVal;
        this.focusDist = focus;
        this.wbKelvin = wb;
        if (isAdjustmentMode) updateRepeatingRequest();
    }

    private void applyManualParams(CaptureRequest.Builder builder, long exp, int sensitivity, float focus) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exp);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity);
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus);
    }

    public CameraCharacteristics getCharacteristics() throws Exception {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        return manager.getCameraCharacteristics(cameraId);
    }

    public void stop() {
        try {
            if (captureSession != null) { captureSession.stopRepeating(); captureSession.close(); }
            if (cameraDevice != null) cameraDevice.close();
            if (rawReader != null) rawReader.close();
        } catch (Exception e) {}
        if (camThread != null) camThread.quitSafely();
        captureSession = null;
        cameraDevice = null;
    }
}
