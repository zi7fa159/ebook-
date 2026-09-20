import math
import wave
import struct

sample_rate = 44100
duration = 5.0 # 5 seconds
num_samples = int(sample_rate * duration)

# Speech: center panned, frequency ~200Hz to 1000Hz (vowel formants)
# Music: low bass (60Hz) + high stereo synth (2500Hz L, 3000Hz R) + drum pulse

with wave.open('test_input.wav', 'w') as wav:
    wav.setnchannels(2)
    wav.setsampwidth(2) # 16-bit PCM
    wav.setframerate(sample_rate)

    frames = bytearray()
    for i in range(num_samples):
        t = i / sample_rate

        # Speech signal (center, present intermittently e.g. 1s-4s)
        if 1.0 <= t <= 4.0:
            speech = 0.4 * math.sin(2 * math.pi * 300 * t) + 0.3 * math.sin(2 * math.pi * 600 * t)
        else:
            speech = 0.0

        # Music signal:
        # Bass (center/stereo) 60Hz
        music_bass = 0.4 * math.sin(2 * math.pi * 60 * t)
        # Stereo music synth (high freq)
        music_left = music_bass + 0.3 * math.sin(2 * math.pi * 2500 * t)
        music_right = music_bass + 0.3 * math.sin(2 * math.pi * 3200 * t)

        # Mixed signal
        left = int(max(-32768, min(32767, (speech + music_left) * 16384)))
        right = int(max(-32768, min(32767, (speech + music_right) * 16384)))

        frames.extend(struct.pack('<hh', left, right))

    wav.writeframes(frames)

print("Generated test_input.wav")
