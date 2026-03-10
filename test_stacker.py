import sys
import types
import numpy as np

# Mock PyQt5
class MockSignal:
    def __init__(self, *args):
        self.slots = []
    def emit(self, *args):
        for slot in self.slots:
            slot(*args)
    def connect(self, slot):
        self.slots.append(slot)
    def __getitem__(self, key): return self

class MockQObject:
    def __init__(self, *args, **kwargs): pass

class MockQThread:
    HighestPriority = 1
    HighPriority = 2
    LowPriority = 3
    TimeCriticalPriority = 4
    def __init__(self, *args, **kwargs):
        self._stop_asked = False
    def start(self, *args): pass
    @staticmethod
    def msleep(ms):
        import time
        time.sleep(ms / 1000.0)
    def wait(self): pass

pyqt5 = types.ModuleType("PyQt5")
sys.modules["PyQt5"] = pyqt5
pyqt5.QtCore = types.ModuleType("PyQt5.QtCore")
sys.modules["PyQt5.QtCore"] = pyqt5.QtCore
pyqt5.QtCore.pyqtSignal = MockSignal
pyqt5.QtCore.QObject = MockQObject
pyqt5.QtCore.QThread = MockQThread
pyqt5.QtCore.Qt = types.ModuleType("Qt")
pyqt5.QtCore.QT_TRANSLATE_NOOP = lambda x, y: y
pyqt5.QtCore.QCoreApplication = types.ModuleType("QCoreApplication")
pyqt5.QtCore.QCoreApplication.translate = lambda x, y: y
pyqt5.QtCore.QTimer = types.ModuleType("QTimer")
pyqt5.QtCore.QFile = types.ModuleType("QFile")
pyqt5.QtCore.QFileInfo = types.ModuleType("QFileInfo")
pyqt5.QtCore.QIODevice = types.ModuleType("QIODevice")
pyqt5.QtCore.QTextStream = types.ModuleType("QTextStream")

pyqt5.QtWidgets = types.ModuleType("PyQt5.QtWidgets")
sys.modules["PyQt5.QtWidgets"] = pyqt5.QtWidgets

pyqt5.QtGui = types.ModuleType("PyQt5.QtGui")
sys.modules["PyQt5.QtGui"] = pyqt5.QtGui
pyqt5.QtGui.QPixmap = types.ModuleType("QPixmap")

# Mock qimage2ndarray
qimage2ndarray = types.ModuleType("qimage2ndarray")
sys.modules["qimage2ndarray"] = qimage2ndarray
qimage2ndarray.array2qimage = lambda *args, **kwargs: None

# Mock pkg_resources
pkg_resources = types.ModuleType("pkg_resources")
sys.modules["pkg_resources"] = pkg_resources
pkg_resources.DistributionNotFound = Exception
def get_distribution(name):
    raise pkg_resources.DistributionNotFound
pkg_resources.get_distribution = get_distribution

sys.path.append("als_repo/src")
from als.model.base import Image, VisualProfile
from als.stack import Stacker
from als.code_utilities import SignalingQueue

queue = SignalingQueue()
profile = VisualProfile()
stacker = Stacker(queue, profile)
stacker.align_before_stack = False # Disable alignment for simple test

def on_new_result(image):
    print(f"New stacked result received! Shape: {image.data.shape}")

stacker.new_result_signal.connect(on_new_result)

# Manually call _handle_item instead of starting thread for this test
img1 = Image(np.ones((100, 100), dtype=np.float32))
img1.origin = "test1"
print("Processing first image...")
stacker._handle_item(img1)

img2 = Image(np.ones((100, 100), dtype=np.float32))
img2.origin = "test2"
print("Processing second image...")
stacker._handle_item(img2)

print(f"Stack size: {stacker.size}")
