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

    public interface CaptureListener {
        void onRawAvailable(Image img, TotalCaptureResult result);
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
            String cameraId = null;
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
            Size[] rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR);
            Size bestRaw = rawSizes[0];
            for (Size s : rawSizes) if (s.getWidth() * s.getHeight() > bestRaw.getWidth() * bestRaw.getHeight()) bestRaw = s;

            rawReader = ImageReader.newInstance(bestRaw.getWidth(), bestRaw.getHeight(), ImageFormat.RAW_SENSOR, 2);
            rawReader.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                // Handled in capture session callback to get metadata
            }, camHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createSession();
                }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); }
                @Override public void onError(CameraDevice camera, int error) { camera.close(); }
            }, camHandler);
        } catch (Exception e) { Log.e(TAG, "Error opening camera", e); }
    }

    private void createSession() {
        try {
            Surface previewSurface = new Surface(textureView.getSurfaceTexture());
            Surface rawSurface = rawReader.getSurface();
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, rawSurface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    startPreview();
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {}
            }, camHandler);
        } catch (Exception e) {}
    }

    private void startPreview() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(new Surface(textureView.getSurfaceTexture()));
            applyManualParams(builder);
            captureSession.setRepeatingRequest(builder.build(), null, camHandler);
        } catch (Exception e) {}
    }

    public void takeRaw() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(rawReader.getSurface());
            applyManualParams(builder);
            captureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Image img = rawReader.acquireLatestImage();
                    if (img != null) listener.onRawAvailable(img, result);
                }
            }, camHandler);
        } catch (Exception e) {}
    }

    public void updateParams(long exp, int isoVal, float focus, int wb) {
        this.exposureNs = exp;
        this.iso = isoVal;
        this.focusDist = focus;
        this.wbKelvin = wb;
        startPreview();
    }

    private void applyManualParams(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);

        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDist);
        // WB is handled via gains in Camera2, but for DNG export the Kelvin value is used in DngCreator
    }

    public CameraCharacteristics getCharacteristics() throws Exception {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        return manager.getCameraCharacteristics(cameraDevice.getId());
    }

    public void stop() {
        if (cameraDevice != null) cameraDevice.close();
        if (camThread != null) camThread.quitSafely();
    }
}
