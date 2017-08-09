package com.putao.ptx.camera.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.exif.ExifInterface;
import com.putao.ptx.camera.R;
import com.putao.ptx.camera.eventbus.EventList;
import com.putao.ptx.camera.helper.CameraEngine;
import com.putao.ptx.camera.helper.MediaRecorderControl;
import com.putao.ptx.camera.model.AudioPlay;
import com.putao.ptx.camera.util.PanoDialog;
import com.putao.ptx.camera.util.SaveTask;
import com.sunnybear.library.util.DensityUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2016/5/12.
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {
    private final String TAG = CameraSurfaceView.class.getSimpleName();
    private Context mContext;
    private SurfaceHolder holder;
    private int orientation = -1;
    protected SaveTask mSaveTask;
    private ExecutorService mExecutor;
    private Camera.PreviewCallback previewCallback;
    private boolean mTakingPic = false;
    private long mTmLastTakePic = 0;
    private MediaRecorderControl.OnSetVideoFrameListener mVideoSetListener;
    private Runnable mActionShowErrorDialog;


    public void copy(CameraSurfaceView newView) {
        if (newView == null) {
            return;
        }
        newView.orientation = orientation;
        newView.mSaveTask = mSaveTask;
        newView.mExecutor = mExecutor;
        newView.previewCallback = previewCallback;
        newView.mTakingPic = mTakingPic;
        newView.mTmLastTakePic = mTmLastTakePic;
        newView.mVideoSetListener = mVideoSetListener == null ? null : getVideoSetListener();
        newView.mIsShouldStopPreview = mIsShouldStopPreview;
    }

    public CameraSurfaceView(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
    }


    private void init() {
        holder = this.getHolder();
//        holder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明 transparent透明
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera(false);
        Log.d(TAG, "surfaceCreated: ");
    }

    public boolean openCamera(boolean bShowDlg) {
        removeCallbacks(mActionShowErrorDialog);
        boolean bOpen = CameraEngine.openCamera(mContext);
        if (!bOpen && getCamera() == null) {
            if (bShowDlg) {
                tryDelayShowErrorDialog();
            }
        }
        SurfaceHolder holder = getHolder();
        CameraEngine.setPreviewDisplay(holder, orientation);
        return getCamera() != null;
    }

    public void closeCamera() {
        CameraEngine.releaseCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        CameraEngine.getCamera().stopPreview();
//        CameraEngine.setDefaultParameters(getContext());
        long tm = SystemClock.elapsedRealtime();
        if (orientation < 0) {
            if (width > height)
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            else
                orientation = Configuration.ORIENTATION_PORTRAIT;
//            orientation = mContext.getResources().getConfiguration().orientation;
        }
        CameraEngine.setPreviewCallback(previewCallback);
        CameraEngine.setPreviewDisplay(holder, orientation);
        EventBus.getDefault().post(EventList.UPDATE_SURFACE);
        Log.d(TAG, "surfaceChanged: " + orientation + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        long tm = SystemClock.elapsedRealtime();
        if (CameraEngine.getSettedActivity() == getContext()) {
            CameraEngine.releaseCamera();
        }
        Log.d(TAG, "surfaceDestroyed: waste time:" + (SystemClock.elapsedRealtime() - tm));
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
        CameraEngine.setPreviewDisplay(holder, orientation);
    }

    public void switchCamera() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (CameraEngine.switchCamera(mContext) >= 0 && getCamera() != null) {
            CameraEngine.setPreviewCallback(previewCallback);
            CameraEngine.setPreviewDisplay(holder, orientation);
        } else {
            tryDelayShowErrorDialog();
        }
        Log.d(TAG, "switchCamera: waste time:" + (SystemClock.elapsedRealtime() - elapsedRealtime));
    }

    /**
     * 后置摄像头耗时650~750ms
     */
    public void freshCamera() {
        long tm = SystemClock.elapsedRealtime();
        switchCamera(CameraEngine.isCameraIdBack());
        Log.d(TAG, "freshCamera: waste time:" + (SystemClock.elapsedRealtime() - tm));
    }

    public void switchCamera(boolean bBack) {
        if (CameraEngine.switchCamera(mContext, bBack) >= 0 && getCamera() != null) {
            CameraEngine.setPreviewCallback(previewCallback);
            CameraEngine.setPreviewDisplay(holder, orientation);
        } else {
            Thread thread = Thread.currentThread();
            if (thread == Looper.getMainLooper().getThread()) {
                tryDelayShowErrorDialog();
            } else {
                post(new Runnable() {
                    @Override
                    public void run() {
                        tryDelayShowErrorDialog();
                    }
                });
            }
        }
    }


    public boolean isTakingPic() {
        return mTakingPic;
    }

    public long getTmLastTakePic() {
        return mTmLastTakePic;
    }

    public boolean isAllowTakePicForTime() {
        return SystemClock.elapsedRealtime() - getTmLastTakePic() > 5000 || !isTakingPic();
    }

    private IsShouldStopPreview mIsShouldStopPreview;

    private AudioPlay mAudioPlay;

    public void takePic(final Context context, final int rotation, final File file,
                        final onPictureTakenListener listener, final ExifInterface exif) {
        mTakingPic = true;
        mTmLastTakePic = SystemClock.elapsedRealtime();
        try {
            Log.d(TAG, "takePic: before Camera.takePicture");
            //Camera.takePicture(...)接口出现过多次ANR问题
            getCamera().takePicture(null/*new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                }
            }*/, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] data, Camera camera) {
                    mTakingPic = false;
                    long tm = SystemClock.elapsedRealtime();
                    Log.d(TAG, "taking_picture onPictureTaken: Camera.takePicture（...） waste time 耗费时间:"
                            + (tm - mTmLastTakePic)
                            + "ms  是否后置摄像头：" + CameraEngine.isCameraIdBack()
                            + "  照片尺寸：" + CameraEngine.getSizePic().width + "*" + CameraEngine.getSizePic().height);
                    if (mIsShouldStopPreview != null && mIsShouldStopPreview.isShouldStopPreview()) {
                        camera.stopPreview();
                    } else {
                        camera.startPreview();
                    }
                    EventBus.getDefault().post(EventList.TAKED_PIC);
                    mSaveTask = new SaveTask(context, rotation, file, listener, exif);
                    mSaveTask.mTmStartWait = SystemClock.elapsedRealtime();
                    //mSaveTask.execute(data);
                    if (!SaveTask.sbCapturePic) {
                        if (mExecutor == null) {
                            mExecutor = Executors.newFixedThreadPool(6/*2*/);
                        }
                        mSaveTask.executeOnExecutor(mExecutor, data);
                    } else {
                        mSaveTask.execute(data);
                    }
                    Log.d(TAG, "onPictureTaken: taking_picture after camera.startPreview:" + (SystemClock.elapsedRealtime() - tm));
                }
            });
        } catch (Exception e) {
            mTakingPic = false;
            e.printStackTrace();
            Log.e(TAG, "拍照时发生错误");
        }
    }

    private com.android.camera.exif.ExifInterface getExitInfo() {
        return null;//TODO
    }

    public void shutdown() {
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallBack) {
        this.previewCallback = previewCallBack;
    }

    public void setAudioPlay(AudioPlay audioPlay) {
        mAudioPlay = audioPlay;
    }

    public interface onPictureTakenListener {
        void onSaved(Bitmap bmp, Uri uri);
    }

    public int getCameraID() {
        return CameraEngine.getCameraID();
    }

    public void setCameraID(int ID) {
        CameraEngine.setCameraID(ID);
    }

    public Camera getCamera() {
        return CameraEngine.getCamera();
    }

    public int getMaxZoom() {
        return CameraEngine.getMaxZoom();
    }

    public void setZooM(int progress) {
        CameraEngine.setZooM(progress);
    }

    public void setPicFocusMode() {
        CameraEngine.setPicFocusMode();
    }

    /**
     * 频繁地调用此接口后牌照时，容易崩溃
     */
    public void setVideoFocusMode() {
        long tm = SystemClock.elapsedRealtime();
        CameraEngine.setVideoFocusMode();
        Log.d(TAG, "setVideoFocusMode: " + (SystemClock.elapsedRealtime() - tm));//耗时8ms
    }

    private void tryDelayShowErrorDialog() {
        if (mActionShowErrorDialog == null) {
            mActionShowErrorDialog = new Runnable() {
                @Override
                public void run() {
                    if (getCamera() == null && isAttachedToWindow()) {
                        cameraErrorDialog();
                    }
                }
            };
        }
        removeCallbacks(mActionShowErrorDialog);
        postDelayed(mActionShowErrorDialog, 500);
    }

    private void cameraErrorDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//        builder.setTitle(getResString(R.string.camera_error)).setMessage(getResString(R.string.camera_error_msg));
//        builder.setCancelable(false);
//        builder.setNegativeButton(getResString(R.string.common_confirm), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//                if (getCamera() == null) {
//                    ((Activity) mContext).finish();
//                }
//            }
//        });
//        Dialog dialog = builder.create();
//        dialog.show();

        View v = LayoutInflater.from(getContext()).inflate(R.layout.dlg_camera_err, null);
        TextView tv = (TextView) v.findViewById(R.id.tv);
        tv.setText(R.string.camera_error_msg);
        TextView btn = (TextView) v.findViewById(R.id.btn);
        btn.setText(R.string.common_confirm);
        btn.setBackgroundResource(R.drawable.selector_btn_yes);
        int width = 800;
        int height = 400;
        final PanoDialog dlg = new PanoDialog(getContext(), width, height, v, R.style.pano_dialog);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
                if (getCamera() == null) {
                    ((Activity) mContext).finish();
                }
            }
        });
        dlg.setCancelable(false);
        dlg.show();
    }

    private String getResString(int id) {
        return mContext.getResources().getString(id);
    }


    public static interface IsShouldStopPreview {
        boolean isShouldStopPreview();
    }

    public void setIsShouldStopPreview(IsShouldStopPreview isShouldStopPreview) {
        mIsShouldStopPreview = isShouldStopPreview;
    }


    public boolean isLayoutMarginZero() {
        RelativeLayout.LayoutParams para = (RelativeLayout.LayoutParams) getLayoutParams();
        return para.leftMargin != 0 || para.topMargin != 0
                || para.rightMargin != 0 || para.bottomMargin != 0;
    }

    public static final boolean sbFillHeight = true;

    public MediaRecorderControl.OnSetVideoFrameListener getVideoSetListener() {
        if (mVideoSetListener == null) {
            mVideoSetListener = new MediaRecorderControl.OnSetVideoFrameListener() {
                int tempWidth = DensityUtil.getDeviceWidth(mContext);//getResources().getDisplayMetrics().widthPixels;
                int tempHeight = DensityUtil.getDeviceHeight(mContext);
                int screenWidth = Math.max(tempWidth, tempHeight);
                int screenHeight = Math.min(tempWidth, tempHeight);
                float radioScreen = screenWidth * 1.0f / screenHeight;

                @Override
                public boolean onSetVideoFrameListener(final int frameWidth, final int frameHeight) {
                    int width = getWidth();
                    int height = getHeight();
                    float radio1 = Math.max(frameWidth, frameHeight) * 1.0f / Math.min(frameWidth, frameHeight);


                    boolean land = CameraEngine.isOrintationLand();
                    ViewGroup.LayoutParams para = getLayoutParams();
                    int newWidth = screenWidth;
                    int newHeight = screenHeight;
                    if (radio1 > radioScreen) {
                        newWidth = (int) (screenHeight * radio1);
                        newHeight = (int) (screenWidth * 1.0f / radio1);
                    }
                    int diff = newWidth - screenWidth;
                    int halfDiff = -diff / 2;
                    //if (cameraSurfaceView.getWidth() > cameraSurfaceView.getHeight()) {
                    boolean bChange = false;

                    if (land) {
                        //bChange = para.leftMargin != halfDiff || para.topMargin != 0
                        //        || para.rightMargin != halfDiff || para.bottomMargin != 0;
                        //bChange = para.width != diff + screenWidth;
                        bChange = sbFillHeight ? para.width != newWidth : para.height != newHeight;
                        if (bChange) {
                            //para.setMargins(halfDiff, 0, halfDiff, 0);
                            para.width = sbFillHeight ? newWidth : screenWidth;
                            para.height = !sbFillHeight ? newHeight : screenHeight;
                        }
                    } else {
                        //bChange = para.leftMargin != 0 || para.topMargin != halfDiff
                        //        || para.rightMargin != 0 || para.bottomMargin != halfDiff;
                        //bChange = para.height != diff + screenWidth;
                        bChange = sbFillHeight ? para.height != newWidth : para.width != newHeight;
                        if (bChange) {
                            //para.setMargins(0, halfDiff, 0, halfDiff);
                            para.width = !sbFillHeight ? newHeight : screenHeight;
                            para.height = sbFillHeight ? newWidth : screenWidth;
                        }
                    }
                    if (bChange) {
//                        para.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//                        para.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//                        para.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//                        para.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                        setLayoutParams(para);
                        getParent().requestLayout();
                        //rootView.requestLayout();
                        requestLayout();
                        invalidate();
//                        holder.setFixedSize(para.width, para.height);
//                        holder.setSizeFromLayout();
                        //CameraEngine.setParametersPreview(true);
                    }
                    Log.d(TAG, "onSetVideoFrameListener run: frameWidth:" + frameWidth
                            + "  frameHeight:" + frameHeight
                            + "  radio1:" + radio1 + "  tempWidth:" + tempWidth
                            + "  tempHeight:" + tempHeight + "  newWidth:" + newWidth
                            + "  width:" + getWidth()
                            + "  height:" + getHeight()
                            + "  bChange:" + bChange + "  land:" + land
                            + "  parawidth/height:" + para.width + " " + para.height);
                    return bChange;
                }

                @Override
                public boolean onReset() {
                    //showBlurAnimate();
                    ViewGroup.LayoutParams para = getLayoutParams();
                    //para.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                    //para.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                    //boolean bChange = !(para.leftMargin == 0 && para.topMargin == 0
                    //        && para.rightMargin == 0 && para.bottomMargin == 0);
                    boolean land = CameraEngine.isOrintationLand();
                    boolean bChange = land && (para.width != screenWidth
                            || para.height != screenHeight)
                            || !land && (para.width != screenHeight
                            || para.height != screenWidth);
                    if (bChange) {
                        if (land) {
                            para.width = screenWidth;
                            para.height = screenHeight;
                        } else {
                            para.width = screenHeight;
                            para.height = screenWidth;
                        }
                        //para.width = RelativeLayout.LayoutParams.MATCH_PARENT;
                        //para.height = RelativeLayout.LayoutParams.MATCH_PARENT;
//                        para.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//                        para.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//                        para.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//                        para.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                        para.addRule(RelativeLayout.CENTER_IN_PARENT);
//                        para.setMargins(0, 0, 0, 0);
                        setLayoutParams(para);
                        try {
                            getParent().requestLayout();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //rootView.requestLayout();
                        requestLayout();
//                        holder.setFixedSize(para.width, para.height);
//                        holder.setSizeFromLayout();
                        //CameraEngine.setParametersPreview(false);
                    }
                    Log.d(TAG, "onSetVideoFrameListener run: "
                            + "  width:" + getWidth()
                            + "  height:" + getHeight()
                            + "  bChange:" + bChange
                            + "  parawidth/height:" + para.width + " " + para.height);
                    return bChange;
                }
            };
        }
        return mVideoSetListener;
    }
}
