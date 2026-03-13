package com.alsclone.astrostacker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Robust 16-bit RGB TIFF writer for A2LS results.
 */
public class TiffWriter {
    public static void saveTiff16Color(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, int blackLevel, int frameCount, boolean isSum, int cfa, int orientation) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, (float)blackLevel, (float)frameCount, isSum, false, null, cfa, orientation);
    }

    public static void saveTiff16Stretched(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float effectiveBlack, ColorEngine.Params params, int cfa, int orientation) throws IOException {
        saveTiff16Internal(path, data, width, height, rGain, gGain, bGain, effectiveBlack, 1.0f, false, true, params, cfa, orientation);
    }

    private static void saveTiff16Internal(String path, float[] data, int width, int height, float rGain, float gGain, float bGain, float black, float frameCount, boolean isSum, boolean stretched, ColorEngine.Params params, int cfa, int orientation) throws IOException {
        int outW = (orientation == 90 || orientation == 270) ? height : width;
        int outH = (orientation == 90 || orientation == 270) ? width : height;

        try (FileOutputStream out = new FileOutputStream(path)) {
            // TIFF Header
            out.write(new byte[]{0x49, 0x49, 0x2A, 0x00});
            int pixelDataSize = outW * outH * 3 * 2;
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
                    float v = (data[i] - effectiveBlack) * Math.max(rGain, Math.max(gGain, bGain));
                    if (v > maxValFound) maxValFound = v;
                }
                // Robust normalization: use the higher of peak value or 20% of sensor range to avoid noise-floor scaling
                targetMax = Math.max(maxValFound, clip * 0.2f);
            }

            byte[] rowBuf = new byte[outW * 3 * 2];
            ByteBuffer bb = ByteBuffer.wrap(rowBuf).order(ByteOrder.LITTLE_ENDIAN);

            for (int dy = 0; dy < outH; dy++) {
                bb.clear();
                for (int dx = 0; dx < outW; dx++) {
                    // Map output coords (dx, dy) back to source coords (sx, sy)
                    int sx, sy;
                    if (orientation == 90) {
                        sx = dy;
                        sy = (outW - 1) - dx;
                    } else if (orientation == 270) {
                        sx = (outH - 1) - dy;
                        sy = dx;
                    } else if (orientation == 180) {
                        sx = (outW - 1) - dx;
                        sy = (outH - 1) - dy;
                    } else {
                        sx = dx;
                        sy = dy;
                    }

                    // Debayering with Bayer phase awareness
                    int bx = (sx / 2) * 2;
                    int by = (sy / 2) * 2;
                    int sIdx = by * width + bx;
                    float v00 = data[sIdx];
                    float v01 = data[sIdx + 1];
                    float v10 = data[sIdx + width];
                    float v11 = data[sIdx + width + 1];

                    float r, g, b;
                    switch(cfa) {
                        case 1: r=v01; g=(v00+v11)/2f; b=v10; break; // GRBG
                        case 2: r=v10; g=(v00+v11)/2f; b=v01; break; // GBRG
                        case 3: r=v11; g=(v01+v10)/2f; b=v00; break; // BGGR
                        default: r=v00; g=(v01+v10)/2f; b=v11; break; // RGGB
                    }

                    r = (r - effectiveBlack) * rGain;
                    g = (g - effectiveBlack) * gGain;
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
            writeTag(out, (short) 256, (short) 4, 1, outW); // ImageWidth
            writeTag(out, (short) 257, (short) 4, 1, outH); // ImageLength
            writeTag(out, (short) 258, (short) 3, 3, (int)bitsPerSampleOffset); // BitsPerSample
            writeTag(out, (short) 259, (short) 3, 1, 1); // Compression (none)
            writeTag(out, (short) 262, (short) 3, 1, 2); // PhotometricInterpretation (RGB)
            writeTag(out, (short) 273, (short) 4, 1, 8); // StripOffsets
            writeTag(out, (short) 277, (short) 3, 1, 3); // SamplesPerPixel
            writeTag(out, (short) 278, (short) 4, 1, outH); // RowsPerStrip
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
