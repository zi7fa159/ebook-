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
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, (float)blackLevel, (float)frameCount, isSum, false, 0.5f, 0);
    }

    public static void saveTiff16Stretched(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float effectiveBlack, float midPoint, float whitePoint) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, effectiveBlack, 1.0f, false, true, midPoint, whitePoint);
    }

    private static void saveTiff16Internal(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float black, float frameCount, boolean isSum, boolean stretched, float midPoint, float whitePoint) throws IOException {
        try (FileOutputStream out = new FileOutputStream(path)) {
            out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});
            int pixelDataSize = width * height * 3 * 2;
            int ifdOffset = 8 + pixelDataSize + 6;
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ifdOffset).array());

            float effectiveBlack = isSum ? (black * frameCount) : black;
            float targetMax = 0;

            float midFactor = (float) (Math.log(0.5) / Math.log(midPoint));

            if (stretched) {
                targetMax = whitePoint - effectiveBlack;
            } else {
                float maxValFound = 0;
                for (int i = 0; i < data.length; i += 1000) {
                    float v = data[i] - effectiveBlack;
                    if (v > maxValFound) maxValFound = v;
                }
                float whiteLimit = (1023.0f - black) * (isSum ? frameCount : 1.0f);
                targetMax = Math.max(maxValFound, whiteLimit * 0.1f);
            }
            if (targetMax <= 0) targetMax = 1;

            byte[] rowBuf = new byte[width * 3 * 2];
            ByteBuffer bb = ByteBuffer.wrap(rowBuf).order(ByteOrder.LITTLE_ENDIAN);
            float whiteClip = (1023.0f - black) * (isSum ? frameCount : 1.0f) * 0.95f;

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

                    if (stretched) {
                        // Professional Midtones Stretch (Power Law) for "Stretched" output
                        float normR = Math.max(0, Math.min(1.0f, r / targetMax));
                        float normG = Math.max(0, Math.min(1.0f, g / targetMax));
                        float normB = Math.max(0, Math.min(1.0f, b / targetMax));
                        r = (float) Math.pow(normR, midFactor) * 65535;
                        g = (float) Math.pow(normG, midFactor) * 65535;
                        b = (float) Math.pow(normB, midFactor) * 65535;
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

            // BitsPerSample array (3 values of 16)
            long bitsPerSampleOffset = 8 + pixelDataSize;
            out.write(ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short)16).putShort((short)16).putShort((short)16).array());

            // IFD
            short numEntries = 10;
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(numEntries).array());

            writeTag(out, (short) 256, (short) 3, 1, width); // Width
            writeTag(out, (short) 257, (short) 3, 1, height); // Height
            writeTag(out, (short) 258, (short) 3, 3, (int)bitsPerSampleOffset); // BitsPerSample (Points to array)
            writeTag(out, (short) 259, (short) 3, 1, 1); // Compression (None)
            writeTag(out, (short) 262, (short) 3, 1, 2); // Photometric (RGB)
            writeTag(out, (short) 273, (short) 4, 1, 8); // StripOffsets
            writeTag(out, (short) 277, (short) 3, 1, 3); // SamplesPerPixel
            writeTag(out, (short) 278, (short) 3, 1, height); // RowsPerStrip
            writeTag(out, (short) 279, (short) 4, 1, pixelDataSize); // StripByteCounts
            writeTag(out, (short) 284, (short) 3, 1, 1); // PlanarConfig (Chunky)

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
