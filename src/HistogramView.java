package com.alsclone.astrostacker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class HistogramView extends View {
    private int[] data;
    private final Paint paint = new Paint();

    public HistogramView(Context context) {
        super(context);
        paint.setColor(0xAAFFFFFF);
    }

    public void setData(int[] data) {
        this.data = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;

        int w = getWidth();
        int h = getHeight();

        int max = 0;
        // Skip first few bins for normalization (background often dominates)
        for (int i = 1; i < 256; i++) {
            if (data[i] > max) max = data[i];
        }

        if (max == 0) return;

        float barWidth = (float) w / 256;
        for (int i = 0; i < 256; i++) {
            float val = (float) data[i] / max;
            float barHeight = val * h;
            canvas.drawRect(i * barWidth, h - barHeight, (i + 1) * barWidth, h, paint);
        }
    }
}
