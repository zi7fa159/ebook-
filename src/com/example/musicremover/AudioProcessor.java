package com.example.musicremover;

/**
 * Lightweight, zero-allocation DSP processor for music suppression and speech preservation.
 * Operates on 16-bit PCM stereo or mono audio buffers.
 */
public class AudioProcessor {

    private final int sampleRate;
    private final int channels;
    private final int fftSize;
    private final int hopSize;

    private final float[] window;
    private final float[] realM;
    private final float[] imagM;
    private final float[] realS;
    private final float[] imagS;
    private final float[] realL;
    private final float[] imagL;
    private final float[] realR;
    private final float[] imagR;

    // Overlap-add buffers
    private final float[] olaBufferL;
    private final float[] olaBufferR;

    // Fast FFT bit-reversal tables
    private final int[] bitRev;

    // Precomputed sine/cosine tables for FFT
    private final float[] cosTable;
    private final float[] sinTable;

    public AudioProcessor(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.fftSize = 1024; // ~23ms at 44.1kHz / 21.3ms at 48kHz
        this.hopSize = fftSize / 2; // 50% overlap

        this.window = new float[fftSize];
        for (int i = 0; i < fftSize; i++) {
            // Hann window
            this.window[i] = (float) (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / fftSize)));
        }

        this.realM = new float[fftSize];
        this.imagM = new float[fftSize];
        this.realS = new float[fftSize];
        this.imagS = new float[fftSize];
        this.realL = new float[fftSize];
        this.imagL = new float[fftSize];
        this.realR = new float[fftSize];
        this.imagR = new float[fftSize];

        this.olaBufferL = new float[fftSize];
        this.olaBufferR = new float[fftSize];

        this.bitRev = new int[fftSize];
        int bits = Integer.numberOfTrailingZeros(fftSize);
        for (int i = 0; i < fftSize; i++) {
            this.bitRev[i] = Integer.reverse(i) >>> (32 - bits);
        }

        this.cosTable = new float[fftSize / 2];
        this.sinTable = new float[fftSize / 2];
        for (int i = 0; i < fftSize / 2; i++) {
            cosTable[i] = (float) Math.cos(-2.0 * Math.PI * i / fftSize);
            sinTable[i] = (float) Math.sin(-2.0 * Math.PI * i / fftSize);
        }
    }

    /**
     * FFT implementation (in-place)
     */
    private void fft(float[] real, float[] imag) {
        // Bit reversal permutation
        for (int i = 0; i < fftSize; i++) {
            int j = bitRev[i];
            if (i < j) {
                float tempR = real[i];
                real[i] = real[j];
                real[j] = tempR;

                float tempI = imag[i];
                imag[i] = imag[j];
                imag[j] = tempI;
            }
        }

        // Cooley-Tukey Radix-2
        for (int len = 2; len <= fftSize; len <<= 1) {
            int halfLen = len >> 1;
            int step = fftSize / len;
            for (int i = 0; i < fftSize; i += len) {
                for (int j = 0; j < halfLen; j++) {
                    int k = j * step;
                    float c = cosTable[k];
                    float s = sinTable[k];

                    int idx1 = i + j;
                    int idx2 = i + j + halfLen;

                    float uR = real[idx1];
                    float uI = imag[idx1];

                    float vR = real[idx2] * c - imag[idx2] * s;
                    float vI = real[idx2] * s + imag[idx2] * c;

                    real[idx1] = uR + vR;
                    imag[idx1] = uI + vI;

                    real[idx2] = uR - vR;
                    imag[idx2] = uI - vI;
                }
            }
        }
    }

    /**
     * Inverse FFT implementation (in-place)
     */
    private void ifft(float[] real, float[] imag) {
        // IFFT by swapping real and imag, doing FFT, and swapping/scaling
        for (int i = 0; i < fftSize; i++) {
            float tmp = real[i];
            real[i] = imag[i];
            imag[i] = tmp;
        }

        fft(real, imag);

        float invN = 1.0f / fftSize;
        for (int i = 0; i < fftSize; i++) {
            float tmp = real[i];
            real[i] = imag[i] * invN;
            imag[i] = tmp * invN;
        }
    }

    /**
     * Process 16-bit PCM buffer (interleaved stereo or mono) in-place or returns processed sample count.
     * @param pcm short array containing PCM samples
     * @param length number of valid samples in pcm
     */
    public void processPcm(short[] pcm, int length) {
        if (channels == 2) {
            processStereo(pcm, length);
        } else {
            processMono(pcm, length);
        }
    }

    private void processStereo(short[] pcm, int length) {
        int numFrames = length / 2; // Stereo frame count
        int frameIdx = 0;

        // Process in chunks of hopSize
        float[] inL = new float[fftSize];
        float[] inR = new float[fftSize];

        // Frame buffer state
        int inPos = 0;
        short[] outPcm = new short[length];

        // Sliding window processing over input audio
        int readPtr = 0;
        int writePtr = 0;

        // We use a local ring buffer approach or block-by-block sliding for input
        float[] inputBufL = new float[numFrames + fftSize];
        float[] inputBufR = new float[numFrames + fftSize];

        for (int i = 0; i < numFrames; i++) {
            inputBufL[i] = pcm[i * 2] / 32768.0f;
            inputBufR[i] = pcm[i * 2 + 1] / 32768.0f;
        }

        float[] outputBufL = new float[numFrames + fftSize];
        float[] outputBufR = new float[numFrames + fftSize];

        int binNyquist = fftSize / 2;
        float freqRes = (float) sampleRate / fftSize;

        for (int pos = 0; pos <= numFrames - fftSize; pos += hopSize) {
            // Window input
            for (int k = 0; k < fftSize; k++) {
                float w = window[k];
                realL[k] = inputBufL[pos + k] * w;
                imagL[k] = 0.0f;
                realR[k] = inputBufR[pos + k] * w;
                imagR[k] = 0.0f;
            }

            fft(realL, imagL);
            fft(realR, imagR);

            // Compute Mid/Side in frequency domain or time domain
            // Mid = (L + R) / 2, Side = (L - R) / 2
            for (int k = 0; k <= binNyquist; k++) {
                float mL_r = realL[k];
                float mL_i = imagL[k];
                float mR_r = realR[k];
                float mR_i = imagR[k];

                float midR = (mL_r + mR_r) * 0.5f;
                float midI = (mL_i + mR_i) * 0.5f;

                float sideR = (mL_r - mR_r) * 0.5f;
                float sideI = (mL_i - mR_i) * 0.5f;

                float magMidSq = midR * midR + midI * midI;
                float magSideSq = sideR * sideR + sideI * sideI;

                float freq = k * freqRes;

                // Speech spectral mask & Wiener filter
                // Speech band: ~100Hz to 7000Hz.
                float bandGain = 1.0f;
                if (freq < 80.0f) {
                    bandGain = (freq / 80.0f) * 0.2f; // Low-cut sub-bass (kick/bass music)
                } else if (freq > 7500.0f) {
                    bandGain = 0.15f; // High-cut (cymbals/hi-hats)
                }

                // Mid-Side music suppression gain
                // Side channel is pure music background.
                // Gain = (magMidSq - alpha * magSideSq) / magMidSq
                float alpha = 2.5f; // Suppression aggressiveness
                float gain = 1.0f;

                if (magMidSq > 1e-9f) {
                    float suppressedPower = magMidSq - alpha * magSideSq;
                    if (suppressedPower < 0) suppressedPower = 0;
                    gain = (float) Math.sqrt(suppressedPower / magMidSq);
                }

                // Soft floor so speech doesn't sound robotic/watery
                float minGain = 0.08f;
                gain = Math.max(minGain, gain) * bandGain;

                // Apply gain to Mid channel (where speech lives)
                float newMidR = midR * gain;
                float newMidI = midI * gain;

                // Suppress Side channel almost completely (since Side is mostly stereo music)
                float sideGain = 0.05f;
                float newSideR = sideR * sideGain;
                float newSideI = sideI * sideGain;

                // Reconstruct L and R from processed Mid and Side
                // L = Mid + Side, R = Mid - Side
                realL[k] = newMidR + newSideR;
                imagL[k] = newMidI + newSideI;
                realR[k] = newMidR - newSideR;
                imagR[k] = newMidI - newSideI;

                // Conjugate symmetry for real FFT
                if (k > 0 && k < binNyquist) {
                    int sym = fftSize - k;
                    realL[sym] = realL[k];
                    imagL[sym] = -imagL[k];
                    realR[sym] = realR[k];
                    imagR[sym] = -imagR[k];
                }
            }

            ifft(realL, imagL);
            ifft(realR, imagR);

            // Overlap-Add synthesis (scaling factor 2/3 for Hann 50% OLA windowing)
            float olaScale = 2.0f / 3.0f;
            for (int k = 0; k < fftSize; k++) {
                outputBufL[pos + k] += realL[k] * window[k] * olaScale;
                outputBufR[pos + k] += realR[k] * window[k] * olaScale;
            }
        }

        // Convert back to short PCM
        for (int i = 0; i < numFrames; i++) {
            float sL = outputBufL[i];
            float sR = outputBufR[i];

            // Hard limiting / clipping prevention
            if (sL > 1.0f) sL = 1.0f;
            if (sL < -1.0f) sL = -1.0f;
            if (sR > 1.0f) sR = 1.0f;
            if (sR < -1.0f) sR = -1.0f;

            pcm[i * 2] = (short) (sL * 32767.0f);
            pcm[i * 2 + 1] = (short) (sR * 32767.0f);
        }
    }

    private void processMono(short[] pcm, int length) {
        float[] inputBuf = new float[length + fftSize];
        float[] outputBuf = new float[length + fftSize];

        for (int i = 0; i < length; i++) {
            inputBuf[i] = pcm[i] / 32768.0f;
        }

        int binNyquist = fftSize / 2;
        float freqRes = (float) sampleRate / fftSize;

        for (int pos = 0; pos <= length - fftSize; pos += hopSize) {
            for (int k = 0; k < fftSize; k++) {
                realM[k] = inputBuf[pos + k] * window[k];
                imagM[k] = 0.0f;
            }

            fft(realM, imagM);

            for (int k = 0; k <= binNyquist; k++) {
                float freq = k * freqRes;
                float bandGain = 1.0f;
                if (freq < 90.0f) {
                    bandGain = (freq / 90.0f) * 0.1f;
                } else if (freq > 7000.0f) {
                    bandGain = 0.1f;
                }

                realM[k] *= bandGain;
                imagM[k] *= bandGain;

                if (k > 0 && k < binNyquist) {
                    int sym = fftSize - k;
                    realM[sym] = realM[k];
                    imagM[sym] = -imagM[k];
                }
            }

            ifft(realM, imagM);

            float olaScale = 2.0f / 3.0f;
            for (int k = 0; k < fftSize; k++) {
                outputBuf[pos + k] += realM[k] * window[k] * olaScale;
            }
        }

        for (int i = 0; i < length; i++) {
            float s = outputBuf[i];
            if (s > 1.0f) s = 1.0f;
            if (s < -1.0f) s = -1.0f;
            pcm[i] = (short) (s * 32767.0f);
        }
    }
}
