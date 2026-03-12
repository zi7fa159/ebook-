package com.alsclone.astrostacker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A simple 16-bit Greyscale TIFF writer for astrophotography results.
 */
public class TiffWriter {
    public static void saveTiff16Color(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, int blackLevel, int frameCount, boolean isSum) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, (float)blackLevel, (float)frameCount, isSum, false, null);
    }

    public static void saveTiff16Stretched(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float effectiveBlack, StretchParams params) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, effectiveBlack, 1.0f, false, true, params);
    }

    private static void saveTiff16Internal(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float black, float frameCount, boolean isSum, boolean stretched, StretchParams params) throws IOException {
        try (FileOutputStream out = new FileOutputStream(path)) {
            // TIFF Header (Little Endian)
            out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});

            int pixelDataSize = width * height * 3 * 2;
            // IFD structure: 8 (header) + pixelData + 6 (BitsPerSample) + 16 (Resolution values)
            int ifdOffset = 8 + pixelDataSize + 6 + 16;
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ifdOffset).array());

            float effectiveBlack = isSum ? (black * frameCount) : black;
            float targetMax = 65535; // Default for linear
            float midFactor = 1.0f;
            float manualBlackOffset = 0;
            float whiteClip = (1023.0f - black) * (isSum ? frameCount : 1.0f) * 0.95f;

            if (stretched && params != null) {
                midFactor = params.midFactor;
                manualBlackOffset = params.blackOffset;
            } else {
                float maxValFound = 0;
                for (int i = 0; i < data.length; i += 1000) {
                    float v = data[i] - effectiveBlack;
                    if (v > maxValFound) maxValFound = v;
                }
                float whiteLimit = (1023.0f - black) * (isSum ? frameCount : 1.0f);
                targetMax = Math.max(maxValFound, whiteLimit * 0.1f);
            }

            byte[] rowBuf = new byte[width * 3 * 2];
            ByteBuffer bb = ByteBuffer.wrap(rowBuf).order(ByteOrder.LITTLE_ENDIAN);

            for (int y = 0; y < height; y++) {
                bb.clear();
                for (int x = 0; x < width; x++) {
                    int bx = (x / 2) * 2;
                    int by = (y / 2) * 2;
                    float r = (data[by * width + bx] - effectiveBlack) * rGain;
                    float g = ((data[by * width + (bx + 1)] + data[(by + 1) * width + bx]) / 2.0f - effectiveBlack) * gGain;
                    float b = (data[(by + 1) * width + (bx + 1)] - effectiveBlack) * bGain;

                    // Purple Highlight Fix
                    if (r > whiteClip || g > whiteClip || b > whiteClip) {
                        float m = Math.max(r, Math.max(g, b));
                        if (m > whiteClip) { r = g = b = m; }
                    }

                    if (stretched && params != null) {
                        r = (r - manualBlackOffset) * params.scale;
                        g = (g - manualBlackOffset) * params.scale;
                        b = (b - manualBlackOffset) * params.scale;

                        // Sqrt stretch only for auto-stretch
                        if (params.useAuto) {
                            r = (float) Math.sqrt(Math.max(0, r / 255.0)) * 65535;
                            g = (float) Math.sqrt(Math.max(0, g / 255.0)) * 65535;
                            b = (float) Math.sqrt(Math.max(0, b / 255.0)) * 65535;
                        } else {
                            // MidFactor is already calculated from log(0.5)/log(midPoint)
                            float normR = Math.max(0, Math.min(1.0f, r / 255.0f));
                            float normG = Math.max(0, Math.min(1.0f, g / 255.0f));
                            float normB = Math.max(0, Math.min(1.0f, b / 255.0f));
                            r = (float) Math.pow(normR, midFactor) * 65535;
                            g = (float) Math.pow(normG, midFactor) * 65535;
                            b = (float) Math.pow(normB, midFactor) * 65535;
                        }
                    } else {
                        r = (r / targetMax) * 65535;
                        g = (g / targetMax) * 65535;
                        b = (b / targetMax) * 65535;
                    }

                    bb.putShort((short) Math.min(65535, (int)Math.max(0, r)));
                    bb.putShort((short) Math.min(65535, (int)Math.max(0, g)));
                    bb.putShort((short) Math.min(65535, (int)Math.max(0, b)));
                }
                out.write(rowBuf);
            }

            // 1. BitsPerSample array (3 values of 16)
            long bitsPerSampleOffset = 8 + pixelDataSize;
            out.write(ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short)16).putShort((short)16).putShort((short)16).array());

            // 2. Resolution values (72/1)
            long resOffset = bitsPerSampleOffset + 6;
            out.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(72).putInt(1)   // XRes numerator/denominator
                .putInt(72).putInt(1).array()); // YRes numerator/denominator

            // IFD (Entries MUST be sorted by tag)
            short numEntries = 13;
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(numEntries).array());

            writeTag(out, (short) 256, (short) 3, 1, width);               // ImageWidth (256)
            writeTag(out, (short) 257, (short) 3, 1, height);              // ImageLength (257)
            writeTag(out, (short) 258, (short) 3, 3, (int)bitsPerSampleOffset); // BitsPerSample (258)
            writeTag(out, (short) 259, (short) 3, 1, 1);                   // Compression (259)
            writeTag(out, (short) 262, (short) 3, 1, 2);                   // PhotometricInterpretation (262)
            writeTag(out, (short) 273, (short) 4, 1, 8);                   // StripOffsets (273)
            writeTag(out, (short) 277, (short) 3, 1, 3);                   // SamplesPerPixel (277)
            writeTag(out, (short) 278, (short) 3, 1, height);              // RowsPerStrip (278)
            writeTag(out, (short) 279, (short) 4, 1, pixelDataSize);       // StripByteCounts (279)
            writeTag(out, (short) 282, (short) 5, 1, (int)resOffset);      // XResolution (282)
            writeTag(out, (short) 283, (short) 5, 1, (int)resOffset + 8);  // YResolution (283)
            writeTag(out, (short) 284, (short) 3, 1, 1);                   // PlanarConfiguration (284)
            writeTag(out, (short) 296, (short) 3, 1, 2);                   // ResolutionUnit (296) - Inches

            out.write(new byte[]{0x00, 0x00, 0x00, 0x00});
        }
    }

    private static void writeTag(FileOutputStream out, short tag, short type, int count, int value) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(tag);
        buf.putShort(type);
        buf.putInt(count);
        buf.putInt(value);
        out.write(buf.array());
    }
}
