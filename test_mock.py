import sys
import types

# Mock PyQt5
class MockSignal:
    def __init__(self, *args): pass
    def emit(self, *args): pass
    def connect(self, slot): pass
    def __getitem__(self, key): return self

class MockQObject:
    def __init__(self, *args, **kwargs): pass

class MockQThread:
    def __init__(self, *args, **kwargs): pass
    def start(self, *args): pass
    @staticmethod
    def msleep(ms): pass
    def wait(self): pass

pyqt5 = types.ModuleType("PyQt5")
sys.modules["PyQt5"] = pyqt5
pyqt5.QtCore = types.ModuleType("PyQt5.QtCore")
sys.modules["PyQt5.QtCore"] = pyqt5.QtCore
pyqt5.QtCore.pyqtSignal = MockSignal
pyqt5.QtCore.QObject = MockQObject
pyqt5.QtCore.QThread = MockQThread
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

# Now try to import als components
sys.path.append("als_repo/src")
try:
    from als.stack import Stacker
    from als.processing import Pipeline
    print("Core components imported successfully with mocks")
except Exception as e:
    print(f"Import failed: {e}")
    import traceback
    traceback.print_exc()
