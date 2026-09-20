package com.example.musicremover;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DspTestRunner {

    public static void main(String[] args) {
        System.out.println("=== Testing AudioProcessor DSP Logic ===");
        try {
            int sampleRate = 44100;
            int channels = 2;
            int durationSec = 10;
            int totalFrames = sampleRate * durationSec;
            short[] pcmData = new short[totalFrames * channels];

            // Generate synthetic test signal: Speech (1000 Hz tone) + Music (440 Hz stereo out-of-phase tone + 100 Hz bass tone)
            for (int i = 0; i < totalFrames; i++) {
                double t = (double) i / sampleRate;

                // Speech signal (center panned)
                double speech = 0.5 * Math.sin(2.0 * Math.PI * 1000.0 * t);

                // Music signal (stereo spread)
                double musicL = 0.4 * Math.sin(2.0 * Math.PI * 440.0 * t) + 0.3 * Math.sin(2.0 * Math.PI * 60.0 * t);
                double musicR = -0.4 * Math.sin(2.0 * Math.PI * 440.0 * t) + 0.3 * Math.sin(2.0 * Math.PI * 60.0 * t);

                pcmData[i * 2] = (short) ((speech + musicL) * 16384.0);
                pcmData[i * 2 + 1] = (short) ((speech + musicR) * 16384.0);
            }

            long startTime = System.currentTimeMillis();
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            AudioProcessor processor = new AudioProcessor(sampleRate, channels);
            processor.processPcm(pcmData, pcmData.length);

            long endTime = System.currentTimeMillis();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

            long processingTimeMs = endTime - startTime;
            double videoDurationSec = durationSec;
            double rtf = (processingTimeMs / 1000.0) / videoDurationSec;

            System.out.println("Processed " + durationSec + "s audio in " + processingTimeMs + " ms");
            System.out.println("RTF (Real Time Factor): " + String.format("%.4f", rtf) + " (smaller is faster, <1.0 is real-time)");
            System.out.println("Memory delta: " + (memoryAfter - memoryBefore) / 1024 + " KB");

            if (rtf < 0.1) {
                System.out.println("DSP Speed: VERY FAST");
            } else if (rtf < 1.0) {
                System.out.println("DSP Speed: REAL-TIME PASS");
            } else {
                System.out.println("DSP Speed: TOO SLOW");
            }

            System.out.println("=== DSP Test Complete ===");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
