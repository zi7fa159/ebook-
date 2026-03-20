package com.example.astroapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
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
    private static final String TAG = "AstroCamera";
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private String cameraId;
    private Size previewSize;
    private Size rawSize;
    private Size jpegSize;
    private ImageReader rawImageReader;
    private ImageReader jpegImageReader;
    private TextureView textureView;
    private CameraCharacteristics characteristics;

    public CameraController(Context context, TextureView textureView) {
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.textureView = textureView;
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            if (backgroundThread != null) {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public void openCamera() {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            rawSize = map.getOutputSizes(ImageFormat.RAW_SENSOR)[0];

            Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
            jpegSize = sizes[sizes.length - 1]; // Default to smallest
            for (Size s : sizes) {
                if (s.getWidth() >= 640 && s.getWidth() <= 1280) {
                    jpegSize = s;
                    break;
                }
            }

            rawImageReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(), ImageFormat.RAW_SENSOR, 5);
            jpegImageReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(), ImageFormat.JPEG, 2);

            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
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
    };

    private void createCaptureSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(texture);
            Surface rawSurface = rawImageReader.getSurface();
            Surface jpegSurface = jpegImageReader.getSurface();

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, rawSurface, jpegSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    startPreview(1000000000L / 30, 800);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void startPreview(long exposureNs, int iso) {
        if (cameraDevice == null || captureSession == null) return;
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(new Surface(textureView.getSurfaceTexture()));

            setupManualRequest(builder, exposureNs, iso);

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setupManualRequest(CaptureRequest.Builder builder, long exposureTimeNs, int iso) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);

        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f);

        builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
        builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
        builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
        builder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_OFF);
    }

    public CameraDevice getCameraDevice() { return cameraDevice; }
    public CameraCaptureSession getCaptureSession() { return captureSession; }
    public ImageReader getRawImageReader() { return rawImageReader; }
    public ImageReader getJpegImageReader() { return jpegImageReader; }
    public CameraCharacteristics getCharacteristics() { return characteristics; }
    public Handler getBackgroundHandler() { return backgroundHandler; }
}
