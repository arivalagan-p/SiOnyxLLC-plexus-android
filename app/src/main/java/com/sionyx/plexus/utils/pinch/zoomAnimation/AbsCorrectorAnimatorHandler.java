package com.sionyx.plexus.utils.pinch.zoomAnimation;

import android.animation.ValueAnimator;

import com.sionyx.plexus.utils.pinch.zoomcustomviews.ImageMatrixCorrector;


public abstract class AbsCorrectorAnimatorHandler implements ValueAnimator.AnimatorUpdateListener {

    private final ImageMatrixCorrector corrector;
    private final float[] values;

    public AbsCorrectorAnimatorHandler(ImageMatrixCorrector corrector) {
        this.corrector = corrector;
        this.values = new float[9];
    }

    public ImageMatrixCorrector getCorrector() {
        return corrector;
    }

    protected float[] getValues() {
        return values;
    }
}
