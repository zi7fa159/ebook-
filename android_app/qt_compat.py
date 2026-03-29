import sys
import types
from threading import Thread
import time

class pyqtSignal:
    def __init__(self, *args):
        self.slots = []
    def emit(self, *args):
        for slot in self.slots:
            try:
                slot(*args)
            except Exception as e:
                print(f"Error in signal slot: {e}")
    def connect(self, slot):
        if slot not in self.slots:
            self.slots.append(slot)
    def disconnect(self, slot):
        if slot in self.slots:
            self.slots.remove(slot)
    def __getitem__(self, key): return self

class QObject:
    def __init__(self, *args, **kwargs):
        pass
    def tr(self, text, disambiguation=None, n=-1): return text
    def __repr__(self):
        return f"<QObject at {id(self)}>"

class QThread(Thread):
    HighestPriority = 1
    HighPriority = 2
    LowPriority = 3
    TimeCriticalPriority = 4
    def __init__(self, *args, **kwargs):
        Thread.__init__(self)
        self._stop_asked = False
    def start(self, priority=None):
        Thread.start(self)
    @staticmethod
    def msleep(ms):
        time.sleep(ms / 1000.0)
    def wait(self):
        self.join()
    def __repr__(self):
        return f"<QThread at {id(self)}>"

def QT_TRANSLATE_NOOP(context, text):
    return text

class QCoreApplication:
    @staticmethod
    def translate(context, text, disambiguation=None, n=-1):
        return text

class QTimer:
    def __init__(self):
        self.interval = 1000
        self.timeout = pyqtSignal()
        self._running = False
    def setInterval(self, ms):
        self.interval = ms
    def start(self, ms=None):
        if ms: self.interval = ms
        self._running = True
        t = Thread(target=self._run, daemon=True)
        t.start()
    def stop(self):
        self._running = False
    def _run(self):
        while self._running:
            time.sleep(self.interval / 1000.0)
            if self._running:
                self.timeout.emit()

class QFile:
    ReadOnly = 0x0001
    WriteOnly = 0x0002
    ReadWrite = ReadOnly | WriteOnly
    Append = 0x0004
    Truncate = 0x0008
    Text = 0x0010
    def __init__(self, name): self.name = name
    def open(self, mode): return True
    def copy(self, dest): pass
    def chmod(self, mode): pass

class QIODevice:
    ReadOnly = 0x0001
    WriteOnly = 0x0002
    ReadWrite = ReadOnly | WriteOnly
    Append = 0x0004
    Truncate = 0x0008
    Text = 0x0010

class QTextStream:
    def __init__(self, file): self.file = file
    def readAll(self): return ""

class QFileInfo:
    def __init__(self, path): self.path = path
    def size(self):
        import os
        try: return os.path.getsize(self.path)
        except: return 0

class QPixmap:
    @staticmethod
    def fromImage(img): return img

def array2qimage(array, normalize=None):
    return array

# Compatibility setup
def install():
    sys.modules["PyQt5"] = types.ModuleType("PyQt5")
    sys.modules["PyQt5.QtCore"] = types.ModuleType("PyQt5.QtCore")
    sys.modules["PyQt5.QtCore"].pyqtSignal = pyqtSignal
    sys.modules["PyQt5.QtCore"].QObject = QObject
    sys.modules["PyQt5.QtCore"].QThread = QThread
    sys.modules["PyQt5.QtCore"].QT_TRANSLATE_NOOP = QT_TRANSLATE_NOOP
    sys.modules["PyQt5.QtCore"].QCoreApplication = QCoreApplication
    sys.modules["PyQt5.QtCore"].QTimer = QTimer
    sys.modules["PyQt5.QtCore"].QFile = QFile
    sys.modules["PyQt5.QtCore"].QFileInfo = QFileInfo
    sys.modules["PyQt5.QtCore"].QIODevice = QIODevice
    sys.modules["PyQt5.QtCore"].QTextStream = QTextStream
    sys.modules["PyQt5.QtCore"].Qt = types.ModuleType("Qt")

    sys.modules["PyQt5.QtGui"] = types.ModuleType("PyQt5.QtGui")
    sys.modules["PyQt5.QtGui"].QPixmap = QPixmap

    sys.modules["qimage2ndarray"] = types.ModuleType("qimage2ndarray")
    sys.modules["qimage2ndarray"].array2qimage = array2qimage

    # pkg_resources mock
    if "pkg_resources" not in sys.modules:
        pkg_resources = types.ModuleType("pkg_resources")
        sys.modules["pkg_resources"] = pkg_resources
        pkg_resources.DistributionNotFound = Exception
        def get_distribution(name):
            raise pkg_resources.DistributionNotFound
        pkg_resources.get_distribution = get_distribution
