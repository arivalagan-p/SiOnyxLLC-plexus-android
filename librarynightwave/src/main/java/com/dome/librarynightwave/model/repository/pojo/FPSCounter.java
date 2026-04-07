package com.dome.librarynightwave.model.repository.pojo;

public class FPSCounter {
    private long startTime;
    private int frameCount;

    public FPSCounter() {
        startTime = System.currentTimeMillis();
        frameCount = 0;
    }

    public void incrementFrameCount() {
        frameCount++;
    }
    private float fps = -1;
    public float calculateFPS() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        if (elapsedTime >= 1000) {  // Calculate FPS every second
            fps = (float) frameCount / (elapsedTime / 1000.0f);
            reset();
            return fps;
        }

        return fps;  // Return -1 if FPS is not yet calculated
    }

    public void reset() {
        startTime = System.currentTimeMillis();
        frameCount = 0;
    }
}


