package com.astroapp;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.util.Arrays;
import java.util.List;

public class CameraController {
    private static final String TAG = "CameraController";
    private final Context context;
    private final CameraManager cameraManager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private CameraCharacteristics characteristics;

    public interface CameraStateCallback {
        void onOpened();
        void onDisconnected();
        void onError(int error);
    }

    public CameraController(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void openCamera(CameraStateCallback callback, Handler handler) throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                cameraId = id;
                break;
            }
        }

        cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                cameraDevice = camera;
                callback.onOpened();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                if (cameraDevice != null) cameraDevice.close();
                callback.onDisconnected();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                if (cameraDevice != null) cameraDevice.close();
                callback.onError(error);
            }
        }, handler);
    }

    public void startPreview(Surface previewSurface, ImageReader rawReader, ImageReader jpegReader, Handler handler) throws CameraAccessException {
        previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewBuilder.addTarget(previewSurface);

        List<Surface> targets = Arrays.asList(previewSurface, rawReader.getSurface(), jpegReader.getSurface());
        cameraDevice.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                captureSession = session;
                updateManualControls(previewBuilder);
                try {
                    captureSession.setRepeatingRequest(previewBuilder.build(), null, handler);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Failed to start repeating preview", e);
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e(TAG, "Configure failed");
            }
        }, handler);
    }

    public void updateManualControls(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f);
        builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
        builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
    }

    public void updateSettings(int iso, long exposureNs) {
        if (previewBuilder == null || captureSession == null) return;
        previewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        previewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs);
        try {
            captureSession.setRepeatingRequest(previewBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Update settings failed", e);
        }
    }

    public CaptureRequest createCaptureRequest(Surface rawSurface, Surface jpegSurface, int iso, long exposureNs) throws CameraAccessException {
        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        builder.addTarget(rawSurface);
        builder.addTarget(jpegSurface);
        updateManualControls(builder);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs);
        return builder.build();
    }

    public CameraCaptureSession getCaptureSession() {
        return captureSession;
    }

    public CameraCharacteristics getCharacteristics() {
        return characteristics;
    }

    public void close() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
