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
import android.util.Log;
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

    private long exposureTimeNs = 1_000_000_000L;
    private int iso = 800;
    private float focusDistance = 0.0f;

    private Range<Long> exposureRange;
    private Range<Integer> isoRange;
    private float minFocusDistance;

    public interface FrameCallback {
        void onYuvFrameReceived(ImageReader reader);
        void onRawFrameReceived(ImageReader reader);
    }

    private FrameCallback frameCallback;

    public CameraController(Context context) {
        this.context = context;
        startBackgroundThread();
    }

    public void openCamera(FrameCallback callback, Runnable onReady) {
        this.frameCallback = callback;
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
            if (map == null) throw new RuntimeException("Cannot get stream configuration map");

            exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

            Size yuvSize = selectSize(map.getOutputSizes(ImageFormat.YUV_420_888), 1280, 720);
            Size rawSize = map.getOutputSizes(ImageFormat.RAW_SENSOR)[0];

            yuvReader = ImageReader.newInstance(yuvSize.getWidth(), yuvSize.getHeight(), ImageFormat.YUV_420_888, 3);
            yuvReader.setOnImageAvailableListener(reader -> {
                if (frameCallback != null) frameCallback.onYuvFrameReceived(reader);
            }, backgroundHandler);

            rawReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(), ImageFormat.RAW_SENSOR, 3);
            rawReader.setOnImageAvailableListener(reader -> {
                if (frameCallback != null) frameCallback.onRawFrameReceived(reader);
            }, backgroundHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createCaptureSession(onReady);
                }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); cameraDevice = null; }
                @Override public void onError(CameraDevice camera, int error) { camera.close(); cameraDevice = null; }
            }, backgroundHandler);
        } catch (CameraAccessException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void createCaptureSession(Runnable onReady) {
        try {
            List<Surface> surfaces = Arrays.asList(yuvReader.getSurface(), rawReader.getSurface());
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    if (onReady != null) onReady.run();
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e("ProAstro", "Capture session configuration failed");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startStreaming() {
        if (cameraDevice == null || captureSession == null) return;
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            builder.addTarget(yuvReader.getSurface());
            builder.addTarget(rawReader.getSurface());

            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);

            if (exposureRange != null) exposureTimeNs = Math.max(exposureRange.getLower(), Math.min(exposureRange.getUpper(), exposureTimeNs));
            if (isoRange != null) iso = Math.max(isoRange.getLower(), Math.min(isoRange.getUpper(), iso));

            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance);
            builder.set(CaptureRequest.SENSOR_FRAME_DURATION, exposureTimeNs + 10_000_000L);

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void stopStreaming() {
        if (captureSession != null) {
            try { captureSession.stopRepeating(); } catch (CameraAccessException e) { e.printStackTrace(); }
        }
    }

    private Size selectSize(Size[] sizes, int targetW, int targetH) {
        for (Size s : sizes) {
            if (s.getWidth() <= targetW && s.getHeight() <= targetH) return s;
        }
        return sizes[0];
    }

    public void setExposure(long ns) { this.exposureTimeNs = ns; }
    public void setIso(int iso) { this.iso = iso; }
    public void setFocus(float distance) { this.focusDistance = distance; }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBg");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void close() {
        stopStreaming();
        if (cameraDevice != null) cameraDevice.close();
        if (yuvReader != null) yuvReader.close();
        if (rawReader != null) rawReader.close();
        if (backgroundThread != null) backgroundThread.quitSafely();
    }
}
