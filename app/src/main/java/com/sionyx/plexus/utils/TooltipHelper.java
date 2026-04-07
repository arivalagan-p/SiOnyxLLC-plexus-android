package com.sionyx.plexus.utils;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import com.sionyx.plexus.BuildConfig;
import com.sionyx.plexus.R;

public class TooltipHelper {

    private BlockingOverlayView dimOverlay;
    private final Context context;
    private static PopupWindow popupWindow;

    public TooltipHelper(Context context) {
        this.context = context;
    }

    public void showToolTip(View anchorView) {
        Activity activity = (Activity) anchorView.getContext();

        // highlight selection
        anchorView.setBackgroundResource(R.drawable.shape_rectangle_background);

        // use the decorView (whole window)
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        decorView.post(() -> {
            // 1) Get anchor rect in window coordinates
            Rect anchorRect = new Rect();
            anchorView.getGlobalVisibleRect(anchorRect);

            // 2) Add dim overlay with cutout at anchor rect
            dimOverlay = new BlockingOverlayView(activity, anchorRect);
            decorView.addView(dimOverlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // 3) Inflate tooltip layout
            View tooltipView = LayoutInflater.from(activity)
                    .inflate(R.layout.tooltip_layout, decorView, false);

            // 4) Configure popup

            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss(); // clean any old one
            }
            popupWindow = new PopupWindow(activity);
            int popupWidth  = dp(activity, 230);
            int popupHeight = dp(activity, 150);

            popupWindow.setWidth(popupWidth);
            popupWindow.setHeight(popupHeight);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(false);
            popupWindow.setFocusable(false);
            popupWindow.setContentView(tooltipView);
            popupWindow.setClippingEnabled(true);

            // 5) Position: below if possible, else above
            int margin = dp(activity, 8);
            int x = anchorRect.centerX() - (popupWidth / 2);

            int yBelow = anchorRect.bottom + margin;
            int yAbove = anchorRect.top - popupHeight - margin;

            int screenHeight = decorView.getHeight();

            int y;
            if (yBelow + popupHeight <= screenHeight) {
                y = yBelow;
            } else {
                y = yAbove;
            }

            if (activity.isFinishing() || activity.isDestroyed()) {
                return; // Don't show tooltip
            }

            popupWindow.showAtLocation(decorView, Gravity.START | Gravity.TOP, x, y);

            // 6) Dismiss handling
            popupWindow.setOnDismissListener(() -> {
                anchorView.setBackgroundResource(0);
                removeDimOverlay(decorView);
            });

            tooltipView.findViewById(R.id.tooltip_ok).setOnClickListener(v -> dismissTooltip());
        });
    }



    public void dismissTooltip() {
        updateTooltipPromptIsShown();
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    private void removeDimOverlay(ViewGroup contentRoot) {
        if (dimOverlay != null) {
            contentRoot.removeView(dimOverlay);
            dimOverlay = null;
        }
    }

    private void updateTooltipPromptIsShown() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("ToolTipPromptState", MODE_PRIVATE);
            SharedPreferences.Editor myEdit = prefs.edit();
            myEdit.putBoolean("isToolTipShown", true);
            myEdit.putString("BuildVersion", BuildConfig.VERSION_NAME);
            myEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    // ---- Overlay that blocks background and cuts out the anchor rect ----
    @SuppressLint("ViewConstructor")
    private static class BlockingOverlayView extends View {
        private final Rect highlightRect;
        private final Paint dimPaint;
        private final Path path = new Path();
        private final float radius;

        public BlockingOverlayView(Context context, Rect highlightRect) {
            super(context);
            this.highlightRect = new Rect(highlightRect);

            dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dimPaint.setStyle(Paint.Style.FILL);
            dimPaint.setColor(Color.parseColor("#80000000")); // 50% black

            radius = dp(context, 20);

            // Ensure we intercept touches
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);

            path.reset();
            path.setFillType(Path.FillType.EVEN_ODD); // important for the "hole"

            // Full-screen dim
            path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);

            // Cutout around the anchor rect
            path.addRoundRect(new RectF(highlightRect), radius, radius, Path.Direction.CW);

            canvas.drawPath(path, dimPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // Block all touches to the background
            return true;
        }

        private static float dp(Context c, int dp) {
            return dp * c.getResources().getDisplayMetrics().density;
        }
    }
    public void dismissIfShowing() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
}
