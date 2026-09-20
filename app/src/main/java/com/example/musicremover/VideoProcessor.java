package com.example.musicremover;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VideoProcessor {
    private static final String TAG = "VideoProcessor";

    public interface ProgressCallback {
        void onProgress(int percentage, String stage);
        void onError(String error);
        void onComplete(String outputVideoPath, long totalTimeMs, double rtf, long videoDurationMs);
    }

    public static void processVideo(final String inputUrlOrPath, final File cacheDir, final ProgressCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                try {
                    callback.onProgress(0, "Obtaining input video...");
                    File inputVideoFile;

                    if (inputUrlOrPath.startsWith("http://") || inputUrlOrPath.startsWith("https://")) {
                        inputVideoFile = new File(cacheDir, "downloaded_input.mp4");
                        downloadFile(inputUrlOrPath, inputVideoFile, callback);
                    } else {
                        inputVideoFile = new File(inputUrlOrPath);
                        if (!inputVideoFile.exists()) {
                            callback.onError("Input video file does not exist: " + inputUrlOrPath);
                            return;
                        }
                    }

                    File processedAudioFile = new File(cacheDir, "processed_audio.aac");
                    File outputVideoFile = new File(cacheDir, "result.mp4");

                    if (processedAudioFile.exists()) processedAudioFile.delete();
                    if (outputVideoFile.exists()) outputVideoFile.delete();

                    callback.onProgress(15, "Extracting & Processing Audio...");
                    long videoDurationUs = processAudioPipeline(inputVideoFile.getAbsolutePath(), processedAudioFile.getAbsolutePath(), callback);

                    callback.onProgress(80, "Muxing video with processed audio...");
                    muxVideoAndAudio(inputVideoFile.getAbsolutePath(), processedAudioFile.getAbsolutePath(), outputVideoFile.getAbsolutePath());

                    long totalTimeMs = System.currentTimeMillis() - startTime;
                    long videoDurationMs = videoDurationUs / 1000;
                    double rtf = videoDurationMs > 0 ? (double) totalTimeMs / (double) videoDurationMs : 0;

                    callback.onProgress(100, "Done");
                    callback.onComplete(outputVideoFile.getAbsolutePath(), totalTimeMs, rtf, videoDurationMs);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing video", e);
                    callback.onError("Processing failed: " + e.getMessage());
                }
            }
        }).start();
    }

    private static void downloadFile(String urlStr, File outputFile, ProgressCallback callback) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        int fileLength = conn.getContentLength();
        InputStream input = new BufferedInputStream(conn.getInputStream());
        FileOutputStream output = new FileOutputStream(outputFile);

        byte[] data = new byte[8192];
        long total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
            total += count;
            output.write(data, 0, count);
            if (fileLength > 0) {
                int progress = (int) (total * 10 / fileLength);
                callback.onProgress(progress, "Downloading video (" + (int)(total / 1024) + " KB)...");
            }
        }

        output.flush();
        output.close();
        input.close();
    }

    private static long processAudioPipeline(String inputVideoPath, String outputAudioPath, ProgressCallback callback) throws Exception {
        MediaExtractor audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(inputVideoPath);

        int audioTrackIndex = -1;
        MediaFormat inputAudioFormat = null;
        long durationUs = 0;

        for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
            MediaFormat format = audioExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioTrackIndex = i;
                inputAudioFormat = format;
                if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    durationUs = format.getLong(MediaFormat.KEY_DURATION);
                }
                break;
            }
        }

        if (audioTrackIndex == -1 || inputAudioFormat == null) {
            audioExtractor.release();
            throw new Exception("No audio track found in video");
        }

        audioExtractor.selectTrack(audioTrackIndex);
        String mime = inputAudioFormat.getString(MediaFormat.KEY_MIME);
        int sampleRate = inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        MediaCodec decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(inputAudioFormat, null, null, 0);
        decoder.start();

        // Setup AAC Encoder
        MediaFormat encoderFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channelCount);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        encoderFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encoderFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        MediaCodec encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        // Setup MediaMuxer to save audio to AAC container file
        MediaMuxer audioMuxer = new MediaMuxer(outputAudioPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int muxerAudioTrackIndex = -1;

        MediaCodec.BufferInfo decBufferInfo = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo encBufferInfo = new MediaCodec.BufferInfo();

        boolean extractorDone = false;
        boolean decoderDone = false;
        boolean encoderDone = false;
        boolean muxerStarted = false;

        AudioDspProcessor dspProcessor = new AudioDspProcessor(sampleRate, channelCount);

        long lastDecPtsUs = 0;

        while (!encoderDone) {
            // 1. Feed decoder from extractor
            if (!extractorDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(5000);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);
                    int sampleSize = audioExtractor.readSampleData(inputBuf, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        extractorDone = true;
                    } else {
                        long pts = audioExtractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, pts, 0);
                        audioExtractor.advance();
                    }
                }
            }

            // 2. Read PCM from decoder, process through DSP, feed to encoder
            if (!decoderDone) {
                int outBufIndex = decoder.dequeueOutputBuffer(decBufferInfo, 5000);
                if (outBufIndex >= 0) {
                    ByteBuffer outBuf = decoder.getOutputBuffer(outBufIndex);
                    if ((decBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decoderDone = true;
                    }

                    if (decBufferInfo.size > 0) {
                        outBuf.position(decBufferInfo.offset);
                        outBuf.limit(decBufferInfo.offset + decBufferInfo.size);

                        byte[] pcmBytes = new byte[decBufferInfo.size];
                        outBuf.get(pcmBytes);

                        // Apply Music Suppression DSP
                        byte[] processedPcmBytes = dspProcessor.processPcmChunk(pcmBytes);

                        lastDecPtsUs = decBufferInfo.presentationTimeUs;

                        // Feed processed PCM to encoder
                        int encInBufIndex = encoder.dequeueInputBuffer(10000);
                        if (encInBufIndex >= 0) {
                            ByteBuffer encInBuf = encoder.getInputBuffer(encInBufIndex);
                            encInBuf.clear();
                            encInBuf.put(processedPcmBytes);
                            encoder.queueInputBuffer(encInBufIndex, 0, processedPcmBytes.length, decBufferInfo.presentationTimeUs, 0);
                        }
                    }
                    decoder.releaseOutputBuffer(outBufIndex, false);

                    if (durationUs > 0) {
                        int pct = 15 + (int) ((lastDecPtsUs * 60) / durationUs);
                        callback.onProgress(Math.min(pct, 75), "Suppressing music (" + (lastDecPtsUs / 1000000) + "s)...");
                    }
                }
                if (decoderDone) {
                    // Send EOS to encoder
                    int encInBufIndex = encoder.dequeueInputBuffer(10000);
                    if (encInBufIndex >= 0) {
                        encoder.queueInputBuffer(encInBufIndex, 0, 0, lastDecPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
            }

            // 3. Read AAC from encoder, write to audio muxer
            int encOutBufIndex = encoder.dequeueOutputBuffer(encBufferInfo, 5000);
            if (encOutBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = encoder.getOutputFormat();
                muxerAudioTrackIndex = audioMuxer.addTrack(newFormat);
                audioMuxer.start();
                muxerStarted = true;
            } else if (encOutBufIndex >= 0) {
                ByteBuffer encOutBuf = encoder.getOutputBuffer(encOutBufIndex);
                if ((encBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    encBufferInfo.size = 0;
                }
                if (encBufferInfo.size > 0 && muxerStarted) {
                    audioMuxer.writeSampleData(muxerAudioTrackIndex, encOutBuf, encBufferInfo);
                }
                encoder.releaseOutputBuffer(encOutBufIndex, false);

                if ((encBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    encoderDone = true;
                }
            }
        }

        audioExtractor.release();
        decoder.stop();
        decoder.release();
        encoder.stop();
        encoder.release();
        if (muxerStarted) {
            audioMuxer.stop();
        }
        audioMuxer.release();

        return durationUs;
    }

    private static void muxVideoAndAudio(String inputVideoPath, String processedAudioPath, String outputVideoPath) throws Exception {
        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(inputVideoPath);

        MediaExtractor audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(processedAudioPath);

        int videoTrackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat format = videoExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) {
                videoTrackIndex = i;
                break;
            }
        }

        int audioTrackIndex = -1;
        for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
            MediaFormat format = audioExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioTrackIndex = i;
                break;
            }
        }

        if (videoTrackIndex == -1) {
            videoExtractor.release();
            audioExtractor.release();
            throw new Exception("No video track found in input video");
        }

        MediaMuxer outputMuxer = new MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        videoExtractor.selectTrack(videoTrackIndex);
        MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrackIndex);
        int outVideoTrack = outputMuxer.addTrack(videoFormat);

        int outAudioTrack = -1;
        if (audioTrackIndex != -1) {
            audioExtractor.selectTrack(audioTrackIndex);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(audioTrackIndex);
            outAudioTrack = outputMuxer.addTrack(audioFormat);
        }

        outputMuxer.start();

        // Copy video track
        int bufferSize = 1024 * 1024;
        ByteBuffer videoBuffer = ByteBuffer.allocateDirect(bufferSize);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();

        while (true) {
            videoBuffer.clear();
            int sampleSize = videoExtractor.readSampleData(videoBuffer, 0);
            if (sampleSize < 0) {
                break;
            }
            videoInfo.offset = 0;
            videoInfo.size = sampleSize;
            videoInfo.presentationTimeUs = videoExtractor.getSampleTime();
            videoInfo.flags = videoExtractor.getSampleFlags();

            outputMuxer.writeSampleData(outVideoTrack, videoBuffer, videoInfo);
            videoExtractor.advance();
        }

        // Copy audio track
        if (audioTrackIndex != -1 && outAudioTrack != -1) {
            ByteBuffer audioBuffer = ByteBuffer.allocateDirect(bufferSize);
            MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

            while (true) {
                audioBuffer.clear();
                int sampleSize = audioExtractor.readSampleData(audioBuffer, 0);
                if (sampleSize < 0) {
                    break;
                }
                audioInfo.offset = 0;
                audioInfo.size = sampleSize;
                audioInfo.presentationTimeUs = audioExtractor.getSampleTime();
                audioInfo.flags = audioExtractor.getSampleFlags();

                outputMuxer.writeSampleData(outAudioTrack, audioBuffer, audioInfo);
                audioExtractor.advance();
            }
        }

        outputMuxer.stop();
        outputMuxer.release();
        videoExtractor.release();
        audioExtractor.release();
    }
}
