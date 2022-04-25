package com.lee.android.demo.metronome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.lee.metronome.type.BeatType;
import com.lee.metronome.type.DotType;

public class BeatHot extends View {
    /**
     * 画笔
     */
    private Paint paint;

    /**
     * 总数
     */
    private int count = 1;

    /**
     * 当前亮的点
     */
    private int current = 0;

    /**
     * 圆点直径
     */
    private final int size = 48;
    private final int sizeMid = 42;
    private final int sizeSmall = 36;

    private DotType[] dotType;

    /**
     * 圆点间距
     */
    private final int space = 16;

    public BeatHot(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BeatHot(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
    }


    public void setCount(int count) {
        this.count = count;
        postInvalidate();
    }

    public void setCurrent(int current) {
        if (this.current == current)
            return;

        this.current = current;
        postInvalidate();
    }

    public int getCurrent() {
        return current;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        for (int i = 0; i < count; i++) {
            int drawSize = size;
            if (dotType[i] == DotType.BIG) {
                drawSize = sizeMid;
            } else if (dotType[i] == DotType.SMALL) {
                drawSize = sizeSmall;
            }

            if (i == current) {
                paint.setColor(Color.WHITE);
            } else {
                paint.setColor(Color.parseColor("#66FFFFFF"));
            }

            canvas.drawCircle((size >> 1) + (i * (size + space)), 64, drawSize >> 1, paint);

        }

    }

    public void setBeat(DotType[] metronomeBeatType) {
        this.dotType = metronomeBeatType;
        setCount(metronomeBeatType.length);
    }
}
