package com.example.musicremover;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class MediaPipeline {

    private static final String TAG = "MediaPipeline";
    private static final long TIMEOUT_US = 10000L;

    public interface ProgressCallback {
        void onProgress(int percent, String stage);
    }

    private static class MuxSample {
        int trackIndex;
        byte[] data;
        MediaCodec.BufferInfo info;

        MuxSample(int trackIndex, byte[] data, MediaCodec.BufferInfo info) {
            this.trackIndex = trackIndex;
            this.data = data;
            this.info = new MediaCodec.BufferInfo();
            this.info.set(info.offset, info.size, info.presentationTimeUs, info.flags);
        }
    }

    public static File obtainInputVideo(Context context, String sourceUrl, ProgressCallback callback) throws Exception {
        if (callback != null) callback.onProgress(5, "Acquiring video...");

        File outputFile = new File(context.getCacheDir(), "input_video.mp4");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        if (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://")) {
            URL url = new URL(sourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP error code: " + responseCode);
            }

            int contentLength = connection.getContentLength();
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                if (contentLength > 0 && callback != null) {
                    int p = 5 + (int) ((totalBytes * 20) / contentLength);
                    callback.onProgress(p, "Downloading video...");
                }
            }

            output.close();
            input.close();
            connection.disconnect();
        } else {
            File localFile = new File(sourceUrl);
            if (!localFile.exists()) {
                throw new RuntimeException("Local input file does not exist: " + sourceUrl);
            }
            InputStream input = new java.io.FileInputStream(localFile);
            FileOutputStream output = new FileOutputStream(outputFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.close();
            input.close();
        }

        return outputFile;
    }

    public static File processVideo(Context context, File inputVideoFile, ProgressCallback callback) throws Exception {
        long startTime = System.currentTimeMillis();

        File outputFile = new File(context.getCacheDir(), "result.mp4");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(inputVideoFile.getAbsolutePath());

        int videoTrackIdx = -1;
        int audioTrackIdx = -1;
        MediaFormat videoFormat = null;
        MediaFormat audioFormat = null;

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/") && videoTrackIdx == -1) {
                videoTrackIdx = i;
                videoFormat = format;
            } else if (mime.startsWith("audio/") && audioTrackIdx == -1) {
                audioTrackIdx = i;
                audioFormat = format;
            }
        }

        if (videoTrackIdx == -1) {
            throw new RuntimeException("No video track found in input file");
        }

        if (callback != null) callback.onProgress(30, "Extracting audio...");

        // If no audio track exists, simply copy the video
        if (audioTrackIdx == -1) {
            if (callback != null) callback.onProgress(90, "Muxing video...");
            MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int newVideoTrack = muxer.addTrack(videoFormat);
            muxer.start();

            extractor.selectTrack(videoTrackIdx);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (true) {
                info.size = extractor.readSampleData(buffer, 0);
                if (info.size < 0) break;
                info.presentationTimeUs = extractor.getSampleTime();
                info.flags = extractor.getSampleFlags();
                muxer.writeSampleData(newVideoTrack, buffer, info);
                extractor.advance();
            }

            muxer.stop();
            muxer.release();
            extractor.release();

            if (callback != null) callback.onProgress(100, "Processing complete!");
            return outputFile;
        }

        // Audio parameters
        int sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        Log.d(TAG, "Audio Track: sampleRate=" + sampleRate + ", channels=" + channelCount);

        // Decode audio to PCM
        extractor.selectTrack(audioTrackIdx);
        MediaCodec decoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
        decoder.configure(audioFormat, null, null, 0);
        decoder.start();

        ArrayList<short[]> pcmChunks = new ArrayList<>();
        int totalPcmSamples = 0;

        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inBufferIdx = decoder.dequeueInputBuffer(TIMEOUT_US);
                if (inBufferIdx >= 0) {
                    ByteBuffer inBuffer = decoder.getInputBuffer(inBufferIdx);
                    int sampleSize = extractor.readSampleData(inBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inBufferIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inBufferIdx, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outBufferIdx = decoder.dequeueOutputBuffer(info, TIMEOUT_US);
            if (outBufferIdx >= 0) {
                ByteBuffer outBuffer = decoder.getOutputBuffer(outBufferIdx);
                if (info.size > 0) {
                    outBuffer.position(info.offset);
                    outBuffer.limit(info.offset + info.size);
                    outBuffer.order(ByteOrder.LITTLE_ENDIAN);

                    short[] chunk = new short[info.size / 2];
                    outBuffer.asShortBuffer().get(chunk);
                    pcmChunks.add(chunk);
                    totalPcmSamples += chunk.length;
                }
                decoder.releaseOutputBuffer(outBufferIdx, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
            }
        }

        decoder.stop();
        decoder.release();
        extractor.unselectTrack(audioTrackIdx);

        if (callback != null) callback.onProgress(50, "Applying AI/DSP Music Suppression...");

        short[] pcmData = new short[totalPcmSamples];
        int pos = 0;
        for (short[] chunk : pcmChunks) {
            System.arraycopy(chunk, 0, pcmData, pos, chunk.length);
            pos += chunk.length;
        }
        pcmChunks.clear();

        // Process audio via AudioProcessor
        AudioProcessor processor = new AudioProcessor(sampleRate, channelCount);
        processor.processPcm(pcmData, pcmData.length);

        if (callback != null) callback.onProgress(70, "Encoding processed audio...");

        // Encode processed PCM back to AAC
        MediaFormat encFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
        encFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        encFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        encoder.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        ArrayList<MuxSample> audioMuxSamples = new ArrayList<>();
        MediaFormat outputAudioFormat = null;

        int pcmReadOffset = 0;
        int pcmBytesTotal = pcmData.length * 2;
        ByteBuffer pcmByteBuffer = ByteBuffer.allocateDirect(pcmBytesTotal);
        pcmByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        pcmByteBuffer.asShortBuffer().put(pcmData);

        sawInputEOS = false;
        sawOutputEOS = false;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inBufferIdx = encoder.dequeueInputBuffer(TIMEOUT_US);
                if (inBufferIdx >= 0) {
                    ByteBuffer inBuffer = encoder.getInputBuffer(inBufferIdx);
                    int remainingBytes = pcmBytesTotal - pcmReadOffset;
                    int bytesToCopy = Math.min(inBuffer.capacity(), remainingBytes);

                    if (bytesToCopy > 0) {
                        pcmByteBuffer.position(pcmReadOffset);
                        pcmByteBuffer.limit(pcmReadOffset + bytesToCopy);
                        inBuffer.clear();
                        inBuffer.put(pcmByteBuffer);

                        long ptsUs = (long) (((pcmReadOffset / 2) / (double) (sampleRate * channelCount)) * 1000000L);
                        encoder.queueInputBuffer(inBufferIdx, 0, bytesToCopy, ptsUs, 0);
                        pcmReadOffset += bytesToCopy;
                    } else {
                        encoder.queueInputBuffer(inBufferIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    }
                }
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outBufferIdx = encoder.dequeueOutputBuffer(info, TIMEOUT_US);
            if (outBufferIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputAudioFormat = encoder.getOutputFormat();
            } else if (outBufferIdx >= 0) {
                ByteBuffer outBuffer = encoder.getOutputBuffer(outBufferIdx);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    info.size = 0;
                }

                if (info.size > 0) {
                    byte[] data = new byte[info.size];
                    outBuffer.position(info.offset);
                    outBuffer.get(data);
                    audioMuxSamples.add(new MuxSample(1, data, info));
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
                encoder.releaseOutputBuffer(outBufferIdx, false);
            }
        }

        encoder.stop();
        encoder.release();

        if (callback != null) callback.onProgress(85, "Extracting video frames & preparing muxer...");

        // CRITICAL FIX 1: Reset Extractor position back to start before video track extraction
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        extractor.selectTrack(videoTrackIdx);

        ArrayList<MuxSample> videoMuxSamples = new ArrayList<>();
        ByteBuffer videoBuffer = ByteBuffer.allocateDirect(2 * 1024 * 1024);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();

        while (true) {
            videoInfo.size = extractor.readSampleData(videoBuffer, 0);
            if (videoInfo.size < 0) break;
            videoInfo.presentationTimeUs = extractor.getSampleTime();
            videoInfo.flags = extractor.getSampleFlags();

            byte[] data = new byte[videoInfo.size];
            videoBuffer.position(0);
            videoBuffer.get(data);

            videoMuxSamples.add(new MuxSample(0, data, videoInfo));
            extractor.advance();
        }
        extractor.release();

        if (callback != null) callback.onProgress(92, "Interleaving and muxing final video...");

        // Setup MediaMuxer
        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int muxerVideoTrackIdx = muxer.addTrack(videoFormat);
        int muxerAudioTrackIdx = (outputAudioFormat != null) ? muxer.addTrack(outputAudioFormat) : -1;
        muxer.start();

        // CRITICAL FIX 2: Interleave video and audio samples strictly by PTS
        int vIdx = 0;
        int aIdx = 0;
        int totalVideo = videoMuxSamples.size();
        int totalAudio = audioMuxSamples.size();

        ByteBuffer writeBuffer = ByteBuffer.allocateDirect(2 * 1024 * 1024);

        while (vIdx < totalVideo || aIdx < totalAudio) {
            MuxSample sampleToWrite = null;
            if (vIdx < totalVideo && aIdx < totalAudio) {
                MuxSample vSample = videoMuxSamples.get(vIdx);
                MuxSample aSample = audioMuxSamples.get(aIdx);

                if (vSample.info.presentationTimeUs <= aSample.info.presentationTimeUs) {
                    sampleToWrite = vSample;
                    sampleToWrite.trackIndex = muxerVideoTrackIdx;
                    vIdx++;
                } else {
                    sampleToWrite = aSample;
                    sampleToWrite.trackIndex = muxerAudioTrackIdx;
                    aIdx++;
                }
            } else if (vIdx < totalVideo) {
                sampleToWrite = videoMuxSamples.get(vIdx);
                sampleToWrite.trackIndex = muxerVideoTrackIdx;
                vIdx++;
            } else {
                sampleToWrite = audioMuxSamples.get(aIdx);
                sampleToWrite.trackIndex = muxerAudioTrackIdx;
                aIdx++;
            }

            if (sampleToWrite != null && sampleToWrite.trackIndex >= 0) {
                writeBuffer.clear();
                writeBuffer.put(sampleToWrite.data);
                writeBuffer.flip();
                muxer.writeSampleData(sampleToWrite.trackIndex, writeBuffer, sampleToWrite.info);
            }
        }

        muxer.stop();
        muxer.release();

        long totalTimeMs = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Video processing completed in " + totalTimeMs + " ms");

        if (callback != null) callback.onProgress(100, "Processing complete!");

        return outputFile;
    }
}
