package com.dome.librarynightwave.model.repository.videoencoder;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class Encoder {

    protected static final int STATE_IDLE = 0;
    protected static final int STATE_RECORDING = 1;
    protected static final int STATE_RECORDING_UNTIL_LAST_FRAME = 2;
    private List<Bitmap> bitmapQueue;
    private EncodeFinishListener encodeFinishListener;
    private Thread encodingThread;
    private int height;
    protected String outputFilePath = null;
    private int state = STATE_IDLE;
    private int width;

    protected boolean shouldLoop = true;
    private final Runnable mRunnableEncoder = new Runnable() {
        public void run() {
            while (shouldLoop) {
                if (state != STATE_RECORDING && bitmapQueue.size() <= 0) {
                    break;
                } else if (bitmapQueue.size() > 0) {
                    Bitmap bitmap;
                    try {
                        bitmap = bitmapQueue.remove(0);
                        onAddFrame(bitmap);
//                        bitmap.recycle();
                        if (state == STATE_RECORDING_UNTIL_LAST_FRAME && bitmapQueue.size() == 0) {
                            Log.e("TAG", "Last frame added: ");
                            break;
                        }
                    } catch (Exception e) {
                        Log.e("TAG", "run: " + e.getLocalizedMessage());
                    }
                }
            }
            Log.e("TAG", "add Frame finished: ");
            onStop();
        }
    };


    public interface EncodeFinishListener {
        void onEncodeFinished();

        void onEncodingError(String err);
    }


    public Encoder() {
        init();
    }

    private void init() {
        onInit();
        initBitmapQueue();
    }


    private void initBitmapQueue() {
        bitmapQueue = Collections.synchronizedList(new ArrayList<>());
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public void setOutputSize(int width, int height) {
        this.width = width;
        this.height = height;
    }


    public void startEncode() {
        bitmapQueue.clear();
        onStart();
        setState(STATE_RECORDING);
        encodingThread = new Thread(this.mRunnableEncoder);
        encodingThread.setName("EncodeThread"+System.currentTimeMillis());
        encodingThread.start();
    }

    protected void notifyEncodeError(String error) {
        if (encodeFinishListener != null) {
            encodeFinishListener.onEncodingError(error);
        }
    }

    protected void notifyEncodeFinish() {
        if (encodeFinishListener != null) {
            encodeFinishListener.onEncodeFinished();
        }
    }

    public void stopEncode() {
        try {
            if (bitmapQueue.size() > 0) {
                Log.e("Encoder", "stopEncode: wait bitmapQueue.size() " + bitmapQueue.size());
                stopEncode();
            } else {
                Log.e("Encoder", "stopEncode: bitmapQueue.size() " + bitmapQueue.size());
                if (encodingThread != null && encodingThread.isAlive()) {
                    encodingThread.interrupt();
                }
                setState(STATE_IDLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addFrame(Bitmap bitmap) {
        if (state == STATE_RECORDING) {
            bitmapQueue.add(bitmap);
        }
    }

    public void setEncodeFinishListener(EncodeFinishListener listener) {
        encodeFinishListener = listener;
    }

    /**
     * Reserved for gif encoder
     */
    public void notifyLastFrameAdded() {
        setState(STATE_RECORDING_UNTIL_LAST_FRAME);
    }


    private void setState(int state) {
        this.state = state;
    }

    protected abstract void onAddFrame(Bitmap bitmap);

    protected abstract void onInit();

    protected abstract void onStart();

    protected abstract void onStop();

    protected int getHeight() {
        return height;
    }

    protected int getWidth() {
        return width;
    }

}
