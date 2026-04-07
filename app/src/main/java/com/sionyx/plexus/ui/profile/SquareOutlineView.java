package com.sionyx.plexus.ui.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SquareOutlineView extends View {
    public Paint outlinePaint;
    private int outlineColor = Color.RED; // Default outline color is red

    public SquareOutlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public void setOutlineColor(int color) {
        outlineColor = color;
        invalidate(); // Request a redraw
    }

    private void init() {
        outlinePaint = new Paint();
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(Color.RED); // Outline color
        outlinePaint.setStrokeWidth(10); // Outline width in pixels
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        outlinePaint.setColor(outlineColor);
        // Draw the outline rectangle around the view
        canvas.drawRect(0, 0, getWidth(), getHeight(), outlinePaint);
    }
}
