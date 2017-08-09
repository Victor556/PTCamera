package com.putao.ptx.camera.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.putao.ptx.camera.R;
import com.putao.ptx.camera.helper.CameraEngine;
import com.putao.ptx.camera.util.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2016/3/17.
 */
public class CameraView extends FrameLayout implements Camera.AutoFocusCallback {
    public static final int TIME_DELAY_CLEAR = 500;
    public static final int DEF_WIDTH = 255;//原生相机中设置为宽255，高400
    public static final int DEF_HEIGHT = 400;
    private static final long TIME_DELAY_FAILED_UPDATE = 1000;//聚焦失败的话1000之后再显示失败状态
    private Context mContext;
    private CameraSurfaceView surfaceView;
    private ImageView focusView;
    private CameraDrawView sdvDrawView;
    private ImageView ivVoicePic;
    private boolean focusing = false;
    private boolean mbEnableFocus = true;
    private ViewGroup mRootView;
    private long mTmLastAutoFocus;
    private Runnable mRunFocus;
    private Runnable mRunClearFocus;
    private ValueAnimator mObjAnim;

    @Override
    public void clearAnimation() {
        super.clearAnimation();
        mObjAnim.end();
    }

    public boolean isFocusing() {
        return focusing;
    }

    public void clearFocus() {
        focusing = false;
//        focusView.setBackgroundDrawable(null);
        focusView.setImageResource(0);
        focusView.setVisibility(View.GONE);
        mRootView.invalidate();
        clearAnimation();

    }

    public Runnable getRunClearFocus() {
        if (mRunClearFocus == null) {
            mRunClearFocus = new Runnable() {
                public void run() {
                    //focusView.setBackgroundDrawable(0);
                    focusView.setImageResource(0);
                    clearFocus();
                }
            };
        }
        return mRunClearFocus;
    }

    private enum MODE {
        NONE, FOCUSING, FOCUSED, FOCUSFAIL

    }

    private MODE mode = MODE.NONE;// 默认模式

    public CameraView(Context context) {
        super(context);
        this.mContext = context;
        initView();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView();
    }


    private void initView() {
        //setWillNotDraw(false);
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.view_camer, null);
        surfaceView = (CameraSurfaceView) rootView.findViewById(R.id.surfaceView);
        //focusView = (ImageView) rootView.findViewById(R.id.focusView);
        ivVoicePic = (ImageView) rootView.findViewById(R.id.ivVoicePic);
        sdvDrawView = (CameraDrawView) rootView.findViewById(R.id.sdvDrawView);
        LayoutParams LayoutParams = new LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        this.addView(rootView, LayoutParams);

        mObjAnim = ValueAnimator.ofFloat(1.4f, 1.0f);
        mObjAnim.setDuration(2500);
        mObjAnim.removeAllUpdateListeners();
        mObjAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float f = (Float) animation.getAnimatedValue();
                if (focusView != null) {
                    focusView.setScaleX(f);
                    focusView.setScaleY(f);
                    float frac = 1.0f - animation.getCurrentPlayTime() * 1.0f / mObjAnim.getDuration();
                    if (frac < 0.3f) {
                        focusView.setImageAlpha((int) (255 * frac * 2));
                        focusView.setAlpha(2 * frac);
                        //Log.d(TAG, "onAnimationUpdate: frac:" + frac);
                    }
                }
            }
        });
        mObjAnim.setInterpolator(new AccelerateDecelerateInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float scale = 7.0f;
                return (float) (Math.cos(Math.min((input * scale + 1) * Math.PI, 2 * Math.PI)) / 2.0f) + 0.5f;
            }
        });
        mObjAnim.removeAllListeners();
        mObjAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (focusView != null) {
                    focusView.setVisibility(View.VISIBLE);
                    focusView.setImageAlpha(255);
                    focusView.setAlpha(1.0f);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (focusView != null) {
                    focusView.setVisibility(View.GONE);
                    focusView.setImageAlpha(255);
                }
            }
        });
    }


    private void setFocusViewPosition(final float centerX, final float centerY) {
        final int width = focusView.getWidth();
        final int height = focusView.getHeight();
        int l = (int) (centerX - width / 2);
        int t = (int) (centerY - height / 2);
        int r = (int) (centerX + width / 2);
        int b = (int) (centerY + height / 2);
//        focusView.layout(l, t, r, b);
        Log.d(TAG, "setFocusViewPosition: l,t,r,b:" + l + "," + t + "," + r + "," + b);
        focusView.setX(centerX - (width / 2));
        focusView.setY(centerY - (height / 2));
//        focusView.setScaleX(1.5f);
//        focusView.setScaleY(1.5f);
//        focusView.animate().scaleX(1.5f).scaleY(1.5f)
//                .setDuration(0).setListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                super.onAnimationEnd(animation);
//                if (CameraEngine.isCameraIdBack()) {
//                    focusView.setVisibility(View.VISIBLE);
//                    focusView.getParent().requestLayout();
//                    focusView.requestLayout();
//                }
//            }
//        }).start();
        focusView.requestLayout();
//        focusView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(350)/*.setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                if (CameraEngine.isCameraIdBack()) {
//                    focusView.setVisibility(View.VISIBLE);
//                    focusView.requestLayout();
//                }
//            }
//        })*/.setInterpolator(new AccelerateDecelerateInterpolator()).start();
        mRootView.invalidate();
        focusView.requestLayout();
    }

    public void setFocusEnable(boolean bEnableFocus) {
        mbEnableFocus = bEnableFocus;
        if (!mbEnableFocus) {
            clearFocus();
        }
    }

    /**
     * 设置手动聚焦和测光区域
     *
     * @param event
     */
    public synchronized void focusOnTouch(Camera camera, MotionEvent event) {
        if (event == null) {
            return;
        }
        focusOnTouch(camera, event.getRawX(), event.getRawY());
    }

    /**
     * 设置识别人脸自动聚焦和测光区域
     *
     * @param centerX
     * @param centerY
     */
    public synchronized void focusOnTouch(final Camera camera, final float centerX, final float centerY) {
//        long bTimeEnough = SystemClock.elapsedRealtime() - mTmLastAutoFocus;
//        if (focusing || bTimeEnough < 1000) {
//            mRunFocus = new Runnable() {
//                @Override
//                public void run() {
//                    autoFocus(camera, centerX, centerY);
//                }
//            };
//            if (bTimeEnough > 5000) {
//                if (mRunFocus != null) {
//                    //mRunFocus.run();
//                }
//            }
//            Log.d(TAG, "focusOnTouch: 正在聚焦！");
//        } else {
//            autoFocus(camera, centerX, centerY);
//        }
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        try {
            autoFocus(camera, centerX, centerY);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "focusOnTouch: " + e.toString());
        }
    }

    private synchronized void autoFocus(Camera camera, float centerX, float centerY) {
        if (camera == null) return;
        Log.d(TAG, "autoFocus: 开始聚焦！");

        Log.d(TAG, "autoFocus: 开始聚焦！ centerX：" + centerX + "  centerY:" + centerY
                + " tx:" + focusView.getX() + "  ty:" + focusView.getY());
//        if (focusing) return;
        focusing = true;
        mTmLastAutoFocus = SystemClock.elapsedRealtime();
        mode = MODE.FOCUSING;
        //focusView.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_focus_focusing));
        //focusView.setBackgroundResource(R.drawable.ic_focus_focusing);
        focusView.setImageResource(R.drawable.ic_focus_focusing);
        if (mbEnableFocus) {
            clearAnimation();
            if (isCameraIdBack() && isFocusViewAllowDisplay()) {
                setFocusViewPosition(centerX, centerY);
                mObjAnim.start();
                delayClearFocus(TIME_DELAY_CLEAR * 10);
            }
        }

        int[] location = new int[2];
        this.getLocationOnScreen(location);
        int width = DEF_WIDTH;//原生相机中设置为宽255，高400
        int height = DEF_HEIGHT;
//        Rect focusRect = calculateTapArea(focusView.getWidth(), focusView.getHeight(), 1f, centerX, centerY,
//                location[0], location[0] + this.getWidth(), location[1], location[1] + this.getHeight());
        Rect focusRect = calculateTapArea(/*10*/10, /*10*/10, 1f, centerX, centerY,
                location[0], location[0] + this.getWidth(), location[1], location[1] + this.getHeight());
        int hw = focusView.getWidth() / 2;
        int hh = focusView.getHeight() / 2;
        focusRect = toCameraRect(new Rect((int) centerX - hw,
                (int) centerY - hh, (int) centerX + hw, (int) centerY + hh));
        int cx = focusRect.centerX();
        int cy = focusRect.centerY();
        focusRect.set(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2);
        rotateCameraRectN90(focusRect, CameraEngine.getCameraRotation(), CameraEngine.isCameraIdBack());
        checkCameraRectEdge(focusRect);
//        Rect meteringRect = calculateTapArea(focusView.getWidth(), focusView.getHeight(), 1.5f, centerX, centerY,
//                location[0], location[0] + this.getWidth(), location[1], location[1] + this.getHeight());
        Rect meteringRect = calculateTapArea(20, 20, 1.5f, centerX, centerY,
                location[0], location[0] + this.getWidth(), location[1], location[1] + this.getHeight());
        meteringRect = toCameraRect(new Rect((int) centerX - hw,
                (int) centerY - hh, (int) centerX + hw, (int) centerY + hh));
        int cx2 = meteringRect.centerX();
        int cy2 = meteringRect.centerY();
        meteringRect.set(cx2 - width / 2, cy2 - height / 2, cx2 + width / 2, cy2 + height / 2);
        rotateCameraRectN90(meteringRect, CameraEngine.getCameraRotation(), CameraEngine.isCameraIdBack());
        checkCameraRectEdge(meteringRect);
        setFocusMeter(focusRect, meteringRect);
        removeCallbacks(mRunResetFocus);
        postDelayed(getRunResetFocus(), TIME_DELAY_RESET_FOCUS_METER);
        //test();
    }

    private void test() {
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), 0, true);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), 90, true);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), 270, true);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), -90, true);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), -270, true);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), 0, false);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), 90, false);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), 270, false);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), -90, false);
        rotateCameraRectN90(new Rect(-1000, -1000, -999, 1000), -270, false);
    }

    private boolean isFocusViewAllowDisplay() {
        return mFocusViewDisplayImpl != null && mFocusViewDisplayImpl.isAllowShow();
    }

    private void setFocusMeterCenter(boolean focus, boolean meter) {
        Rect focusRect = null;
        Rect meteringRect = null;
        int left = -DEF_WIDTH / 2;
        int top = -DEF_HEIGHT / 2;
        if (focus) {
            focusRect = new Rect(left, top, left + DEF_WIDTH, top + DEF_HEIGHT);
        }
        if (meter) {
            meteringRect = new Rect(left, top, left + DEF_WIDTH, top + DEF_HEIGHT);
        }
        setFocusMeter(focusRect, meteringRect);
    }

    private void setFocusMeter(Rect focusRect, Rect meteringRect) {
        Camera camera = CameraEngine.getCamera();
        if (camera == null) {
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null) {
            return;
        }
        if (!mbEnableFocus) {
            focusRect = null;
        }
        if (focusRect == null && meteringRect == null) {
            return;
        }
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);//更改对焦模式

        printFile(parameters);
        boolean bShouldUpdate = false;

        int maxNumFocusAreas = 0;
        List<Camera.Area> focusAreasSetted = null;
        if (focusRect != null) {
            maxNumFocusAreas = parameters.getMaxNumFocusAreas();
            focusAreasSetted = null;
            try {
                focusAreasSetted = parameters.getFocusAreas();
                if (focusAreasSetted == null) {
                    focusAreasSetted = new ArrayList<>();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (maxNumFocusAreas > 0) {
                List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                focusAreas.add(new Camera.Area(focusRect, 1));
                parameters.setFocusAreas(focusAreas);
                bShouldUpdate = true;
            }
        }

        int maxNumMeteringAreas = 0;
        List<Camera.Area> meteringAreasSetted = null;
        if (meteringRect != null) {
            maxNumMeteringAreas = parameters.getMaxNumMeteringAreas();
            meteringAreasSetted = null;
            try {
                meteringAreasSetted = parameters.getMeteringAreas();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (maxNumMeteringAreas > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(meteringRect, 1));
                parameters.setMeteringAreas(meteringAreas);
                bShouldUpdate = true;
            }
        }
        int focusAreasSettedSize = focusAreasSetted == null ? 0 : focusAreasSetted.size();
        int meteringAreasSettedSize = meteringAreasSetted == null ? 0 : meteringAreasSetted.size();

        String strFocus = focusRect == null ? "" : "  focusRect: width:"
                + focusRect.width() + "  height:" + focusRect.height()
                + "  " + focusRect.left + "  " + focusRect.top
                + "  " + focusRect.right + "  " + focusRect.bottom;
        String strMeter = meteringRect == null ? "" : "  meteringRect: width:"
                + meteringRect.width() + "  height:" + meteringRect.height()
                + "  " + meteringRect.left + "  " + meteringRect.top
                + "  " + meteringRect.right + "  " + meteringRect.bottom;
        Log.d(TAG, "autoFocus setFocusMeter: maxNumFocusAreas:" + maxNumFocusAreas +
                "  focusAreasSettedSize:" + focusAreasSettedSize
                + "  maxNumMeteringAreas:" + maxNumMeteringAreas
                + "  meteringAreasSettedSize" + meteringAreasSettedSize
                + "  mbEnableFocus:" + mbEnableFocus
                + strFocus + strMeter);
        try {
            //if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            //    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //}
            if (bShouldUpdate) {
                camera.setParameters(parameters);
                CameraEngine.printCameraInfo(parameters, "setFocusMeter");
                camera.cancelAutoFocus();
                camera.autoFocus(this);//TODO
            }
        } catch (Exception e) {
            Log.e(TAG, "autoFocus: " + e.toString() + "  设置参数异常！ parameters:" + parameters.flatten().replace(';', '\n'));
            e.printStackTrace();
            focusView.setBackgroundDrawable(null);
            focusView.setImageResource(0);
            focusing = false;

            if (mRunFocus != null) {
                mRunFocus.run();
            }
        }
    }

    private void delayClearFocus(int timeDelayClear) {
        Runnable runClearFocus = getRunClearFocus();
        removeCallbacks(runClearFocus);
        postDelayed(runClearFocus, timeDelayClear);
    }

    private static final String TAG = "CameraView";

    /**
     * 计算焦点及测光区域
     *
     * @param focusWidth
     * @param focusHeight
     * @param areaMultiple
     * @param x
     * @param y
     * @param previewLeft
     * @param previewRight
     * @param previewTop
     * @param previewBottom
     * @return Rect(left, top, right, bottom) :  left、top、right、bottom是以显示区域中心为原点的坐标
     */
    public Rect calculateTapArea(int focusWidth, int focusHeight, float areaMultiple,
                                 float x, float y, int previewLeft, int previewRight, int previewTop, int previewBottom) {
        if (previewRight == 0) {
            previewRight = 2048;
            previewBottom = 1536;
        }
        if (focusWidth == 0) {
            focusWidth = 80;
            focusHeight = 80;
        }
        int areaWidth = (int) (focusWidth * areaMultiple);
        int areaHeight = (int) (focusHeight * areaMultiple);
        int centerX = (previewLeft + previewRight) / 2;
        int centerY = (previewTop + previewBottom) / 2;
        double unitx = ((double) previewRight - (double) previewLeft) / 2000;
        double unity = ((double) previewBottom - (double) previewTop) / 2000;
        int left = clamp((int) (((x - areaWidth / 2) - centerX) / unitx), -1000, 1000);
        int top = clamp((int) (((y - areaHeight / 2) - centerY) / unity), -1000, 1000);
        int right = clamp((int) (left + areaWidth / unitx), -1000, 1000);
        int bottom = clamp((int) (top + areaHeight / unity), -1000, 1000);
        Log.d(TAG, "calculateTapArea: left:" + left + "  top:" + top + "  right:" + right + "  bottom:" + bottom);
        return new Rect(left, top, right, bottom);
    }

    public int clamp(int x, int min, int max) {
        if (x > max)
            return max;
        if (x < min)
            return min;
        return x;
    }


    /**
     * 可能会出现失败后0.7s又成功的回调
     *
     * @param success
     * @param camera
     */
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "onAutoFocus: 已完成聚焦！" + success);
        focusing = false;
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (elapsedRealtime - mTmLastAutoFocus < TIME_DELAY_FAILED_UPDATE) {
            removeCallbacks(mRunAutoFocusFailed);
        }
        try {
            if (success) {
                onAutoFocusCallback(success, camera);
            } else {
                postDelayed(getRunAutoFocusFailed(), TIME_DELAY_FAILED_UPDATE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            focusView.setVisibility(View.GONE);
            Log.e(TAG, "onAutoFocus: " + e);
        }

        mTmLastAutoFocus = elapsedRealtime;
    }

    private void onAutoFocusCallback(boolean success, Camera camera) {
        if (camera == null) {
            return;
        }
        Camera.Parameters para = camera.getParameters();
        List<Camera.Area> focusAreas = para.getFocusAreas();
        List<Camera.Area> meterAreas = para.getMeteringAreas();
        if (focusAreas == null && meterAreas != null) {
            resetFocusMeterPara(false, true);
        }
        if (isCameraIdBack() && focusView.getVisibility() != View.VISIBLE) {
            if (para == null) {
                return;
            }

            if (focusAreas == null || focusAreas.size() < 1) {
                focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(getDefCenterFocusRect(), 1));
                //return;
            }
            Rect cameraRect = focusAreas.get(0).rect;
            rotateCameraRectN90(cameraRect, -CameraEngine.getCameraRotation(), CameraEngine.isCameraIdBack());
            Rect screenRect = toScreenRect(cameraRect);
            if (meterAreas != null && meterAreas.size() > 0) {
                Rect r = meterAreas.get(0).rect;
                Log.d(TAG, "onAutoFocus:" +
                        " meterAreas: centerX:" + r.centerX()
                        + " centerY:" + r.centerY()
                        + " focusAreas: centerX:" + cameraRect.centerX()
                        + " centerY:" + cameraRect.centerY());
            }

            if (isFocusViewAllowDisplay()) {
                //处于中心位置才显示
                if (Math.abs(cameraRect.centerX()) < 100 && Math.abs(cameraRect.centerY()) < 100) {
                    setFocusViewPosition(screenRect.centerX(), screenRect.centerY());
                    mObjAnim.start();
                }
            }
        }

        if (success) {
            mode = MODE.FOCUSED;
            //focusView.setBackgroundResource(R.drawable.ic_focus_focused);
            focusView.setImageResource(R.drawable.ic_focus_focused);

            if (focusCompleteCallBack != null) {
                focusCompleteCallBack.onFocus(true);
            }
        } else {
            mode = MODE.FOCUSFAIL;
            //focusView.setBackgroundResource(R.drawable.ic_focus_failed);
            focusView.setImageResource(R.drawable.ic_focus_failed);

            if (focusCompleteCallBack != null) {
                focusCompleteCallBack.onFocus(false);
            }
        }
        focusView.clearAnimation();
        if (mRunFocus != null) {
            mRunFocus.run();
            mRunFocus = null;
        }
        //focusView.setBackgroundDrawable(0);
        //delayClearFocus(TIME_DELAY_CLEAR);
    }

    private Rect getDefCenterFocusRect() {
        return new Rect(-DEF_WIDTH / 2, -DEF_HEIGHT / 2, DEF_WIDTH / 2, DEF_HEIGHT / 2);
    }

    private boolean isCameraIdBack() {
        return CameraEngine.isCameraIdBack();
    }


    public View getFocusView() {
        return focusView;
    }

//    public GLSurfaceView getGlSurfaceView() {
//        return glSurfaceView;
//    }

    public CameraDrawView getSdvDrawView() {
        return sdvDrawView;
    }

    public ImageView getIvVoicePic() {
        return ivVoicePic;
    }

    public CameraSurfaceView getSurfaceView() {
        return surfaceView;
    }

    public interface FocusCompleteCallBack {
        void onFocus(boolean success);
    }

    FocusCompleteCallBack focusCompleteCallBack = null;

    //聚焦完成后的回调
    public void setFocusCompleteCallBack(FocusCompleteCallBack focusCompleteCallBack) {
        this.focusCompleteCallBack = focusCompleteCallBack;
    }

    public void setFocusView(ImageView focusView) {
        this.focusView = focusView;
    }

    public void setRootView(ViewGroup rootView) {
        mRootView = rootView;
    }


    private boolean mbPrintFile = false;

    private void printFile(Camera.Parameters parameters) {
        if (!mbPrintFile) {
            String str = parameters.flatten();
            str = str.replace(";", "\n");
            FileUtils.saveTextValue("/sdcard/parameters.txt", str, false);
            mbPrintFile = true;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEventFocusMeasure(CameraEngine.EventFocusMeasure event) {
        if (event == null) {
            return;
        }
        switch (event) {
            case FOCUS_ONLY:
                setFocusMeterCenter(true, false);
                break;
            case MEASURE_ONLY:
                setFocusMeterCenter(false, true);
                break;
            case FOCUS_MEASURE:
                setFocusMeterCenter(true, true);
                break;
            case MEASURE_FULL:
                setFocusMeter(null, new Rect(-1000, -1000, 1000, 1000));
                break;
        }
    }

    public View getCameraView() {
        return mCameraView;
    }

    public void setCameraView(View cameraView) {
        mCameraView = cameraView;
    }

    private View mCameraView;


    private Rect toCameraRect(Rect screenRect) {
        View cameraView = getCameraView();
        if (screenRect == null || cameraView == null) {
            return screenRect;
        }
        int width = cameraView.getWidth();
        float scaleWidth = width * 1.0f / 2000;
        int height = cameraView.getHeight();
        float scaleHeight = height * 1.0f / 2000;
        int left = (int) ((screenRect.left - cameraView.getLeft()) / scaleWidth);
        int right = (int) ((screenRect.right - cameraView.getLeft()) / scaleWidth);
        int top = (int) ((screenRect.top - cameraView.getTop()) / scaleHeight);
        int bottom = (int) ((screenRect.bottom - cameraView.getTop()) / scaleHeight);
        Rect rect = new Rect(left, top, right, bottom);
        rect.offset(-1000, -1000);
        Log.d(TAG, "toCameraRect:"
                + "  left:" + cameraView.getLeft() + "  getTop:" + cameraView.getTop()
                + "  screenRect:" + screenRect.toString()
                + "  CameraRect:" + rect.toString()
                + "  screenRect:" + screenRect.exactCenterX() + "  " + screenRect.exactCenterY()
                + "  cameraRect:" + rect.exactCenterX() + "  " + rect.exactCenterY());
        return rect;
    }

    private Rect toScreenRect(Rect cameraRect) {
        View cameraView = getCameraView();
        if (cameraRect == null || cameraView == null) {
            return cameraRect;
        }
        Rect tmp = new Rect(cameraRect);
        tmp.offset(1000, 1000);
        int width = cameraView.getWidth();
        float scaleWidth = width * 1.0f / 2000;
        int height = cameraView.getHeight();
        float scaleHeight = height * 1.0f / 2000;
        int left = (int) (tmp.left * scaleWidth);
        int right = (int) (tmp.right * scaleWidth);
        int top = (int) (tmp.top * scaleHeight);
        int bottom = (int) (tmp.bottom * scaleHeight);
        Rect rect = new Rect(left, top, right, bottom);
        rect.offset(cameraView.getLeft(), cameraView.getTop());
        Log.d(TAG, "toScreenRect: "
                + "  left:" + cameraView.getLeft() + "  getTop:" + cameraView.getTop()
                + "  cameraRect: center:" + cameraRect.centerX() + "  " + cameraRect.centerY()
                + "  screenRect: centerX:" + rect.centerX() + "  centerY:" + rect.centerY());
        return rect;
    }

    public void checkCameraRectEdge(Rect r) {
        if (r == null) {
            return;
        }
        if (r.left < -1000) {
            r.left = -1000;
        }
        if (r.top < -1000) {
            r.top = -1000;
        }
        if (r.right > 1000) {
            r.right = 1000;
        }
        if (r.bottom > 1000) {
            r.bottom = 1000;
        }
    }

    public void rotateCameraRectN90(Rect r, final int rotationFinal, boolean bBack) {
        int rotation = rotationFinal;
        if (rotation < 0) {
            rotation += 360;
        }
        if (r == null || rotation % 90 != 0) {
            return;
        }
        CameraEngine.getCameraRotation();

        Rect tmp = new Rect(r);
        float cx = tmp.exactCenterX();
        float cy = tmp.exactCenterY();
        float w = tmp.width();
        float h = tmp.height();
        double k = Math.toRadians(rotation);
        float ww = tmp.width();
        float hh = tmp.height();
        float cxx, cyy;

        if (bBack) {
            switch (rotation) {//顺时针移动
                case 90:
                    k += Math.PI;
                    ww = h;
                    hh = w;
                    break;
                case 180:
                    ww = w;
                    hh = h;
                    break;
                case 270:
                    k -= Math.PI;
                    ww = h;
                    hh = w;
                    break;
                case 0:
                    ww = tmp.width();
                    hh = tmp.height();
                    break;
                default:
                    ww = tmp.width();
                    hh = tmp.height();
                    break;
            }

            cxx = (float) (cx * Math.cos(k) - cy * Math.sin(k));
            cyy = (float) (cx * Math.sin(k) + cy * Math.cos(k));
        } else {

            switch (rotation) {//顺时针移动
                case 90://TODO//y?
//                    k += Math.PI;
                    cxx = (float) (cx * Math.cos(k) - cy * Math.sin(k));
                    cyy = (float) (cx * Math.sin(k) + cy * Math.cos(k));
                    cyy *= -1;
                    ww = tmp.height();
                    hh = tmp.width();
                    break;
                case 180://y
                    k -= Math.PI;
                    cxx = (float) (cx * Math.cos(k) - cy * Math.sin(k));
                    cyy = (float) (cx * Math.sin(k) + cy * Math.cos(k));
                    cyy *= -1;
                    ww = tmp.width();
                    hh = tmp.height();
                    break;
                case 270://yy
                    //k -= Math.PI;
                    cxx = (float) (cx * Math.cos(k) - cy * Math.sin(k));
                    cyy = (float) (cx * Math.sin(k) + cy * Math.cos(k));
                    cyy *= -1;
                    ww = tmp.height();
                    hh = tmp.width();
                    break;
                case 0://yy
                    cxx = (float) (cx * Math.cos(k) - cy * Math.sin(k));
                    cyy = (float) (cx * Math.sin(k) + cy * Math.cos(k));
                    cxx *= -1;
                    ww = tmp.width();
                    hh = tmp.height();

                    break;
                default:
                    cxx = (float) (cx * Math.cos(k) - cy * Math.sin(k));
                    cyy = (float) (cx * Math.sin(k) + cy * Math.cos(k));
                    ww = tmp.width();
                    hh = tmp.height();
                    break;
            }
        }
//        x1=cos(angle)*x-sin(angle)*y;
//        y1=cos(angle)*y+sin(angle)*x;

        r.left = (int) (cxx - ww / 2);
        r.right = (int) (cxx + ww / 2);
        r.top = (int) (cyy - hh / 2);
        r.bottom = (int) (cyy + hh / 2);

        Log.d(TAG, "rotateCameraRectN90: "
                + "  tmpCx:" + tmp.exactCenterX() + "  " + tmp.exactCenterY()
                + "  rstCx:" + r.exactCenterX() + "  " + r.exactCenterY()
                + "  k:" + k + "  rotationFinal:" + rotationFinal
                + "  isBack:" + bBack
                + "  rotation:" + rotation);
    }

    private FocusViewDisplayImpl mFocusViewDisplayImpl;

    public interface FocusViewDisplayImpl {
        boolean isAllowShow();
    }


    public FocusViewDisplayImpl getFocusViewDisplayImpl() {
        return mFocusViewDisplayImpl;
    }

    public void setFocusViewDisplayImpl(FocusViewDisplayImpl focusViewDisplayImpl) {
        mFocusViewDisplayImpl = focusViewDisplayImpl;
    }

    private final int TIME_DELAY_RESET_FOCUS_METER = 3000;

    public Runnable getRunResetFocus() {
        if (mRunResetFocus == null) {
            mRunResetFocus = new Runnable() {
                @Override
                public void run() {
                    //特定对焦模式下才进行复位
                    //if (CameraEngine.getFocusMode().equals(
                    //Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    removeCallbacks(mRunResetFocus);
                    resetFocusMeterPara(true, false);
                    //setFocusMeterCenter(true, true);
                    //}
                }
            };
        }
        return mRunResetFocus;
    }

    private Runnable mRunResetFocus = null;

    public void cancelReset() {
        removeCallbacks(mRunResetFocus);
    }

    public void resetFocusMeterPara() {
        resetFocusMeterPara(true, true);
    }

    public void resetFocusMeterPara(boolean focus, boolean meter) {
        if (!focus && !meter) {
            return;
        }

        Camera camera = CameraEngine.getCamera();
        if (camera == null) {
            return;
        }
        try {
            Camera.Parameters para = camera.getParameters();
            para.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            List<Camera.Area> focusAreas = para.getFocusAreas();
            if (focusAreas != null && focusAreas.size() != 0) {
                focusAreas.clear();
            }
            List<Camera.Area> meteringAreas = para.getMeteringAreas();
            if (meteringAreas != null && meteringAreas.size() != 0) {
                meteringAreas.clear();
            }
            if (focus) {
                //para.setFocusAreas(null/*focusAreas*/);//不能设置FocusAreas为null
            }
            if (meter) {
                //para.setMeteringAreas(null/*focusAreas*/);//不能设置MeteringAreas为null
            }
            camera.setParameters(para);
            CameraEngine.printCameraInfo(para, "resetFocusMeterPara "
                    + "  focus:" + focus
                    + "  meter:" + meter);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "resetFocusMeterPara: " + e);
        }
    }


    public Runnable getRunAutoFocusFailed() {
        if (mRunAutoFocusFailed == null) {
            mRunAutoFocusFailed = new Runnable() {
                @Override
                public void run() {
                    try {
                        onAutoFocusCallback(false, CameraEngine.getCamera());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "run: " + e);
                    }
                }
            };
        }
        return mRunAutoFocusFailed;
    }

    private Runnable mRunAutoFocusFailed;

}
