import sys
import qt_compat
qt_compat.install()

import numpy as np
sys.path.append("android_app")
from als.model.base import Image, VisualProfile
from als.stack import Stacker
from als.code_utilities import SignalingQueue

def test_stacking():
    queue = SignalingQueue()
    profile = VisualProfile()
    stacker = Stacker(queue, profile)
    stacker.align_before_stack = False

    results = []
    stacker.new_result_signal.connect(lambda img: results.append(img))

    img1 = Image(np.ones((100, 100), dtype=np.float32))
    stacker._handle_item(img1)

    img2 = Image(np.ones((100, 100), dtype=np.float32))
    stacker._handle_item(img2)

    print(f"Stack size: {stacker.size}")
    assert stacker.size == 2
    assert len(results) == 2
    print("Logic test passed!")

if __name__ == "__main__":
    test_stacking()
