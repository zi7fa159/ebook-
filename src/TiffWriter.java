package com.alsclone.astrostacker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Robust 16-bit RGB TIFF writer for A2LS results.
 */
public class TiffWriter {
    public static void saveTiff16Color(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, int blackLevel, int frameCount, boolean isSum, int cfa) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, (float)blackLevel, (float)frameCount, isSum, false, null, cfa);
    }

    public static void saveTiff16Stretched(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float effectiveBlack, ColorEngine.Params params, int cfa) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, effectiveBlack, 1.0f, false, true, params, cfa);
    }

    private static void saveTiff16Internal(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float black, float frameCount, boolean isSum, boolean stretched, ColorEngine.Params params, int cfa) throws IOException {
        try (FileOutputStream out = new FileOutputStream(path)) {
            // TIFF Header
            out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});
            int pixelDataSize = width * height * 3 * 2;
            int extraDataSize = 6 + 16; // BitsPerSample values + Resolution values
            int ifdOffset = 8 + pixelDataSize + extraDataSize;
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ifdOffset).array());

            float effectiveBlack = isSum ? (black * frameCount) : black;
            float whiteLevel = (params != null) ? params.whiteLevel : 1023.0f;
            float clip = (whiteLevel - black) * (isSum ? frameCount : 1.0f);

            float targetMax = 1.0f;
            if (!stretched) {
                float maxValFound = 0;
                for (int i = 0; i < data.length; i += 2000) {
                    float v = data[i] - effectiveBlack;
                    if (v > maxValFound) maxValFound = v;
                }
                targetMax = Math.max(maxValFound, clip * 0.05f);
            }

            byte[] rowBuf = new byte[width * 3 * 2];
            ByteBuffer bb = ByteBuffer.wrap(rowBuf).order(ByteOrder.LITTLE_ENDIAN);

            for (int y = 0; y < height; y++) {
                bb.clear();
                int rowOff = y * width;

                for (int x = 0; x < width; x++) {
                    int bx = (x / 2) * 2;
                    int by = (y / 2) * 2;
                    int b00 = by * width + bx;
                    int b01 = b00 + 1;
                    int b10 = (by + 1) * width + bx;
                    int b11 = b10 + 1;

                    float v00 = data[b00];
                    float v01 = data[b01];
                    float v10 = data[b10];
                    float v11 = data[b11];

                    float r, g1, g2, b;
                    switch(cfa) {
                        case 1: r=v01; g1=v00; g2=v11; b=v10; break; // GRBG
                        case 2: r=v10; g1=v00; g2=v11; b=v01; break; // GBRG
                        case 3: r=v11; g1=v01; g2=v10; b=v00; break; // BGGR
                        default: r=v00; g1=v01; g2=v10; b=v11; break; // RGGB
                    }

                    r = (r - effectiveBlack) * rGain;
                    float g = ((g1 + g2) / 2.0f - effectiveBlack) * gGain;
                    b = (b - effectiveBlack) * bGain;

                    if (r > clip || g > clip || b > clip) {
                        float m = Math.max(r, Math.max(g, b));
                        r = g = b = m;
                    }

                    if (stretched && params != null) {
                        r = (r - params.manualBlackOffset) * params.stretchScale / 255.0f;
                        g = (g - params.manualBlackOffset) * params.stretchScale / 255.0f;
                        b = (b - params.manualBlackOffset) * params.stretchScale / 255.0f;

                        if (params.useAutoStretch) {
                            r = (float) Math.sqrt(Math.max(0, r));
                            g = (float) Math.sqrt(Math.max(0, g));
                            b = (float) Math.sqrt(Math.max(0, b));
                        } else if (params.gamma != 1.0f) {
                            r = (float) Math.pow(Math.max(0, r), params.gamma);
                            g = (float) Math.pow(Math.max(0, g), params.gamma);
                            b = (float) Math.pow(Math.max(0, b), params.gamma);
                        }
                        r *= 65535; g *= 65535; b *= 65535;
                    } else {
                        r = (r / targetMax) * 65535;
                        g = (g / targetMax) * 65535;
                        b = (b / targetMax) * 65535;
                    }

                    bb.putShort((short) Math.max(0, Math.min(65535, (int)r)));
                    bb.putShort((short) Math.max(0, Math.min(65535, (int)g)));
                    bb.putShort((short) Math.max(0, Math.min(65535, (int)b)));
                }
                out.write(rowBuf);
            }

            // Extra Data
            long bitsPerSampleOffset = 8 + pixelDataSize;
            out.write(ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).putShort((short)16).putShort((short)16).putShort((short)16).array());
            long resOffset = bitsPerSampleOffset + 6;
            out.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putInt(72).putInt(1).putInt(72).putInt(1).array());

            // IFD
            short numEntries = 13;
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(numEntries).array());
            // Tags MUST be sorted by tag number
            writeTag(out, (short) 256, (short) 4, 1, width); // ImageWidth
            writeTag(out, (short) 257, (short) 4, 1, height); // ImageLength
            writeTag(out, (short) 258, (short) 3, 3, (int)bitsPerSampleOffset); // BitsPerSample
            writeTag(out, (short) 259, (short) 3, 1, 1); // Compression (none)
            writeTag(out, (short) 262, (short) 3, 1, 2); // PhotometricInterpretation (RGB)
            writeTag(out, (short) 273, (short) 4, 1, 8); // StripOffsets
            writeTag(out, (short) 277, (short) 3, 1, 3); // SamplesPerPixel
            writeTag(out, (short) 278, (short) 4, 1, height); // RowsPerStrip
            writeTag(out, (short) 279, (short) 4, 1, pixelDataSize); // StripByteCounts
            writeTag(out, (short) 282, (short) 5, 1, (int)resOffset); // XResolution
            writeTag(out, (short) 283, (short) 5, 1, (int)resOffset + 8); // YResolution
            writeTag(out, (short) 284, (short) 3, 1, 1); // PlanarConfiguration
            writeTag(out, (short) 296, (short) 3, 1, 2); // ResolutionUnit (inch)
            out.write(new byte[]{0x00, 0x00, 0x00, 0x00}); // Next IFD offset
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
