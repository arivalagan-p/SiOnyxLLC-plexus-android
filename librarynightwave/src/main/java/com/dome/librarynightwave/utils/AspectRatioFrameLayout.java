package com.dome.librarynightwave.utils;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.dome.librarynightwave.R;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


public final class AspectRatioFrameLayout extends FrameLayout {

    public interface AspectRatioListener {

        void onAspectRatioUpdated(
                float targetAspectRatio, float naturalAspectRatio, boolean aspectRatioMismatch);
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
            RESIZE_MODE_FIT,
            RESIZE_MODE_FIXED_WIDTH,
            RESIZE_MODE_FIXED_HEIGHT,
            RESIZE_MODE_FILL,
            RESIZE_MODE_ZOOM
    })
    public @interface ResizeMode {}

    public static final int RESIZE_MODE_FIT = 0;

    public static final int RESIZE_MODE_FIXED_WIDTH = 1;

    public static final int RESIZE_MODE_FIXED_HEIGHT = 2;

    public static final int RESIZE_MODE_FILL = 3;

    public static final int RESIZE_MODE_ZOOM = 4;

    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;

    private final AspectRatioUpdateDispatcher aspectRatioUpdateDispatcher;

    @Nullable private AspectRatioListener aspectRatioListener;

    private float videoAspectRatio;
    private @ResizeMode int resizeMode;

    public AspectRatioFrameLayout(Context context) {
        this(context, /* attrs= */ null);
    }

    public AspectRatioFrameLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        resizeMode = RESIZE_MODE_FIT;
        if (attrs != null) {
            TypedArray a = context
                            .getTheme()
                            .obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout, 0, 0);
            try {
                resizeMode = a.getInt(R.styleable.AspectRatioFrameLayout_resize_mode, RESIZE_MODE_FIT);
            } finally {
                a.recycle();
            }
        }
        aspectRatioUpdateDispatcher = new AspectRatioUpdateDispatcher();
    }

    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    public void setAspectRatioListener(@Nullable AspectRatioListener listener) {
        this.aspectRatioListener = listener;
    }

    public @ResizeMode int getResizeMode() {
        return resizeMode;
    }

    public void setResizeMode(@ResizeMode int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspectRatio <= 0) {
            // Aspect ratio not set.
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float viewAspectRatio = (float) width / height;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance.
            aspectRatioUpdateDispatcher.scheduleUpdate(videoAspectRatio, viewAspectRatio, false);
            return;
        }

        switch (resizeMode) {
            case RESIZE_MODE_FIXED_WIDTH:
                height = (int) (width / videoAspectRatio);
                break;
            case RESIZE_MODE_FIXED_HEIGHT:
                width = (int) (height * videoAspectRatio);
                break;
            case RESIZE_MODE_ZOOM:
                if (aspectDeformation > 0) {
                    width = (int) (height * videoAspectRatio);
                } else {
                    height = (int) (width / videoAspectRatio);
                }
                break;
            case RESIZE_MODE_FIT:
                if (aspectDeformation > 0) {
                    height = (int) (width / videoAspectRatio);
                } else {
                    width = (int) (height * videoAspectRatio);
                }
                break;
            case RESIZE_MODE_FILL:
            default:
                // Ignore target aspect ratio
                break;
        }
        aspectRatioUpdateDispatcher.scheduleUpdate(videoAspectRatio, viewAspectRatio, true);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    private final class AspectRatioUpdateDispatcher implements Runnable {

        private float targetAspectRatio;
        private float naturalAspectRatio;
        private boolean aspectRatioMismatch;
        private boolean isScheduled;

        public void scheduleUpdate(
                float targetAspectRatio, float naturalAspectRatio, boolean aspectRatioMismatch) {
            this.targetAspectRatio = targetAspectRatio;
            this.naturalAspectRatio = naturalAspectRatio;
            this.aspectRatioMismatch = aspectRatioMismatch;

            if (!isScheduled) {
                isScheduled = true;
                post(this);
            }
        }

        @Override
        public void run() {
            isScheduled = false;
            if (aspectRatioListener == null) {
                return;
            }
            aspectRatioListener.onAspectRatioUpdated(
                    targetAspectRatio, naturalAspectRatio, aspectRatioMismatch);
        }
    }
}
