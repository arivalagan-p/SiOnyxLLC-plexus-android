package com.dome.librarynightwave.model.repository.videoencoder;

import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
import static android.media.MediaCodec.INFO_TRY_AGAIN_LATER;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
import static com.dome.librarynightwave.model.repository.TCPRepository.FRAME_RATE;
import static com.dome.librarynightwave.utils.Constants.locationLatitude;
import static com.dome.librarynightwave.utils.Constants.locationLongitude;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MP4Encoder extends Encoder {
    private static final String TAG = MP4Encoder.class.getSimpleName();
    private static final int BIT_RATE = 2000000;

    private static final int I_FRAME_INTERVAL = 5;
    private static final long ONE_SEC = 1000000;
    private static final int TIMEOUT_US = 10000;
    private long presentationTimeUs = 0;
    private int videoTrackIndex = -1;
    private int addedFrameCount;
    private int encodedFrameCount;
    private boolean isMuxerStarted = false;
    private boolean isStarted = false;
    private Surface surface;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private MediaMuxer mediaMuxer;
    private MediaCodec videoCodec;


    @Override
    protected void onInit() {
    }

    @Override
    protected void onStart() {
        isStarted = true;
        addedFrameCount = 0;
        encodedFrameCount = 0;
        int width = getWidth();
        int height = getHeight();
        try {
            videoCodec = MediaCodec.createEncoderByType("video/avc"/*"video/mp4v-es"*/);
            MediaFormat format = MediaFormat.createVideoFormat("video/avc"/*"video/mp4v-es"*/, width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatSurface);

            videoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = videoCodec.createInputSurface();
            videoCodec.start();

            mediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }
    }


    @Override
    protected void onAddFrame(Bitmap bitmap) {
        if (!isStarted) {
            Log.d(TAG, "already finished. can't add Frame ");
        } else if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
        } else {
            addedFrameCount++;
            shouldLoop = false;
            encodeToMp4(bitmap);
        }
    }

    private void encodeToMp4(Bitmap bitmap) {
        try {
            presentationTimeUs += 1000000 / FRAME_RATE;
            Canvas canvas = surface.lockCanvas(null);
            canvas.drawBitmap(bitmap, 0, 0, null);
            surface.unlockCanvasAndPost(canvas);
            encodeVideo();
        } catch (Exception e) {
            notifyEncodeError(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void encodeVideo() {
        try {
            int encoderStatus = videoCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
//        Log.i("EncodeVideo", "Video encoderStatus = " + encoderStatus + ", presentationTimeUs = " + bufferInfo.presentationTimeUs);
            if (encoderStatus == INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat videoFormat = videoCodec.getOutputFormat();
                Log.i("EncodeVideo", "output format changed. video format: %s" + videoFormat + " LOCATION: " + locationLatitude + "/" + locationLongitude);
                videoTrackIndex = mediaMuxer.addTrack(videoFormat);
                mediaMuxer.setLocation((float) locationLatitude, (float) locationLongitude);
                mediaMuxer.start();
                isMuxerStarted = true;
            } else if (encoderStatus == INFO_TRY_AGAIN_LATER) {
                Log.i("EncodeVideo", "no output from video encoder available");
            } else {
                ByteBuffer encodedData = videoCodec.getOutputBuffer(encoderStatus);
                if (encodedData != null) {
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    encodedData.position(bufferInfo.offset);
                    if (isMuxerStarted) {
                        mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                    }
                    videoCodec.releaseOutputBuffer(encoderStatus, false);
                    encodedFrameCount++;
                }
                //            Log.i("EncodeVideo", "encoderOutputBuffer " + encoderStatus );
            }
            shouldLoop = true;
        } catch (Exception e) {
            e.printStackTrace();

            if (this.addedFrameCount > 0) {
                Log.i(TAG, "Total frame count = %s " + this.addedFrameCount);
                videoCodec.signalEndOfInputStream();// Only required for input surface encoding
                if (videoCodec != null) {
                    videoCodec.stop();
                    videoCodec.release();
                    videoCodec = null;
                    Log.i(TAG, "RELEASE VIDEO CODEC");
                }

                if (mediaMuxer != null) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    Log.i(TAG, "RELEASE MUXER");
                }
            } else {
                Log.e(TAG, "not added any frame");
            }
            isStarted = false;
        }
    }

    @Override
    protected void onStop() {
        if (isStarted) {
            encodeVideo();
            try {
                if (this.addedFrameCount > 0) {
                    Log.i(TAG, "Total frame count = %s " + this.addedFrameCount);
                    videoCodec.signalEndOfInputStream();// Only required for input surface encoding
                    if (videoCodec != null) {
                        videoCodec.stop();
                        videoCodec.release();
                        videoCodec = null;
                        Log.i(TAG, "RELEASE VIDEO CODEC");
                    }

                    if (mediaMuxer != null) {
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                        Log.i(TAG, "RELEASE MUXER");
                    }
                } else {
                    Log.e(TAG, "not added any frame");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isStarted = false;
            notifyEncodeFinish();
        }
    }
}
