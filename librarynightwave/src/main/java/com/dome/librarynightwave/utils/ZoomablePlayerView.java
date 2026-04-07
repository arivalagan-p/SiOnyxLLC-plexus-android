package com.dome.librarynightwave.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.dome.librarynightwave.R;

public class ZoomablePlayerView extends TextureView {

    private static final String SUPERSTATE_KEY = "superState";
    private static final String MIN_SCALE_KEY = "minScale";
    private static final String MAX_SCALE_KEY = "maxScale";
    private final Context context;
    private float minScale = 1f;
    private float maxScale = 10f;
    private float saveScale = 1f;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private final Matrix matrix = new Matrix();
    private ScaleGestureDetector mScaleDetector;
    private float[] m;
    private final PointF last = new PointF();
    private final PointF start = new PointF();
    private float right, bottom;
    private Touch touch;
    ScaleGestureDetector detectorOw;
    GestureDetector gestureDetector;

    public ZoomablePlayerView(Context context) {
        super(context);
        this.context = context;
        initView(null);
    }

    public ZoomablePlayerView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView(attrs);
    }

    public ZoomablePlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initView(attrs);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPERSTATE_KEY, super.onSaveInstanceState());
        bundle.putFloat(MIN_SCALE_KEY, minScale);
        bundle.putFloat(MAX_SCALE_KEY, maxScale);
        return bundle;

    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            this.minScale = bundle.getFloat(MIN_SCALE_KEY);
            this.maxScale = bundle.getFloat(MAX_SCALE_KEY);
            state = bundle.getParcelable(SUPERSTATE_KEY);
        }
        super.onRestoreInstanceState(state);
    }

    private void initView(AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ZoomablePlayerView,
                0, 0);
        try {
            minScale = a.getFloat(R.styleable.ZoomablePlayerView_minimumScale, minScale);
            maxScale = a.getFloat(R.styleable.ZoomablePlayerView_maximumScale, maxScale);
        } finally {
            a.recycle();
        }
        setOpaque(false);  // Important for TextureView transformations
        setOnTouchListener(new ZoomOnTouchListeners());
    }

    public void setListener(Touch touch) {
        this.touch = touch;
    }

    private class ZoomOnTouchListeners implements OnTouchListener {
        public ZoomOnTouchListeners() {
            super();
            m = new float[9];
            mScaleDetector = new ScaleGestureDetector(context, new ZoomOnTouchListeners.ScaleListener());
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mScaleDetector.onTouchEvent(motionEvent);
            gestureDetector.onTouchEvent(motionEvent);
            matrix.getValues(m);
            float x = m[Matrix.MTRANS_X];
            float y = m[Matrix.MTRANS_Y];
            PointF curr = new PointF(motionEvent.getX(), motionEvent.getY());

            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    last.set(motionEvent.getX(), motionEvent.getY());
                    start.set(last);
                    mode = DRAG;
                    if (touch != null) {
                        touch.onTouch();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mode != ZOOM) {
                        if (touch != null) {
                            touch.onTouchReleased();
                        }
                    }
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    last.set(motionEvent.getX(), motionEvent.getY());
                    start.set(last);
                    mode = ZOOM;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == ZOOM || (mode == DRAG && saveScale > minScale)) {
                        float deltaX = curr.x - last.x;// x difference
                        float deltaY = curr.y - last.y;// y difference
//                        Log.e("TAG", "onTouch: "+deltaX+" "+deltaY );
                        if (y + deltaY > 0)
                            deltaY = -y;
                        else if (y + deltaY < -bottom)
                            deltaY = -(y + bottom);

                        if (x + deltaX > 0)
                            deltaX = -x;
                        else if (x + deltaX < -right)
                            deltaX = -(x + right);
                        matrix.postTranslate(deltaX, deltaY);
                        last.set(curr.x, curr.y);
                    }
                    break;
            }
            ZoomablePlayerView.this.setTransform(matrix);
            ZoomablePlayerView.this.invalidate();
            return true;
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                mode = ZOOM;
                detectorOw = detector;
                if (zoomGestureListener != null) {
                    zoomGestureListener.onStartZoom();
                }
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float mScaleFactor = detector.getScaleFactor();
                float origScale = saveScale;
                saveScale *= mScaleFactor;
                if (saveScale > maxScale) {
                    saveScale = maxScale;
                    mScaleFactor = maxScale / origScale;
                } else if (saveScale < minScale) {
                    saveScale = minScale;
                    mScaleFactor = minScale / origScale;
                }
                right = getWidth() * saveScale - getWidth();
                bottom = getHeight() * saveScale - getHeight();
                if (right == 0.0 && bottom == 0.0) {
                    zoomGestureListener.onResetZoom();
                } else {
                    if (mScaleFactor > 0.98) {
                        zoomGestureListener.onStartZoom();
                    }
                }
                if (0 <= getWidth() || 0 <= getHeight()) {
                    matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
                    if (mScaleFactor < 1) {
                        matrix.getValues(m);
                        float x = m[Matrix.MTRANS_X];
                        float y = m[Matrix.MTRANS_Y];
                        if (mScaleFactor < 1) {
                            if (0 < getWidth()) {
                                if (y < -bottom)
                                    matrix.postTranslate(0, -(y + bottom));
                                else if (y > 0)
                                    matrix.postTranslate(0, -y);
                            } else {
                                if (x < -right)
                                    matrix.postTranslate(-(x + right), 0);
                                else if (x > 0)
                                    matrix.postTranslate(-x, 0);
                            }
                        }
                    }
                } else {
                    matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
                    matrix.getValues(m);
                    float x = m[Matrix.MTRANS_X];
                    float y = m[Matrix.MTRANS_Y];
                    if (mScaleFactor < 1) {
                        if (x < -right)
                            matrix.postTranslate(-(x + right), 0);
                        else if (x > 0)
                            matrix.postTranslate(-x, 0);
                        if (y < -bottom)
                            matrix.postTranslate(0, -(y + bottom));
                        else if (y > 0)
                            matrix.postTranslate(0, -y);
                    }
                }
                return true;
            }

        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (zoomGestureListener != null) {
                zoomGestureListener.onStartZoom();
            }
            // Calculate target scale (toggle between min and max scale)
            float targetScale = (saveScale > minScale + 0.1f) ? minScale : maxScale;

            // Animate the zoom
            ValueAnimator animator = ValueAnimator.ofFloat(saveScale, targetScale);
            animator.setDuration(200);
            animator.addUpdateListener(animation -> {
                float currentScale = (float) animation.getAnimatedValue();
                float scaleFactor = currentScale / saveScale;

                // Reset matrix to identity before applying new transformations
                matrix.reset();

                // Apply scale centered at the double-tap position
                matrix.postScale(currentScale, currentScale);

                // Adjust translation to keep content centered
                float focusX = e.getX();
                float focusY = e.getY();
                matrix.postTranslate(
                        -focusX * (currentScale - 1),
                        -focusY * (currentScale - 1)
                );

                // Update scale and boundaries
                saveScale = currentScale;
                right = getWidth() * saveScale - getWidth();
                bottom = getHeight() * saveScale - getHeight();
                // Apply the transformation
                ZoomablePlayerView.this.setTransform(matrix);
                ZoomablePlayerView.this.invalidate();
            });
            animator.start();
            return true;
        }
    }

    public interface ZoomGestureListener {
        void onStartZoom();

        void onResetZoom();
    }

    private ZoomGestureListener zoomGestureListener;

    public void setZoomGestureListener(ZoomGestureListener listener) {
        this.zoomGestureListener = listener;
    }

    public void reset() {
        matrix.reset();
        saveScale = minScale;
        right = getWidth() * saveScale - getWidth();
        bottom = getHeight() * saveScale - getHeight();
        setTransform(matrix);
        invalidate();
    }
}
