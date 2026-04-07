package com.dome.librarynightwave.utils;

import static com.dome.librarynightwave.utils.Constants.locationLatitude;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoRecorder {
    private static final String TAG = "VideoRecorder";
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    private static int FRAME_RATE = 30;
    private static final int I_FRAME_INTERVAL = 5;
    private static final int I_FRAME_LATENCY = 2;

    private MediaMuxer muxer;
    private int videoTrackIndex = -1;
    private boolean muxerStarted = false;
    private boolean isRecording = false;
    private String outputPath;

    private byte[] spsData;
    private byte[] ppsData;

    private final int videoWidth;
    private final int videoHeight;
    private final int bitRate;
    private long lastPresentationTimeUs = 0;

    private long startTimestampUs = -1;
    private boolean firstKeyFrameWritten = false;

    private static long FRAME_DURATION_US = 1_000_000 / FRAME_RATE;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public VideoRecorder(int width, int height, int bitRate) {
        this.videoWidth = width;
        this.videoHeight = height;
        this.bitRate = bitRate;
    }

    public void setSpsPpsData(byte[] sps, byte[] pps) {
        this.spsData = sps;
        this.ppsData = pps;
    }

    public void onFpsChanged(int framesPerSecond) {
        if (framesPerSecond != 0)
            FRAME_RATE = framesPerSecond;
        FRAME_DURATION_US = 1_000_000 / FRAME_RATE;

        Log.d(TAG, "Frames_per_second " + FRAME_RATE);
    }

    public void startRecording(File outputFile) throws IOException {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress");
            return;
        }

        this.outputPath = outputFile.getAbsolutePath();
        muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaFormat format = createMediaFormat();
        videoTrackIndex = muxer.addTrack(format);
        muxer.setLocation((float) locationLatitude, (float) locationLatitude);
        muxer.start();
        muxerStarted = true;
        isRecording = true;
        firstKeyFrameWritten = false;

        Log.d(TAG, "Recording started: " + outputPath);
    }

    private MediaFormat createMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate); //videoWidth * videoHeight * 3
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        format.setInteger(MediaFormat.KEY_LATENCY, I_FRAME_LATENCY);
        format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
        format.setInteger(MediaFormat.KEY_PRIORITY, 0);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024); // 256KB for large IDR frames
        if (Build.MANUFACTURER.equalsIgnoreCase("qcom")) {
            format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
        } else if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
            format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
        }

        if (spsData != null && ppsData != null) {
            format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            format.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 1);
            format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
            format.setInteger(MediaFormat.KEY_PRIORITY, 0);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        }

        return format;
    }

    public void writeFrame(ByteBuffer buffer, long streamPtsUs, boolean isKeyFrame) {
        if (!isRecording || muxer == null || !muxerStarted || buffer == null || buffer.remaining() <= 0) {
            return;
        }

        if (startTimestampUs < 0) {
            startTimestampUs = streamPtsUs;
            lastPresentationTimeUs = 0;
        }
        long presentationTimeUs = streamPtsUs - startTimestampUs;

        if (presentationTimeUs <= lastPresentationTimeUs) {
            presentationTimeUs = lastPresentationTimeUs + 1;
        }
        lastPresentationTimeUs = presentationTimeUs;

        // commented for using dynamic timestamp
//        long presentationTimeUs = lastPresentationTimeUs + FRAME_DURATION_US;
//        lastPresentationTimeUs = presentationTimeUs;

        try {
            if (isKeyFrame && !firstKeyFrameWritten && spsData != null && ppsData != null) {
                writeCodecConfigSample(spsData, presentationTimeUs);
                writeCodecConfigSample(ppsData, presentationTimeUs);
                firstKeyFrameWritten = true;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.offset = 0;
            bufferInfo.size = buffer.remaining();
            bufferInfo.presentationTimeUs = presentationTimeUs;
            bufferInfo.flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;

            buffer.position(0);
            buffer.limit(bufferInfo.size);
            muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
        } catch (Exception e) {
            Log.e(TAG, "Error writing sample data", e);
        }
    }

    private void writeCodecConfigSample(byte[] data, long pts) {
        ByteBuffer csdBuffer = ByteBuffer.wrap(data);
        MediaCodec.BufferInfo csdInfo = new MediaCodec.BufferInfo();
        csdInfo.offset = 0;
        csdInfo.size = data.length;
        csdInfo.presentationTimeUs = pts;
        csdInfo.flags = MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        muxer.writeSampleData(videoTrackIndex, csdBuffer, csdInfo);
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;

        // Grace period for last buffers to be written
        handler.postDelayed(() -> {
            try {
                if (muxer != null && muxerStarted) {
                    muxer.stop();
                    muxer.release();
                    muxer = null;
                    muxerStarted = false;
                    Log.d(TAG, "Recording stopped and saved to: " + outputPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping muxer", e);
            }
        }, 500);
    }

    public boolean isRecording() {
        return isRecording;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void release() {
        stopRecording();
        handler.removeCallbacksAndMessages(null);
    }
}
