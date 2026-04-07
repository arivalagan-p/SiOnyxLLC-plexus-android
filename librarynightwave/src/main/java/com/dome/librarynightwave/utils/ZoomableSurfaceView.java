package com.dome.librarynightwave.utils;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

public class ZoomableSurfaceView extends SurfaceView {

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private float scale = 1f;
    private final float minScale = 1f;
    private final float maxScale = 5f;

    private float width = 0f, height = 0f;
    private float lastX = 0f, lastY = 0f;
    private boolean isDragging = false;

    public ZoomableSurfaceView(Context context) {
        this(context, null);
    }

    public ZoomableSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        init();
    }



    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        isDragging = true;

                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isDragging) {
                            float dx = event.getRawX() - lastX;
                            float dy = event.getRawY() - lastY;
                            setX(getX() + dx);
                            setY(getY() + dy);
                            lastX = event.getRawX();
                            lastY = event.getRawY();
                            checkBounds();
                        }
                        break;
                    case MotionEvent.ACTION_UP:

                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        break;
                }
            }

            return true;
        });

        post(() -> {
            width = getWidth();
            height = getHeight();
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float newScale = scale * factor;

            if (newScale >= minScale && newScale <= maxScale) {
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                // Calculate the current offset of the focus point from the view's top-left
                float offsetX = focusX - getX();
                float offsetY = focusY - getY();

                scale = newScale;
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        (int) (width * scale), (int) (height * scale));
                setLayoutParams(params);

                // Calculate the new offset after scaling
                float newOffsetX = offsetX * factor;
                float newOffsetY = offsetY * factor;

                // Adjust the view's position to keep the focus point relatively stable
                setX(focusX - newOffsetX);
                setY(focusY - newOffsetY);

                checkBounds();
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (scale < minScale || scale > maxScale) {
                float targetScale = Math.max(minScale, Math.min(scale, maxScale));
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                animateZoom(scale, targetScale, focusX, focusY);
            }
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean zoomedIn = false;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            zoomedIn = !zoomedIn;
            float targetScale = zoomedIn ? 2f : 1f;
            animateZoom(scale, targetScale, e.getX(), e.getY());
            return true;
        }
    }

    private void checkBounds() {
        View parent = (View) getParent();
        if (parent == null) return;

        float viewW = getWidth();
        float viewH = getHeight();
        float parentW = parent.getWidth();
        float parentH = parent.getHeight();

        float newX = getX();
        float newY = getY();

        if (newX > 0) newX = 0;
        if (newY > 0) newY = 0;
        if (newX + viewW < parentW) newX = parentW - viewW;
        if (newY + viewH < parentH) newY = parentH - viewH;

        setX(newX);
        setY(newY);
    }
    private void animateZoom(float fromScale, float toScale, float focusX, float focusY) {
        ValueAnimator animator = ValueAnimator.ofFloat(fromScale, toScale);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float currentScale = (float) animation.getAnimatedValue();
            float scaleFactor = currentScale / scale;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    (int) (width * currentScale), (int) (height * currentScale));
            setLayoutParams(params);

            float dx = (focusX - getX()) - (focusX - getX()) * scaleFactor;
            float dy = (focusY - getY()) - (focusY - getY()) * scaleFactor;

            setX(getX() + dx);
            setY(getY() + dy);

            scale = currentScale;
            checkBounds();
        });
        animator.start();
    }

}
