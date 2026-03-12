package com.alsclone.astrostacker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class HistogramView extends View {
    private int[] data;
    private float bp, mp, wp;
    private final Paint paint = new Paint();

    public HistogramView(Context context) {
        super(context);
        paint.setColor(0xAAFFFFFF);
    }

    public void setData(int[] data) {
        this.data = data;
        invalidate();
    }

    public void setMarkers(float bp, float mp, float wp) {
        this.bp = bp;
        this.mp = mp;
        this.wp = wp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;

        int w = getWidth();
        int h = getHeight();

        // Use logarithmic scale for Y axis to see faint background signal
        double maxLog = 0;
        for (int i = 1; i < 256; i++) {
            if (data[i] > 0) {
                double val = Math.log10(data[i]);
                if (val > maxLog) maxLog = val;
            }
        }

        if (maxLog == 0) return;

        float barWidth = (float) w / 256;
        paint.setStrokeWidth(barWidth);

        // Draw grid
        paint.setColor(0x33FFFFFF);
        for(int i=0; i<=4; i++) {
            float x = i * w / 4.0f;
            canvas.drawLine(x, 0, x, h, paint);
        }

        paint.setColor(0xAA00FF00);
        for (int i = 0; i < 256; i++) {
            if (data[i] > 0) {
                float val = (float) (Math.log10(data[i]) / maxLog);
                float barHeight = val * h;
                canvas.drawRect(i * barWidth, h - barHeight, (i + 1) * barWidth, h, paint);
            }
        }

        // Draw X-axis markers
        paint.setColor(0xFFAAAAAA);
        paint.setTextSize(20);
        canvas.drawText("0", 5, h - 5, paint);
        canvas.drawText("128", w/2.0f - 15, h - 5, paint);
        canvas.drawText("255", w - 35, h - 5, paint);

        // Draw markers
        paint.setStrokeWidth(2);
        paint.setColor(0xFFFF0000); // Black point
        canvas.drawLine(bp * w, 0, bp * w, h, paint);

        paint.setColor(0xFFFFFF00); // Mid point
        canvas.drawLine(mp * w, 0, mp * w, h, paint);

        paint.setColor(0xFFFFFFFF); // White point
        canvas.drawLine(wp * w, 0, wp * w, h, paint);
    }
}
