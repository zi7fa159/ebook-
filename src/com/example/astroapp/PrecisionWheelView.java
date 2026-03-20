package com.example.astroapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class PrecisionWheelView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GestureDetector gestureDetector;
    private Scroller scroller;
    private float scrollOffset = 0;
    private float minVal, maxVal;
    private float sensitivity = 1.0f;
    private OnValueChangeListener listener;
    private boolean isLogarithmic = false;

    public interface OnValueChangeListener {
        void onValueChanged(float value);
    }

    public PrecisionWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new Scroller(context);
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                scrollOffset += distanceX * sensitivity;
                invalidate();
                notifyListener();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                scroller.fling((int)scrollOffset, 0, (int)-velocityX, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
                invalidate();
                return true;
            }
        });
    }

    public void setRange(float min, float max, float sensitivity, boolean logarithmic) {
        this.minVal = min;
        this.maxVal = max;
        this.sensitivity = sensitivity;
        this.isLogarithmic = logarithmic;
        // Adjust scrollOffset to current value if needed, but let's assume default start at min
        invalidate();
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public float getValue() {
        // High resolution: ~3 full turns to change unit?
        // Let's define "1 unit" change as 3000 pixels (3 full turns on a 1000px screen)
        float range = maxVal - minVal;
        float val;

        if (isLogarithmic) {
            // ISO: sensitivity affects how fast we move through octaves
            // Let's say 5000 pixels for full range
            val = (float) (minVal * Math.pow(maxVal / minVal, clamp(scrollOffset / 5000.0f, 0, 1)));
        } else {
            // Shutter: ~3 turns per second? If max is 30s, that's 90 turns?
            // Let's use a scale where 1 second = 3000 pixels.
            float pixelsPerSecond = 3000.0f;
            val = minVal + (scrollOffset / pixelsPerSecond);
            val = Math.max(minVal, Math.min(maxVal, val));
        }
        return val;
    }

    private float clamp(double val, float min, float max) {
        return (float) Math.max(min, Math.min(max, val));
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onValueChanged(getValue());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollOffset = scroller.getCurrX();
            invalidate();
            notifyListener();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        paint.setColor(Color.DKGRAY);
        canvas.drawRect(0, 0, w, h, paint);

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);

        float step = 50f;
        float startX = -(scrollOffset % step);
        for (float x = startX; x < w; x += step) {
            float totalScrollX = x + scrollOffset;
            float height = ( (int)(totalScrollX/step) % 5 == 0 ) ? h/2 : h/4;
            canvas.drawLine(x, h, x, h - height, paint);
        }

        paint.setColor(Color.RED);
        paint.setStrokeWidth(4);
        canvas.drawLine(w / 2, 0, w / 2, h, paint);

        paint.setColor(Color.YELLOW);
        paint.setTextSize(35);
        canvas.drawText(String.format("%.4f", getValue()), 20, 40, paint);
    }

    public void setValue(float value) {
        if (isLogarithmic) {
             scrollOffset = (float) (5000.0f * Math.log(value / minVal) / Math.log(maxVal / minVal));
        } else {
             float pixelsPerSecond = 3000.0f;
             scrollOffset = (value - minVal) * pixelsPerSecond;
        }
        invalidate();
    }
}
