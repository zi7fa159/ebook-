import os
import sys

# Install Qt compatibility layer before importing ALS modules
import qt_compat
qt_compat.install()

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.image import Image as KivyImage
from kivy.uix.slider import Slider
from kivy.clock import Clock
from kivy.graphics.texture import Texture

import numpy as np
import cv2

from als.logic import Controller
from als.model.data import DYNAMIC_DATA
from camera_bridge import AndroidCameraScanner

class ALSAndroidApp(App):
    def build(self):
        self.controller = Controller()

        # Setup paths
        self.user_data_dir = self.get_user_data_dir()
        self.temp_dir = os.path.join(self.user_data_dir, "temp_frames")
        self.work_dir = os.path.join(self.user_data_dir, "work")
        self.web_dir = os.path.join(self.user_data_dir, "web")

        for d in [self.temp_dir, self.work_dir, self.web_dir]:
            if not os.path.exists(d): os.makedirs(d)

        # Configure ALS
        from als import config
        config.set_scan_folder_path(self.temp_dir)
        config.set_work_folder_path(self.work_dir)
        config.set_web_folder_path(self.web_dir)

        # Override the scanner with our Android bridge
        self.scanner = AndroidCameraScanner(self.temp_dir)
        self.controller._input_scanner = self.scanner

        # UI Layout
        root = BoxLayout(orientation='vertical')

        # Live preview of stacked result
        self.preview = KivyImage(allow_stretch=True)
        root.add_widget(self.preview)

        self.status_label = Label(text="Ready", size_hint_y=None, height=100)
        root.add_widget(self.status_label)

        # Camera Controls
        controls = GridLayout(cols=2, size_hint_y=None, height=300)

        controls.add_widget(Label(text="Exposure (ms)"))
        self.expo_slider = Slider(min=1, max=1000, value=100)
        self.expo_slider.bind(value=self.on_expo_change)
        controls.add_widget(self.expo_slider)

        controls.add_widget(Label(text="ISO"))
        self.iso_slider = Slider(min=100, max=6400, value=800, step=100)
        self.iso_slider.bind(value=self.on_iso_change)
        controls.add_widget(self.iso_slider)

        root.add_widget(controls)

        # Action Buttons
        actions = BoxLayout(size_hint_y=None, height=120)
        self.start_btn = Button(text="Start Session")
        self.start_btn.bind(on_release=self.toggle_session)
        actions.add_widget(self.start_btn)

        self.capture_btn = Button(text="Capture Frame")
        self.capture_btn.bind(on_release=lambda x: self.scanner.capture_frame())
        actions.add_widget(self.capture_btn)

        root.add_widget(actions)

        # Options
        options = BoxLayout(size_hint_y=None, height=80)
        self.delete_opt = Button(text="Delete frames: OFF")
        self.delete_opt.bind(on_release=self.toggle_delete)
        options.add_widget(self.delete_opt)
        root.add_widget(options)

        # Update UI timer
        Clock.schedule_interval(self.update_ui, 1.0)

        # Connect signals
        self.controller._post_process_pipeline.new_result_signal.connect(self.on_new_stack_result)

        return root

    def on_expo_change(self, instance, value):
        self.scanner.set_exposure(int(value * 1000000))

    def on_iso_change(self, instance, value):
        self.scanner.set_iso(int(value))

    def toggle_session(self, instance):
        if DYNAMIC_DATA.session.is_running:
            self.controller.stop_session()
            self.start_btn.text = "Start Session"
        else:
            try:
                self.controller.start_session()
                self.start_btn.text = "Stop Session"
            except Exception as e:
                self.status_label.text = f"Error: {e}"

    def toggle_delete(self, instance):
        self.scanner.delete_after_stack = not self.scanner.delete_after_stack
        self.delete_opt.text = f"Delete frames: {'ON' if self.scanner.delete_after_stack else 'OFF'}"

    def on_new_stack_result(self, image):
        Clock.schedule_once(lambda dt: self.update_preview(image))

        if self.scanner.delete_after_stack and hasattr(image, 'ticket'):
            try:
                if os.path.exists(image.ticket):
                    os.remove(image.ticket)
            except Exception as e:
                print(f"Failed to delete frame: {e}")

    def update_preview(self, image):
        # Convert uint16 to uint8 for Kivy
        # Use simple scaling for now
        data = (image.data / 256).astype(np.uint8)
        if len(data.shape) == 2: # B&W
            data = cv2.cvtColor(data, cv2.COLOR_GRAY2RGB)

        h, w = data.shape[:2]
        texture = Texture.create(size=(w, h), colorfmt='rgb')
        texture.blit_buffer(data.tobytes(), colorfmt='rgb', bufferfmt='ubyte')
        texture.flip_vertical() # Kivy texture is upside down
        self.preview.texture = texture

    def update_ui(self, dt):
        self.status_label.text = f"Stacked: {DYNAMIC_DATA.stack_size} | Expo: {DYNAMIC_DATA.total_exposure_time}s\nRAM: {DYNAMIC_DATA.last_timing}s"

    def get_user_data_dir(self):
        if sys.platform == 'android':
            from android.storage import app_storage_path
            return app_storage_path()
        return os.path.expanduser("~/.als_android")

if __name__ == '__main__':
    ALSAndroidApp().run()
