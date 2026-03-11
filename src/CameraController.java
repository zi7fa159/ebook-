package com.alsclone.astrostacker;

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
import java.util.Arrays;

public class CameraController {
    private static final String TAG = "CameraController";
    private final Context context;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader rawImageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private long exposureTimeNs = 1000000000L; // 1s
    private int iso = 800;
    private float focusDistance = 0.0f;
    private String cameraId;
    private Size rawSize;
    private CameraCharacteristics characteristics;

    private volatile FrameProcessor frameProcessor;

    public CameraController(Context context, FrameProcessor frameProcessor) {
        this.context = context;
        this.frameProcessor = frameProcessor;
    }

    public void setFrameProcessor(FrameProcessor frameProcessor) {
        this.frameProcessor = frameProcessor;
    }

    public void start() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        openCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                this.characteristics = characteristics;
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        Size[] rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR);
                        if (rawSizes != null && rawSizes.length > 0) {
                            // Find largest (50MP)
                            rawSize = rawSizes[0];
                            for (Size s : rawSizes) {
                                if (s.getWidth() * s.getHeight() > rawSize.getWidth() * rawSize.getHeight()) {
                                    rawSize = s;
                                }
                            }
                        }
                    }
                    break;
                }
            }

            if (cameraId != null && rawSize != null) {
                rawImageReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(), ImageFormat.RAW_SENSOR, 3);
                rawImageReader.setOnImageAvailableListener(reader -> {
                    FrameProcessor fp = frameProcessor;
                    Image img = reader.acquireLatestImage();
                    if (img == null) return;

                    if (fp != null) {
                        fp.processFrame(img);
                    } else {
                        img.close();
                    }
                }, backgroundHandler);

                manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        cameraDevice = camera;
                        createCaptureSession();
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
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Access exception", e);
        }
    }

    private void createCaptureSession() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(rawImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
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

            // Frame duration must be >= exposure time
            builder.set(CaptureRequest.SENSOR_FRAME_DURATION, exposureTimeNs + 100000000L); // Add 100ms buffer

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Access exception", e);
        }
    }

    public void updateParams(long exposureTimeNs, int iso, float focusDistance) {
        this.exposureTimeNs = exposureTimeNs;
        this.iso = iso;
        this.focusDistance = focusDistance;
        if (captureSession != null) {
            startCapture();
        }
    }

    public Size getRawSize() {
        return rawSize;
    }

    public CameraCharacteristics getCharacteristics() {
        return characteristics;
    }

    public int getBayerPattern() {
        if (characteristics == null) return -1;
        Integer pattern = characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
        return pattern != null ? pattern : -1;
    }
}
