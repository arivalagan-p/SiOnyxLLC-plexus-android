package com.dome.librarynightwave.utils;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class FpsStabilizer {
    private final String TAG = "FpsStabilizer";

    // Configuration
    private static final int JITTER_WINDOW_SIZE = 30;
    private static final long MAX_FRAME_INDEX = 1_000_000L;

    // State tracking
    private long frameIntervalUs;
    private long startTimeNs = 0;
    private long frameIndex = 0;
    private long streamStartOffsetUs = 0;
    private long prevPtsUs = 0;
    private long lastRenderTimeNs = 0;

    // Buffering control
    private final int initialBufferCount;
    private boolean initialBuffering = true;

    // Jitter management
    private final Queue<Long> jitterLog = new LinkedList<>();
    private double jitterTolerance = 0.3;
    private double dynamicSleepThresholdUs = 2000;

    // Mode control
    private PtsMode ptsMode = PtsMode.HYBRID;
    private final boolean enableLogging;

    // Frame skipping support
    private int framesSkipped = 0;
    private int totalFramesProcessed = 0;
    private long lastSkipLogTimeNs = 0;
    private static final long SKIP_LOG_INTERVAL_NS = 1_000_000_000L; // 1 second

    public enum PtsMode {
        SYSTEM_CLOCK,      // Use system time only
        STREAM_CLOCK,      // Use stream timestamps only
        HYBRID             // Adaptive combination of both
    }

    public FpsStabilizer(int fps, int bufferFrames, boolean logging) {
        setFps(fps);
        this.initialBufferCount = Math.max(1, bufferFrames);
        this.enableLogging = logging;
    }

    /**
     * Sets the target frame rate and recalculates timing intervals
     */
//    public void setFps(int fps) {
//        this.frameIntervalUs = 1_000_000L / Math.max(1, fps);
//        this.dynamicSleepThresholdUs = Math.max(1000, frameIntervalUs / 2);
//    }

    public void setFps(int fps) {
        this.frameIntervalUs = 1_000_000L / Math.max(1, fps);

        // ADD/REPLACE HERE — higher threshold to avoid frequent small sleeps (reduces freeze risk)
        this.dynamicSleepThresholdUs = Math.max(5000, frameIntervalUs); // Only sleep for larger gaps (>=5ms or one frame)
    }

    /**
     * Adjusts how much jitter is tolerated before buffering (0.1-1.0)
     */
    public void setJitterTolerance(double tolerance) {
        this.jitterTolerance = Math.max(0.1, Math.min(1.0, tolerance));
    }

    /**
     * Sets the timestamp generation mode
     */
    public void setPtsMode(PtsMode mode) {
        this.ptsMode = mode;
    }

    /**
     * Resets all timing state (call on stream changes/keyframes)
     */
    public void reset() {
        startTimeNs = 0;
        frameIndex = 0;
        initialBuffering = true;
        jitterLog.clear();
        streamStartOffsetUs = 0;
        prevPtsUs = 0;
        lastRenderTimeNs = 0;
        framesSkipped = 0;
        totalFramesProcessed = 0;
        firstFramePtsUs = -1;
    }

    /**
     * Calculates presentation timestamp for a frame
     */
    public long calculatePtsUs(long streamPtsUs) {
        if (startTimeNs == 0) {
            initializeTiming(streamPtsUs);
        }

        // Handle PTS normalization - if PTS is huge, use relative calculation
        long normalizedPts = streamPtsUs;
        if (streamPtsUs > 10_000_000L) { // > 10 seconds
            // For large PTS values, calculate relative to first frame
            if (firstFramePtsUs == -1) {
                firstFramePtsUs = streamPtsUs;
            }
            normalizedPts = streamPtsUs - firstFramePtsUs;

            // Handle wrap-around
            if (normalizedPts < 0) {
                firstFramePtsUs = streamPtsUs;
                normalizedPts = 0;
            }
        }

        long ptsUs = calculateCurrentPts(normalizedPts);
        long expectedPtsUs = frameIndex * frameIntervalUs;

        // Handle large gaps (missing frames)
        if (frameIndex > 0 && expectedPtsUs - prevPtsUs > frameIntervalUs * 1.5) {
            expectedPtsUs = prevPtsUs + frameIntervalUs;
            if (enableLogging) {
                Log.d(TAG, String.format("Frame gap adjusted: %d -> %d", frameIndex * frameIntervalUs, expectedPtsUs));
            }
        }

        logAndSync(ptsUs, expectedPtsUs);
        prevPtsUs = expectedPtsUs;
        return expectedPtsUs;
    }

    // Add this field to FpsStabilizer
    private long firstFramePtsUs = -1;

    /**
     * Calculates precise render time in nanoseconds
     */
    public long calculateRenderTimeNs(long presentationTimeUs) {
        if (startTimeNs == 0) {
            return System.nanoTime(); // Fallback
        }

        long renderTimeNs = startTimeNs + (presentationTimeUs * 1000);
        long currentTimeNs = System.nanoTime();

        // If frame is late, render immediately
        if (renderTimeNs < currentTimeNs) {
            return currentTimeNs;
        }

        // Ensure smooth progression
        if (renderTimeNs <= lastRenderTimeNs) {
            renderTimeNs = lastRenderTimeNs + (1_000_000_000 / (1_000_000 / frameIntervalUs));
        }

        lastRenderTimeNs = renderTimeNs;
        return renderTimeNs;
    }

    /**
     * Call when a frame has been processed and rendered
     */
    public void onFrameProcessed() {
        frameIndex++;
        totalFramesProcessed++;
        checkFrameIndexWrap();

        // Check buffering state transitions
        if (initialBuffering) {
            if (frameIndex >= initialBufferCount &&
                    getAvgJitterUs() < frameIntervalUs * jitterTolerance) {
                initialBuffering = false;
                if (enableLogging) Log.d(TAG, "Buffering complete");
            }
        } else {
            // Re-enter buffering if jitter spikes
//            if (getAvgJitterUs() > frameIntervalUs * jitterTolerance * 1.5) {
//                initialBuffering = true;
//                if (enableLogging) Log.d(TAG, "High jitter, re-buffering");
//            }

            if (getAvgJitterUs() > frameIntervalUs * jitterTolerance * 3.0) {
                initialBuffering = true;
                if (enableLogging) Log.d(TAG, "High jitter, re-buffering");
            }
        }
    }

    /**
     * Call when a frame has been skipped (not rendered)
     * Important: We increment frameIndex but don't log jitter for skipped frames
     */
    public void onFrameSkipped() {
        framesSkipped++;
        totalFramesProcessed++;

        // Still increment frame index to maintain timing
        frameIndex++;
        checkFrameIndexWrap();

        // Log skip statistics periodically
        long currentTimeNs = System.nanoTime();
        if (currentTimeNs - lastSkipLogTimeNs >= SKIP_LOG_INTERVAL_NS) {
            double skipRate = (totalFramesProcessed > 0) ?
                    (framesSkipped * 100.0 / totalFramesProcessed) : 0;
            if (enableLogging) {
                Log.d(TAG, String.format("Frame skipping: %d skipped of %d total (%.1f%%)",
                        framesSkipped, totalFramesProcessed, skipRate));
            }
            lastSkipLogTimeNs = currentTimeNs;
        }
    }

    /**
     * Returns current measured FPS based on frame timing (only rendered frames)
     */
    public double getMeasuredFps() {
        if (jitterLog.size() < 2) return 0;
        long sum = 0;
        for (long interval : jitterLog) {
            sum += interval;
        }
        return 1_000_000.0 / (sum / (double)jitterLog.size());
    }

    /**
     * Returns average jitter in microseconds
     */
    public double getAvgJitterUs() {
        if (jitterLog.isEmpty()) return 0;
        long sum = 0;
        for (long jitter : jitterLog) {
            sum += jitter;
        }
        return sum / (double)jitterLog.size();
    }

    /**
     * Returns frame skipping statistics
     */
    public int getFramesSkipped() {
        return framesSkipped;
    }

    /**
     * Returns total frames processed (rendered + skipped)
     */
    public int getTotalFramesProcessed() {
        return totalFramesProcessed;
    }

    /**
     * Returns the current skip rate as a percentage
     */
    public double getSkipRate() {
        if (totalFramesProcessed == 0) return 0;
        return (framesSkipped * 100.0) / totalFramesProcessed;
    }

    // Private helper methods
    private void initializeTiming(long streamPtsUs) {
        startTimeNs = System.nanoTime();
        if (ptsMode != PtsMode.SYSTEM_CLOCK) {
            streamStartOffsetUs = streamPtsUs;
        }
    }

    private long calculateCurrentPts(long streamPtsUs) {
        switch (ptsMode) {
            case SYSTEM_CLOCK:
                return (System.nanoTime() - startTimeNs) / 1000;

            case STREAM_CLOCK:
                return streamPtsUs - streamStartOffsetUs;

            case HYBRID:
            default:
                return calculateHybridPts(streamPtsUs);
        }
    }

    private long calculateHybridPts(long streamPtsUs) {
        long systemPtsUs = (System.nanoTime() - startTimeNs) / 1000;
        long streamRelativeUs = streamPtsUs - streamStartOffsetUs;

        // Adaptive weighting based on jitter
        double jitterRatio = Math.min(getAvgJitterUs() / frameIntervalUs, 1.0);
        double streamWeight = 0.3 + (jitterRatio * 0.5); // 30-80% weight

        return (long)(systemPtsUs * (1 - streamWeight) + streamRelativeUs * streamWeight);
    }

    private void logAndSync(long currentPtsUs, long expectedPtsUs) {
        long syncErrorUs = currentPtsUs - expectedPtsUs;
        logJitter(Math.abs(syncErrorUs));

        if (!initialBuffering) {
            long sleepUs = expectedPtsUs - currentPtsUs;
            if (sleepUs > dynamicSleepThresholdUs) {
                preciseSleep(sleepUs);
                // Dynamically adjust sleep threshold
                dynamicSleepThresholdUs = Math.max(1000,
                        Math.min(getAvgJitterUs() / 2, 5000));
            }
        }
    }

//    private void preciseSleep(long sleepUs) {
//        long sleepStartNs = System.nanoTime();
//        try {
//            long mainSleep = Math.max(0, sleepUs - 2000);
//            if (mainSleep > 0) {
//                Thread.sleep(mainSleep / 1000, (int)(mainSleep % 1000) * 1000);
//            }
//            // Busy-wait for precision
//            while ((System.nanoTime() - sleepStartNs) / 1000 < sleepUs) {
//                Thread.yield();
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }

    private void preciseSleep(long sleepUs) {
        if (sleepUs <= 0) return;

        long sleepMs = sleepUs / 1000;
        int sleepNs = (int)((sleepUs % 1000) * 1000);

        try {
            if (sleepMs > 0) {
                Thread.sleep(sleepMs, sleepNs);
            }
            // REMOVE busy-wait — it's unnecessary and causes jitter/CPU spikes
            // while loop removed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logJitter(long diffUs) {
        jitterLog.add(diffUs);
        if (jitterLog.size() > JITTER_WINDOW_SIZE) {
            jitterLog.poll();
        }
    }

    private void checkFrameIndexWrap() {
        if (frameIndex >= MAX_FRAME_INDEX) {
            long elapsedUs = (System.nanoTime() - startTimeNs) / 1000;
            startTimeNs = System.nanoTime() - (elapsedUs % 1_000_000) * 1000;
            frameIndex = frameIndex % MAX_FRAME_INDEX;
            streamStartOffsetUs += elapsedUs - (elapsedUs % 1_000_000);
            if (enableLogging) Log.d(TAG, "Timing base adjusted");
        }
    }
}