package com.dome.librarynightwave.model.repository.h264;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class H264Decoder {
    private static final String TAG = "H264Decoder";
    private static final String MIME_TYPE = "video/avc";
    private static final int TIMEOUT_US = 10000;
    private static final int MAX_QUEUE_SIZE = 10; // Prevent memory overload
    private static final long MAX_FRAME_DELAY_US = 200000; // 200ms max delay

    private final ConcurrentLinkedQueue<FrameData> frameQueue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition frameAvailable = queueLock.newCondition();
    private final AtomicLong lastPts = new AtomicLong(0);
    private final AtomicLong ptsOffset = new AtomicLong(0);

    private MediaCodec mediaCodec;
    private final Object codecLock = new Object();

    private volatile boolean isRunning = false;
    private volatile boolean isStreamingReadyToView = false;
    private Thread decoderThread;
    private Surface outputSurface;
    private long frameProcessingStartTime;
    private boolean isHighResolution = false;

    public interface SizeChangedCallback {
        void onOutputFormatChanged(int width, int height);

        void enableStreamingView(boolean isVisible);
    }

    private SizeChangedCallback callback;

    public void setSizeChangedCallback(SizeChangedCallback callback) {
        this.callback = callback;
    }
    private static class FrameData {
        final ByteBuffer buffer;
        final int size;
        final long pts;
        final int flags;
        final boolean isKeyFrame;

        FrameData(ByteBuffer buffer, int size, long pts, int flags, boolean isKeyFrame) {
            this.buffer = buffer;
            this.size = size;
            this.pts = pts;
            this.flags = flags;
            this.isKeyFrame = isKeyFrame;
        }
    }

    public void initialize(Surface surface, int width, int height, boolean isHighRes) {
        try {
            Log.e(TAG,"initialise called width : " + width + " height : " + height);
            isHighResolution = isHighRes;
            outputSurface = surface;
            if (mediaCodec == null) {
                Log.e(TAG, "initialize mediacodec");
                mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            } else {
                try {
                    mediaCodec.stop();
                    mediaCodec.reset();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping/resetting codec: " + e.getMessage());
                    try {
                        if (mediaCodec != null) {
                            mediaCodec.release();
                            mediaCodec = null;
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error releasing codec after stop/reset failure: " + ex.getMessage());
                    }
                    mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
                }
            }

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            format.setInteger(MediaFormat.KEY_LATENCY, 2);
            format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
            format.setInteger(MediaFormat.KEY_PRIORITY, 0);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

            if (Build.MANUFACTURER.equalsIgnoreCase("qcom")) {
                format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
            } else if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
                format.setInteger(MediaFormat.KEY_TEMPORAL_LAYERING, 0);
            }

            if (/*outputSurface != null && */outputSurface.isValid()) {
                Log.e(TAG, "initialized mediacodec to start");
                mediaCodec.configure(format, outputSurface, null, 0);
                mediaCodec.start();
            } else {
                Log.e(TAG, "not a valid surface");
                return;
            }

            isRunning = true;
            startDecoderThread();
        } catch (Exception e) {
            Log.e(TAG, "Initialize error: " + e.getLocalizedMessage());
        }
    }

    public void release() {
        isRunning = false;
        isStreamingReadyToView = false;

        queueLock.lock();
        try {
            frameAvailable.signalAll();
        } finally {
            queueLock.unlock();
        }

        if (decoderThread != null) {
            try {
                decoderThread.join(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Decoder thread join interrupted");
            }
        }

        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "MediaCodec stop failed: " + e.getMessage());
            }
            mediaCodec.release();
            mediaCodec = null;
        }

        frameQueue.clear();
        outputSurface = null;
    }

    public void enqueueFrame(ByteBuffer frameBuffer, int size, long pts, int flags) {
        if (!isRunning || frameBuffer == null || size <= 0) return;

        boolean isKeyFrame = (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
        queueLock.lock();
        try {
            // Prevent queue from growing too large
            while (frameQueue.size() >= MAX_QUEUE_SIZE && isRunning) {
                try {
                    frameAvailable.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (!isRunning) return;

            long calculatedPts = pts == 0 ? calculatePts(size, isKeyFrame) : pts;
            ByteBuffer copyBuffer = ByteBuffer.allocateDirect(size);
            frameBuffer.position(0).limit(size);
            copyBuffer.put(frameBuffer);
            copyBuffer.flip();

            frameQueue.offer(new FrameData(copyBuffer, size, calculatedPts, flags, isKeyFrame));
            frameAvailable.signal();
        } finally {
            queueLock.unlock();
        }
    }

    private long calculatePts(int frameSize, boolean isKeyFrame) {
        long currentTime = System.nanoTime() / 1000;

        if (lastPts.get() == 0) {
            ptsOffset.set(currentTime);
            lastPts.set(currentTime);
            return currentTime;
        }

        // Dynamic frame duration adjustment based on frame size
        long baseDuration = 33333; // 30fps base
        long sizeAdjustedDuration = baseDuration - (frameSize / 1024); // Add 1µs per KB

        // Key frames get slightly more time
        if (isKeyFrame) {
            sizeAdjustedDuration = (long) (sizeAdjustedDuration * 1.2);
        }

        // Clamp to reasonable values
        if (isHighResolution) {
            sizeAdjustedDuration = Math.min(sizeAdjustedDuration, 50000); // Max 50ms per frame
            sizeAdjustedDuration = Math.max(sizeAdjustedDuration, 20000); // Min 20ms per frame
        } else {
            sizeAdjustedDuration = Math.min(sizeAdjustedDuration, 40000); // Max 35ms per frame
            sizeAdjustedDuration = Math.max(sizeAdjustedDuration, 20000); // Min 20ms per frame
        }

        long newPts = lastPts.get() + sizeAdjustedDuration;
        long maxAllowedPts = currentTime - ptsOffset.get() + MAX_FRAME_DELAY_US;

        if (newPts > maxAllowedPts) {
            newPts = maxAllowedPts;
//            Log.d(TAG, "PTS clamping applied to maintain real-time playback");
        }

        lastPts.set(newPts);
        return newPts;
    }

    private void startDecoderThread() {
        decoderThread = new Thread(() -> {
            while (isRunning) {
                FrameData frameData;
                queueLock.lock();
                try {
                    frameData = frameQueue.poll();
                    if (frameData == null) {
                        frameAvailable.await(10, TimeUnit.MILLISECONDS);
                        continue;
                    }
                    frameAvailable.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    queueLock.unlock();
                }

                frameProcessingStartTime = System.nanoTime();
                decodeFrame(frameData);

                // Adaptive sleep to maintain frame rate
                long processingTime = (System.nanoTime() - frameProcessingStartTime) / 1000;
                long targetTime;
                if (isHighResolution) {
                    targetTime = frameData.isKeyFrame ? 10000 : 2000; // µs
                } else {
                    targetTime = frameData.isKeyFrame ? 3000 : 2000; // µs
                }

                if (processingTime < targetTime) {
                    try {
                        Thread.sleep((targetTime - processingTime) / 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "H264DecoderThread");
        decoderThread.setPriority(Thread.MAX_PRIORITY);
        decoderThread.start();
    }

    private void decodeFrame(FrameData frameData) {
        synchronized (codecLock) {
            try {
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        frameData.buffer.position(0).limit(frameData.size);
                        inputBuffer.put(frameData.buffer);
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, frameData.size, frameData.pts, 0);
                    }
                }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex;
            while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) >= 0) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "Output buffer with BUFFER_FLAG_CODEC_CONFIG, skipping rendering.");

                    MediaFormat format = mediaCodec.getOutputFormat();
                    int width = format.getInteger(MediaFormat.KEY_WIDTH);
                    int height = format.getInteger(MediaFormat.KEY_HEIGHT);
                    if (callback != null) {
                        callback.onOutputFormatChanged(width, height);
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    continue;
                }

                    long presentationDelay = (bufferInfo.presentationTimeUs - (System.nanoTime() / 1000 - ptsOffset.get()));
                    if (presentationDelay > 0) {
                        try {
                            Thread.sleep(presentationDelay / 1000, (int) (presentationDelay % 1000) * 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, bufferInfo.presentationTimeUs * 1000);

                    if (callback != null && !isStreamingReadyToView) {
                        callback.enableStreamingView(true);
                        isStreamingReadyToView = true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Decode frame error: " + e.getMessage());
            }
        }
    }
}