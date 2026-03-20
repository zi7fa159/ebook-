package com.astroapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PrecisionWheelView extends View {
    public interface OnValueChangeListener {
        void onValueChanged(double value);
    }

    private OnValueChangeListener listener;
    private double value;
    private double min, max;
    private double sensitivity; // value change per full rotation (2PI)
    private float lastX, lastY;
    private Paint paint;
    private String label = "";

    public PrecisionWheelView(Context context) {
        super(context);
        init();
    }

    public PrecisionWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    public void setRange(double min, double max, double sensitivity) {
        this.min = min;
        this.max = max;
        this.sensitivity = sensitivity;
    }

    public void setValue(double value) {
        this.value = Math.max(min, Math.min(max, value));
        invalidate();
    }

    public double getValue() {
        return value;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                double angle1 = Math.atan2(lastY - centerY, lastX - centerX);
                double angle2 = Math.atan2(y - centerY, x - centerX);
                double dAngle = angle2 - angle1;

                // Normalize dAngle
                if (dAngle > Math.PI) dAngle -= 2 * Math.PI;
                if (dAngle < -Math.PI) dAngle += 2 * Math.PI;

                // Precision: dAngle of 2PI means change of 'sensitivity'
                double deltaValue = (dAngle / (2 * Math.PI)) * sensitivity;

                // Non-linear scaling for Shutter Speed (SS)
                // Sensitivity decreases as value increases to provide finer control at long exposures
                if (label.equals("SS")) {
                    if (value > 5.0) {
                        deltaValue *= 0.5; // Half sensitivity (slower) for > 5s
                    }
                    if (value > 15.0) {
                        deltaValue *= 0.5; // Quarter sensitivity for > 15s
                    }
                }

                value = Math.max(min, Math.min(max, value + deltaValue));

                if (listener != null) {
                    listener.onValueChanged(value);
                }

                lastX = x;
                lastY = y;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - 20;

        // Draw outer ring
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        canvas.drawCircle(cx, cy, radius, paint);

        // Draw ticks
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(2);
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6);
            float inner = (i % 5 == 0) ? radius - 15 : radius - 8;
            float x1 = (float) (cx + inner * Math.cos(angle));
            float y1 = (float) (cy + inner * Math.sin(angle));
            float x2 = (float) (cx + radius * Math.cos(angle));
            float y2 = (float) (cy + radius * Math.sin(angle));
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        // Current position indicator
        double currentAngle = (value / sensitivity) * 2 * Math.PI;
        float ix = (float) (cx + (radius - 25) * Math.cos(currentAngle));
        float iy = (float) (cy + (radius - 25) * Math.sin(currentAngle));
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(ix, iy, 8, paint);

        // Center label
        paint.setColor(Color.WHITE);
        canvas.drawText(label, cx, cy + 10, paint);
    }
}
