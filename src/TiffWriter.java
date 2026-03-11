package com.alsclone.astrostacker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A simple 16-bit Greyscale TIFF writer for astrophotography results.
 */
public class TiffWriter {
    public static void saveTiff16Color(String path, float[] data, int width, int height, float rGain, float bGain) throws IOException {
        try (FileOutputStream out = new FileOutputStream(path)) {
            // TIFF Header (Little Endian)
            out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});

            // IFD comes after pixel data
            // Each pixel becomes 3 RGB shorts = 6 bytes.
            // But we debayer, so result is width/2 x height/2 color pixels?
            // No, standard debayering produces same resolution.

            int pixelDataSize = width * height * 3 * 2;
            int ifdOffset = 8 + pixelDataSize + 6; // +6 for BitsPerSample array
            byte[] ifdOffsetBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ifdOffset).array();
            out.write(ifdOffsetBytes);

            float maxVal = 0;
            for (float f : data) if (f > maxVal) maxVal = f;
            if (maxVal == 0) maxVal = 1;

            // Pixel Data (Debayered)
            byte[] rowBuf = new byte[width * 3 * 2];
            ByteBuffer bb = ByteBuffer.wrap(rowBuf).order(ByteOrder.LITTLE_ENDIAN);

            for (int y = 0; y < height; y++) {
                bb.clear();
                for (int x = 0; x < width; x++) {
                    // Simple nearest neighbor / block debayer for simplicity in pure Java 16-bit
                    // Map current (x,y) to Bayer RGGB
                    int bx = (x / 2) * 2;
                    int by = (y / 2) * 2;

                    float r = data[by * width + bx] * rGain;
                    float g = (data[by * width + (bx + 1)] + data[(by + 1) * width + bx]) / 2.0f;
                    float b = data[(by + 1) * width + (bx + 1)] * bGain;

                    bb.putShort((short) Math.min(65535, (int)((r / maxVal) * 65535)));
                    bb.putShort((short) Math.min(65535, (int)((g / maxVal) * 65535)));
                    bb.putShort((short) Math.min(65535, (int)((b / maxVal) * 65535)));
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
