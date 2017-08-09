package com.putao.ptx.camera.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.putao.ptx.camera.R;

import java.lang.ref.SoftReference;


public class NonUiThread extends Thread {
    final SoftReference<Activity> mRefActivity;
    final Bitmap mBitmap;
    private final long mTmCreate;
    private int mTimeMil;
    private final OnCameraSwitchingListener mCameraSwitchingListener;
    private Runnable mEndAction;
    private boolean mbSwitchingFinished = false;

    public NonUiThread(Activity act, Bitmap btm, int timeMil,
                       OnCameraSwitchingListener cameraSwitchingListener, Runnable endAction) {
        super();
        mRefActivity = new SoftReference<Activity>(act);
        mBitmap = btm;
        mTimeMil = timeMil;
        mCameraSwitchingListener = cameraSwitchingListener;
        mEndAction = endAction;
        mTmCreate = SystemClock.elapsedRealtime();
    }

    private static final String TAG = "NonUiThread";

    @Override
    public void run() {
        Activity act = mRefActivity.get();
        if (act == null) {
            return;
        }
        Looper.prepare();
        mbSwitchingFinished = false;
        final View v = LayoutInflater.from(act).inflate(R.layout.camera_switch, null);
        final ImageView iv = (ImageView) v.findViewById(R.id.ivSwitch);
        final View rootView = v.findViewById(R.id.rootView);
        if (mBitmap != null) {
            iv.setImageBitmap(mBitmap);
        }
        final WindowManager windowManager = act.getWindowManager();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                /*500, 500, *//*100, 200, */WindowManager.LayoutParams.FIRST_SUB_WINDOW,
                WindowManager.LayoutParams.TYPE_TOAST, PixelFormat.OPAQUE);
        windowManager.addView(v, params);
        iv.setCameraDistance(-5000);
        int max = Math.max(mTimeMil, 500);
        //rootView.animate().alpha(0.5f).setDuration(max).start();
        iv.animate().rotationY(180).setDuration(max)
                .alpha(0.7f).setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    private Runnable mAction = new Runnable() {
                        int cnt = 0;
                        final int MAX_CNT = 20;

                        @Override
                        public void run() {
                            if (cnt > MAX_CNT || mCameraSwitchingListener == null
                                    || mCameraSwitchingListener.isSwitchingFinished()) {
                                onAnimationEnd();
                            } else {
                                cnt++;
                                Log.d(TAG, "onAnimationEnd: 正在翻转相机！"
                                        + "  cnt:" + cnt
                                        + "  waste time:" + (SystemClock.elapsedRealtime() - mTmCreate));
                                iv.postDelayed(this, 50);
                            }
                        }
                    };

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAction.run();
                    }

                    private void onAnimationEnd() {
                        v.setVisibility(View.GONE);
                        windowManager.removeView(v);
                        Handler handler = new Handler(Looper.getMainLooper());
                        if (mEndAction != null) {
                            handler.post(mEndAction);
                        }
                        final Looper looper = Looper.myLooper();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                looper.quitSafely();
                            }
                        }, 200);
                        mbSwitchingFinished = true;
                    }
                }).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rootView.setBackgroundColor(Color.BLACK);
                iv.setBackgroundColor(Color.BLACK);
                //Log.d(TAG, "onAnimationUpdate: 刷新背景！");//大约每17ms打印一次
            }
        }).start();
        Log.d(TAG, "run: 子线程设置UI！ 线程反应 waste time:" + (SystemClock.elapsedRealtime() - mTmCreate));
        Looper.loop();
    }

    public boolean isSwitchingFinished() {
        return mbSwitchingFinished;
    }

    public interface OnCameraSwitchingListener {
        boolean isSwitchingFinished();
    }
}