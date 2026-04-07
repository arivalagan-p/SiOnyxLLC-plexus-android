package com.dome.librarynightwave.utils;



import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;



public final class PlayerView extends FrameLayout {

    private final AspectRatioFrameLayout contentFrame;
    private final ZoomablePlayerView textureView;


    public PlayerView(Context context) {
        this(context, null);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Set FrameLayout gravity to center children
        setForegroundGravity(Gravity.CENTER);

        contentFrame = new AspectRatioFrameLayout(context);
        LayoutParams contentParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        contentFrame.setLayoutParams(contentParams);

        textureView = new ZoomablePlayerView(context);
        textureView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        contentFrame.addView(textureView);
        addView(contentFrame);


        textureView.setZoomGestureListener(new ZoomablePlayerView.ZoomGestureListener() {
            @Override
            public void onStartZoom() {
                // Switch to zoom mode for fullscreen experience
                contentFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            }
            @Override
            public void onResetZoom() {
                contentFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            }
        });
    }

    public void setResizeMode(int resizeMode) {
        contentFrame.setResizeMode(resizeMode);
    }

    public void setVideoSize(int width, int height) {
        if (width > 0 && height > 0) {
            float aspectRatio = (float) width / height;
            contentFrame.setAspectRatio(aspectRatio);
        }
    }
    public void attachLister(Touch context){
        if (textureView != null){
            textureView.setListener((Touch) context);
        }
    }
    public ZoomablePlayerView getTextureView() {
        return textureView;
    }

    public void reset(){
        if (textureView != null){
            textureView.reset();
        }
    }
}
