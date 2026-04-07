/*Original*/
//package com.dome.librarynightwave.utils;
//
//import android.media.MediaCodec;
//import android.media.MediaCodecInfo;
//import android.media.MediaFormat;
//import android.os.Build;
//import android.util.Log;
//import android.util.SparseIntArray;
//import android.view.Surface;
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;
//
//public class H264DecoderDeviceSpecific {
//    private static final String TAG = "H264Decoder";
//    private static final String MIME_TYPE = "video/avc";
//    private static final int TIMEOUT_US = 10000;
//    private static final int MAX_QUEUE_SIZE = 60;
//
//    // Add shutdown control
//    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
//    private final AtomicBoolean isDecoderStopped = new AtomicBoolean(true);
//
//    // Device detection
//    private final boolean isSamsungDevice;
//    private final boolean isQcomDevice;
//    private final boolean isLowLatencySupported;
//
//    // FPS tracking
//    private final AtomicLong lastRenderTimeNs = new AtomicLong(0);
//
//    // Target FPS control
//    private static final int ORIGINAL_TARGET_FPS = 30;
//    private volatile int commandedTargetFps = ORIGINAL_TARGET_FPS;
//
//    // Frame queue
//    private final ConcurrentLinkedQueue<FrameData> frameQueue = new ConcurrentLinkedQueue<>();
//    private final ReentrantLock queueLock = new ReentrantLock();
//    private final Condition frameAvailable = queueLock.newCondition();
//
//    // Decoder components
//    private MediaCodec mediaCodec;
//    private final AtomicBoolean isRunning = new AtomicBoolean(false);
//    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
//    private Thread decoderThread;
//    private final Object codecLock = new Object();
//    private Thread renderThread;
//    private Surface outputSurface;
//    private long frameProcessingStartTime;
//    boolean isHighResolution = false;
//
//    private volatile boolean isStreamingReadyToView = false;
//    private final FpsStabilizer stabilizer = new FpsStabilizer(ORIGINAL_TARGET_FPS, 5, true);
//    private final SparseIntArray bufferStates = new SparseIntArray();
//    private final ReentrantLock bufferLock = new ReentrantLock();
//
//    private byte[] spsData;
//    private byte[] ppsData;
//
//    public void setSpsPpsData(byte[] sps, byte[] pps) {
//        this.spsData = sps;
//        this.ppsData = pps;
//    }
//
//    public interface SizeChangedCallback {
//        void onOutputFormatChanged(int width, int height);
//
//        void enableStreamingView(boolean isVisible);
//
//        void setSpsPpsWithStartCode();
//    }
//
//    private SizeChangedCallback callback;
//
//    public H264DecoderDeviceSpecific() {
//        this.isSamsungDevice = Build.MANUFACTURER.equalsIgnoreCase("samsung");
//        this.isQcomDevice = Build.HARDWARE.toLowerCase().contains("qcom");
//        this.isLowLatencySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
//    }
//
//    public void setSizeChangedCallback(SizeChangedCallback callback) {
//        this.callback = callback;
//    }
//
//    private static class FrameData {
//        final ByteBuffer buffer;
//        final int size;
//        long pts;
//        final int flags;
//        final boolean isKeyFrame;
//
//        FrameData(ByteBuffer buffer, int size, long pts, int flags, boolean isKeyFrame) {
//            this.buffer = buffer;
//            this.size = size;
//            this.pts = pts;
//            this.flags = flags;
//            this.isKeyFrame = isKeyFrame;
//        }
//    }
//
//    public boolean isInitialized() {
//        return isInitialized.get() && !isShuttingDown.get();
//    }
//
//    public void onFpsChanged(int newFps) {
//        if (newFps >= 55) {
//            commandedTargetFps = 60;
//        } else if (newFps >= 45) {
//            commandedTargetFps = 50;
//        } else if (newFps >= 35) {
//            commandedTargetFps = 40;
//        } else if (newFps >= 20) {
//            commandedTargetFps = 30;
//        } else if (newFps >= 10) {
//            commandedTargetFps = 20;
//        } else {
//            commandedTargetFps = ORIGINAL_TARGET_FPS;
//        }
//
//        if (commandedTargetFps <= 0) {
//            commandedTargetFps = ORIGINAL_TARGET_FPS;
//            Log.w(TAG, "Commanded target FPS was non-positive, falling back to " + ORIGINAL_TARGET_FPS);
//        }
//        stabilizer.setFps(commandedTargetFps);
//        stabilizer.setJitterTolerance(0.5f);
//    }
//
//    public boolean initialize(Surface surface, int width, int height, boolean isHighRes) {
//        try {
//            Log.e(TAG, "initialize called ");
//
//            // Reset shutdown state
//            isShuttingDown.set(false);
//            isDecoderStopped.set(false);
//
//            // Reset state
//            isRunning.set(false);
//            isInitialized.set(false);
//
//            if (surface == null || !surface.isValid()) {
//                Log.e(TAG, "Invalid surface provided");
//                return false;
//            }
//
//            isHighResolution = isHighRes;
//            outputSurface = surface;
//
//            if (mediaCodec == null) {
//                Log.e(TAG, "initialize mediacodec");
//                mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
//            } else {
//                try {
//                    mediaCodec.stop();
//                    mediaCodec.reset();
//                } catch (Exception e) {
//                    Log.e(TAG, "Error stopping/resetting codec: " + e.getMessage());
//                    try {
//                        if (mediaCodec != null) {
//                            mediaCodec.release();
//                            mediaCodec = null;
//                        }
//                    } catch (Exception ex) {
//                        Log.e(TAG, "Error releasing codec after stop/reset failure: " + ex.getMessage());
//                    }
//                    mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
//                }
//            }
//
//            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
//            format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
//            format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
//            format.setInteger(MediaFormat.KEY_LATENCY, 1);  // Low latency mode
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
//                format.setInteger(MediaFormat.KEY_PRIORITY, 0);  // Real-time priority
//            }
//            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024); // 20MB
//
//            // Device-specific configuration
//            if (isSamsungDevice) {
//                format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
//                format.setInteger("vendor.sec-ext-dec-low-latency-mode.value", 1);
//                format.setInteger("vendor.sec-ext-dec-render-mode", 0);
//                format.setInteger("vendor.sec-ext-dec-frame-skip-mode", 1);
//                format.setInteger("vendor.sec-ext-dec-performance-tuning.enable", 1);
//                format.setInteger("vendor.sec-ext-dec-h264-enable-sps-pps", 1);
//                format.setInteger("vendor.sec-ext-dec-rate-control-enable", 1);
//                format.setInteger("vendor.sec-ext-dec-extra-buffers", 2);
//            } else if (isQcomDevice) {
//                format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
//                format.setInteger("vendor.qti-ext-dec-low-latency-mode.value", 1);
//            }
//
//            if (spsData != null && ppsData != null) {
//                format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
//                format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if (isSamsungDevice) {
//                    outputSurface.setFrameRate(commandedTargetFps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
//                }
//                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//            }
//
//            if (isLowLatencySupported) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
//                }
//                format.setInteger(MediaFormat.KEY_TEMPORAL_LAYERING, 0);
//            }
//
//            if (isHighRes || width * height > 1920 * 1080) {
//                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024);
//                format.setInteger("max-width", width);
//                format.setInteger("max-height", height);
//            }
//
//            mediaCodec.configure(format, outputSurface, null, 0);
//            mediaCodec.start();
//
//            isRunning.set(true);
//            isInitialized.set(true);
//            startDecoderThread();
//
//            stabilizer.setJitterTolerance(0.5f); // More tolerant of timing variations
//            stabilizer.setPtsMode(FpsStabilizer.PtsMode.HYBRID); // Best for variable streams
//
//            Log.d(TAG, "Decoder initialized successfully");
//            return true;
//        } catch (MediaCodec.CodecException e) {
//            Log.e(TAG, "CodecException during init "+e.getLocalizedMessage(), e);
//            throw e;
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "IllegalStateException – codec misuse "+e.getLocalizedMessage(), e);
//            throw e;
//        } catch (Exception e) {
//            Log.e(TAG, "Unexpected init error", e);
//            throw new IllegalStateException("Decoder init failed", e);
//        }
//    }
//
//    private void cleanupBuffers() {
//        try {
//            bufferLock.lock();
//            try {
//                bufferStates.clear();
//                if (mediaCodec != null) {
//                    try {
//                        mediaCodec.flush();
//                    } catch (IllegalStateException e) {
//                        Log.w(TAG, "Flush failed during cleanup");
//                    }
//                }
//            } finally {
//                bufferLock.unlock();
//            }
//        } catch (Exception e) {
//            Log.w(TAG, "Error in cleanupBuffers", e);
//        }
//    }
//
//    // Add this method to gracefully stop the decoder
//    public void stopDecoding() {
//        Log.w(TAG, "stopDecoding called");
//        isShuttingDown.set(true);
//        isRunning.set(false);
//
//        // Signal waiting threads to wake up
//        queueLock.lock();
//        try {
//            frameAvailable.signalAll();
//        } finally {
//            queueLock.unlock();
//        }
//
//        // Clear the queue to prevent further processing
//        frameQueue.clear();
//
//        // Wait a bit for threads to notice the shutdown
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    public void release() {
//        Log.w(TAG, "Decoder release called");
//
//        // 1. Stop decoding first
//        stopDecoding();
//
//        // 2. Wait for decoder thread to stop
//        if (decoderThread != null) {
//            try {
//                decoderThread.join(500); // Shorter timeout
//            } catch (InterruptedException e) {
//                Log.w(TAG, "Decoder thread join interrupted", e);
//                Thread.currentThread().interrupt();
//            } finally {
//                decoderThread = null;
//            }
//        }
//
//        // 3. Now clean up MediaCodec
//        synchronized (codecLock) {
//            if (mediaCodec != null) {
//                try {
//                    // First stop
//                    try {
//                        mediaCodec.stop();
//                    } catch (IllegalStateException e) {
//                        Log.w(TAG, "MediaCodec already stopped: " + e.getMessage());
//                    }
//
//                    // Small delay
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//
//                    // Then release
//                    try {
//                        mediaCodec.release();
//                    } catch (Exception e) {
//                        Log.e(TAG, "MediaCodec release failed: " + e.getLocalizedMessage(), e);
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Unexpected error during codec cleanup", e);
//                } finally {
//                    mediaCodec = null;
//                }
//            }
//        }
//
//        // 4. Clean up other resources
//        cleanupBuffers();
//        stabilizer.reset();
//        isStreamingReadyToView = false;
//
//        // 5. Clear queue again
//        frameQueue.clear();
//
//        // 6. Reset surface reference (don't release it here - let owner do that)
//        outputSurface = null;
//
//        // 7. Reset flags
//        lastRenderTimeNs.set(0);
//        isInitialized.set(false);
//        isDecoderStopped.set(true);
//
//        Log.w(TAG, "Decoder release completed");
//    }
//
//    public void enqueueFrame(ByteBuffer frameBuffer, int size, long pts, int flags) {
//        // Don't accept frames if shutting down or stopped
//        if (isShuttingDown.get() || !isRunning.get() || frameBuffer == null || size <= 0) {
//            return;
//        }
//
//        boolean isKeyFrame = (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
//        queueLock.lock();
//        try {
//            // Prevent queue from growing too large
//            while (frameQueue.size() >= MAX_QUEUE_SIZE && isRunning.get() && !isShuttingDown.get()) {
//                try {
//                    frameAvailable.await(10, TimeUnit.MILLISECONDS);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    return;
//                }
//            }
//
//            if (!isRunning.get() || isShuttingDown.get()) return;
//
//            ByteBuffer copyBuffer = ByteBuffer.allocateDirect(size);
//            frameBuffer.position(0).limit(size);
//            copyBuffer.put(frameBuffer);
//            copyBuffer.flip();
//
//            frameQueue.offer(new FrameData(copyBuffer, size, System.nanoTime() + 1000, flags, isKeyFrame));
//            frameAvailable.signal();
//        } finally {
//            queueLock.unlock();
//        }
//    }
//
//    private void startDecoderThread() {
//        decoderThread = new Thread(() -> {
//            while (isRunning.get() && !isShuttingDown.get()) {
//                FrameData frameData;
//                queueLock.lock();
//                try {
//                    frameData = frameQueue.poll();
//                    if (frameData == null) {
//                        // Check if we should exit
//                        if (!isRunning.get() || isShuttingDown.get()) {
//                            break;
//                        }
//                        frameAvailable.await(10, TimeUnit.MILLISECONDS);
//                        continue;
//                    }
//                    frameAvailable.signal();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                } finally {
//                    queueLock.unlock();
//                }
//
//                frameProcessingStartTime = System.nanoTime();
//                if (frameData.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                    decodeFrame(frameData);
//                }
//
//                // Adaptive sleep to maintain frame rate
//                long processingTime = (System.nanoTime() - frameProcessingStartTime) / 1000;
//                long ff = 1_000_000 / commandedTargetFps;
//                if (processingTime < ff) {
//                    try {
//                        Thread.sleep((ff - processingTime) / 1000);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        break;
//                    }
//                }
//            }
//            Log.d(TAG, "Decoder thread exiting");
//        }, "H264DecoderThread");
//
//        decoderThread.setPriority(Thread.MAX_PRIORITY);
//        decoderThread.start();
//    }
//
//    private void decodeFrame(FrameData frameData) {
//        synchronized (codecLock) {
//            if (!isRunning.get() || isShuttingDown.get()) {
//                return;
//            }
//
//            int inputBufferIndex = -1;
//            try {
//                // 1. Get input buffer with state tracking
//                bufferLock.lock();
//                try {
//                    inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
//                    if (inputBufferIndex >= 0) {
//                        bufferStates.put(inputBufferIndex, 1); // Mark as in-use
//                    }
//                } finally {
//                    bufferLock.unlock();
//                }
//
//                if (inputBufferIndex < 0) {
//                    return;
//                }
//
//                // 2. Handle keyframes
//                if (frameData.isKeyFrame) {
//                    stabilizer.reset();
//                    bufferLock.lock();
//                    try {
//                        bufferStates.clear(); // Reset buffer states on keyframe
//                    } finally {
//                        bufferLock.unlock();
//                    }
//                }
//
//                // 3. Prepare input
//                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
//                if (inputBuffer == null) {
//                    returnBuffer(inputBufferIndex);
//                    return;
//                }
//
//                inputBuffer.clear();
//                frameData.buffer.position(0).limit(frameData.size);
//                inputBuffer.put(frameData.buffer);
//
//                // Ensure flags are valid
//                int validFlags = 0;
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
//                }
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
//                    Log.d(TAG, "FLAG: BUFFER_FLAG_CODEC_CONFIG");
//                }
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
//                    Log.d(TAG, "FLAG: BUFFER_FLAG_PARTIAL_FRAME");
//                }
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
//                    Log.d(TAG, "FLAG: BUFFER_FLAG_END_OF_STREAM");
//                }
//
//                // 4. Calculate and validate PTS
//                long pts = stabilizer.calculatePtsUs(frameData.pts);
//                mediaCodec.queueInputBuffer(inputBufferIndex, 0, frameData.size, pts, validFlags);
//
//                // 5. Process output
//                processOutputBuffers();
//
//            } catch (Exception e) {
//                Log.e(TAG, "Decode error", e);
//                if (inputBufferIndex >= 0) {
//                    returnBuffer(inputBufferIndex);
//                }
//                // Don't handle decoder failure during shutdown
//                if (!isShuttingDown.get()) {
//                    handleDecoderFailure(e);
//                }
//            }
//        }
//    }
//
//    private void processOutputBuffers() {
//        if (isShuttingDown.get()) {
//            return;
//        }
//
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex;
//
//        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) >= 0) {
//            try {
//                // Calculate render time with stabilization
//                long renderTimeNs;
//                if (bufferInfo.presentationTimeUs > 0) {
//                    renderTimeNs = stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs);
//                } else {
//                    renderTimeNs = System.nanoTime();
//                }
//
//                mediaCodec.releaseOutputBuffer(outputBufferIndex, renderTimeNs);
//                stabilizer.onFrameProcessed();
//
//                // Update render timing
//                lastRenderTimeNs.set(renderTimeNs);
//                if (callback != null && !isStreamingReadyToView) {
//                    callback.enableStreamingView(true);
//                    isStreamingReadyToView = true;
//                }
//
//            } finally {
//                returnBuffer(outputBufferIndex);
//            }
//        }
//
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            handleFormatChange();
//        }
//    }
//
//    private void returnBuffer(int bufferIndex) {
//        bufferLock.lock();
//        try {
//            bufferStates.delete(bufferIndex);
//        } finally {
//            bufferLock.unlock();
//        }
//    }
//
//    private void handleFormatChange() {
//        try {
//            MediaFormat newFormat = mediaCodec.getOutputFormat();
//            if (callback != null) {
//                int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
//                int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
//                callback.onOutputFormatChanged(width, height);
//            }
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "Error handling format change: " + e.getMessage());
//        }
//    }
//
//    private void handleDecoderFailure(Exception ex) {
//        Log.e(TAG, "Decoder failure, releasing", ex);
//        // Don't call callback during shutdown
//        if (!isShuttingDown.get() && callback != null) {
//            callback.setSpsPpsWithStartCode();
//        }
//    }
//
//    // Add this method to check if decoder is stopped
//    public boolean isStopped() {
//        return isDecoderStopped.get();
//    }
//}

/*Frame Skipping*/
//package com.dome.librarynightwave.utils;
//
//import android.media.MediaCodec;
//import android.media.MediaFormat;
//import android.os.Build;
//import android.util.Log;
//import android.util.SparseIntArray;
//import android.view.Surface;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;
//
//public class H264DecoderDeviceSpecific {
//    private static final String TAG = "H264Decoder";
//    private static final String MIME_TYPE = "video/avc";
//    private static final int TIMEOUT_US = 10000;
//
//    // Intelligent queue management
//    private volatile int maxQueueSize = 30;
//
//    // Shutdown control
//    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
//    private final AtomicBoolean isDecoderStopped = new AtomicBoolean(true);
//
//    // Device detection
//    private final boolean isSamsungDevice;
//    private final boolean isQcomDevice;
//    private final boolean isLowLatencySupported;
//
//    // Performance tracking
//    private final AtomicLong lastRenderTimeNs = new AtomicLong(0);
//    private volatile long lastFpsUpdateTime = System.currentTimeMillis();
//    private volatile int framesRendered = 0;
//    private volatile int framesReceived = 0;
//    private volatile int measuredInputFps = 30;
//    private volatile int measuredOutputFps = 30;
//    private volatile long lastFrameTimestamp = 0;
//
//    // Target FPS control
//    private static final int ORIGINAL_TARGET_FPS = 30;
//    private volatile int commandedTargetFps = ORIGINAL_TARGET_FPS;
//    private volatile int actualTargetFps = ORIGINAL_TARGET_FPS;
//
//    // Frame queue
//    private final ConcurrentLinkedQueue<FrameData> frameQueue = new ConcurrentLinkedQueue<>();
//    private final ReentrantLock queueLock = new ReentrantLock();
//    private final Condition frameAvailable = queueLock.newCondition();
//
//    // Decoder components
//    private MediaCodec mediaCodec;
//    private final AtomicBoolean isRunning = new AtomicBoolean(false);
//    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
//    private Thread decoderThread;
//    private final Object codecLock = new Object();
//    private Surface outputSurface;
//    private long frameProcessingStartTime;
//    boolean isHighResolution = false;
//
//    private volatile boolean isStreamingReadyToView = false;
//    private final FpsStabilizer stabilizer = new FpsStabilizer(ORIGINAL_TARGET_FPS, 3, true);
//    private final SparseIntArray bufferStates = new SparseIntArray();
//    private final ReentrantLock bufferLock = new ReentrantLock();
//
//    // Intelligent Frame Skipping System
//    private static class FrameSkipLogic {
//        // Configuration
//        private static final int MIN_FPS_FOR_SKIP = 40;
//        private static final int MAX_SKIP_RATIO = 3; // Max skip every 3rd frame
//        private static final int MIN_KEYFRAME_INTERVAL_MS = 1000; // I-frames every 1 second
//        private static final int QUALITY_PRESERVATION_WINDOW = 10; // Frames to analyze
//
//        // State
//        private int consecutiveNonKeyFrames = 0;
//        private int framesSinceLastKeyFrame = 0;
//        private long lastKeyFrameTime = 0;
//        private final Deque<Boolean> recentFrameQuality = new ArrayDeque<>();
//        private int skipCounter = 0;
//        private int skipInterval = 1; // 1 = no skipping
//
//        /**
//         * Decide whether to skip a frame
//         * @param isKeyFrame Whether this is a key frame
//         * @param currentFps Current input FPS
//         * @param targetFps Target output FPS
//         * @return true to skip, false to render
//         */
//        boolean shouldSkipFrame(boolean isKeyFrame, int currentFps, int targetFps) {
//            long currentTime = System.currentTimeMillis();
//
//            // Reset on key frame
//            if (isKeyFrame) {
//                lastKeyFrameTime = currentTime;
//                framesSinceLastKeyFrame = 0;
//                consecutiveNonKeyFrames = 0;
//                recentFrameQuality.clear();
//                skipCounter = 0;
//                skipInterval = 1;
//                Log.d(TAG, "Key frame detected - resetting skip logic");
//                return false; // NEVER skip key frames
//            }
//
//            framesSinceLastKeyFrame++;
//
//            // Don't skip if FPS is manageable
//            if (currentFps < MIN_FPS_FOR_SKIP || currentFps <= targetFps + 5) {
//                skipCounter = 0;
//                return false;
//            }
//
//            // Calculate how close we are to next key frame
//            long timeSinceLastKeyFrame = currentTime - lastKeyFrameTime;
//            boolean closeToNextKeyFrame = timeSinceLastKeyFrame > (MIN_KEYFRAME_INTERVAL_MS - 200);
//
//            // Adaptive skip interval based on FPS difference
//            int fpsDifference = currentFps - targetFps;
//            int calculatedSkipInterval = Math.min(MAX_SKIP_RATIO,
//                    Math.max(2, (fpsDifference / 15) + 1)); // More conservative skipping
//
//            // Be more conservative as we approach next key frame
//            if (closeToNextKeyFrame && calculatedSkipInterval < 3) {
//                calculatedSkipInterval = Math.max(2, calculatedSkipInterval - 1);
//            }
//
//            // Update skip interval gradually
//            if (Math.abs(calculatedSkipInterval - skipInterval) > 1) {
//                skipInterval = (skipInterval + calculatedSkipInterval) / 2;
//            } else {
//                skipInterval = calculatedSkipInterval;
//            }
//
//            // Apply skip logic
//            skipCounter++;
//            boolean shouldSkip = (skipCounter % skipInterval) == 0;
//
//            if (shouldSkip) {
//                consecutiveNonKeyFrames++;
//
//                // Don't skip too many consecutive non-key frames
//                if (consecutiveNonKeyFrames > 5) {
//                    shouldSkip = false;
//                    consecutiveNonKeyFrames = 0;
//                    skipCounter = 0;
//                    Log.v(TAG, "Preserving frame after too many consecutive skips");
//                }
//
//                // Track quality impact
//                recentFrameQuality.addLast(shouldSkip);
//                if (recentFrameQuality.size() > QUALITY_PRESERVATION_WINDOW) {
//                    recentFrameQuality.removeFirst();
//                }
//
//                // Calculate skip rate in recent window
//                long skipCount = recentFrameQuality.stream().filter(skip -> skip).count();
//                double skipRate = (double) skipCount / recentFrameQuality.size();
//
//                // Reduce skipping if quality is degrading
//                if (skipRate > 0.5) { // More than 50% skipping
//                    skipInterval = Math.max(2, skipInterval - 1);
//                    Log.v(TAG, "Reducing skip rate to preserve quality");
//                }
//            } else {
//                consecutiveNonKeyFrames = 0;
//            }
//
//            if (shouldSkip) {
//                Log.v(TAG, String.format("Skipping frame: FPS in/out=%d/%d, interval=%d, sinceKeyFrame=%dms",
//                        currentFps, targetFps, skipInterval, timeSinceLastKeyFrame));
//            }
//
//            return shouldSkip;
//        }
//
//        void reset() {
//            consecutiveNonKeyFrames = 0;
//            framesSinceLastKeyFrame = 0;
//            lastKeyFrameTime = 0;
//            recentFrameQuality.clear();
//            skipCounter = 0;
//            skipInterval = 1;
//        }
//    }
//
//    private final FrameSkipLogic frameSkipLogic = new FrameSkipLogic();
//
//    private byte[] spsData;
//    private byte[] ppsData;
//
//    public void setSpsPpsData(byte[] sps, byte[] pps) {
//        this.spsData = sps;
//        this.ppsData = pps;
//    }
//
//    public interface SizeChangedCallback {
//        void onOutputFormatChanged(int width, int height);
//        void enableStreamingView(boolean isVisible);
//        void setSpsPpsWithStartCode();
//    }
//
//    private SizeChangedCallback callback;
//
//    public H264DecoderDeviceSpecific() {
//        this.isSamsungDevice = Build.MANUFACTURER.equalsIgnoreCase("samsung");
//        this.isQcomDevice = Build.HARDWARE.toLowerCase().contains("qcom");
//        this.isLowLatencySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
//    }
//
//    public void setSizeChangedCallback(SizeChangedCallback callback) {
//        this.callback = callback;
//    }
//
//    private static class FrameData {
//        final ByteBuffer buffer;
//        final int size;
//        final long pts;
//        final int flags;
//        final boolean isKeyFrame;
//
//        FrameData(ByteBuffer buffer, int size, long pts, int flags, boolean isKeyFrame) {
//            this.buffer = buffer;
//            this.size = size;
//            this.pts = pts;
//            this.flags = flags;
//            this.isKeyFrame = isKeyFrame;
//        }
//    }
//
//    public boolean isInitialized() {
//        return isInitialized.get() && !isShuttingDown.get();
//    }
//
//    public void onFpsChanged(int newFps) {
//        Log.d(TAG, "Input FPS changed: " + newFps);
//
//        // Update measured input FPS
//        this.measuredInputFps = newFps;
//
//        // Adaptive target FPS adjustment
//        int newTargetFps;
//        if (newFps >= 70) {
//            newTargetFps = 50; // Cap at 50 for very high FPS
//            maxQueueSize = 20;
//        } else if (newFps >= 50) {
//            newTargetFps = 40;
//            maxQueueSize = 25;
//        } else if (newFps >= 40) {
//            newTargetFps = 35;
//            maxQueueSize = 30;
//        } else if (newFps >= 30) {
//            newTargetFps = 30;
//            maxQueueSize = 35;
//        } else {
//            newTargetFps = Math.max(25, newFps - 5);
//            maxQueueSize = 40; // Larger buffer for low FPS
//        }
//
//        // Smooth transition
//        if (Math.abs(newTargetFps - actualTargetFps) >= 5) {
//            actualTargetFps = newTargetFps;
//            commandedTargetFps = actualTargetFps;
//            stabilizer.setFps(actualTargetFps);
//
//            // Adjust stabilizer tolerance based on FPS
//            if (actualTargetFps > 40) {
//                stabilizer.setJitterTolerance(0.4f);
//            } else {
//                stabilizer.setJitterTolerance(0.6f);
//            }
//
//            Log.d(TAG, "Target FPS adjusted: " + actualTargetFps +
//                    " (input: " + newFps + ")");
//        }
//    }
//
//    private void updateFpsMetrics() {
//        long currentTime = System.currentTimeMillis();
//        if (currentTime - lastFpsUpdateTime >= 1000) {
//            measuredOutputFps = framesRendered;
//
//            Log.d(TAG, String.format("FPS Stats - Input: %d, Output: %d, Target: %d, Queue: %d",
//                    measuredInputFps, measuredOutputFps, actualTargetFps, frameQueue.size()));
//
//            // Reset counters
//            framesRendered = 0;
//            framesReceived = 0;
//            lastFpsUpdateTime = currentTime;
//        }
//    }
//
//    public boolean initialize(Surface surface, int width, int height, boolean isHighRes) {
//        try {
//            Log.d(TAG, "Initializing decoder");
//
//            // Reset states
//            isShuttingDown.set(false);
//            isDecoderStopped.set(false);
//            isRunning.set(false);
//            isInitialized.set(false);
//
//            // Reset metrics
//            measuredInputFps = 30;
//            measuredOutputFps = 30;
//            framesRendered = 0;
//            framesReceived = 0;
//            lastFpsUpdateTime = System.currentTimeMillis();
//            frameSkipLogic.reset();
//
//            if (surface == null || !surface.isValid()) {
//                Log.e(TAG, "Invalid surface provided");
//                return false;
//            }
//
//            isHighResolution = isHighRes;
//            outputSurface = surface;
//
//            // Create or reset codec
//            if (mediaCodec == null) {
//                mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
//            } else {
//                try {
//                    mediaCodec.stop();
//                    mediaCodec.reset();
//                } catch (Exception e) {
//                    Log.e(TAG, "Error resetting codec: " + e.getMessage());
//                    try {
//                        if (mediaCodec != null) {
//                            mediaCodec.release();
//                        }
//                    } catch (Exception ex) {
//                        Log.e(TAG, "Error releasing codec: " + ex.getMessage());
//                    }
//                    mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
//                }
//            }
//
//            // Configure format with low-latency settings
//            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
//            format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
//            format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
//            format.setInteger(MediaFormat.KEY_LATENCY, 1);
//
//            // Adaptive buffer sizing
//            if (isHighRes || width * height > 1920 * 1080) {
//                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8 * 1024 * 1024);
//            } else {
//                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4 * 1024 * 1024);
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
//                format.setInteger(MediaFormat.KEY_PRIORITY, 0);
//            }
//
//            // Device-specific optimizations
//            if (isSamsungDevice) {
//                format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
//                format.setInteger("vendor.sec-ext-dec-low-latency-mode.value", 1);
//                format.setInteger("vendor.sec-ext-dec-frame-skip-mode", 1);
//                format.setInteger("vendor.sec-ext-dec-performance-tuning.enable", 1);
//                format.setInteger("vendor.sec-ext-dec-extra-buffers", 1);
//            } else if (isQcomDevice) {
//                format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
//                format.setInteger("vendor.qti-ext-dec-low-latency-mode.value", 1);
//            }
//
//            if (spsData != null && ppsData != null) {
//                format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
//                format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
//            }
//
//            if (isLowLatencySupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
//            }
//
//            mediaCodec.configure(format, outputSurface, null, 0);
//            mediaCodec.start();
//
//            isRunning.set(true);
//            isInitialized.set(true);
//            startDecoderThread();
//
//            Log.d(TAG, "Decoder initialized successfully");
//            return true;
//
//        } catch (Exception e) {
//            Log.e(TAG, "Decoder initialization failed", e);
//            return false;
//        }
//    }
//
//    public void enqueueFrame(ByteBuffer frameBuffer, int size, long pts, int flags) {
//        if (isShuttingDown.get() || !isRunning.get() || frameBuffer == null || size <= 0) {
//            return;
//        }
//
//        framesReceived++;
//        updateFpsMetrics();
//
//        boolean isKeyFrame = (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
//
//        queueLock.lock();
//        try {
//            // Intelligent queue management
//            if (frameQueue.size() >= maxQueueSize) {
//                if (measuredInputFps > 50) {
//                    // For high FPS, drop oldest non-key frame
//                    FrameData oldest = frameQueue.peek();
//                    if (oldest != null && !oldest.isKeyFrame) {
//                        frameQueue.poll();
//                        Log.v(TAG, "Dropped old frame for low latency");
//                    }
//                } else {
//                    // For moderate FPS, wait briefly
//                    try {
//                        frameAvailable.await(5, TimeUnit.MILLISECONDS);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        return;
//                    }
//                }
//            }
//
//            if (!isRunning.get() || isShuttingDown.get()) return;
//
//            // Copy frame data
//            ByteBuffer copyBuffer = ByteBuffer.allocateDirect(size);
//            frameBuffer.position(0).limit(size);
//            copyBuffer.put(frameBuffer);
//            copyBuffer.flip();
//
//            frameQueue.offer(new FrameData(copyBuffer, size, pts > 0 ? pts : System.nanoTime() / 1000, flags, isKeyFrame));
//            frameAvailable.signal();
//
//        } finally {
//            queueLock.unlock();
//        }
//    }
//
//    private void startDecoderThread() {
//        decoderThread = new Thread(() -> {
//            while (isRunning.get() && !isShuttingDown.get()) {
//                FrameData frameData;
//                queueLock.lock();
//                try {
//                    frameData = frameQueue.poll();
//                    if (frameData == null) {
//                        if (!isRunning.get() || isShuttingDown.get()) {
//                            break;
//                        }
//                        // Adaptive wait based on FPS
//                        long waitTime = measuredInputFps > 50 ? 2 : 10;
//                        frameAvailable.await(waitTime, TimeUnit.MILLISECONDS);
//                        continue;
//                    }
//                    frameAvailable.signal();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                } finally {
//                    queueLock.unlock();
//                }
//
//                if (frameData.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                    decodeFrame(frameData);
//                }
//
//            }
//            Log.d(TAG, "Decoder thread exiting");
//        }, "H264DecoderThread");
//
//        decoderThread.setPriority(Thread.MAX_PRIORITY);
//        decoderThread.start();
//    }
//
//    private void decodeFrame(FrameData frameData) {
//        synchronized (codecLock) {
//            if (!isRunning.get() || isShuttingDown.get()) {
//                return;
//            }
//
//            int inputBufferIndex = -1;
//            try {
//                // Get input buffer
//                bufferLock.lock();
//                try {
//                    inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
//                    if (inputBufferIndex >= 0) {
//                        bufferStates.put(inputBufferIndex, 1);
//                    }
//                } finally {
//                    bufferLock.unlock();
//                }
//
//                if (inputBufferIndex < 0) {
//                    Log.v(TAG, "No input buffer available");
//                    return;
//                }
//
//                // Reset on key frame
//                if (frameData.isKeyFrame) {
//                    stabilizer.reset();
//                    bufferLock.lock();
//                    try {
//                        bufferStates.clear();
//                    } finally {
//                        bufferLock.unlock();
//                    }
//                    Log.v(TAG, "Processing key frame");
//                }
//
//                // Prepare input buffer
//                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
//                if (inputBuffer == null) {
//                    returnBuffer(inputBufferIndex);
//                    return;
//                }
//
//                inputBuffer.clear();
//                frameData.buffer.position(0).limit(frameData.size);
//                inputBuffer.put(frameData.buffer);
//
//                // Calculate PTS
//                long pts = frameData.pts > 0 ? frameData.pts : stabilizer.calculatePtsUs(System.nanoTime() / 1000);
//                mediaCodec.queueInputBuffer(inputBufferIndex, 0, frameData.size, pts, frameData.flags);
//
//                // Process output with intelligent skipping
//                processOutputBuffers();
//
//            } catch (Exception e) {
//                Log.e(TAG, "Decode error", e);
//                if (inputBufferIndex >= 0) {
//                    returnBuffer(inputBufferIndex);
//                }
//                if (!isShuttingDown.get()) {
//                    handleDecoderFailure(e);
//                }
//            }
//        }
//    }
//
//    private void processOutputBuffers() {
//        if (isShuttingDown.get()) {
//            return;
//        }
//
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex;
//
//        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)) >= 0) {
//            try {
//                // Intelligent frame skipping decision
//                boolean shouldSkip = frameSkipLogic.shouldSkipFrame(
//                        (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
//                        measuredInputFps,
//                        actualTargetFps
//                );
//
//                if (shouldSkip) {
//                    // Skip rendering but mark as processed
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//                    stabilizer.onFrameSkipped();
//                } else {
//                    // Calculate render time
//                    long renderTimeNs;
//                    if (bufferInfo.presentationTimeUs > 0) {
//                        renderTimeNs = stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs);
//                    } else {
//                        renderTimeNs = System.nanoTime();
//                    }
//
//                    // Render frame
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, renderTimeNs);
//
//                    framesRendered++;
//                    stabilizer.onFrameProcessed();
//                    lastRenderTimeNs.set(renderTimeNs);
//
//                    if (callback != null && !isStreamingReadyToView) {
//                        callback.enableStreamingView(true);
//                        isStreamingReadyToView = true;
//                    }
//                }
//
//            } finally {
//                returnBuffer(outputBufferIndex);
//            }
//        }
//
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            handleFormatChange();
//        }
//    }
//
//    private void returnBuffer(int bufferIndex) {
//        bufferLock.lock();
//        try {
//            bufferStates.delete(bufferIndex);
//        } finally {
//            bufferLock.unlock();
//        }
//    }
//
//    private void handleFormatChange() {
//        try {
//            MediaFormat newFormat = mediaCodec.getOutputFormat();
//            if (callback != null) {
//                int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
//                int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
//                callback.onOutputFormatChanged(width, height);
//            }
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "Error handling format change: " + e.getMessage());
//        }
//    }
//
//    private void handleDecoderFailure(Exception ex) {
//        Log.e(TAG, "Decoder failure", ex);
//        if (!isShuttingDown.get() && callback != null) {
//            callback.setSpsPpsWithStartCode();
//        }
//    }
//
//    public void stopDecoding() {
//        Log.w(TAG, "stopDecoding called");
//        isShuttingDown.set(true);
//        isRunning.set(false);
//
//        queueLock.lock();
//        try {
//            frameAvailable.signalAll();
//        } finally {
//            queueLock.unlock();
//        }
//
//        frameQueue.clear();
//    }
//
//    public void release() {
//        Log.w(TAG, "Decoder release called");
//
//        stopDecoding();
//
//        if (decoderThread != null) {
//            try {
//                decoderThread.join(300);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        synchronized (codecLock) {
//            if (mediaCodec != null) {
//                try {
//                    mediaCodec.stop();
//                } catch (IllegalStateException e) {
//                    Log.w(TAG, "Codec already stopped");
//                }
//
//                try {
//                    mediaCodec.release();
//                } catch (Exception e) {
//                    Log.e(TAG, "Error releasing codec", e);
//                } finally {
//                    mediaCodec = null;
//                }
//            }
//        }
//
//        outputSurface = null;
//        isInitialized.set(false);
//        isDecoderStopped.set(true);
//
//        Log.w(TAG, "Decoder released");
//    }
//
//    public boolean isStopped() {
//        return isDecoderStopped.get();
//    }
//
//    public String getDecoderStats() {
//        return String.format("In: %d fps, Out: %d fps, Target: %d fps, Queue: %d/%d",
//                measuredInputFps, measuredOutputFps, actualTargetFps,
//                frameQueue.size(), maxQueueSize);
//    }
//}

/*Combined*/
//package com.dome.librarynightwave.utils;
//
//import android.media.MediaCodec;
//import android.media.MediaCodecInfo;
//import android.media.MediaFormat;
//import android.os.Build;
//import android.util.Log;
//import android.util.SparseIntArray;
//import android.view.Surface;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.ReentrantLock;
//
//public class H264DecoderDeviceSpecific {
//    private static final String TAG = "H264Decoder";
//    private static final String MIME_TYPE = "video/avc";
//    private static final int TIMEOUT_US = 10000;
//    private static final int MAX_QUEUE_SIZE = 60;
//
//    // Add shutdown control
//    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
//    private final AtomicBoolean isDecoderStopped = new AtomicBoolean(true);
//
//    // Device detection
//    private final boolean isSamsungDevice;
//    private final boolean isQcomDevice;
//    private final boolean isLowLatencySupported;
//
//    // FPS tracking
//    private final AtomicLong lastRenderTimeNs = new AtomicLong(0);
//
//    // FPS metrics from new code
//    private volatile long lastFpsUpdateTime = System.currentTimeMillis();
//    private volatile int framesRendered = 0;
//    private volatile int framesReceived = 0;
//    private volatile int measuredInputFps = 30;
//    private volatile int measuredOutputFps = 30;
//
//    // Target FPS control
//    private static final int ORIGINAL_TARGET_FPS = 30;
//    private volatile int commandedTargetFps = ORIGINAL_TARGET_FPS;
//    private volatile int actualTargetFps = ORIGINAL_TARGET_FPS;
//
//    // Frame queue
//    private final ConcurrentLinkedQueue<FrameData> frameQueue = new ConcurrentLinkedQueue<>();
//    private final ReentrantLock queueLock = new ReentrantLock();
//    private final Condition frameAvailable = queueLock.newCondition();
//
//    // Decoder components
//    private MediaCodec mediaCodec;
//    private final AtomicBoolean isRunning = new AtomicBoolean(false);
//    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
//    private Thread decoderThread;
//    private final Object codecLock = new Object();
//    private Thread renderThread;
//    private Surface outputSurface;
//    private long frameProcessingStartTime;
//    boolean isHighResolution = false;
//
//    private volatile boolean isStreamingReadyToView = false;
//    private final FpsStabilizer stabilizer = new FpsStabilizer(ORIGINAL_TARGET_FPS, 5, true);
//    private final SparseIntArray bufferStates = new SparseIntArray();
//    private final ReentrantLock bufferLock = new ReentrantLock();
//
//    // Frame skipping logic from new code (only used when FPS > 35)
//    private final FrameSkipLogic frameSkipLogic = new FrameSkipLogic();
//
//    private byte[] spsData;
//    private byte[] ppsData;
//
//    public void setSpsPpsData(byte[] sps, byte[] pps) {
//        this.spsData = sps;
//        this.ppsData = pps;
//    }
//
//    public interface SizeChangedCallback {
//        void onOutputFormatChanged(int width, int height);
//        void enableStreamingView(boolean isVisible);
//        void setSpsPpsWithStartCode();
//    }
//
//    private SizeChangedCallback callback;
//
//    public H264DecoderDeviceSpecific() {
//        this.isSamsungDevice = Build.MANUFACTURER.equalsIgnoreCase("samsung");
//        this.isQcomDevice = Build.HARDWARE.toLowerCase().contains("qcom");
//        this.isLowLatencySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
//    }
//
//    public void setSizeChangedCallback(SizeChangedCallback callback) {
//        this.callback = callback;
//    }
//
//    private static class FrameData {
//        final ByteBuffer buffer;
//        final int size;
//        long pts;
//        final int flags;
//        final boolean isKeyFrame;
//
//        FrameData(ByteBuffer buffer, int size, long pts, int flags, boolean isKeyFrame) {
//            this.buffer = buffer;
//            this.size = size;
//            this.pts = pts;
//            this.flags = flags;
//            this.isKeyFrame = isKeyFrame;
//        }
//    }
//
//    // Frame skipping logic from new code
//    private static class FrameSkipLogic {
//        private static final int MIN_FPS_FOR_SKIP = 40;
//        private static final int MAX_SKIP_RATIO = 3;
//        private static final int MIN_KEYFRAME_INTERVAL_MS = 1000;
//        private static final int QUALITY_PRESERVATION_WINDOW = 10;
//
//        private int consecutiveNonKeyFrames = 0;
//        private int framesSinceLastKeyFrame = 0;
//        private long lastKeyFrameTime = 0;
//        private final Deque<Boolean> recentFrameQuality = new ArrayDeque<>();
//        private int skipCounter = 0;
//        private int skipInterval = 1;
//
//        boolean shouldSkipFrame(boolean isKeyFrame, int currentFps, int targetFps) {
//            long currentTime = System.currentTimeMillis();
//
//            if (isKeyFrame) {
//                lastKeyFrameTime = currentTime;
//                framesSinceLastKeyFrame = 0;
//                consecutiveNonKeyFrames = 0;
//                recentFrameQuality.clear();
//                skipCounter = 0;
//                skipInterval = 1;
//                Log.d(TAG, "Key frame detected - resetting skip logic");
//                return false;
//            }
//
//            framesSinceLastKeyFrame++;
//
//            if (currentFps < MIN_FPS_FOR_SKIP || currentFps <= targetFps + 5) {
//                skipCounter = 0;
//                return false;
//            }
//
//            long timeSinceLastKeyFrame = currentTime - lastKeyFrameTime;
//            boolean closeToNextKeyFrame = timeSinceLastKeyFrame > (MIN_KEYFRAME_INTERVAL_MS - 200);
//
//            int fpsDifference = currentFps - targetFps;
//            int calculatedSkipInterval = Math.min(MAX_SKIP_RATIO, Math.max(2, (fpsDifference / 15) + 1));
//
//            if (closeToNextKeyFrame && calculatedSkipInterval < 3) {
//                calculatedSkipInterval = Math.max(2, calculatedSkipInterval - 1);
//            }
//
//            if (Math.abs(calculatedSkipInterval - skipInterval) > 1) {
//                skipInterval = (skipInterval + calculatedSkipInterval) / 2;
//            } else {
//                skipInterval = calculatedSkipInterval;
//            }
//
//            skipCounter++;
//            boolean shouldSkip = (skipCounter % skipInterval) == 0;
//
//            if (shouldSkip) {
//                consecutiveNonKeyFrames++;
//
//                if (consecutiveNonKeyFrames > 5) {
//                    shouldSkip = false;
//                    consecutiveNonKeyFrames = 0;
//                    skipCounter = 0;
//                    Log.v(TAG, "Preserving frame after too many consecutive skips");
//                }
//
//                recentFrameQuality.addLast(shouldSkip);
//                if (recentFrameQuality.size() > QUALITY_PRESERVATION_WINDOW) {
//                    recentFrameQuality.removeFirst();
//                }
//
//                long skipCount = 0;
//                for (Boolean skip : recentFrameQuality) {
//                    if (skip) skipCount++;
//                }
//                double skipRate = (double) skipCount / recentFrameQuality.size();
//
//                if (skipRate > 0.5) {
//                    skipInterval = Math.max(2, skipInterval - 1);
//                    Log.v(TAG, "Reducing skip rate to preserve quality");
//                }
//            } else {
//                consecutiveNonKeyFrames = 0;
//            }
//
//            if (shouldSkip) {
//                Log.v(TAG, String.format("Skipping frame: FPS in/out=%d/%d, interval=%d, sinceKeyFrame=%dms",
//                        currentFps, targetFps, skipInterval, timeSinceLastKeyFrame));
//            }
//
//            return shouldSkip;
//        }
//
//        void reset() {
//            consecutiveNonKeyFrames = 0;
//            framesSinceLastKeyFrame = 0;
//            lastKeyFrameTime = 0;
//            recentFrameQuality.clear();
//            skipCounter = 0;
//            skipInterval = 1;
//        }
//    }
//
//    public boolean isInitialized() {
//        return isInitialized.get() && !isShuttingDown.get();
//    }
//
//    public void onFpsChanged(int newFps) {
//        // Update measured FPS
//        this.measuredInputFps = newFps;
//
//        if (newFps <= 35) {
//            // Use original logic for ≤35 FPS
//            if (newFps >= 55) {
//                commandedTargetFps = 60;
//            } else if (newFps >= 45) {
//                commandedTargetFps = 50;
//            } else if (newFps >= 35) {
//                commandedTargetFps = 40;
//            } else if (newFps >= 20) {
//                commandedTargetFps = 30;
//            } else if (newFps >= 10) {
//                commandedTargetFps = 20;
//            } else {
//                commandedTargetFps = ORIGINAL_TARGET_FPS;
//            }
//
//            if (commandedTargetFps <= 0) {
//                commandedTargetFps = ORIGINAL_TARGET_FPS;
//                Log.w(TAG, "Commanded target FPS was non-positive, falling back to " + ORIGINAL_TARGET_FPS);
//            }
//            stabilizer.setFps(commandedTargetFps);
//            stabilizer.setJitterTolerance(0.5f);
//        } else {
//            // Use new logic for >35 FPS
//            int newTargetFps;
//            if (newFps >= 70) {
//                newTargetFps = 50;
//            } else if (newFps >= 50) {
//                newTargetFps = 40;
//            } else if (newFps >= 40) {
//                newTargetFps = 35;
//            } else if (newFps >= 30) {
//                newTargetFps = 30;
//            } else {
//                newTargetFps = Math.max(25, newFps - 5);
//            }
//
//            if (Math.abs(newTargetFps - actualTargetFps) >= 5) {
//                actualTargetFps = newTargetFps;
//                commandedTargetFps = actualTargetFps;
//                stabilizer.setFps(actualTargetFps);
//
//                if (actualTargetFps > 40) {
//                    stabilizer.setJitterTolerance(0.4f);
//                } else {
//                    stabilizer.setJitterTolerance(0.6f);
//                }
//
//                Log.d(TAG, "Target FPS adjusted: " + actualTargetFps +
//                        " (input: " + newFps + ")");
//            }
//        }
//    }
//
//    private void updateFpsMetrics() {
//        long currentTime = System.currentTimeMillis();
//        if (currentTime - lastFpsUpdateTime >= 1000) {
//            measuredOutputFps = framesRendered;
//
//            Log.d(TAG, String.format("FPS Stats - Input: %d, Output: %d, Target: %d, Queue: %d",
//                    measuredInputFps, measuredOutputFps, actualTargetFps, frameQueue.size()));
//
//            framesRendered = 0;
//            framesReceived = 0;
//            lastFpsUpdateTime = currentTime;
//        }
//    }
//
//    public boolean initialize(Surface surface, int width, int height, boolean isHighRes) {
//        try {
//            Log.e(TAG, "initialize called ");
//
//            // Reset shutdown state
//            isShuttingDown.set(false);
//            isDecoderStopped.set(false);
//
//            // Reset state
//            isRunning.set(false);
//            isInitialized.set(false);
//
//            // Reset metrics
//            measuredInputFps = 30;
//            measuredOutputFps = 30;
//            framesRendered = 0;
//            framesReceived = 0;
//            lastFpsUpdateTime = System.currentTimeMillis();
//            frameSkipLogic.reset();
//
//            if (surface == null || !surface.isValid()) {
//                Log.e(TAG, "Invalid surface provided");
//                return false;
//            }
//
//            isHighResolution = isHighRes;
//            outputSurface = surface;
//
//            if (mediaCodec == null) {
//                Log.e(TAG, "initialize mediacodec");
//                mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
//            } else {
//                try {
//                    mediaCodec.stop();
//                    mediaCodec.reset();
//                } catch (Exception e) {
//                    Log.e(TAG, "Error stopping/resetting codec: " + e.getMessage());
//                    try {
//                        if (mediaCodec != null) {
//                            mediaCodec.release();
//                            mediaCodec = null;
//                        }
//                    } catch (Exception ex) {
//                        Log.e(TAG, "Error releasing codec after stop/reset failure: " + ex.getMessage());
//                    }
//                    mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
//                }
//            }
//
//            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
//            format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
//            format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
//            format.setInteger(MediaFormat.KEY_LATENCY, 1);  // Low latency mode
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
//                format.setInteger(MediaFormat.KEY_PRIORITY, 0);  // Real-time priority
//            }
//            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024); // 20MB
//
//            // Device-specific configuration
//            if (isSamsungDevice) {
//                format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
//                format.setInteger("vendor.sec-ext-dec-low-latency-mode.value", 1);
//                format.setInteger("vendor.sec-ext-dec-render-mode", 0);
//                format.setInteger("vendor.sec-ext-dec-frame-skip-mode", 1);
//                format.setInteger("vendor.sec-ext-dec-performance-tuning.enable", 1);
//                format.setInteger("vendor.sec-ext-dec-h264-enable-sps-pps", 1);
//                format.setInteger("vendor.sec-ext-dec-rate-control-enable", 1);
//                format.setInteger("vendor.sec-ext-dec-extra-buffers", 2);
//            } else if (isQcomDevice) {
//                format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
//                format.setInteger("vendor.qti-ext-dec-low-latency-mode.value", 1);
//            }
//
//            if (spsData != null && ppsData != null) {
//                format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
//                format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if (isSamsungDevice) {
//                    outputSurface.setFrameRate(commandedTargetFps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
//                }
//                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//            }
//
//            if (isLowLatencySupported) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
//                }
//                format.setInteger(MediaFormat.KEY_TEMPORAL_LAYERING, 0);
//            }
//
//            if (isHighRes || width * height > 1920 * 1080) {
//                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024);
//                format.setInteger("max-width", width);
//                format.setInteger("max-height", height);
//            }
//
//            mediaCodec.configure(format, outputSurface, null, 0);
//            mediaCodec.start();
//
//            isRunning.set(true);
//            isInitialized.set(true);
//            startDecoderThread();
//
//            stabilizer.setJitterTolerance(0.5f); // More tolerant of timing variations
//            stabilizer.setPtsMode(FpsStabilizer.PtsMode.HYBRID); // Best for variable streams
//
//            Log.d(TAG, "Decoder initialized successfully");
//            return true;
//        } catch (MediaCodec.CodecException e) {
//            Log.e(TAG, "CodecException during init "+e.getLocalizedMessage(), e);
//            throw e;
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "IllegalStateException – codec misuse "+e.getLocalizedMessage(), e);
//            throw e;
//        } catch (Exception e) {
//            Log.e(TAG, "Unexpected init error", e);
//            throw new IllegalStateException("Decoder init failed", e);
//        }
//    }
//
//    private void cleanupBuffers() {
//        try {
//            bufferLock.lock();
//            try {
//                bufferStates.clear();
//                if (mediaCodec != null) {
//                    try {
//                        mediaCodec.flush();
//                    } catch (IllegalStateException e) {
//                        Log.w(TAG, "Flush failed during cleanup");
//                    }
//                }
//            } finally {
//                bufferLock.unlock();
//            }
//        } catch (Exception e) {
//            Log.w(TAG, "Error in cleanupBuffers", e);
//        }
//    }
//
//    // Add this method to gracefully stop the decoder
//    public void stopDecoding() {
//        Log.w(TAG, "stopDecoding called");
//        isShuttingDown.set(true);
//        isRunning.set(false);
//
//        // Signal waiting threads to wake up
//        queueLock.lock();
//        try {
//            frameAvailable.signalAll();
//        } finally {
//            queueLock.unlock();
//        }
//
//        // Clear the queue to prevent further processing
//        frameQueue.clear();
//
//        // Wait a bit for threads to notice the shutdown
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    public void release() {
//        Log.w(TAG, "Decoder release called");
//
//        // 1. Stop decoding first
//        stopDecoding();
//
//        // 2. Wait for decoder thread to stop
//        if (decoderThread != null) {
//            try {
//                decoderThread.join(500); // Shorter timeout
//            } catch (InterruptedException e) {
//                Log.w(TAG, "Decoder thread join interrupted", e);
//                Thread.currentThread().interrupt();
//            } finally {
//                decoderThread = null;
//            }
//        }
//
//        // 3. Now clean up MediaCodec
//        synchronized (codecLock) {
//            if (mediaCodec != null) {
//                try {
//                    // First stop
//                    try {
//                        mediaCodec.stop();
//                    } catch (IllegalStateException e) {
//                        Log.w(TAG, "MediaCodec already stopped: " + e.getMessage());
//                    }
//
//                    // Small delay
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//
//                    // Then release
//                    try {
//                        mediaCodec.release();
//                    } catch (Exception e) {
//                        Log.e(TAG, "MediaCodec release failed: " + e.getLocalizedMessage(), e);
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Unexpected error during codec cleanup", e);
//                } finally {
//                    mediaCodec = null;
//                }
//            }
//        }
//
//        // 4. Clean up other resources
//        cleanupBuffers();
//        stabilizer.reset();
//        isStreamingReadyToView = false;
//
//        // 5. Clear queue again
//        frameQueue.clear();
//
//        // 6. Reset surface reference (don't release it here - let owner do that)
//        outputSurface = null;
//
//        // 7. Reset flags
//        lastRenderTimeNs.set(0);
//        isInitialized.set(false);
//        isDecoderStopped.set(true);
//
//        Log.w(TAG, "Decoder release completed");
//    }
//
//    public void enqueueFrame(ByteBuffer frameBuffer, int size, long pts, int flags) {
//        // Don't accept frames if shutting down or stopped
//        if (isShuttingDown.get() || !isRunning.get() || frameBuffer == null || size <= 0) {
//            return;
//        }
//
//        // Update FPS metrics
//        framesReceived++;
//        updateFpsMetrics();
//
//        boolean isKeyFrame = (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
//        queueLock.lock();
//        try {
//            // Prevent queue from growing too large - use different strategies based on FPS
//            if (measuredInputFps > 35) {
//                // New logic for high FPS
//                if (frameQueue.size() >= MAX_QUEUE_SIZE) {
//                    if (measuredInputFps > 50) {
//                        // For very high FPS, drop oldest non-key frame
//                        FrameData oldest = frameQueue.peek();
//                        if (oldest != null && !oldest.isKeyFrame) {
//                            frameQueue.poll();
//                            Log.v(TAG, "Dropped old frame for low latency");
//                        }
//                    } else {
//                        // For moderate high FPS, wait briefly
//                        try {
//                            frameAvailable.await(5, TimeUnit.MILLISECONDS);
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                            return;
//                        }
//                    }
//                }
//            } else {
//                // Original logic for low FPS
//                while (frameQueue.size() >= MAX_QUEUE_SIZE && isRunning.get() && !isShuttingDown.get()) {
//                    try {
//                        frameAvailable.await(10, TimeUnit.MILLISECONDS);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        return;
//                    }
//                }
//            }
//
//            if (!isRunning.get() || isShuttingDown.get()) return;
//
//            ByteBuffer copyBuffer = ByteBuffer.allocateDirect(size);
//            frameBuffer.position(0).limit(size);
//            copyBuffer.put(frameBuffer);
//            copyBuffer.flip();
//
//            frameQueue.offer(new FrameData(copyBuffer, size, System.nanoTime() + 1000, flags, isKeyFrame));
//            frameAvailable.signal();
//        } finally {
//            queueLock.unlock();
//        }
//    }
//
//    private void startDecoderThread() {
//        decoderThread = new Thread(() -> {
//            while (isRunning.get() && !isShuttingDown.get()) {
//                FrameData frameData;
//                queueLock.lock();
//                try {
//                    frameData = frameQueue.poll();
//                    if (frameData == null) {
//                        // Check if we should exit
//                        if (!isRunning.get() || isShuttingDown.get()) {
//                            break;
//                        }
//
//                        // Use different wait times based on FPS
//                        if (measuredInputFps > 35) {
//                            frameAvailable.await(measuredInputFps > 50 ? 2 : 10, TimeUnit.MILLISECONDS);
//                        } else {
//                            frameAvailable.await(10, TimeUnit.MILLISECONDS);
//                        }
//                        continue;
//                    }
//                    frameAvailable.signal();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                } finally {
//                    queueLock.unlock();
//                }
//
//                frameProcessingStartTime = System.nanoTime();
//                if (frameData.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
//                    decodeFrame(frameData);
//                }
//
//                // Adaptive sleep to maintain frame rate - only for low FPS
//                if (measuredInputFps <= 35) {
//                    long processingTime = (System.nanoTime() - frameProcessingStartTime) / 1000;
//                    long ff = 1_000_000 / commandedTargetFps;
//                    if (processingTime < ff) {
//                        try {
//                            Thread.sleep((ff - processingTime) / 1000);
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                            break;
//                        }
//                    }
//                }
//            }
//            Log.d(TAG, "Decoder thread exiting");
//        }, "H264DecoderThread");
//
//        decoderThread.setPriority(Thread.MAX_PRIORITY);
//        decoderThread.start();
//    }
//
//    private void decodeFrame(FrameData frameData) {
//        synchronized (codecLock) {
//            if (!isRunning.get() || isShuttingDown.get()) {
//                return;
//            }
//
//            int inputBufferIndex = -1;
//            try {
//                // 1. Get input buffer with state tracking
//                bufferLock.lock();
//                try {
//                    inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
//                    if (inputBufferIndex >= 0) {
//                        bufferStates.put(inputBufferIndex, 1); // Mark as in-use
//                    }
//                } finally {
//                    bufferLock.unlock();
//                }
//
//                if (inputBufferIndex < 0) {
//                    return;
//                }
//
//                // 2. Handle keyframes
//                if (frameData.isKeyFrame) {
//                    stabilizer.reset();
//                    bufferLock.lock();
//                    try {
//                        bufferStates.clear(); // Reset buffer states on keyframe
//                    } finally {
//                        bufferLock.unlock();
//                    }
//
//                    // Also reset frame skip logic on key frame
//                    if (measuredInputFps > 35) {
//                        frameSkipLogic.reset();
//                    }
//                }
//
//                // 3. Prepare input
//                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
//                if (inputBuffer == null) {
//                    returnBuffer(inputBufferIndex);
//                    return;
//                }
//
//                inputBuffer.clear();
//                frameData.buffer.position(0).limit(frameData.size);
//                inputBuffer.put(frameData.buffer);
//
//                // Ensure flags are valid
//                int validFlags = 0;
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
//                }
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
//                    Log.d(TAG, "FLAG: BUFFER_FLAG_CODEC_CONFIG");
//                }
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
//                    Log.d(TAG, "FLAG: BUFFER_FLAG_PARTIAL_FRAME");
//                }
//                if ((frameData.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                    validFlags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
//                    Log.d(TAG, "FLAG: BUFFER_FLAG_END_OF_STREAM");
//                }
//
//                // 4. Calculate and validate PTS
//                long pts = stabilizer.calculatePtsUs(frameData.pts);
//                mediaCodec.queueInputBuffer(inputBufferIndex, 0, frameData.size, pts, validFlags);
//
//                // 5. Process output with appropriate strategy
//                if (measuredInputFps > 35) {
//                    processOutputBuffersHighFps();
//                } else {
//                    processOutputBuffers();
//                }
//
//            } catch (Exception e) {
//                Log.e(TAG, "Decode error", e);
//                if (inputBufferIndex >= 0) {
//                    returnBuffer(inputBufferIndex);
//                }
//                // Don't handle decoder failure during shutdown
//                if (!isShuttingDown.get()) {
//                    handleDecoderFailure(e);
//                }
//            }
//        }
//    }
//
//    private void processOutputBuffers() {
//        if (isShuttingDown.get()) {
//            return;
//        }
//
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex;
//
//        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) >= 0) {
//            try {
//                // Calculate render time with stabilization
//                long renderTimeNs;
//                if (bufferInfo.presentationTimeUs > 0) {
//                    renderTimeNs = stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs);
//                } else {
//                    renderTimeNs = System.nanoTime();
//                }
//
//                mediaCodec.releaseOutputBuffer(outputBufferIndex, renderTimeNs);
//                stabilizer.onFrameProcessed();
//
//                // Update FPS metrics
//                framesRendered++;
//                updateFpsMetrics();
//
//                // Update render timing
//                lastRenderTimeNs.set(renderTimeNs);
//                if (callback != null && !isStreamingReadyToView) {
//                    callback.enableStreamingView(true);
//                    isStreamingReadyToView = true;
//                }
//
//            } finally {
//                returnBuffer(outputBufferIndex);
//            }
//        }
//
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            handleFormatChange();
//        }
//    }
//
//    private void processOutputBuffersHighFps() {
//        if (isShuttingDown.get()) {
//            return;
//        }
//
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex;
//
//        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)) >= 0) {
//            try {
//                // Intelligent frame skipping decision for high FPS
//                boolean shouldSkip = frameSkipLogic.shouldSkipFrame(
//                        (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
//                        measuredInputFps,
//                        actualTargetFps
//                );
//
//                if (shouldSkip) {
//                    // Skip rendering but mark as processed
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//                    stabilizer.onFrameSkipped();
//                } else {
//                    // Calculate render time
//                    long renderTimeNs;
//                    if (bufferInfo.presentationTimeUs > 0) {
//                        renderTimeNs = stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs);
//                    } else {
//                        renderTimeNs = System.nanoTime();
//                    }
//
//                    // Render frame
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, renderTimeNs);
//
//                    framesRendered++;
//                    stabilizer.onFrameProcessed();
//                    updateFpsMetrics();
//                    lastRenderTimeNs.set(renderTimeNs);
//
//                    if (callback != null && !isStreamingReadyToView) {
//                        callback.enableStreamingView(true);
//                        isStreamingReadyToView = true;
//                    }
//                }
//
//            } finally {
//                returnBuffer(outputBufferIndex);
//            }
//        }
//
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            handleFormatChange();
//        }
//    }
//
//    private void returnBuffer(int bufferIndex) {
//        bufferLock.lock();
//        try {
//            bufferStates.delete(bufferIndex);
//        } finally {
//            bufferLock.unlock();
//        }
//    }
//
//    private void handleFormatChange() {
//        try {
//            MediaFormat newFormat = mediaCodec.getOutputFormat();
//            if (callback != null) {
//                int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
//                int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
//                callback.onOutputFormatChanged(width, height);
//            }
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "Error handling format change: " + e.getMessage());
//        }
//    }
//
//    private void handleDecoderFailure(Exception ex) {
//        Log.e(TAG, "Decoder failure, releasing", ex);
//        // Don't call callback during shutdown
//        if (!isShuttingDown.get() && callback != null) {
//            callback.setSpsPpsWithStartCode();
//        }
//    }
//
//    // Add this method to check if decoder is stopped
//    public boolean isStopped() {
//        return isDecoderStopped.get();
//    }
//}


/*Somewhat okay*/
package com.dome.librarynightwave.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class H264DecoderDeviceSpecific {
    private static final String TAG = "H264Decoder";
    private static final String MIME_TYPE = "video/avc";
    private static final int TIMEOUT_US = 10000;
    private static final int MAX_QUEUE_SIZE = 20;

    // Add shutdown control
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean isDecoderStopped = new AtomicBoolean(true);

    // Device detection
    private final boolean isSamsungDevice;
    private final boolean isQcomDevice;
    private final boolean isLowLatencySupported;

    private final AtomicLong lastPtsUs = new AtomicLong(0);

    // FPS tracking
    private final AtomicLong lastRenderTimeNs = new AtomicLong(0);

    // FPS metrics from new code
    private volatile long lastFpsUpdateTime = System.currentTimeMillis();
    private volatile int framesRendered = 0;
    private volatile int framesReceived = 0;
    private volatile int measuredInputFps = 30;
    private volatile int measuredOutputFps = 30;

    // Target FPS control
    private static final int ORIGINAL_TARGET_FPS = 30;
    private volatile int commandedTargetFps = ORIGINAL_TARGET_FPS;
    private volatile int actualTargetFps = ORIGINAL_TARGET_FPS;

    // Frame queue
    private final ConcurrentLinkedQueue<FrameData> frameQueue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition frameAvailable = queueLock.newCondition();

    // Decoder components
    private MediaCodec mediaCodec;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private Thread decoderThread;
    private final Object codecLock = new Object();
    private Thread renderThread;
    private Surface outputSurface;
    private long frameProcessingStartTime;
    boolean isHighResolution = false;

    private volatile boolean isStreamingReadyToView = false;
    private final FpsStabilizer stabilizer = new FpsStabilizer(ORIGINAL_TARGET_FPS, 5, true);
    private final SparseIntArray bufferStates = new SparseIntArray();
    private final ReentrantLock bufferLock = new ReentrantLock();

    // Frame skipping logic from new code (only used when FPS > 35)
    private final FrameSkipLogic frameSkipLogic = new FrameSkipLogic();

    private byte[] spsData;
    private byte[] ppsData;

    private boolean codecActive = false;

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

    public H264DecoderDeviceSpecific() {
        this.isSamsungDevice = Build.MANUFACTURER.equalsIgnoreCase("samsung");
        this.isQcomDevice = Build.HARDWARE.toLowerCase().contains("qcom");
        this.isLowLatencySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public void setSizeChangedCallback(SizeChangedCallback callback) {
        this.callback = callback;
    }

    private static class FrameData {
        final ByteBuffer buffer;
        final int size;
        long pts;
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

    // REVISED Frame skipping logic - LESS AGGRESSIVE
    private static class FrameSkipLogic {
        // More conservative configuration
        private static final int MIN_FPS_FOR_SKIP = 45; // Increased from 40
        private static final int MAX_SKIP_RATIO = 4; // Increased from 3 (skip less frequently)
        private static final int MIN_KEYFRAME_INTERVAL_MS = 1000;
        private static final int QUALITY_PRESERVATION_WINDOW = 15; // Increased window

        // New: Skip threshold - only skip if FPS difference is significant
        private static final int MIN_FPS_DIFFERENCE_FOR_SKIP = 15;

        private int consecutiveNonKeyFrames = 0;
        private int framesSinceLastKeyFrame = 0;
        private long lastKeyFrameTime = 0;
        private final Deque<Boolean> recentFrameQuality = new ArrayDeque<>();
        private int skipCounter = 0;
        private int skipInterval = 1;
        private int consecutiveSkips = 0;
        private long lastSkipTime = 0;

        boolean shouldSkipFrame(boolean isKeyFrame, int currentFps, int targetFps) {
            long currentTime = System.currentTimeMillis();

            if (isKeyFrame) {
                lastKeyFrameTime = currentTime;
                framesSinceLastKeyFrame = 0;
                consecutiveNonKeyFrames = 0;
                recentFrameQuality.clear();
                skipCounter = 0;
                skipInterval = 1;
                consecutiveSkips = 0;
                Log.d(TAG, "Key frame detected - resetting skip logic");
                return false;
            }

            framesSinceLastKeyFrame++;

            // More conservative FPS check
            if (currentFps < MIN_FPS_FOR_SKIP) {
                skipCounter = 0;
                return false;
            }

            // Check FPS difference - don't skip if difference is small
            int fpsDifference = currentFps - targetFps;
            if (fpsDifference < MIN_FPS_DIFFERENCE_FOR_SKIP) {
                skipCounter = 0;
                return false;
            }

            long timeSinceLastKeyFrame = currentTime - lastKeyFrameTime;
            boolean closeToNextKeyFrame = timeSinceLastKeyFrame > (MIN_KEYFRAME_INTERVAL_MS - 300); // More conservative

            // More conservative skip interval calculation
            // Skip less aggressively: only skip every 3rd or 4th frame instead of every 2nd
            int calculatedSkipInterval;
            if (fpsDifference > 25) {
                calculatedSkipInterval = 3; // Skip every 3rd frame
            } else if (fpsDifference > 20) {
                calculatedSkipInterval = 4; // Skip every 4th frame
            } else if (fpsDifference > 15) {
                calculatedSkipInterval = 5; // Skip every 5th frame
            } else {
                calculatedSkipInterval = 6; // Skip every 6th frame
            }

            // Cap at MAX_SKIP_RATIO
            calculatedSkipInterval = Math.min(MAX_SKIP_RATIO, calculatedSkipInterval);

            // Be more conservative as we approach next key frame
            if (closeToNextKeyFrame) {
                calculatedSkipInterval = Math.max(4, calculatedSkipInterval + 1); // Skip even less
            }

            // Update skip interval gradually
            if (Math.abs(calculatedSkipInterval - skipInterval) > 1) {
                skipInterval = (skipInterval + calculatedSkipInterval) / 2;
            } else {
                skipInterval = calculatedSkipInterval;
            }

            // Apply skip logic
            skipCounter++;
            boolean shouldSkip = (skipCounter % skipInterval) == 0;

            if (shouldSkip) {
                consecutiveSkips++;
                consecutiveNonKeyFrames++;

                // Limit consecutive skips more strictly
                if (consecutiveSkips > 2) {
                    shouldSkip = false;
                    consecutiveSkips = 0;
                    skipCounter = 0;
                    Log.v(TAG, "Preventing consecutive skips to maintain smoothness");
                }

                // Don't skip too many consecutive non-key frames
                if (consecutiveNonKeyFrames > 3) { // Reduced from 5
                    shouldSkip = false;
                    consecutiveNonKeyFrames = 0;
                    skipCounter = 0;
                    Log.v(TAG, "Preserving frame after consecutive non-key frames");
                }

                // Track quality impact
                recentFrameQuality.addLast(shouldSkip);
                if (recentFrameQuality.size() > QUALITY_PRESERVATION_WINDOW) {
                    recentFrameQuality.removeFirst();
                }

                // Calculate skip rate in recent window
                long skipCount = 0;
                for (Boolean skip : recentFrameQuality) {
                    if (skip) skipCount++;
                }
                double skipRate = (double) skipCount / recentFrameQuality.size();

                // Reduce skipping if quality is degrading - more sensitive threshold
                if (skipRate > 0.35) { // Reduced from 0.5 (more conservative)
                    skipInterval = Math.max(4, skipInterval + 1); // Skip even less
                    Log.v(TAG, "Reducing skip rate to preserve quality. Current rate: " + (skipRate * 100) + "%");
                }

                // Reset skip counter if we haven't skipped in a while
                if (currentTime - lastSkipTime > 200) { // 200ms without skipping
                    skipCounter = 0;
                }
                lastSkipTime = currentTime;

            } else {
                consecutiveNonKeyFrames = 0;
                consecutiveSkips = 0;
            }

            if (shouldSkip) {
                Log.v(TAG, String.format("Skipping frame: FPS in/out=%d/%d, diff=%d, interval=%d, sinceKeyFrame=%dms",
                        currentFps, targetFps, fpsDifference, skipInterval, timeSinceLastKeyFrame));
            }

            return shouldSkip;
        }

        void reset() {
            consecutiveNonKeyFrames = 0;
            framesSinceLastKeyFrame = 0;
            lastKeyFrameTime = 0;
            recentFrameQuality.clear();
            skipCounter = 0;
            skipInterval = 1;
            consecutiveSkips = 0;
            lastSkipTime = 0;
        }
    }

    public boolean isInitialized() {
        return isInitialized.get() && !isShuttingDown.get();
    }

    public void onFpsChanged(int newFps) {
        // Update measured FPS
        this.measuredInputFps = newFps;

        if (newFps <= 35) {
            // Use original logic for ≤35 FPS
            if (newFps >= 30) {
                commandedTargetFps = 40;
            } else if (newFps >= 20) {
                commandedTargetFps = 30;
            } else if (newFps >= 10) {
                commandedTargetFps = 20;
            } else {
                commandedTargetFps = 10;//ORIGINAL_TARGET_FPS;
            }

            if (commandedTargetFps <= 0) {
                commandedTargetFps = 10;//ORIGINAL_TARGET_FPS;
                Log.w(TAG, "Commanded target FPS was non-positive, falling back to " + ORIGINAL_TARGET_FPS);
            }
            stabilizer.setFps(commandedTargetFps);
            stabilizer.setJitterTolerance(0.5f);
        } else {
            // Use new logic for >35 FPS - but less aggressive
            int newTargetFps;
            if (newFps >= 70) {
                newTargetFps = 50;
            } else if (newFps >= 50) {
                newTargetFps = 45; // Increased from 40
            } else if (newFps >= 40) {
                newTargetFps = 38; // Increased from 35
            } else {
                newTargetFps = Math.max(30, newFps - 3); // Less reduction
            }

            // Always update for high FPS to keep it responsive
            actualTargetFps = newTargetFps;
            commandedTargetFps = actualTargetFps;
            stabilizer.setFps(actualTargetFps);

            // More tolerant jitter for smoother playback
            if (actualTargetFps > 40) {
                stabilizer.setJitterTolerance(0.6f); // Increased tolerance
            } else {
                stabilizer.setJitterTolerance(0.8f); // Increased tolerance
            }

            Log.d(TAG, "Target FPS adjusted: " + actualTargetFps +
                    " (input: " + newFps + ")");
        }
    }

    private void updateFpsMetrics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsUpdateTime >= 1000) {
            measuredOutputFps = framesRendered;

            Log.d(TAG, String.format("FPS Stats - Input: %d, Output: %d, Target: %d, Queue: %d",
                    measuredInputFps, measuredOutputFps, actualTargetFps, frameQueue.size()));

            framesRendered = 0;
            framesReceived = 0;
            lastFpsUpdateTime = currentTime;
        }
    }

    public boolean initialize(Surface surface, int width, int height, boolean isHighRes) {
        try {
            Log.e(TAG, "initialize called ");

            // Reset shutdown state
            isShuttingDown.set(false);
            isDecoderStopped.set(false);

            // Reset state
            isRunning.set(false);
            isInitialized.set(false);

            // Reset metrics
            measuredInputFps = 30;
            measuredOutputFps = 30;
            framesRendered = 0;
            framesReceived = 0;
            lastFpsUpdateTime = System.currentTimeMillis();
            frameSkipLogic.reset();

            if (surface == null || !surface.isValid() || codecActive) {
                Log.e(TAG, "Invalid surface provided");
                return false;
            }

            isHighResolution = isHighRes;
            outputSurface = surface;
            synchronized (codecLock) {
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
                format.setInteger(MediaFormat.KEY_LATENCY, 1);  // Low latency mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE);
                    format.setInteger(MediaFormat.KEY_PRIORITY, 0);  // Real-time priority
                }
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024); // 20MB

                // Device-specific configuration
                if (isSamsungDevice) {
                    format.setInteger("vendor.sec-ext-dec-low-latency.enable", 1);
                    format.setInteger("vendor.sec-ext-dec-low-latency-mode.value", 1);
                    format.setInteger("vendor.sec-ext-dec-render-mode", 0);
                    format.setInteger("vendor.sec-ext-dec-frame-skip-mode", 1);
                    format.setInteger("vendor.sec-ext-dec-performance-tuning.enable", 1);
                    format.setInteger("vendor.sec-ext-dec-h264-enable-sps-pps", 1);
                    format.setInteger("vendor.sec-ext-dec-rate-control-enable", 1);
                    format.setInteger("vendor.sec-ext-dec-extra-buffers", 2);
                } else if (isQcomDevice) {
                    format.setInteger("vendor.qti-ext-dec-low-latency.enable", 1);
                    format.setInteger("vendor.qti-ext-dec-low-latency-mode.value", 1);
                }

                if (spsData != null && ppsData != null) {
                    format.setByteBuffer("csd-0", ByteBuffer.wrap(spsData));
                    format.setByteBuffer("csd-1", ByteBuffer.wrap(ppsData));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (isSamsungDevice) {
                        outputSurface.setFrameRate(commandedTargetFps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
                    }
                    format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
                }

                if (isLowLatencySupported) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isHighRes) {
                        format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
                    }
                    format.setInteger(MediaFormat.KEY_TEMPORAL_LAYERING, 0);
                }

                if (isHighRes || width * height > 1920 * 1080) {
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 20 * 1024 * 1024);
                    format.setInteger("max-width", width);
                    format.setInteger("max-height", height);
                }

                mediaCodec.configure(format, outputSurface, null, 0);
                mediaCodec.start();
                codecActive = true;

                isRunning.set(true);
                isInitialized.set(true);
                startDecoderThread();

                stabilizer.setJitterTolerance(0.5f); // More tolerant of timing variations
                stabilizer.setPtsMode(FpsStabilizer.PtsMode.HYBRID); // Best for variable streams
            }
            Log.d(TAG, "Decoder initialized successfully");
            return true;
        } catch (MediaCodec.CodecException e) {
            Log.e(TAG, "CodecException during init "+e.getLocalizedMessage(), e);
            throw e;
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException – codec misuse "+e.getLocalizedMessage(), e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected init error", e);
            throw new IllegalStateException("Decoder init failed", e);
        }
    }


    // Add this method to gracefully stop the decoder
//    public void stopDecoding() {
//        Log.w(TAG, "stopDecoding called");
//        isShuttingDown.set(true);
//        isRunning.set(false);
//
//        // Signal waiting threads to wake up
//        queueLock.lock();
//        try {
//            frameAvailable.signalAll();
//        } finally {
//            queueLock.unlock();
//        }
//
//        // Clear the queue to prevent further processing
//        frameQueue.clear();
//
//        // Wait a bit for threads to notice the shutdown
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    public void release() {
//        Log.w(TAG, "Decoder release called");
//
//        // 1. Stop decoding first
//        stopDecoding();
//
//        // 2. Wait for decoder thread to stop
//        if (decoderThread != null) {
//            try {
//                decoderThread.join(500); // Shorter timeout
//            } catch (InterruptedException e) {
//                Log.w(TAG, "Decoder thread join interrupted", e);
//                Thread.currentThread().interrupt();
//            } finally {
//                decoderThread = null;
//            }
//        }
//
//        // 3. Now clean up MediaCodec
//        synchronized (codecLock) {
//            if (mediaCodec != null) {
//                try {
//                    // First stop
//                    try {
//                        mediaCodec.stop();
//                    } catch (IllegalStateException e) {
//                        Log.w(TAG, "MediaCodec already stopped: " + e.getMessage());
//                    }
//
//                    // Small delay
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//
//                    // Then release
//                    try {
//                        mediaCodec.release();
//                    } catch (Exception e) {
//                        Log.e(TAG, "MediaCodec release failed: " + e.getLocalizedMessage(), e);
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Unexpected error during codec cleanup", e);
//                } finally {
//                    mediaCodec = null;
//                }
//            }
//        }
//
//        // 4. Clean up other resources
//        cleanupBuffers();
//        stabilizer.reset();
//        isStreamingReadyToView = false;
//
//        // 5. Clear queue again
//        frameQueue.clear();
//
//        // 6. Reset surface reference (don't release it here - let owner do that)
//        outputSurface = null;
//
//        // 7. Reset flags
//        lastRenderTimeNs.set(0);
//        isInitialized.set(false);
//        isDecoderStopped.set(true);
//
//        Log.w(TAG, "Decoder release completed");
//    }


    private void cleanupBuffers() {
        try {
            bufferLock.lock();
            try {
                bufferStates.clear();
                if (mediaCodec != null) {
                    try {
                        mediaCodec.flush();
                    } catch (IllegalStateException e) {
                        Log.w(TAG, "Flush failed during cleanup");
                    }
                }
            } finally {
                bufferLock.unlock();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in cleanupBuffers", e);
        }
    }

    public void safeStop() {
        Log.w(TAG, "safeStop called");

        // 1. Set flags to stop processing
        isShuttingDown.set(true);
        isRunning.set(false);

        // 2. Clear queue to stop new frames
        frameQueue.clear();

        // 3. Signal all waiting threads
        queueLock.lock();
        try {
            frameAvailable.signalAll();
        } finally {
            queueLock.unlock();
        }

        // 4. Interrupt decoder thread
        if (decoderThread == null)
            return;

        if (decoderThread.isAlive()) {
            decoderThread.interrupt();
        }
    }

    public void forceStop() {
        Log.w(TAG, "forceStop called");

        // Immediately set all flags
        isShuttingDown.set(true);
        isRunning.set(false);
        isDecoderStopped.set(true);

        // Force interrupt
        if (decoderThread != null && decoderThread.isAlive()) {
            decoderThread.interrupt();
            try {
                decoderThread.join(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // Force MediaCodec stop
        synchronized (codecLock) {
            if (mediaCodec != null) {
                try {
                    mediaCodec.stop();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    public void forceRelease() {
        Log.w(TAG, "forceRelease called");

        if (!codecActive) return;
        forceStop(); // First stop everything

        // Now release resources without waiting
        synchronized (codecLock) {
            if (mediaCodec != null) {
                try {
                    mediaCodec.release();
                } catch (Exception e) {
                    // Ignore errors during force release
                } finally {
                    mediaCodec = null;
                    codecActive = false;
                }
            }
        }

        // Clear all queues and buffers
        frameQueue.clear();
        cleanupBuffers();

        // Reset all states
        outputSurface = null;
        isInitialized.set(false);
        isStreamingReadyToView = false;
        lastRenderTimeNs.set(0);
    }

    public boolean isRunning() {
        return isRunning.get() && !isShuttingDown.get();
    }

    public void release() {
        Log.w(TAG, "Decoder release called");

        // 1. First ensure we're stopped
        if (isRunning.get()) {
            safeStop();
        }

        // 2. Wait for decoder thread to stop with better logic
        if (decoderThread != null && decoderThread.isAlive()) {
            try {
                // First try graceful join
                decoderThread.join(300);

                // If still alive, interrupt and try again
                if (decoderThread.isAlive()) {
                    decoderThread.interrupt();
                    decoderThread.join(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Force interrupt if we were interrupted
                if (decoderThread != null && decoderThread.isAlive()) {
                    decoderThread.interrupt();
                }
            } finally {
                decoderThread = null;
            }
        }

        // 3. Now clean up MediaCodec
        synchronized (codecLock) {
            if (mediaCodec != null) {
                try {
                    // Stop if not already stopped
                    try {
                        mediaCodec.stop();
                    } catch (IllegalStateException e) {
                        Log.w(TAG, "MediaCodec already stopped");
                    }

                    // Release immediately
                    mediaCodec.release();
                } catch (Exception e) {
                    Log.e(TAG, "MediaCodec release failed", e);
                } finally {
                    mediaCodec = null;
                    codecActive = false;
                }
            }
        }

        // 4. Clean up other resources
        cleanupBuffers();
        stabilizer.reset();

        // 5. Clear all states
        frameQueue.clear();
        outputSurface = null;
        isStreamingReadyToView = false;
        lastRenderTimeNs.set(0);
        isInitialized.set(false);
        isDecoderStopped.set(true);

        Log.w(TAG, "Decoder release completed");
    }






    public void enqueueFrame(ByteBuffer frameBuffer, int size, long pts, int flags) {
        // Don't accept frames if shutting down or stopped
        if (isShuttingDown.get() || !isRunning.get() || frameBuffer == null || size <= 0) {
            return;
        }

        // Update FPS metrics
        framesReceived++;
        updateFpsMetrics();

        boolean isKeyFrame = (flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
        queueLock.lock();
        try {
            // For high FPS, use more aggressive queue management
            if (measuredInputFps > 35) {
                // Don't let queue fill up - drop frames if needed
                if (frameQueue.size() >= 20) { // Smaller threshold for high FPS
                    // Drop oldest non-key frame
                    FrameData oldest = frameQueue.peek();
                    if (oldest != null && !oldest.isKeyFrame) {
                        frameQueue.poll();
                        Log.v(TAG, "Dropped old frame for low latency (high FPS mode)");
                    }
                }
            } else {
                // Original logic for low FPS
                while (frameQueue.size() >= MAX_QUEUE_SIZE && isRunning.get() && !isShuttingDown.get()) {
                    try {
                        frameAvailable.await(10, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            if (!isRunning.get() || isShuttingDown.get()) return;

            ByteBuffer copyBuffer = ByteBuffer.allocateDirect(size);
            frameBuffer.position(0).limit(size);
            copyBuffer.put(frameBuffer);
            copyBuffer.flip();

            long frameDurationUs = 1_000_000L / Math.max(measuredInputFps, 1);
            long ptsUs = lastPtsUs.addAndGet(frameDurationUs);

            frameQueue.offer(new FrameData(copyBuffer, size, ptsUs, flags, isKeyFrame));
            frameAvailable.signal();
        } finally {
            queueLock.unlock();
        }
    }

    private void startDecoderThread() {
        decoderThread = new Thread(() -> {
            while (isRunning.get() && !isShuttingDown.get()) {
                FrameData frameData;
                queueLock.lock();
                try {
                    frameData = frameQueue.poll();
                    if (frameData == null) {
                        // Check if we should exit
                        if (!isRunning.get() || isShuttingDown.get()) {
                            break;
                        }

                        // Use shorter wait for high FPS to be more responsive
                        if (measuredInputFps > 35) {
                            frameAvailable.await(2, TimeUnit.MILLISECONDS); // Shorter wait
                        } else {
                            frameAvailable.await(10, TimeUnit.MILLISECONDS);
                        }
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
                if (frameData.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    decodeFrame(frameData);
                }

                // Adaptive sleep to maintain frame rate - only for low FPS
                if (measuredInputFps <= 35) {
                    long processingTime = (System.nanoTime() - frameProcessingStartTime) / 1000;
                    long ff = 1_000_000 / commandedTargetFps;
                    if (processingTime < ff) {
                        try {
                            Thread.sleep((ff - processingTime) / 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            Log.d(TAG, "Decoder thread exiting");
        }, "H264DecoderThread");

        decoderThread.setPriority(Thread.MAX_PRIORITY);
        decoderThread.start();
    }

    private void decodeFrame(FrameData frameData) {
        synchronized (codecLock) {
            if (!isRunning.get() || isShuttingDown.get()) {
                return;
            }

            int inputBufferIndex = -1;
            try {
                // 1. Get input buffer with state tracking
                bufferLock.lock();
                try {
                    inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
                    if (inputBufferIndex >= 0) {
                        bufferStates.put(inputBufferIndex, 1); // Mark as in-use
                    }
                } finally {
                    bufferLock.unlock();
                }

                if (inputBufferIndex < 0) {
                    return;
                }

                // 2. Handle keyframes
                if (frameData.isKeyFrame) {
                    stabilizer.reset();
                    bufferLock.lock();
                    try {
                        bufferStates.clear(); // Reset buffer states on keyframe
                    } finally {
                        bufferLock.unlock();
                    }

                    // Also reset frame skip logic on key frame
                    if (measuredInputFps > 35) {
                        frameSkipLogic.reset();
                    }
                }

                // 3. Prepare input
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                if (inputBuffer == null) {
                    returnBuffer(inputBufferIndex);
                    return;
                }

                inputBuffer.clear();
                frameData.buffer.position(0).limit(frameData.size);
                inputBuffer.put(frameData.buffer);

                // Ensure flags are valid
                int validFlags = 0;
                if ((frameData.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    validFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
                }
                if ((frameData.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    validFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
                    Log.d(TAG, "FLAG: BUFFER_FLAG_CODEC_CONFIG");
                }
                if ((frameData.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
                    validFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
                    Log.d(TAG, "FLAG: BUFFER_FLAG_PARTIAL_FRAME");
                }
                if ((frameData.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    validFlags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    Log.d(TAG, "FLAG: BUFFER_FLAG_END_OF_STREAM");
                }

                // 4. Calculate and validate PTS
//                long pts = stabilizer.calculatePtsUs(frameData.pts);
//                mediaCodec.queueInputBuffer(inputBufferIndex, 0, frameData.size, pts, validFlags);

                //stabilize FPS
                int fps = measuredInputFps <= 0 ? 15 : measuredInputFps;

                if (fps < 12) fps = 10;
                else if (fps < 18) fps = 15;
                else fps = 30;

                // generate monotonic PTS
                long frameDurationUs = 1_000_000L / fps;
                long ptsUs = lastPtsUs.addAndGet(frameDurationUs);

                // queue to decoder
                mediaCodec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        frameData.size,
                        ptsUs,
                        validFlags
                );

                // 5. Process output with appropriate strategy
                if (measuredInputFps > 35) {
                    processOutputBuffersHighFps();
                } else {
                    processOutputBuffers();
                }

            } catch (Exception e) {
                Log.e(TAG, "Decode error", e);
                if (inputBufferIndex >= 0) {
                    returnBuffer(inputBufferIndex);
                }
                // Don't handle decoder failure during shutdown
                if (!isShuttingDown.get()) {
                    handleDecoderFailure(e);
                }
            }
        }
    }

//    private void processOutputBuffers() {
//        if (isShuttingDown.get()) {
//            return;
//        }
//
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex;
//
//        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) >= 0) {
//            try {
//                // Calculate render time with stabilization
//                long renderTimeNs;
//                if (bufferInfo.presentationTimeUs > 0) {
//                    renderTimeNs = stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs);
//                } else {
//                    renderTimeNs = System.nanoTime();
//                }
//
//                mediaCodec.releaseOutputBuffer(outputBufferIndex, renderTimeNs);
//                stabilizer.onFrameProcessed();
//
//                // Update FPS metrics
//                framesRendered++;
//                updateFpsMetrics();
//
//                // Update render timing
//                lastRenderTimeNs.set(renderTimeNs);
//                if (callback != null && !isStreamingReadyToView) {
//                    callback.enableStreamingView(true);
//                    isStreamingReadyToView = true;
//                }
//
//            } finally {
//                returnBuffer(outputBufferIndex);
//            }
//        }
//
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            handleFormatChange();
//        }
//    }

    private void processOutputBuffers() {
        if (isShuttingDown.get()) return;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex;

        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) >= 0) {
            try {
                long currentTimeNs = System.nanoTime();

                // Use stabilizer for PTS-based timing, but cap future scheduling
                long scheduledRenderTimeNs = (bufferInfo.presentationTimeUs > 0)
                        ? stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs)
                        : currentTimeNs;

                // If frame is already late or scheduling too far ahead, render IMMEDIATELY
                long maxFutureDelayNs = 100_000_000L; // 100ms max future scheduling (adjustable)
                if (scheduledRenderTimeNs < currentTimeNs ||
                        scheduledRenderTimeNs > currentTimeNs + maxFutureDelayNs) {
                    scheduledRenderTimeNs = currentTimeNs; // Immediate render
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, scheduledRenderTimeNs);
                stabilizer.onFrameProcessed();

                // Update metrics
                framesRendered++;
                updateFpsMetrics();
                lastRenderTimeNs.set(scheduledRenderTimeNs);

                if (callback != null && !isStreamingReadyToView) {
                    callback.enableStreamingView(true);
                    isStreamingReadyToView = true;
                }

            } finally {
                returnBuffer(outputBufferIndex);
            }
        }

        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            handleFormatChange();
        }
    }

//    private void processOutputBuffersHighFps() {
//        if (isShuttingDown.get()) {
//            return;
//        }
//
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        int outputBufferIndex;
//
//        // Use longer timeout for high FPS to avoid busy waiting
//        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000)) >= 0) { // Increased from 0
//            try {
//                // Intelligent frame skipping decision for high FPS - LESS AGGRESSIVE
//                boolean shouldSkip = false;
//
//                // Only apply skipping if FPS is significantly higher than target
//                if (measuredInputFps > actualTargetFps + 10) {
//                    shouldSkip = frameSkipLogic.shouldSkipFrame(
//                            (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
//                            measuredInputFps,
//                            actualTargetFps
//                    );
//                }
//
//                if (shouldSkip) {
//                    // Skip rendering but mark as processed
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//                    stabilizer.onFrameSkipped();
//                    Log.v(TAG, "Frame skipped (high FPS mode)");
//                } else {
//                    // Calculate render time with more tolerant timing
//                    long renderTimeNs;
//                    if (bufferInfo.presentationTimeUs > 0) {
//                        renderTimeNs = stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs);
//                    } else {
//                        renderTimeNs = System.nanoTime();
//                    }
//
//                    // Render frame immediately without delay
//                    mediaCodec.releaseOutputBuffer(outputBufferIndex, renderTimeNs);
//
//                    framesRendered++;
//                    stabilizer.onFrameProcessed();
//                    updateFpsMetrics();
//                    lastRenderTimeNs.set(renderTimeNs);
//
//                    if (callback != null && !isStreamingReadyToView) {
//                        callback.enableStreamingView(true);
//                        isStreamingReadyToView = true;
//                    }
//                }
//
//            } finally {
//                returnBuffer(outputBufferIndex);
//            }
//        }
//
//        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            handleFormatChange();
//        }
//    }

    private void processOutputBuffersHighFps() {
        if (isShuttingDown.get()) return;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex;

        while ((outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) >= 0) {
            try {
                // Frame skipping logic (unchanged)
                boolean shouldSkip = false;
                if (measuredInputFps > actualTargetFps + 10) {
                    shouldSkip = frameSkipLogic.shouldSkipFrame(
                            (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
                            measuredInputFps,
                            actualTargetFps
                    );
                }

                if (shouldSkip) {
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    stabilizer.onFrameSkipped();
                    Log.v(TAG, "Frame skipped (high FPS mode)");
                    continue;
                }

                long currentTimeNs = System.nanoTime();

                // SAME scheduling logic as processOutputBuffers
                long scheduledRenderTimeNs = (bufferInfo.presentationTimeUs > 0)
                        ? stabilizer.calculateRenderTimeNs(bufferInfo.presentationTimeUs)
                        : currentTimeNs;

                long maxFutureDelayNs = 100_000_000L; // 100ms cap
                if (scheduledRenderTimeNs < currentTimeNs ||
                        scheduledRenderTimeNs > currentTimeNs + maxFutureDelayNs) {
                    scheduledRenderTimeNs = currentTimeNs;
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, scheduledRenderTimeNs);

                stabilizer.onFrameProcessed();
                framesRendered++;
                updateFpsMetrics();
                lastRenderTimeNs.set(scheduledRenderTimeNs);

                if (callback != null && !isStreamingReadyToView) {
                    callback.enableStreamingView(true);
                    isStreamingReadyToView = true;
                }

            } finally {
                returnBuffer(outputBufferIndex);
            }
        }

        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            handleFormatChange();
        }
    }

    private void returnBuffer(int bufferIndex) {
        bufferLock.lock();
        try {
            bufferStates.delete(bufferIndex);
        } finally {
            bufferLock.unlock();
        }
    }

    private void handleFormatChange() {
        try {
            MediaFormat newFormat = mediaCodec.getOutputFormat();
            if (callback != null) {
                int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
                callback.onOutputFormatChanged(width, height);
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error handling format change: " + e.getMessage());
        }
    }

    private void handleDecoderFailure(Exception ex) {
        Log.e(TAG, "Decoder failure, releasing", ex);
        // Don't call callback during shutdown
        if (!isShuttingDown.get() && callback != null) {
            callback.setSpsPpsWithStartCode();
        }
    }

    // Add this method to check if decoder is stopped
    public boolean isStopped() {
        return isDecoderStopped.get();
    }

}






