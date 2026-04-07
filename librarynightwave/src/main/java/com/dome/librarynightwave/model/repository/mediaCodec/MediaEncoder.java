package com.dome.librarynightwave.model.repository.mediaCodec;

import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
import static android.media.MediaCodec.INFO_TRY_AGAIN_LATER;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
import static android.media.MediaFormat.KEY_COLOR_FORMAT;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

import static com.dome.librarynightwave.utils.Constants.locationLatitude;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.util.Log;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaEncoder {

    private final String TAG = "MediaEncoder";
    private static final int BIT_RATE = 1000000;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private static final int FRAME_RATE = 30;

    private static Context mContext;
    private static MediaEncoder mediaEncoder;
    private FileOutputStream fos;
    private MediaCodec encoder;
    private MediaMuxer muxer;

    private int videoTrackIndex = -1;
    private boolean isRecording = false;
    private boolean isMuxerStarted = false;
    private String outputPath;
    private long firstFrameTimestampUs = -1;
    private boolean isFirstFrame = false;


    public static synchronized MediaEncoder getInstance(Context context) {
        if (mediaEncoder == null) {
            mediaEncoder = new MediaEncoder();
        }
        mContext = context;
        return mediaEncoder;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording(String outputFile) {
        outputPath = outputFile;
        if (encoder == null || muxer == null) {
            Log.e(TAG, "Encoder or muxer not initialized");
            try {
                prepareEncoder();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "startRecording: Called "+outputPath);
        encoder.start();
        isRecording = true;
    }

    private void prepareEncoder() throws IOException {
        Log.e(TAG, "prepareEncoder: ");
        MediaFormat encoderFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
        encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible);
        encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            encoderFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            encoderFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
        }
        encoder = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        videoTrackIndex = -1;
        isMuxerStarted = false;
    }

    public void onH264FrameReceived(byte[] h264Data) {
        try {
            ByteBuffer inputBuffer = null;
            int inputBufferIndex = encoder.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                inputBuffer = encoder.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(h264Data);
                encoder.queueInputBuffer(inputBufferIndex, 0, h264Data.length, System.nanoTime() / 1000, 0);
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            if (outputBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat videoFormat = encoder.getOutputFormat();
                videoFormat.setInteger(KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible);
                videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
                videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
                videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    videoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
                    videoFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
                }
                videoTrackIndex = muxer.addTrack(videoFormat);
                muxer.setLocation((float) locationLatitude, (float) locationLatitude);
                muxer.start();
                isMuxerStarted = true;
                Log.i("EncodeVideo", "output format changed. video format: %s" + videoFormat + " " + videoTrackIndex);
            } else if (outputBufferIndex == INFO_TRY_AGAIN_LATER) {
                Log.i("EncodeVideo", "no output from video encoder available");
            } else {
                if (!isFirstFrame) {
                    firstFrameTimestampUs = bufferInfo.presentationTimeUs;
                    isFirstFrame = true;
                }
                /*Record Using Input Buffer*/
                bufferInfo.offset = 0;
                bufferInfo.size = h264Data.length;
                if (isMuxerStarted) {
                    muxer.writeSampleData(videoTrackIndex, inputBuffer, bufferInfo);
                }
//                ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferIndex);
                encoder.releaseOutputBuffer(outputBufferIndex, false);

//                Log.i("EncodeVideo", "encoderOutputBuffer " + outputBufferIndex );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stopRecording() {
        if (encoder == null || muxer == null || !isRecording) {
            Log.e(TAG, "Encoder or muxer not properly initialized or not recording");
            return;
        }
        Log.e(TAG, "stopRecording: Called");
        try {

            isMuxerStarted = false;
            videoTrackIndex = -1;
            isRecording = false;

            muxer.stop();
            muxer.release();
            muxer = null;

            encoder.stop();
            encoder.release();
            encoder = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
        // Add the recorded file to the MediaStore so it appears in gallery apps
        MediaScannerConnection.scanFile(mContext, new String[]{outputPath}, null, null);
    }


    /*Start of file write*/
    public void initiateFileWriting(String outputFile) {
        try {
            fos = new FileOutputStream(outputFile);
            isRecording = true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeFile(byte[] byteArray) {
        try {
            fos.write(byteArray);
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
    }

    public void stopWrite() {
        try {
            if (fos != null) {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*End of file write*/
}
