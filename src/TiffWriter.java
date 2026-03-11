package com.alsclone.astrostacker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A simple 16-bit Greyscale TIFF writer for astrophotography results.
 */
public class TiffWriter {
    public static void saveTiff16(String path, float[] data, int width, int height) throws IOException {
        try (FileOutputStream out = new FileOutputStream(path)) {
            // TIFF Header (Little Endian)
            out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});

            int ifdOffset = 8 + (width * height * 2);
            byte[] ifdOffsetBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ifdOffset).array();
            out.write(ifdOffsetBytes);

            // Pixel Data
            ByteBuffer pixelBuffer = ByteBuffer.allocate(width * height * 2).order(ByteOrder.LITTLE_ENDIAN);
            float maxVal = 0;
            for (float f : data) if (f > maxVal) maxVal = f;
            if (maxVal == 0) maxVal = 1;

            for (float f : data) {
                int val = (int) ((f / maxVal) * 65535);
                if (val > 65535) val = 65535;
                if (val < 0) val = 0;
                pixelBuffer.putShort((short) val);
            }
            out.write(pixelBuffer.array());

            // IFD (Image File Directory)
            short numEntries = 8;
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(numEntries).array());

            // 1. Width (256)
            writeTag(out, (short) 256, (short) 3, 1, width);
            // 2. Height (257)
            writeTag(out, (short) 257, (short) 3, 1, height);
            // 3. BitsPerSample (258)
            writeTag(out, (short) 258, (short) 3, 1, 16);
            // 4. Compression (259) - 1 = No compression
            writeTag(out, (short) 259, (short) 3, 1, 1);
            // 5. PhotometricInterpretation (262) - 1 = BlackIsZero
            writeTag(out, (short) 262, (short) 3, 1, 1);
            // 6. StripOffsets (273)
            writeTag(out, (short) 273, (short) 4, 1, 8);
            // 7. RowsPerStrip (278)
            writeTag(out, (short) 278, (short) 3, 1, height);
            // 8. StripByteCounts (279)
            writeTag(out, (short) 279, (short) 4, 1, width * height * 2);

            // Next IFD Offset (0)
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
