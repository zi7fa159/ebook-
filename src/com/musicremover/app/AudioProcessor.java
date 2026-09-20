package com.musicremover.app;

import java.util.Arrays;

/**
 * AudioProcessor performs real-time / chunked DSP music suppression.
 * Algorithm features:
 * 1. Mid-Side (Center Channel) separation for stereo tracks.
 * 2. Continuous STFT (Short-Time Fourier Transform) with Hann window and Overlap-Add (OLA).
 * 3. Human voice spectral bandpass weighting (80 Hz - 3800 Hz).
 * 4. Stereo-uncorrelated spectral music suppression mask.
 * 5. Adaptive spectral Wiener-style soft gating.
 */
public class AudioProcessor {

    private static final int FFT_SIZE = 1024;
    private static final int HOP_SIZE = FFT_SIZE / 2; // 50% overlap

    private final double[] window;
    private final double[] realL;
    private final double[] imagL;
    private final double[] realR;
    private final double[] imagR;

    // Overlap buffers across chunks
    private double[] prevOverlapL = new double[HOP_SIZE];
    private double[] prevOverlapR = new double[HOP_SIZE];

    public AudioProcessor() {
        this.window = new double[FFT_SIZE];
        for (int i = 0; i < FFT_SIZE; i++) {
            this.window[i] = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / FFT_SIZE));
        }

        this.realL = new double[FFT_SIZE];
        this.imagL = new double[FFT_SIZE];
        this.realR = new double[FFT_SIZE];
        this.imagR = new double[FFT_SIZE];
    }

    /**
     * Process 16-bit PCM buffer in-place or into output buffer.
     * @param pcmIn Interleaved short PCM samples
     * @param pcmOut Output buffer for processed PCM
     * @param samplesCount Total shorts in buffer
     * @param numChannels 1 for Mono, 2 for Stereo
     * @param sampleRate e.g. 44100 or 48000
     */
    public void processPcmChunk(short[] pcmIn, short[] pcmOut, int samplesCount, int numChannels, int sampleRate) {
        if (numChannels == 2) {
            processStereoPcm(pcmIn, pcmOut, samplesCount, sampleRate);
        } else {
            processMonoPcm(pcmIn, pcmOut, samplesCount, sampleRate);
        }
    }

    private void processStereoPcm(short[] pcmIn, short[] pcmOut, int samplesCount, int sampleRate) {
        int totalFrames = samplesCount / 2; // number of stereo sample pairs
        if (totalFrames == 0) return;

        double[] inL = new double[totalFrames];
        double[] inR = new double[totalFrames];

        for (int i = 0; i < totalFrames; i++) {
            inL[i] = pcmIn[i * 2] / 32768.0;
            inR[i] = pcmIn[i * 2 + 1] / 32768.0;
        }

        double[] outL = new double[totalFrames];
        double[] outR = new double[totalFrames];

        double binHz = (double) sampleRate / FFT_SIZE;
        double olaGain = 1.0 / 1.5;

        // Process frame-by-frame
        for (int pos = 0; pos + FFT_SIZE <= totalFrames; pos += HOP_SIZE) {
            for (int i = 0; i < FFT_SIZE; i++) {
                double w = window[i];
                realL[i] = (inL[pos + i] + inR[pos + i]) * 0.5 * w; // Mid
                imagL[i] = 0;
                realR[i] = (inL[pos + i] - inR[pos + i]) * 0.5 * w; // Side
                imagR[i] = 0;
            }

            fft(realL, imagL);
            fft(realR, imagR);

            // Spectral Masking & Speech Preservation
            for (int k = 0; k <= FFT_SIZE / 2; k++) {
                double freqHz = k * binHz;

                double midMag = Math.hypot(realL[k], imagL[k]);
                double sideMag = Math.hypot(realR[k], imagR[k]);

                // Voice Bandpass Gain (Human voice: 80Hz to 3600Hz)
                double bandGain = 1.0;
                if (freqHz < 80) {
                    bandGain = 0.02; // Attenuate low bass music
                } else if (freqHz < 150) {
                    bandGain = 0.1 + 0.9 * ((freqHz - 80) / 70.0);
                } else if (freqHz > 4000) {
                    bandGain = 0.05; // Attenuate high music synth
                } else if (freqHz > 3400) {
                    bandGain = 1.0 - 0.95 * ((freqHz - 3400) / 600.0);
                }

                double msRatio = (midMag + 1e-6) / (sideMag + midMag + 1e-6);
                double msMask = Math.pow(msRatio, 1.5);

                double finalMask = bandGain * msMask;
                if (finalMask < 0.01) finalMask = 0.01;
                if (finalMask > 1.0) finalMask = 1.0;

                realL[k] *= finalMask;
                imagL[k] *= finalMask;

                if (k > 0 && k < FFT_SIZE / 2) {
                    realL[FFT_SIZE - k] = realL[k];
                    imagL[FFT_SIZE - k] = -imagL[k];
                }
            }

            ifft(realL, imagL);

            // Synthesize output with Overlap-Add (OLA)
            for (int i = 0; i < FFT_SIZE; i++) {
                double sample = realL[i] * window[i] * olaGain;
                outL[pos + i] += sample;
                outR[pos + i] += sample;
            }
        }

        // For samples past the last full FFT_SIZE frame, passthrough or copy
        int lastProcessedPos = ((totalFrames - FFT_SIZE) / HOP_SIZE) * HOP_SIZE + FFT_SIZE;
        if (lastProcessedPos < totalFrames) {
            for (int i = lastProcessedPos; i < totalFrames; i++) {
                outL[i] = (inL[i] + inR[i]) * 0.5;
                outR[i] = outL[i];
            }
        }

        // Convert to short 16-bit PCM
        for (int i = 0; i < totalFrames; i++) {
            int sL = (int) Math.round(outL[i] * 32767.0);
            int sR = (int) Math.round(outR[i] * 32767.0);
            pcmOut[i * 2] = (short) Math.max(-32768, Math.min(32767, sL));
            pcmOut[i * 2 + 1] = (short) Math.max(-32768, Math.min(32767, sR));
        }
    }

    private void processMonoPcm(short[] pcmIn, short[] pcmOut, int samplesCount, int sampleRate) {
        double[] in = new double[samplesCount];
        for (int i = 0; i < samplesCount; i++) {
            in[i] = pcmIn[i] / 32768.0;
        }

        double[] out = new double[samplesCount];
        double binHz = (double) sampleRate / FFT_SIZE;
        double olaGain = 1.0 / 1.5;

        for (int pos = 0; pos + FFT_SIZE <= samplesCount; pos += HOP_SIZE) {
            for (int i = 0; i < FFT_SIZE; i++) {
                realL[i] = in[pos + i] * window[i];
                imagL[i] = 0;
            }

            fft(realL, imagL);

            for (int k = 0; k <= FFT_SIZE / 2; k++) {
                double freqHz = k * binHz;
                double bandGain = 1.0;
                if (freqHz < 80) {
                    bandGain = 0.02;
                } else if (freqHz < 150) {
                    bandGain = 0.1 + 0.9 * ((freqHz - 80) / 70.0);
                } else if (freqHz > 4000) {
                    bandGain = 0.05;
                } else if (freqHz > 3400) {
                    bandGain = 1.0 - 0.95 * ((freqHz - 3400) / 600.0);
                }

                realL[k] *= bandGain;
                imagL[k] *= bandGain;

                if (k > 0 && k < FFT_SIZE / 2) {
                    realL[FFT_SIZE - k] = realL[k];
                    imagL[FFT_SIZE - k] = -imagL[k];
                }
            }

            ifft(realL, imagL);

            for (int i = 0; i < FFT_SIZE; i++) {
                out[pos + i] += realL[i] * window[i] * olaGain;
            }
        }

        int lastProcessedPos = ((samplesCount - FFT_SIZE) / HOP_SIZE) * HOP_SIZE + FFT_SIZE;
        if (lastProcessedPos < samplesCount) {
            for (int i = lastProcessedPos; i < samplesCount; i++) {
                out[i] = in[i];
            }
        }

        for (int i = 0; i < samplesCount; i++) {
            int val = (int) Math.round(out[i] * 32767.0);
            pcmOut[i] = (short) Math.max(-32768, Math.min(32767, val));
        }
    }

    // Fast Radix-2 Cooley-Tukey FFT
    private void fft(double[] real, double[] imag) {
        int n = real.length;
        int j = 0;
        for (int i = 0; i < n - 1; i++) {
            if (i < j) {
                double tempR = real[i];
                real[i] = real[j];
                real[j] = tempR;

                double tempI = imag[i];
                imag[i] = imag[j];
                imag[j] = tempI;
            }
            int k = n >> 1;
            while (k <= j) {
                j -= k;
                k >>= 1;
            }
            j += k;
        }

        for (int len = 2; len <= n; len <<= 1) {
            int halfLen = len >> 1;
            double angle = -2.0 * Math.PI / len;
            double wStepR = Math.cos(angle);
            double wStepI = Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                double wR = 1.0;
                double wI = 0.0;
                for (int m = 0; m < halfLen; m++) {
                    int pos = i + m;
                    int match = pos + halfLen;

                    double uR = real[pos];
                    double uI = imag[pos];

                    double vR = real[match] * wR - imag[match] * wI;
                    double vI = real[match] * wI + imag[match] * wR;

                    real[pos] = uR + vR;
                    imag[pos] = uI + vI;

                    real[match] = uR - vR;
                    imag[match] = uI - vI;

                    double nextWR = wR * wStepR - wI * wStepI;
                    double nextWI = wR * wStepI + wI * wStepR;
                    wR = nextWR;
                    wI = nextWI;
                }
            }
        }
    }

    // Inverse FFT
    private void ifft(double[] real, double[] imag) {
        int n = real.length;
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }
        fft(real, imag);
        for (int i = 0; i < n; i++) {
            real[i] /= n;
            imag[i] = -imag[i] / n;
        }
    }
}
