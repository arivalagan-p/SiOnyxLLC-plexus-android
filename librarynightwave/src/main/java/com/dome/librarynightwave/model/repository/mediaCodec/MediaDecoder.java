package com.dome.librarynightwave.model.repository.mediaCodec;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

import static java.time.LocalTime.now;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;


import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.pojo.FPSCounter;
import com.dome.librarynightwave.model.repository.pojo.IFrame;
import com.dome.librarynightwave.model.repository.pojo.OpsinPackets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class MediaDecoder {

    private final String TAG = "MediaDecoder";
    private final PriorityQueue<OpsinPackets> safeQueue = new PriorityQueue<>(Comparator.comparingInt(OpsinPackets::getSequenceNumber));
    private final List<OpsinPackets> tempSafeList = Collections.synchronizedList(new ArrayList<>());
    private final List<IFrame> iFrameList = Collections.synchronizedList(new ArrayList<>());
    private final List<byte[]> iFramePackets = Collections.synchronizedList(new ArrayList<>());


    // Constants for RTP header fields
    private static final int FRAME_RATE = 30;
    private static final int RTP_PAYLOAD_TYPE = 96;
    private static final int MAX_BUFFER_SIZE = 120;
    private static final int MAX_SEQUENCE_NUMBER = 10;
    private static final int BIT_RATE = 4000000;//4 MBPS
    private static final long TIMEOUT_US = 1000;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private static final int MAX_NUMBER = 65535;
    private static final int H264_END_FRAME_MAX_SIZE = 1024;


    private boolean isSpsPpsAndIframeReceived = false;
    private boolean isMediaCodecConfigured = false;
    private boolean isImageCaptureCalled = false;
    private boolean isOutputSurfaceImage = false;
    private boolean isFirstFrame = false;
    private boolean isOutOfOrder = false;
    private int previousSequenceNumber = -1;
    private long firstFrameTimestampUs = -1;
    private int outOfOrderCount = 0;
    private int frameRateCount = FRAME_RATE;
    private int iFrameSize = 0;


    private Surface mSurface;
    private MediaCodec mDecoder;
    private MediaFormat mediaFormat;
    private ImageReader imageReader;
    private static Context mContext;
    private static MediaDecoder mediaDecoder;
    private static MediaEncoder mediaEncoder;
    private static H264Utils h264Utils;
    private final FPSCounter fpsCounter = new FPSCounter();
    private Callbacks callback;
    private Thread queueInputBuffer;
    private Thread renderOutputBuffer;
    private boolean isReachedMaxSeqNum = false;
    private int frameNumber = 1;
    private int totalUdpReceivedOpsin = 0;
    private int missingCountPerFrame = 0;

    public void triggerOpsingRecordingState() {
        callback.triggerGetOpsinRecordingState();
    }

    public interface Callbacks {
        void updateOpsinLiveStreamingStats(int iFrameSize, int outOfOrderCount, int fpscount, int totalUdpReceivedOpsin);

        void onOpsinBitmapAvailable(Bitmap bitmap);

        void triggerGetOpsinRecordingState();
    }

    public static synchronized MediaDecoder getInstance(Context context) {
        if (mediaDecoder == null) {
            mediaDecoder = new MediaDecoder();
        }
        mContext = context;
        mediaEncoder = MediaEncoder.getInstance(context);
        initializeH264Object();
        return mediaDecoder;
    }

    private static void initializeH264Object() {
        if (h264Utils == null) {
            h264Utils = H264Utils.getInstance();
        }
    }

    public void registerClient(TCPRepository application) {
        this.callback = (Callbacks) application;
    }

    public void setTotalUdpPacketReceived(int totalUdpReceivedOpsin) {
        this.totalUdpReceivedOpsin = totalUdpReceivedOpsin;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void setSurface(Surface mSurfaceView) {
        this.mSurface = mSurfaceView;
    }


    public void setImageCaptureCalled(boolean imageCaptureCalled) {
        isImageCaptureCalled = imageCaptureCalled;
    }

    public boolean isSpsPpsAndIframeReceived() {
        return isSpsPpsAndIframeReceived;
    }

    public void setSpsPpsAndIframeReceived(boolean spsPpsAndIframeReceived) {
        isSpsPpsAndIframeReceived = spsPpsAndIframeReceived;
    }

    public void createBuffer(byte[] bytesAddressPair, int received_length) {
        RtpHeader rtpHeader = RtpHeader.fromBytes(bytesAddressPair);
        int rtpSequenceNumber = rtpHeader.getSequenceNumber();

        handleMaxSequenceNumber(rtpSequenceNumber);
        if (!isReachedMaxSeqNum) {
            safeQueue.add(new OpsinPackets(rtpHeader, rtpSequenceNumber, received_length, bytesAddressPair));
            if (safeQueue.size() > MAX_BUFFER_SIZE) {
                iterateBuffer();
            }
        } else {
            tempSafeList.add(new OpsinPackets(rtpHeader, rtpSequenceNumber, received_length, bytesAddressPair));
            iterateBuffer();
            if (safeQueue.isEmpty()) {
                safeQueue.addAll(tempSafeList);
                tempSafeList.clear();
                isReachedMaxSeqNum = false;
            }
        }
    }

    private void iterateBuffer() {
        if (safeQueue.isEmpty()) {
            return;
        }
        OpsinPackets packets = safeQueue.poll();
        if (packets != null) {
            int sequenceNumber = packets.getSequenceNumber();
            int receivedLength = packets.getReceivedLength();
            ByteBuffer packetData = ByteBuffer.wrap(packets.getArray());
            RtpHeader rtpHeader = packets.getRtpHeader();
            processForStream(packetData, receivedLength, sequenceNumber, rtpHeader);
        }
    }

    private void handleMaxSequenceNumber(int rtpSequenceNumber) {
        if (!isReachedMaxSeqNum && rtpSequenceNumber >= 0 && rtpSequenceNumber <= MAX_SEQUENCE_NUMBER && previousSequenceNumber > 64000) {
            Log.e(TAG, "processForStream: Reached Max Sequence Number rtpSequenceNumber: " + rtpSequenceNumber + " previousSequenceNumber: " + previousSequenceNumber);
            isReachedMaxSeqNum = true;
            previousSequenceNumber = -1;
        }
    }

    private void processForStream(ByteBuffer packetData, int packetLength, int currentSequenceNumber, RtpHeader rtpHeader) {
        if (previousSequenceNumber != currentSequenceNumber) {
            long timestamp = rtpHeader.getTimestamp();
            int headerLength = rtpHeader.getHeaderLength();

            // Extract H.264 payload data without copying
            byte[] payloadData = new byte[packetLength - headerLength];
            packetData.position(headerLength); // Move position to payload start
            packetData.get(payloadData);

            handlePayloadType(rtpHeader.getPayloadType(), payloadData, currentSequenceNumber, timestamp);
        }
    }

    private void handlePayloadType(int payloadType, byte[] payloadData, int currentSequenceNumber, long timestamp) {
        if (payloadType == RTP_PAYLOAD_TYPE) {
            if (h264Utils.isIframe(payloadData)) {
                handleIframe(payloadData);
                previousSequenceNumber = currentSequenceNumber;
            } else if (h264Utils.isNonIDRFrame(payloadData)) {
                byte[] iFrame = updateStatsAndGetIFrame(iFramePackets);
                addIntoTheIFrameList(iFrame, false, timestamp);
                iFramePackets.add(payloadData);
                previousSequenceNumber = currentSequenceNumber;
            } else {
                handleOutOfOrderPacket(currentSequenceNumber, payloadData, timestamp);
            }
        } else {
            Log.e(TAG, "PayloadType: " + payloadType);
        }
    }

    private void handleIframe(byte[] payloadData) {
        if (!isSpsPpsAndIframeReceived) {
            setSpsPpsAndIframeReceived(true);
            createMediaCodec();
        }

        iFramePackets.clear();
        isOutOfOrder = false;
        fpsCounter.incrementFrameCount();
        frameRateCount = (int) fpsCounter.calculateFPS();
        callback.updateOpsinLiveStreamingStats(iFrameSize, outOfOrderCount, frameRateCount, totalUdpReceivedOpsin);

        iFramePackets.add(payloadData);

    }

    private void handleOutOfOrderPacket(int currentSequenceNumber, byte[] payloadData, long timestamp) {
        if (!isOutOfOrder && iFramePackets.size() > 0 && isSpsPpsAndIframeReceived) {
            int expectedSequenceNumber = previousSequenceNumber + 1;
            if (currentSequenceNumber == expectedSequenceNumber) {
                iFramePackets.add(payloadData);
                if (payloadData.length < H264_END_FRAME_MAX_SIZE) {
                    byte[] iFrame = updateStatsAndGetIFrame(iFramePackets);
                    addIntoTheIFrameList(iFrame, true, timestamp);
                    iFramePackets.clear();
                    missingCountPerFrame = 0;
                }
                previousSequenceNumber = currentSequenceNumber;
            } else if (isReachedMaxSeqNum && previousSequenceNumber == -1) {
                iFramePackets.add(payloadData);
                if (payloadData.length < H264_END_FRAME_MAX_SIZE) {
                    byte[] iFrame = updateStatsAndGetIFrame(iFramePackets);
                    addIntoTheIFrameList(iFrame, true, timestamp);
                    iFramePackets.clear();
                    missingCountPerFrame = 0;
                }
                previousSequenceNumber = currentSequenceNumber;
            } else {
                if (missingCountPerFrame < 3) {// Allowing 3 invalid sequences
                    iFramePackets.add(payloadData);
                    if (payloadData.length < H264_END_FRAME_MAX_SIZE) {
                        byte[] iFrame = updateStatsAndGetIFrame(iFramePackets);
                        addIntoTheIFrameList(iFrame, true, timestamp);
                        iFramePackets.clear();
                        previousSequenceNumber = currentSequenceNumber;
                    }
                    missingCountPerFrame = missingCountPerFrame + 1;
                } else {
                    missingCountPerFrame = 0;

                    iFramePackets.clear();
                    outOfOrderCount = outOfOrderCount + 1;
                    isOutOfOrder = true;
                    callback.updateOpsinLiveStreamingStats(iFrameSize, outOfOrderCount, frameRateCount, totalUdpReceivedOpsin);
                }

            }
        }
    }

    private void createMediaCodec() {
        byte[] spsData = h264Utils.getSpsData();
        byte[] ppsData = h264Utils.getPpsData();

        if (isMediaCodecConfigured) {
            return;
        } else {
            if (spsData != null) {
                //Calculate Width and Height of Frame using SPS data
            }
        }
        try {
            if (spsData != null && getSurface() != null) {
                Log.e(TAG, "createMediaCodec: called");
                imageReader = ImageReader.newInstance(VIDEO_WIDTH, VIDEO_HEIGHT, ImageFormat.YUV_420_888, 1);
                imageReader.setOnImageAvailableListener(reader -> {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        Log.e(TAG, "onImageAvailable: ");
                        Bitmap bitmap = h264Utils.convertImageToBitmapArgb(image);
                        callback.onOpsinBitmapAvailable(bitmap);
                        image.close();
                        isImageCaptureCalled = false;
                    }
                }, new Handler(Looper.getMainLooper()));

                mediaFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mediaFormat.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 1);
                    mediaFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
                    mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
                }
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
                }

                mDecoder = MediaCodec.createDecoderByType(MIMETYPE_VIDEO_AVC);
                int[] colorFormats = mDecoder.getCodecInfo().getCapabilitiesForType(MIMETYPE_VIDEO_AVC).colorFormats;
                Log.e(TAG, "createMediaCodec: " + Arrays.toString(colorFormats));

                mDecoder.configure(mediaFormat, getSurface(), null, 0);
                mDecoder.start();
                isMediaCodecConfigured = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] updateStatsAndGetIFrame(List<byte[]> iFramePackets) {
        byte[] iFrameData = h264Utils.createIFrameFromPackets(iFramePackets);
        iFrameSize = iFrameData.length;
        callback.updateOpsinLiveStreamingStats(iFrameSize, outOfOrderCount, frameRateCount, totalUdpReceivedOpsin);
        return iFrameData;

    }

    private void addIntoTheIFrameList(byte[] iFramePackets, boolean isIframe, long timestamp) {
        if (isSpsPpsAndIframeReceived && mediaEncoder.isRecording()) {
            mediaEncoder.onH264FrameReceived(iFramePackets);
        }
        byte[] spsData = h264Utils.getSpsData();
        byte[] ppsData = h264Utils.getPpsData();
        iFrameList.add(new IFrame(iFramePackets, isIframe, timestamp, spsData, ppsData));
    }

    public void queueInputBuffer() {
        if (queueInputBuffer == null && mDecoder != null) {
            Log.e(TAG, "queueInputBuffer: " + iFrameList.size());
            queueInputBuffer = new Thread(() -> {
                while (true) {
                    if (iFrameList.size() > 0 /*&& !isStopped*/) {
                        IFrame iFrame = iFrameList.get(0);
                        long timestamp = iFrame.getTimestamp();
                        byte[] iFrameData = iFrame.getiFrameData();
                        boolean iframe = iFrame.isIframe();
                        byte[] spsData = iFrame.getSpsData();
                        byte[] ppsData = iFrame.getPpsData();
                        iFrameList.remove(0);
                        prepareInputBufferQueue(iFrameData, iframe, timestamp, spsData, ppsData);
                    }
                }
            });
            queueInputBuffer.start();
        }
    }

    public void prepareInputBufferQueue(byte[] iFrameData, boolean isIframe, long timestamp, byte[] spsData, byte[] ppsData) {
        if (mDecoder != null) {
            try {
                ByteBuffer configBuffer = ByteBuffer.allocateDirect(spsData.length + ppsData.length);
                configBuffer.put(spsData);
                configBuffer.put(ppsData);
                configBuffer.flip();
                int inputBufferIndex1 = mDecoder.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufferIndex1 >= 0) {
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferIndex1);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        inputBuffer.put(configBuffer);
                        long pt = (long) (frameNumber * (1.0 / frameRateCount) * 1000000);
                        mDecoder.queueInputBuffer(inputBufferIndex1, 0, configBuffer.limit(), pt, BUFFER_FLAG_CODEC_CONFIG);
                    }
                }

                int inputBufferIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferIndex);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        inputBuffer.put(iFrameData);
                        inputBuffer.flip();
                        long presentationTimeUs = (long) (frameNumber * (1.0 / frameRateCount) * 1000000);
                        if (isIframe) {
                            mDecoder.queueInputBuffer(inputBufferIndex, 0, iFrameData.length, (int) presentationTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        } else {
                            mDecoder.queueInputBuffer(inputBufferIndex, 0, iFrameData.length, presentationTimeUs, 0);
                        }
                        frameNumber++;
                    }
                } else {
                    Log.e(TAG, "getInputBuffer: else");
                }

            } catch (Exception e) {
                Log.e(TAG, "DecodeActivity: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }


    /*Update Iframe Directly from JNI Class*/
    private void createMediaCodec(byte[] spsData, byte[] ppsData) {
        if (isMediaCodecConfigured) {
            return;
        }
        try {
            if (spsData != null && getSurface() != null) {
                Log.e(TAG, "createMediaCodec: called");
                imageReader = ImageReader.newInstance(VIDEO_WIDTH, VIDEO_HEIGHT, ImageFormat.YUV_420_888, 1);
                imageReader.setOnImageAvailableListener(reader -> {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        Log.e(TAG, "onImageAvailable: ");
                        Bitmap bitmap = h264Utils.convertImageToBitmapArgb(image);
                        callback.onOpsinBitmapAvailable(bitmap);
                        image.close();
                        isImageCaptureCalled = false;
                    }
                }, new Handler(Looper.getMainLooper()));

                mediaFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Flexible);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mediaFormat.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 0);
                    mediaFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
                    mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
                }
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));

                mDecoder = MediaCodec.createDecoderByType(MIMETYPE_VIDEO_AVC);
                int[] colorFormats = mDecoder.getCodecInfo().getCapabilitiesForType(MIMETYPE_VIDEO_AVC).colorFormats;
                Log.e(TAG, "createMediaCodec: colorFormats: " + Arrays.toString(colorFormats));

                mDecoder.configure(mediaFormat, getSurface(), null, 0);
                mDecoder.start();
                isMediaCodecConfigured = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void prepareInputBufferQueue(byte[] iFrameData, boolean isIframe, byte[] spsData, byte[] ppsData) {
        if (mDecoder != null && isSpsPpsAndIframeReceived) {
            try {
                iFrameData = removeSpsPpsFromPayload(iFrameData);
                fpsCounter.incrementFrameCount();
                frameRateCount = (int) fpsCounter.calculateFPS();
                iFrameSize = iFrameData.length;
                callback.updateOpsinLiveStreamingStats(iFrameSize, outOfOrderCount, frameRateCount, totalUdpReceivedOpsin);

                ByteBuffer configBuffer = ByteBuffer.allocateDirect(spsData.length + ppsData.length);
                configBuffer.put(spsData);
                configBuffer.put(ppsData);
                configBuffer.flip();
                int inputBufferIndex1 = mDecoder.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufferIndex1 >= 0) {
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferIndex1);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        inputBuffer.put(configBuffer);
                        long presentationTimeUs = (long) (frameNumber * (1.0 / frameRateCount) * 1000000);
                        mDecoder.queueInputBuffer(inputBufferIndex1, 0, configBuffer.limit(), (int) presentationTimeUs, BUFFER_FLAG_CODEC_CONFIG);
                    }
                }

                int inputBufferIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferIndex);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        inputBuffer.put(iFrameData);
                        inputBuffer.flip();
                        long presentationTimeUs = (long) (frameNumber * (1.0 / frameRateCount) * 1000000);
                        int flag = isIframe ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                        mDecoder.queueInputBuffer(inputBufferIndex, 0, iFrameData.length, (int) presentationTimeUs, flag);
                        frameNumber++;
                    }
                } else {
                    Log.e(TAG, "getInputBuffer: else " + inputBufferIndex);
                }

            } catch (Exception e) {
                Log.e(TAG, "DecodeActivity: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else if (!isSpsPpsAndIframeReceived) {
            Log.e(TAG, "prepareInputBufferQueue: isSpsPpsAndIframeReceived \nSPS: " + Arrays.toString(spsData) + " \nPPS: " + Arrays.toString(ppsData));
            if (ppsData.length != 0 || spsData.length != 0) {
                isMediaCodecConfigured = false;
                setSpsPpsAndIframeReceived(true);
                createMediaCodec(spsData, ppsData);
            }
        }
    }

    public byte[] removeSpsPpsFromPayload(byte[] originalArray) {
        int newLength = originalArray.length - 32;//32 is sps+pps length
        byte[] newArray = new byte[newLength];
        System.arraycopy(originalArray, 32, newArray, 0, newLength);
        return newArray;
    }

    public void increaseMissingCount() {
        outOfOrderCount = outOfOrderCount + 1;
        fpsCounter.incrementFrameCount();
        frameRateCount = (int) fpsCounter.calculateFPS();
    }

    public void renderOutputBuffer() {
        if (renderOutputBuffer == null) {
            renderOutputBuffer = new Thread(() -> {
                while (true) {
                    try {
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        if (mDecoder != null && isMediaCodecConfigured) {
                            int outputBufferIndex = mDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
                            switch (outputBufferIndex) {
                                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                    MediaFormat newFormat = mDecoder.getOutputFormat();
                                    int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
                                    int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
                                    Log.d(TAG, "New format " + mDecoder.getOutputFormat() + " WIDTH:HEIGHT " + width + " " + height);
                                    break;
                                case MediaCodec.INFO_TRY_AGAIN_LATER:
                                    break;
                                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                    break;
                                default:
                                    if (!isFirstFrame) {
                                        Log.d(TAG, "decodeIFrame: firsttimestamp");
                                        firstFrameTimestampUs = bufferInfo.presentationTimeUs;
                                        isFirstFrame = true;
                                    }
                                    if (isImageCaptureCalled) {
                                        isOutputSurfaceImage = true;
                                        mDecoder.setOutputSurface(imageReader.getSurface());
                                    } else if (isOutputSurfaceImage) {
                                        isOutputSurfaceImage = false;
                                        mDecoder.setOutputSurface(getSurface());
                                    }
                                    mDecoder.releaseOutputBuffer(outputBufferIndex, true);
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            renderOutputBuffer.start();
        }
    }

    public void stopThread() {
        if (queueInputBuffer != null && queueInputBuffer.isAlive()) {
            queueInputBuffer.interrupt();
        }
        if (renderOutputBuffer != null && renderOutputBuffer.isAlive()) {
            renderOutputBuffer.interrupt();
        }
    }

    public void stopDecoder() {
        try {
            Log.e(TAG, "stopReceiver: called ");
            isSpsPpsAndIframeReceived = false;
            isMediaCodecConfigured = false;

            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
                mDecoder = null;
                iFrameSize = 0;
                outOfOrderCount = 0;
                frameRateCount = 0;
            }
            stopThread();
        } catch (Exception e) {
            Log.e(TAG, "surfaceDestroyed: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
