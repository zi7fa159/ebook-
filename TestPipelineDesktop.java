import com.musicremover.app.AudioProcessor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestPipelineDesktop {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Running Standalone Desktop AudioProcessor Test ===");

        File inputFile = new File("test_input.wav");
        if (!inputFile.exists()) {
            System.err.println("Error: test_input.wav not found! Run generate_test_audio.py first.");
            System.exit(1);
        }

        FileInputStream fis = new FileInputStream(inputFile);
        byte[] header = new byte[44];
        fis.read(header);

        // Parse WAV header
        int channels = header[22] | (header[23] << 8);
        int sampleRate = (header[24] & 0xFF) | ((header[25] & 0xFF) << 8) | ((header[26] & 0xFF) << 16) | ((header[27] & 0xFF) << 24);
        System.out.println("Input WAV: Channels=" + channels + ", SampleRate=" + sampleRate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = fis.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        fis.close();

        byte[] pcmBytes = baos.toByteArray();
        int totalShorts = pcmBytes.length / 2;
        short[] pcmIn = new short[totalShorts];
        short[] pcmOut = new short[totalShorts];

        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcmIn);

        AudioProcessor dsp = new AudioProcessor();

        long startTime = System.currentTimeMillis();

        // Chunked processing (simulating MediaCodec 4096-short buffer chunks)
        int chunkSize = 4096;
        for (int i = 0; i < totalShorts; i += chunkSize) {
            int len = Math.min(chunkSize, totalShorts - i);
            short[] chunkIn = new short[len];
            short[] chunkOut = new short[len];
            System.arraycopy(pcmIn, i, chunkIn, 0, len);

            dsp.processPcmChunk(chunkIn, chunkOut, len, channels, sampleRate);

            System.arraycopy(chunkOut, 0, pcmOut, i, len);
        }

        long elapsedTimeMs = System.currentTimeMillis() - startTime;
        double durationSec = (double) (totalShorts / channels) / sampleRate;
        double rtf = (elapsedTimeMs / 1000.0) / durationSec;

        System.out.println("Audio Duration: " + String.format("%.2f", durationSec) + " s");
        System.out.println("Processing Time: " + elapsedTimeMs + " ms");
        System.out.println("RTF (Real-Time Factor): " + String.format("%.4f", rtf) + " (x" + String.format("%.1f", 1.0 / rtf) + " faster than real-time)");

        // Measure speech vs music energy before and after
        // Recall from synthetic generator: speech was 1.0s to 4.0s
        double totalInEnergy = 0;
        double totalOutEnergy = 0;
        for (int i = 0; i < totalShorts; i++) {
            totalInEnergy += pcmIn[i] * (double) pcmIn[i];
            totalOutEnergy += pcmOut[i] * (double) pcmOut[i];
        }

        System.out.println("Total Input RMS Energy: " + String.format("%.1f", Math.sqrt(totalInEnergy / totalShorts)));
        System.out.println("Total Processed RMS Energy: " + String.format("%.1f", Math.sqrt(totalOutEnergy / totalShorts)));

        // Save output WAV
        File outputFile = new File("test_processed.wav");
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(header); // Write back original WAV header

        byte[] outPcmBytes = new byte[pcmBytes.length];
        ByteBuffer.wrap(outPcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmOut);
        fos.write(outPcmBytes);
        fos.close();

        System.out.println("Saved processed WAV to: " + outputFile.getAbsolutePath());
        System.out.println("=== Standalone AudioProcessor Test COMPLETED SUCCESSFULLY ===");
    }
}
