package com.putao.ptx.camera.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.putao.ptx.camera.R;

/**
 * Created by admin on 2016/8/4.
 */
public class MenuView extends View {
    private String mText = "";
    private int mTextColor = Color.WHITE;
    private int mColorShade = Color.GRAY;
    private int mTextSize = 32;
    private Paint mPaint;

    public void setTextSize(int textSize) {
        mTextSize = textSize;
        invalidate();
    }

    public int getTextSize() {
        return mTextSize;
    }

    public String getText() {
        return mText;
    }

    public MenuView(Context context) {
        super(context);
        init(mText, getResources().getDimensionPixelSize(R.dimen.text_size_45sp));
    }

    public MenuView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public MenuView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(mText, mTextSize);
    }

    public MenuView(Context context, String text, int textSize) {
        super(context);
        init(text, textSize);
    }

    private void init(String text, int textSize) {
        try {
            mText = text;
            mTextSize = textSize;
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        mPaint = new Paint();
        mPaint.setColor(mTextColor);
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        int width = getWidth();
        int height = getHeight();
        int cx = width / 2;
        int cy = height / 2;
        mPaint.setColor(mColorShade);
        mPaint.setTextSize(mTextSize);
        //mPaint.setAlpha((int) (getAlpha() * 255));
        int offset = (int) Math.max(mTextSize * 1.0f / 20, 1);
        canvas.drawText(mText, cx + offset, cy + offset + mTextSize / 2 * 0.8f, mPaint);
        mPaint.setColor(mTextColor);
        //mPaint.setAlpha((int) (getAlpha() * 255));
        canvas.drawText(mText, cx, cy + mTextSize / 2 * 0.8f, mPaint);
    }

    public void setTextColor(int textColor) {
        mTextColor = textColor;
        invalidate();
    }

    public void setText(String text) {
        mText = text;
        invalidate();
    }
}
