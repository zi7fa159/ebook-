package com.example.astrostacker;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraController {
    private final Context context;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader yuvReader;
    private ImageReader rawReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private long exposureTimeNs = 1_000_000_000L; // 1s default
    private int iso = 800;
    private float focusDistance = 0.0f; // Infinity
    private boolean useRaw = false;

    private Range<Long> exposureRange;
    private Range<Integer> isoRange;
    private float minFocusDistance;
    private boolean isRawSupported = false;

    public interface FrameCallback {
        void onYuvFrameReceived(ImageReader reader);
        void onRawFrameReceived(ImageReader reader);
    }

    private FrameCallback frameCallback;

    public CameraController(Context context) {
        this.context = context;
    }

    public void openCamera(FrameCallback callback, SessionCallback sessionCallback) {
        if (cameraDevice != null) return;
        this.frameCallback = callback;
        startBackgroundThread();
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            if (cameraId == null) cameraId = manager.getCameraIdList()[0];

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

            int[] caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            for (int cap : caps) {
                if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                    isRawSupported = true;
                    break;
                }
            }

            Size[] yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            Size yuvSize = yuvSizes[0];
            for (Size s : yuvSizes) {
                int mp = (s.getWidth() * s.getHeight()) / 1_000_000;
                if (mp >= 3 && mp <= 5) {
                    yuvSize = s;
                    break;
                }
            }

            yuvReader = ImageReader.newInstance(yuvSize.getWidth(), yuvSize.getHeight(), ImageFormat.YUV_420_888, 3);
            yuvReader.setOnImageAvailableListener(reader -> {
                if (frameCallback != null) frameCallback.onYuvFrameReceived(reader);
            }, backgroundHandler);

            if (isRawSupported) {
                Size[] rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR);
                Size rawSize = rawSizes[0]; // Usually largest
                rawReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(), ImageFormat.RAW_SENSOR, 3);
                rawReader.setOnImageAvailableListener(reader -> {
                    if (frameCallback != null) frameCallback.onRawFrameReceived(reader);
                }, backgroundHandler);
            }

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createCaptureSession(sessionCallback);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);
        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public interface SessionCallback {
        void onReady();
    }

    private void createCaptureSession(SessionCallback sessionCallback) {
        try {
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(yuvReader.getSurface());
            if (rawReader != null) surfaces.add(rawReader.getSurface());

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    if (sessionCallback != null) sessionCallback.onReady();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {}
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startCapture() {
        if (cameraDevice == null || captureSession == null) return;
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            if (useRaw && rawReader != null) {
                builder.addTarget(rawReader.getSurface());
            } else {
                builder.addTarget(yuvReader.getSurface());
            }

            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance);

            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
            builder.set(CaptureRequest.SENSOR_FRAME_DURATION, exposureTimeNs + 10_000_000L);

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setRawMode(boolean enabled) {
        this.useRaw = enabled && isRawSupported;
    }

    public boolean isRawMode() { return useRaw; }

    public void setExposure(long ns) {
        if (exposureRange != null) {
            this.exposureTimeNs = Math.max(exposureRange.getLower(), Math.min(exposureRange.getUpper(), ns));
        } else {
            this.exposureTimeNs = ns;
        }
    }

    public void setIso(int iso) {
        if (isoRange != null) {
            this.iso = Math.max(isoRange.getLower(), Math.min(isoRange.getUpper(), iso));
        } else {
            this.iso = iso;
        }
    }

    public void setFocus(float distance) {
        this.focusDistance = Math.max(0.0f, Math.min(minFocusDistance, distance));
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void close() {
        stopCapture();
        if (cameraDevice != null) cameraDevice.close();
        if (yuvReader != null) yuvReader.close();
        if (rawReader != null) rawReader.close();
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Range<Long> getExposureRange() { return exposureRange; }
    public Range<Integer> getIsoRange() { return isoRange; }
    public float getMinFocusDistance() { return minFocusDistance; }
    public boolean isRawSupported() { return isRawSupported; }
}
