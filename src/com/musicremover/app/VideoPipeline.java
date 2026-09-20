package com.musicremover.app;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VideoPipeline {

    private static final String TAG = "VideoPipeline";

    public interface ProgressListener {
        void onProgress(int percent, String stage);
    }

    public static class PipelineResult {
        public String outputPath;
        public double videoDurationSec;
        public long totalTimeMs;
        public long inputAcquisitionMs;
        public long audioProcessingMs;
        public long muxingMs;
        public double peakRamMb;
        public double rtf;
    }

    public static PipelineResult processVideo(Context context, String inputUrlOrPath, File outputFile, ProgressListener listener) throws Exception {
        PipelineResult result = new PipelineResult();
        long startTime = System.currentTimeMillis();

        // 1. Obtain input file (download if URL, or verify local file)
        if (listener != null) listener.onProgress(0, "Obtaining input video...");
        long acqStart = System.currentTimeMillis();
        File inputFile = obtainInputFile(context, inputUrlOrPath, listener);
        result.inputAcquisitionMs = System.currentTimeMillis() - acqStart;

        if (!inputFile.exists() || inputFile.length() == 0) {
            throw new IllegalArgumentException("Input file does not exist or is empty: " + inputUrlOrPath);
        }

        // 2. Set up MediaExtractor
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(inputFile.getAbsolutePath());

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        MediaFormat videoFormat = null;
        MediaFormat audioFormat = null;

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null) {
                if (mime.startsWith("video/") && videoTrackIndex < 0) {
                    videoTrackIndex = i;
                    videoFormat = format;
                } else if (mime.startsWith("audio/") && audioTrackIndex < 0) {
                    audioTrackIndex = i;
                    audioFormat = format;
                }
            }
        }

        if (videoTrackIndex < 0 && audioTrackIndex < 0) {
            throw new IllegalArgumentException("No video or audio tracks found in file");
        }

        long durationUs = 0;
        if (videoFormat != null && videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
            durationUs = videoFormat.getLong(MediaFormat.KEY_DURATION);
        } else if (audioFormat != null && audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
            durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION);
        }
        result.videoDurationSec = durationUs / 1000000.0;
        if (result.videoDurationSec <= 0) result.videoDurationSec = 1.0;

        // 3. Set up MediaMuxer
        if (outputFile.exists()) outputFile.delete();
        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int muxerVideoTrack = -1;
        int muxerAudioTrack = -1;

        if (videoFormat != null) {
            muxerVideoTrack = muxer.addTrack(videoFormat);
        }

        long dspStart = System.currentTimeMillis();

        if (audioTrackIndex >= 0) {
            if (listener != null) listener.onProgress(20, "Extracting & Suppressing Music...");
            int sampleRate = audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channelCount = audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;

            MediaFormat outputAudioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
            outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

            muxerAudioTrack = muxer.addTrack(outputAudioFormat);
        }

        muxer.start();

        // Pass 1: Copy Video Track directly (passthrough without re-encoding)
        long muxStart = System.currentTimeMillis();
        if (videoTrackIndex >= 0 && muxerVideoTrack >= 0) {
            if (listener != null) listener.onProgress(40, "Copying video stream...");
            extractor.selectTrack(videoTrackIndex);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) break;

                bufferInfo.offset = 0;
                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                bufferInfo.flags = extractor.getSampleFlags();

                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo);
                extractor.advance();
            }
            extractor.unselectTrack(videoTrackIndex);
        }

        // Pass 2: Reset extractor position, then extract, DSP process, encode, and mux audio track
        if (audioTrackIndex >= 0 && muxerAudioTrack >= 0) {
            if (listener != null) listener.onProgress(50, "Processing audio DSP...");
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            processAndMuxAudioTrack(extractor, audioTrackIndex, audioFormat, muxer, muxerAudioTrack, durationUs, listener);
        }

        result.audioProcessingMs = System.currentTimeMillis() - dspStart;
        result.muxingMs = System.currentTimeMillis() - muxStart;

        muxer.stop();
        muxer.release();
        extractor.release();

        long totalTimeMs = System.currentTimeMillis() - startTime;
        result.totalTimeMs = totalTimeMs;
        result.outputPath = outputFile.getAbsolutePath();
        result.rtf = (totalTimeMs / 1000.0) / result.videoDurationSec;

        Runtime runtime = Runtime.getRuntime();
        long usedMemBytes = runtime.totalMemory() - runtime.freeMemory();
        result.peakRamMb = usedMemBytes / (1024.0 * 1024.0);

        if (listener != null) listener.onProgress(100, "Processing complete!");

        return result;
    }

    private static File obtainInputFile(Context context, String inputUrlOrPath, ProgressListener listener) throws Exception {
        if (!inputUrlOrPath.startsWith("http://") && !inputUrlOrPath.startsWith("https://")) {
            return new File(inputUrlOrPath);
        }

        File downloadFile = new File(context.getCacheDir(), "download_input.mp4");
        if (downloadFile.exists()) downloadFile.delete();

        URL url = new URL(inputUrlOrPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.connect();

        int totalLen = conn.getContentLength();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        FileOutputStream fos = new FileOutputStream(downloadFile);

        byte[] buf = new byte[16384];
        int read;
        long downloaded = 0;

        while ((read = is.read(buf)) != -1) {
            fos.write(buf, 0, read);
            downloaded += read;
            if (totalLen > 0 && listener != null) {
                int pct = (int) ((downloaded * 15) / totalLen);
                listener.onProgress(pct, "Downloading video: " + (downloaded / 1024) + " KB");
            }
        }

        fos.flush();
        fos.close();
        is.close();
        conn.disconnect();

        return downloadFile;
    }

    private static void processAndMuxAudioTrack(MediaExtractor extractor, int audioTrackIndex, MediaFormat inFormat,
                                                 MediaMuxer muxer, int muxerAudioTrack, long totalDurationUs,
                                                 ProgressListener listener) throws Exception {
        extractor.selectTrack(audioTrackIndex);

        String mime = inFormat.getString(MediaFormat.KEY_MIME);
        int sampleRate = inFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
        int channelCount = inFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;

        MediaCodec decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(inFormat, null, null, 0);
        decoder.start();

        MediaFormat outFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
        outFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        outFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        outFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        outFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        encoder.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        AudioProcessor dsp = new AudioProcessor();

        MediaCodec.BufferInfo decInfo = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo encInfo = new MediaCodec.BufferInfo();

        boolean extractorDone = false;
        boolean decoderDone = false;
        boolean encoderDone = false;

        long TIMEOUT_US = 5000;

        while (!encoderDone) {
            // 1. Feed Extractor to Decoder
            if (!extractorDone) {
                int decInIdx = decoder.dequeueInputBuffer(TIMEOUT_US);
                if (decInIdx >= 0) {
                    ByteBuffer decBuf = decoder.getInputBuffer(decInIdx);
                    int sampleSize = extractor.readSampleData(decBuf, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(decInIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        extractorDone = true;
                    } else {
                        long sampleTime = extractor.getSampleTime();
                        decoder.queueInputBuffer(decInIdx, 0, sampleSize, sampleTime, 0);
                        extractor.advance();
                    }
                }
            }

            // 2. Read PCM from Decoder, apply DSP, feed to Encoder without frame loss
            if (!decoderDone) {
                int decOutIdx = decoder.dequeueOutputBuffer(decInfo, TIMEOUT_US);
                if (decOutIdx >= 0) {
                    ByteBuffer decOutBuf = decoder.getOutputBuffer(decOutIdx);
                    if ((decInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decoderDone = true;
                    }

                    if (decInfo.size > 0) {
                        decOutBuf.position(decInfo.offset);
                        decOutBuf.limit(decInfo.offset + decInfo.size);

                        int shortCount = decInfo.size / 2;
                        short[] pcmIn = new short[shortCount];
                        short[] pcmOut = new short[shortCount];

                        decOutBuf.order(ByteOrder.nativeOrder()).asShortBuffer().get(pcmIn);

                        // Apply DSP AI / STFT Music Removal
                        dsp.processPcmChunk(pcmIn, pcmOut, shortCount, channelCount, sampleRate);

                        // Feed to Encoder (retry until queued to prevent frame loss)
                        boolean queued = false;
                        while (!queued) {
                            int encInIdx = encoder.dequeueInputBuffer(10000);
                            if (encInIdx >= 0) {
                                ByteBuffer encInBuf = encoder.getInputBuffer(encInIdx);
                                encInBuf.clear();
                                encInBuf.order(ByteOrder.nativeOrder()).asShortBuffer().put(pcmOut);
                                encoder.queueInputBuffer(encInIdx, 0, shortCount * 2, decInfo.presentationTimeUs,
                                        decoderDone ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                                queued = true;
                            } else {
                                // Drain encoder output if full
                                drainEncoder(encoder, muxer, muxerAudioTrack, encInfo, false);
                            }
                        }
                    }

                    decoder.releaseOutputBuffer(decOutIdx, false);

                    if (totalDurationUs > 0 && listener != null) {
                        int pct = 50 + (int) ((decInfo.presentationTimeUs * 45) / totalDurationUs);
                        listener.onProgress(Math.min(95, pct), "Processing & Suppressing Music...");
                    }
                }
            }

            // 3. Read AAC from Encoder, write to MediaMuxer
            drainEncoder(encoder, muxer, muxerAudioTrack, encInfo, decoderDone);
            if (decoderDone && (encInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                encoderDone = true;
            }
        }

        decoder.stop();
        decoder.release();

        encoder.stop();
        encoder.release();

        extractor.unselectTrack(audioTrackIndex);
    }

    private static void drainEncoder(MediaCodec encoder, MediaMuxer muxer, int trackIndex, MediaCodec.BufferInfo info, boolean endOfStream) {
        long TIMEOUT_US = 5000;
        while (true) {
            int encOutIdx = encoder.dequeueOutputBuffer(info, TIMEOUT_US);
            if (encOutIdx < 0) {
                break;
            }

            ByteBuffer encOutBuf = encoder.getOutputBuffer(encOutIdx);
            if (info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                encOutBuf.position(info.offset);
                encOutBuf.limit(info.offset + info.size);
                muxer.writeSampleData(trackIndex, encOutBuf, info);
            }

            encoder.releaseOutputBuffer(encOutIdx, false);

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }
        }
    }
}
