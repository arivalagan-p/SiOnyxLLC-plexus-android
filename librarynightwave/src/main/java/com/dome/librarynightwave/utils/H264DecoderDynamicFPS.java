package com.dome.librarynightwave.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class H264DecoderDynamicFPS { // Keeping the class name as provided by the user
    private static final String TAG = "H264Decoder";
    private static final String MIME_TYPE = "video/avc";
    private static final int TIMEOUT_US = 10000;
    private static final int MAX_QUEUE_SIZE = 200;
    private static final long MAX_FRAME_DELAY_US = 200000; // 200ms max delay

    // New constants and variables for FPS smoothing
    private static final long FPS_SMOOTHING_WINDOW_MS = 5000; // 5 seconds
    // Initial average FPS is set to 30.0, serving as your fixed target/default
    private volatile double averagedFps = 30.0; // This will now represent the actual observed FPS
    private final long[] frameTimestamps = new long[30 * 5]; // For 5 seconds at 30 FPS, adjust size based on max expected FPS
    private int frameTimestampIndex = 0;
    private int frameCount = 0;
    private final ReentrantLock fpsLock = new ReentrantLock(); // To protect FPS calculation variables
    private final AtomicLong lastRenderTimeNs = new AtomicLong(0); // Tracks the actual system time of last frame render

    // ADDED: Constants and variables for commanded target FPS based on user's rules
    private static final int ORIGINAL_TARGET_FPS = 30;
    private volatile int commandedTargetFps = ORIGINAL_TARGET_FPS;

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<FrameData> frameQueue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition frameAvailable = queueLock.newCondition();
    private final AtomicLong lastPts = new AtomicLong(0);
    private final AtomicLong ptsOffset = new AtomicLong(0);

    private MediaCodec mediaCodec;

    private final Object codecLock = new Object();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread decoderThread;
    private Surface outputSurface;
    private long frameProcessingStartTime;
    private long lastPresentedPtsUs = -1;

    private volatile boolean isStreamingReadyToView = false;

    private byte[] spsData;
    private byte[] ppsData;

    public void setSpsPpsData(byte[] sps, byte[] pps) {
        this.spsData = sps;
        this.ppsData = pps;
    }

    public interface SizeChangedCallback {
        void onOutputFormatChanged(int width, int height);

        void enableStreamingView(boolean isVisible);

        void setSpsPpsWithStartCode();
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
            isRunning.set(false);
            isInitialized.set(false);
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
            format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
            format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
            format.setInteger(MediaFormat.KEY_LATENCY, 2);
            format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
            format.setInteger(MediaFormat.KEY_PRIORITY, 0);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024); //2MB

            if (Build.MANUFACTURER.equalsIgnoreCase("qcom")) {
                format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
                format.setInteger("vendor.qti-ext-dec-low-latency-mode.value", 1);
            } else if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
                format.setInteger("vendor.sec-ext-dec-low-latency-mode.value", 1);
            }

            if (spsData != null && ppsData != null) {
                format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
                format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
                format.setInteger(MediaFormat.KEY_TEMPORAL_LAYERING, 0);
                format.setInteger(MediaFormat.KEY_PRIORITY, 0);
//                format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1);
            }
            if (isHighRes || width * height > 1920 * 1080) {
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024); // 8MB
                format.setInteger("max-width", width);
                format.setInteger("max-height", height);
            }

            if (/*outputSurface != null && */outputSurface.isValid()) {
                Log.e(TAG, "initialized mediacodec to start");
                mediaCodec.configure(format, outputSurface, null, 0);
                mediaCodec.start();

            } else {
                Log.e(TAG, "not a valid surface");
                return;
            }

            isRunning.set(true);
            isInitialized.set(true);
            startDecoderThread();

        } catch (Exception e) {
            Log.e(TAG, "Initialize error: " + e.getLocalizedMessage());
        }
    }
    public boolean isInitialized() {
        return isInitialized.get();
    }
    public void release() {
        Log.w(TAG, "Decoder release called");
        isRunning.set(false);
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
        lastRenderTimeNs.set(0); // Reset for next initialization
        lastPresentedPtsUs = -1; // NEW: Reset last presented PTS
    }

    /**
     * Called when the source FPS changes. This method updates the internal 'fps'
     * and sets the 'commandedTargetFps' based on the user's specified rules.
     * The `commandedTargetFps` will then influence the timing logic.
     *
     * @param newFps The new FPS value from the source.
     */
    public void onFpsChanged(int newFps) {
        if (newFps >= 55) {
            commandedTargetFps = 60;
        } else if (newFps >= 45) {
            commandedTargetFps = 50;
        } else if (newFps >= 35) {
            commandedTargetFps = 40;
        } else if (newFps >= 20) {
            commandedTargetFps = 30;
        } else if (newFps > 0) {
            commandedTargetFps = newFps;  // Accept low FPS
        } else {
            commandedTargetFps = ORIGINAL_TARGET_FPS; // Fallback for 0 or negative input
        }

        // Ensure commandedTargetFps is never zero or negative to avoid division by zero
        if (commandedTargetFps <= 0) {
            commandedTargetFps = ORIGINAL_TARGET_FPS; // Fallback to a safe default
            Log.w(TAG, "Commanded target FPS was non-positive (" + newFps + "), falling back to " + ORIGINAL_TARGET_FPS);
        }
    }

    public void enqueueFrame(ByteBuffer frameBuffer, int size, long pts, int flags) {
        if (!isRunning.get() || frameBuffer == null || size <= 0) {
            Log.w(TAG, "Skipping frame - decoder not running or invalid frame");
            return;
        }
        boolean isKeyFrame = (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
        queueLock.lock();
        try {
            // If the queue is full, drop the incoming frame to avoid blocking and OOM.
            if (frameQueue.size() >= MAX_QUEUE_SIZE) {
                Log.w(TAG, "Frame queue full, dropping frame."); // Optional: log dropped frames
                return; // Drop the frame
            }

            // Using commandedTargetFps for PTS calculation when PTS is not provided by source
            long calculatedPts = pts == 0 ? calculatePts(size, isKeyFrame) : pts;
            ByteBuffer copyBuffer = ByteBuffer.allocateDirect(size);
            frameBuffer.position(0).limit(size);
            copyBuffer.put(frameBuffer);
            copyBuffer.flip();

            frameQueue.offer(new FrameData(copyBuffer, size, calculatedPts, flags, isKeyFrame));
            frameAvailable.signal(); // Signal that a new frame is available
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

        // Use commandedTargetFps for calculating target frame duration when generating PTS
        long targetFrameDurationUs = (long) (1_000_000.0 / (commandedTargetFps > 0 ? commandedTargetFps : ORIGINAL_TARGET_FPS));

        long newPts = lastPts.get() + targetFrameDurationUs;

        long maxAllowedPts = currentTime - ptsOffset.get() + MAX_FRAME_DELAY_US;
        if (newPts > maxAllowedPts) {
            newPts = maxAllowedPts;
//            Log.d(TAG, "PTS clamping applied to maintain real-time playback: newPts=" + newPts + ", maxAllowedPts=" + maxAllowedPts);
        }
        lastPts.set(newPts);
        return newPts;
    }

    /**
     * Updates the 'averagedFps' based on the actual presentation times of rendered frames.
     * This provides a real-time measure of the *actual* output FPS.
     *
     * @param presentationTimeUs The presentation timestamp of the currently rendered frame in microseconds.
     */
    private void updateAveragedFps(long presentationTimeUs) {
        fpsLock.lock();
        try {
            // Convert to milliseconds for the smoothing window, assuming it's more practical
            long currentFrameTimestampMs = presentationTimeUs / 1000;
            frameTimestamps[frameTimestampIndex] = currentFrameTimestampMs;
            frameTimestampIndex = (frameTimestampIndex + 1) % frameTimestamps.length;
            if (frameCount < frameTimestamps.length) {
                frameCount++;
            }

            if (frameCount < 2) {
                return;
            }

            long oldestTimestamp = Long.MAX_VALUE;
            long newestTimestamp = 0;

            // Find the oldest and newest timestamps in the current window
            for (int i = 0; i < frameCount; i++) {
                if (frameTimestamps[i] < oldestTimestamp) {
                    oldestTimestamp = frameTimestamps[i];
                }
                if (frameTimestamps[i] > newestTimestamp) {
                    newestTimestamp = frameTimestamps[i];
                }
            }

            long timeSpanMs = newestTimestamp - oldestTimestamp;

            // Ensure the time span covers at least a significant portion of the smoothing window
            // This prevents very high initial FPS readings if only a few frames are processed quickly
            if (timeSpanMs > FPS_SMOOTHING_WINDOW_MS / 2 && timeSpanMs > 0) {
                double currentInstantFps = (double) frameCount * 1000.0 / timeSpanMs;
                double alpha = 0.1; // Responsiveness factor for Exponential Moving Average
                averagedFps = (alpha * currentInstantFps) + ((1 - alpha) * averagedFps);
            }
        } finally {
            fpsLock.unlock();
        }
    }

    private void startDecoderThread() {
        decoderThread = new Thread(() -> {
            while (isRunning.get()) {
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

                long processingTime = (System.nanoTime() - frameProcessingStartTime) / 1000;
                long minimalSleepUs = 1000; // e.g., 1ms to yield CPU

                if (processingTime < minimalSleepUs) {
                    try {
                        Thread.sleep((minimalSleepUs - processingTime) / 1000);
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

                    // Log the PTS of every frame processed by MediaCodec
//                Log.d(TAG, "MediaCodec Output PTS: " + bufferInfo.presentationTimeUs + ", Flags: " + bufferInfo.flags);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "Output EOS");
                        break;
                    }

                    if (bufferInfo.size == 0) {
                        Log.d(TAG, "Skipping zero-size output buffer");
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        continue;
                    }

                    if (bufferInfo.presentationTimeUs > lastPresentedPtsUs) {
                        lastPresentedPtsUs = bufferInfo.presentationTimeUs;
                        long currentSystemTimeNs = System.nanoTime(); // The actual current time
                        long delayNs;

                        // Prioritize Keyframe presentation (render immediately)
                        if (frameData.isKeyFrame) {
                            delayNs = 0; // Render keyframe immediately, effectively
//                        Log.d(TAG, "Keyframe detected, rendering immediately. PTS: " + bufferInfo.presentationTimeUs);
                            lastRenderTimeNs.set(currentSystemTimeNs); // Update lastRenderTimeNs for keyframes
                        } else {
                            // For non-keyframes, aim for consistent presentation based on commandedTargetFps
                            // This directly uses the FPS set by your rule.
                            long targetFrameDurationNs = (long) (1_000_000_000.0 / (commandedTargetFps > 0 ? commandedTargetFps : ORIGINAL_TARGET_FPS));
                            long expectedRenderTimeNs;

                            if (lastRenderTimeNs.get() == 0) { // First frame or after a reset/keyframe
                                expectedRenderTimeNs = currentSystemTimeNs;
                            } else {
                                expectedRenderTimeNs = lastRenderTimeNs.get() + targetFrameDurationNs;
                            }

                            delayNs = expectedRenderTimeNs - currentSystemTimeNs;

                            // Apply limits to delay:
                            // - Max positive delay to prevent excessive buffering/latency
                            // - If we are very late (negative delay), render immediately to catch up
                            long maxPositiveDelayNs = targetFrameDurationNs * 2; // Allow up to 2 frame durations of delay
                            if (delayNs > maxPositiveDelayNs) {
                                delayNs = maxPositiveDelayNs;
//                            Log.d(TAG, "Clamping positive delay to " + (delayNs / 1_000_000) + "ms for non-keyframe. Commanded FPS: " + commandedTargetFps);
                            } else if (delayNs < -targetFrameDurationNs) { // If we're more than one frame duration late
                                delayNs = 0; // Catch up, render immediately
//                            Log.w(TAG, "Non-keyframe significantly late, rendering immediately. Late by: " + (-delayNs / 1_000_000) + "ms. Commanded FPS: " + commandedTargetFps);
                            }

//                        Log.d(TAG, "Non-keyframe. Commanded FPS: " + commandedTargetFps +
//                                ", Target duration: " + targetFrameDurationNs / 1_000_000 + "ms" +
//                                ", Calculated delay: " + delayNs / 1_000_000 + "ms");
                        }

                        if (delayNs > 0) {
                            try {
                                Thread.sleep(delayNs / 1_000_000, (int) (delayNs % 1_000_000));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // Update lastRenderTimeNs to the actual system time this frame gets released/rendered
                        // This is crucial for calculating the expected render time of the *next* frame.
                        lastRenderTimeNs.set(System.nanoTime());
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, bufferInfo.presentationTimeUs * 1000);

                        // Update the averagedFps which now represents the actual observed output FPS
                        updateAveragedFps(bufferInfo.presentationTimeUs);

                        if (callback != null && !isStreamingReadyToView) {
                            callback.enableStreamingView(true);
                            isStreamingReadyToView = true;
                        }
                    } else {
                        // This is an old frame being re-dequeued (PTS not strictly newer).
                        // Release it without rendering to the surface to prevent repetition.
                        Log.d(TAG, "Skipping re-rendering old frame. Current PTS: " + bufferInfo.presentationTimeUs + ", Last Presented PTS: " + lastPresentedPtsUs);
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false); // Release without rendering
                        if (callback != null && !isStreamingReadyToView) {
                            callback.enableStreamingView(true);
                            isStreamingReadyToView = true;
                        }
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "Decode frame error: " + e.getMessage());
            }
        }
    }
}
