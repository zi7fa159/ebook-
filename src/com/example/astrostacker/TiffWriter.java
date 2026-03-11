package com.example.astrostacker;

import java.io.OutputStream;

public class TiffWriter {
    public static void write16BitGrayscale(OutputStream out, float[] buffer, int width, int height) throws Exception {
        // TIFF Header: Little Endian
        out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});

        int pixelDataSize = width * height * 2;
        int ifdOffset = 8 + pixelDataSize;

        // Offset to IFD
        out.write(ifdOffset & 0xFF);
        out.write((ifdOffset >> 8) & 0xFF);
        out.write((ifdOffset >> 16) & 0xFF);
        out.write((ifdOffset >> 24) & 0xFF);

        // Pixel Data (Scale 10/12-bit to 16-bit)
        // Find max for normalization
        float max = 0;
        for (float f : buffer) if (f > max) max = f;
        if (max < 1024) max = 1024; // Assume at least 10-bit
        float scale = 65535.0f / max;

        for (float val : buffer) {
            int v = (int)(val * scale);
            if (v > 65535) v = 65535; else if (v < 0) v = 0;
            out.write(v & 0xFF);
            out.write((v >> 8) & 0xFF);
        }

        // IFD
        short numEntries = 9;
        out.write(numEntries & 0xFF);
        out.write((numEntries >> 8) & 0xFF);

        writeEntry(out, (short)256, (short)4, 1, width);         // ImageWidth
        writeEntry(out, (short)257, (short)4, 1, height);        // ImageLength
        writeEntry(out, (short)258, (short)3, 1, 16);            // BitsPerSample
        writeEntry(out, (short)259, (short)3, 1, 1);             // Compression: None
        writeEntry(out, (short)262, (short)3, 1, 1);             // PhotometricInterpretation: BlackIsZero
        writeEntry(out, (short)273, (short)4, 1, 8);             // StripOffsets
        writeEntry(out, (short)277, (short)3, 1, 1);             // SamplesPerPixel
        writeEntry(out, (short)278, (short)4, 1, height);        // RowsPerStrip
        writeEntry(out, (short)279, (short)4, 1, pixelDataSize); // StripByteCounts

        out.write(new byte[]{0, 0, 0, 0}); // Next IFD offset
    }

    private static void writeEntry(OutputStream out, short tag, short type, int count, int val) throws Exception {
        out.write(tag & 0xFF); out.write((tag >> 8) & 0xFF);
        out.write(type & 0xFF); out.write((type >> 8) & 0xFF);
        out.write(count & 0xFF); out.write((count >> 8) & 0xFF);
        out.write((count >> 16) & 0xFF); out.write((count >> 24) & 0xFF);
        out.write(val & 0xFF); out.write((val >> 8) & 0xFF);
        out.write((val >> 16) & 0xFF); out.write((val >> 24) & 0xFF);
    }
}
