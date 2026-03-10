import os
import time
from als.streams.input import InputScanner
from PyQt5.QtCore import pyqtSignal

# Only import jnius when running on Android
try:
    from jnius import autoclass, cast, PythonJavaClass, java_method
    from android.permissions import request_permissions, Permission
    ANDROID = True
except ImportError:
    ANDROID = False

if ANDROID:
    PythonActivity = autoclass('org.kivy.android.PythonActivity')
    Context = autoclass('android.content.Context')
    CameraManager = autoclass('android.hardware.camera2.CameraManager')
    CameraDevice = autoclass('android.hardware.camera2.CameraDevice')
    CaptureRequest = autoclass('android.hardware.camera2.CaptureRequest')
    ImageReader = autoclass('android.media.ImageReader')
    Handler = autoclass('android.os.Handler')
    Looper = autoclass('android.os.Looper')
    ImageFormat = autoclass('android.graphics.ImageFormat')
    Surface = autoclass('android.view.Surface')

class AndroidCameraScanner(InputScanner):
    """
    Bridge between Android Camera2 API and ALS InputScanner
    """
    def __init__(self, temp_dir):
        super().__init__()
        self.temp_dir = temp_dir
        if not os.path.exists(self.temp_dir):
            os.makedirs(self.temp_dir)

        self.is_running = False
        self.delete_after_stack = False

        # Camera settings defaults
        self.exposure_time = 100000000 # 100ms in ns
        self.iso = 800
        self.focus_distance = 0.0 # infinity

        self._camera_manager = None
        self._camera_device = None
        self._capture_session = None
        self._image_reader = None

    def start(self):
        if not ANDROID:
            print("Not running on Android, camera won't start")
            return

        request_permissions([Permission.CAMERA, Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_EXTERNAL_STORAGE])
        self.is_running = True

        activity = PythonActivity.mActivity
        self._camera_manager = activity.getSystemService(Context.CAMERA_SERVICE)

        # In real Android, we would iterate camera IDs and open one.
        # camera_id = self._camera_manager.getCameraIdList()[0]
        # self._camera_manager.openCamera(camera_id, state_callback, None)

        print("Android Camera2 Scanner initialized")

    def stop(self):
        self.is_running = False
        if self._camera_device:
            self._camera_device.close()
        print("Android Camera2 Scanner stopped")

    def capture_frame(self):
        """
        Triggered by UI.
        """
        if not self.is_running:
            return

        timestamp = int(time.time() * 1000)
        filename = f"frame_{timestamp}.jpg"
        filepath = os.path.join(self.temp_dir, filename)

        # Real Camera2 Capture Sequence:
        # 1. Create a CaptureRequest.Builder
        # 2. Set manual parameters
        # 3. Add ImageReader surface as target
        # 4. Call capture() on the session

        if ANDROID and self._camera_device:
            # builder = self._camera_device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            # builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
            # builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, self.exposure_time)
            # builder.set(CaptureRequest.SENSOR_SENSITIVITY, self.iso)
            # builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, self.focus_distance)
            # builder.addTarget(self._image_reader.getSurface())
            # self._capture_session.capture(builder.build(), None, None)
            pass

        # For sandbox simulation (so the rest of the ALS pipeline can be tested):
        import numpy as np
        import cv2
        # Use something more interesting than noise if possible, but noise is fine for stacking tests
        dummy_data = np.random.randint(0, 255, (1080, 1920, 3), dtype=np.uint8)
        cv2.imwrite(filepath, dummy_data)

        print(f"Captured frame: {filepath}")
        self.broadcast_image_path(filepath)

    def set_exposure(self, ns):
        self.exposure_time = ns

    def set_iso(self, iso):
        self.iso = iso

    def set_focus(self, dist):
        self.focus_distance = dist
