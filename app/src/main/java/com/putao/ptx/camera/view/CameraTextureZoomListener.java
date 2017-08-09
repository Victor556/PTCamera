package com.putao.ptx.camera.view;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Administrator on 2016/3/29.
 */
public class CameraTextureZoomListener implements View.OnTouchListener {
    private String TAG = CameraTextureZoomListener.class.getSimpleName();
    private Context context;
    private int mode = 0;
    private float oldDist;
    private int zoom = 0;

    public CameraTextureZoomListener(Context context, CameraTextureTouchCallBack touchCallBack) {
        this.context = context;
        this.touchCallBack = touchCallBack;
    }

    float downX, downY;
    private boolean mulitTouch = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = 1;
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (touchCallBack != null) {
                    touchCallBack.onActionUp();
                }
                if (mode == 1) {
                    if (downX - event.getX() > 300 && !mulitTouch) {
//                        if (touchCallBack != null)
//                            touchCallBack.onJumpPicView();
                    } else if (Math.abs(downX - event.getX()) < 60 && Math.abs(downY - event.getY()) < 60 && !mulitTouch) {
                        if (touchCallBack != null) {
                            touchCallBack.onFocus(event);
                        }
                    } else if (Math.abs(downY - event.getY()) > 100 && !mulitTouch && downY > 50) {
                        if (touchCallBack != null) {
                            touchCallBack.onSwitchMode(downY - event.getY() > 0 ? true : false);
                        }
                    }
                }
                Log.d(TAG, "onTouch:   downX:" + downX + "  downY:" + downY);
                mode = 0;
                mulitTouch = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode -= 1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mulitTouch = true;
                oldDist = spacing(event);
                mode += 1;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode >= 2) {
                    float newDist = spacing(event);
                    int threshould = 70/*1*/;
                    if (newDist > oldDist + threshould) {
                        if (touchCallBack != null) {
                            zoom = zoom + 1;
                            touchCallBack.onZoom(zoom);
                        }
                        oldDist = newDist;
                    }
                    if (newDist < oldDist - threshould) {
                        if (touchCallBack != null) {
                            zoom = zoom - 1;
                            touchCallBack.onZoom(zoom > 0 ? zoom : 0);
                        }
                        oldDist = newDist;
                    }
                }
                break;
        }
        return true;
    }


    private float spacing(MotionEvent event) {//出现过java.lang.IllegalArgumentException: pointerIndex out of range异常
        try {
            if (event != null && event.getPointerCount() > 1) {
                float x = event.getX(0) - event.getX(1);
                float y = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(x * x + y * y);
            } else {
                return 0f;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private CameraTextureTouchCallBack touchCallBack = null;

    public interface CameraTextureTouchCallBack {
        void onZoom(int value);

        void onFocus(MotionEvent event);

        void onSwitchMode(boolean isUp);

        void onJumpPicView();

        void onActionUp();
    }
}
