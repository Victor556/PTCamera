package com.putao.ptx.camera.helper;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.OrientationEventListener;

import com.putao.ptx.camera.eventbus.ScreenOrienEvent;

import org.greenrobot.eventbus.EventBus;

public class ScreenOrienManager {
    private OrientationEventListener mOrEventListener;
    private Activity mActivity;
    private static ScreenOrienManager instance = new ScreenOrienManager();

    public static ScreenOrienManager getInstance() {
        return instance;
    }

    public void start(Activity activity) {
        this.mActivity = activity;
        if (mOrEventListener == null) {
            initListener();
        }
        mOrEventListener.enable();
    }

    public void stop() {
        if (mOrEventListener != null) {
            mOrEventListener.disable();
        }

    }


    private void initListener() {
        mOrEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int rotation) {
                int orientation = -1;
                if (((rotation >= 0) && (rotation <= 40)) || (rotation > 310)) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else if ((rotation > 40) && (rotation <= 130)) {
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else if ((rotation > 130) && (rotation <= 220)) {
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else if ((rotation > 220) && (rotation <= 310)) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                }
                ScreenOrienEvent event = new ScreenOrienEvent();
                event.orientation=orientation;
                EventBus.getDefault().post(event);
            }
        };
    }
}
