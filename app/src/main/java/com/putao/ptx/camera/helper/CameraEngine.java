package com.putao.ptx.camera.helper;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.putao.ptx.camera.model.EmCameraMode;
import com.putao.ptx.camera.model.EmPreviewSizeMode;
import com.sunnybear.library.util.DensityUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2016/6/13.
 */
public class CameraEngine {
    private static final String TAG = CameraEngine.class.getSimpleName();
    private static Camera mCamera = null;
    public static Object sSynObj = new Object();
    private static int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private static int maxZoom;
    private static int sCameraRotation;
    private static Camera.Size sMaxVideoSize;
    private static byte[] sOnPreviewFrameData;
    private static OnPreviewFrameListener sOnPreviewFrameListener;

    private float screenRate;
    private static Activity sAct;
    private static Camera.Size sSizePre;
    private static Camera.Size sSizePreVideo;
    private static Camera.Size sSizePre1280_720;
    private static Camera.Size sSizePre1920_1080;
    private static Camera.Size sSizePic;
    private static Camera.FaceDetectionListener sFaceDetectionListener;

    private static boolean sbSupportFrontVideo960_720 = false;
    private static boolean sbSupportBackVideo1440_1080 = false;

    private static final String KEY_APPMODE_ZSD = "zsl";
    private static final String ON = "on";
    private static final String OFF = "off";

    public static Camera getCamera() {
        return mCamera;
    }

    public static int getCameraID() {
        return mCameraID;
    }

    public static boolean isCameraIdBack() {
        return getCameraID() == Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    public static boolean isCameraIdBackOpen() {
        return isCameraIdBack() && mCamera != null;
    }

    public static void setCameraID(int ID) {
        mCameraID = ID;
    }

    /**
     * 耗时约800~1500ms
     *
     * @param context
     * @return
     */
    public static boolean openCamera(Context context) {
        synchronized (sSynObj) {
            if (mCamera == null) {
                try {
                    long tm = SystemClock.elapsedRealtime();
                    try {
                        mCamera = Camera.open(mCameraID);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "openCamera: " + e);
                        Log.d(TAG, "openCamera: first  Exception");
                    }
                    if (mCamera == null) {
                        Log.d(TAG, "openCamera: first " + mCameraID + "failed!");
                        try {
                            mCamera = Camera.open(mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "openCamera: " + e);
                            Log.d(TAG, "openCamera: second  Exception");
                        }
                        if (mCamera != null) {
                            mCamera.release();
                            Log.d(TAG, "openCamera: second reverse " + mCameraID + " succeed!");

                            try {
                                mCamera = Camera.open(mCameraID);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "openCamera: " + e);
                                Log.d(TAG, "openCamera: third Exception");
                            }
                        } else {
                            Log.d(TAG, "openCamera: second reverse " + mCameraID + " failed!");
                        }
                    }

                    if (mCamera != null) {
                        setDefaultParameters(context);
                        if (sbUseFrameBuff) {
                            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                            byte[] buffer = new byte[previewSize.width * previewSize.height * 3 / 2];
                            Log.d(TAG, "openCamera: previewSize length:" + buffer.length);
                            mCamera.addCallbackBuffer(buffer);
                            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                                @Override
                                public void onPreviewFrame(byte[] data, Camera camera) {
                                    camera.addCallbackBuffer(data);
                                    Log.d(TAG, "onPreviewFrame: setPreviewCallbackWithBuffer");
                                }
                            });
                            mCamera.addCallbackBuffer(buffer);
                        }
                        mCamera.startPreview();

                        try {
                            //mCamera.startFaceDetection();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "openCamera: ", e);
                        }
                        //mCamera.setFaceDetectionListener(getFaceDetectionListener());
                        Log.d(TAG, "openCamera: waste time:" + (SystemClock.elapsedRealtime() - tm));
                    } else {
                        Log.e(TAG, "相机打开时发生错误" + "  mCameraID:" + mCameraID
                                + "  isFrontCamera:" + (mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT));
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "相机打开时发生错误" + "  mCameraID:" + mCameraID
                            + "  isFrontCamera:" + (mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT));
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    public static int switchCamera(Context context) {
        int camerCount = Camera.getNumberOfCameras();
        Log.d(TAG, "switchCamera: camerCount:" + camerCount);
        if (camerCount == 1) {
            Log.e(TAG, "switchCamera:  相机的摄像头数量为1，驱动接口有误！");
            return mCameraID;
        }
        if (camerCount == 2) {
            if (mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            } else {
                mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
        }
        releaseCamera();
        if (!openCamera(context)) {
            return -1;
        }
        //setDefaultParameters(context);
        //mCamera.startPreview();
        Log.d(TAG, "switchCamera: ");
        return mCameraID;
    }

    public static int switchCamera(Context context, boolean toBack) {
        mCameraID = toBack ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;
        return switchCamera(context);
    }

    /**
     * 耗时180~240ms
     */
    public static void releaseCamera() {
        synchronized (sSynObj) {
            if (mCamera != null) {
                long tm = SystemClock.elapsedRealtime();
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                Log.d(TAG, "releaseCamera:  waste time:" + (SystemClock.elapsedRealtime() - tm));
            }
        }
    }

    public static int getCameraRotation() {
        return sCameraRotation;
    }

    public static void setDefaultParameters(Context context) {
        if (sAct != null) {
            context = sAct;
        }
        if (mCamera == null) {
            return;
        }

        synchronized (sSynObj) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setJpegQuality(100);
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setPreviewFormat(ImageFormat.NV21);
            updateZsd(parameters, true);
            //前stoppreview& startpreview一次，这样才能使ZSD配置生效。
            //最大缩放值
            maxZoom = (int) (parameters.getMaxZoom() / 3f);
            //变焦缩放比例值。
            List<Integer> zoomRatios = parameters.getZoomRatios();

            int orientation = context.getResources().getConfiguration().orientation;
            float screenRate = (float) DensityUtil.getDeviceHeight(context) / DensityUtil.getDeviceWidth(context);
            printPreviewSize(parameters);
            MediaRecorderControl.printVideoSize();
            printPictureSize(parameters);
            printVideoSize(parameters);
            printSupportedFocusModes(parameters);
            if (isCameraIdBack()) {
                sbSupportBackVideo1440_1080 = isSupportSize(parameters.getSupportedVideoSizes(), 1440, 1080);
            } else {
                sbSupportFrontVideo960_720 = isSupportSize(parameters.getSupportedVideoSizes(), 960, 720);
            }

            //int widthTmp = context.getResources().getDisplayMetrics().widthPixels;
            //int heightTmp = context.getResources().getDisplayMetrics().heightPixels;
            int widthTmp = DensityUtil.getDeviceWidth(context);
            int heightTmp = DensityUtil.getDeviceHeight(context);
            int width = Math.max(widthTmp, heightTmp);
            int height = Math.min(widthTmp, heightTmp);
            List<Camera.Size> tmp = parameters.getSupportedPreviewSizes();
            List<Camera.Size> tmpPic = parameters.getSupportedPictureSizes();
            List<Camera.Size> tmpVideo = parameters.getSupportedVideoSizes();
            float radio = width * 1.0f / height;
            Camera.Size sizeTmp = getProperPreviewSize(tmp, width, height);
            sSizePre = mCamera.new Size(sizeTmp.width, sizeTmp.height);
            //sSizePre = getMaxProperSize(tmp, radio);

            sizeTmp = getProperPreviewSize(tmp, width, (int) (width / 1.777778));
            sSizePreVideo = mCamera.new Size(sizeTmp.width, sizeTmp.height);
            ;

            CamcorderProfile frontProfile = MediaRecorderControl.getCamcorderProfile(false);
            CamcorderProfile backProfile = MediaRecorderControl.getCamcorderProfile(true);
            sSizePre1280_720 = mCamera.new Size(frontProfile.videoFrameWidth,
                    frontProfile.videoFrameHeight);//getProperPreviewSize(tmp, 1280, 720);
            sSizePre1920_1080 = mCamera.new Size(backProfile.videoFrameWidth,
                    backProfile.videoFrameHeight);//getProperPreviewSize(tmp, 1920, 1080);

            sizeTmp = getMaxProperSize(tmpPic, radio);
            sSizePic = mCamera.new Size(sizeTmp.width, sizeTmp.height);

            sizeTmp = getMaxProperSize(tmpVideo, radio);
            sMaxVideoSize = mCamera.new Size(sizeTmp.width, sizeTmp.height);

            if (sSizePic == null) {
                sSizePic = mCamera.new Size(width, height);
            }
            boolean bLand = orientation != Configuration.ORIENTATION_LANDSCAPE;
            //parameters.setPictureSize(bLand ? sizePic.width : sizePic.height, !bLand ? sizePic.width : sizePic.height);
            parameters.setPictureSize(sSizePic.width, sSizePic.height);
            //parameters.setPreviewSize(sSizePre.width, sSizePre.height);//此操作无意义
            //parameters.set
            //setPicFocusMode();//此操作无意义
            setWhiteBalance(parameters);
            setExposure(parameters);
            resetExposureCompensationToDefault(parameters);
            EmPreviewSizeMode.OnPreviewSizeListener previewSizeListener = getPreviewSizeListener();
            updatePreviewSizeParameter(parameters, previewSizeListener);
            EmCameraMode.OnCameraModeListener cameraMode = getCameraModeListener();
            updatePreviewFocusMode(parameters, cameraMode);
            mCamera.setParameters(parameters);
            printCameraInfo(parameters, "setDefaultParameters");

            Log.d(TAG, "*******************************************************************");
            if (sMaxVideoSize != null) {
                Log.d(TAG, "getVideoSize: " + sMaxVideoSize.width + "*" + sMaxVideoSize.height);
            }
            Log.d(TAG, "setDefaultParameters: "
                    + "\n*******************************************************************"
                    + "\n  sSizePre:" + sSizePre.width + "*" + sSizePre.height
                    + "\n  sSizePre1280_720:" + sSizePre1280_720.width + "*" + sSizePre1280_720.height
                    + "\n  sSizePre1920_1080:" + sSizePre1920_1080.width + "*" + sSizePre1920_1080.height
                    + "\n  getPreviewSize: " + parameters.getPreviewSize().width + "*" + parameters.getPreviewSize().height
                    + "\n   getPictureSize: " + parameters.getPictureSize().width + "*" + parameters.getPictureSize().height
                    + "\n*******************************************************************");
        }
    }

    private static void updateZsd(Camera.Parameters parameters, boolean bOn) {
        enableZsd = false;
        if (enableZsd) {
            parameters.set(KEY_APPMODE_ZSD, bOn ? ON : OFF);
        }
    }

    private static boolean enableZsd = false;

    /**
     * 1.       扩展CameraParameters:: KEY_APPMODE_ZSD = "zsl"，可取的值是”on”, “off”.
     * <p>
     * 2.       Camera通过preview时调用startPreview函数之前设置 参数“zsl” 为on，从而打开ZSD 模式。
     * <p>
     * 3.       当camera工作到video模式时，这种情况下请设置“zsl“ 为off 去关闭ZSD，与MtK这边已确认，这需要进入video之
     * 前stoppreview& startpreview一次，这样才能使ZSD配置生效。
     *
     * @param bOn
     * @return
     */
    public static boolean enableZsd(boolean bOn) {
        if (mCamera == null) {
            return false;
        }
        enableZsd = false;
        if (enableZsd) {
            Camera.Parameters parameters = mCamera.getParameters();
            updateZsd(parameters, bOn);
            mCamera.stopPreview();
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            return true;
        } else {
            return false;
        }
    }

    public static void updatePreviewFocusMode(Camera.Parameters parameters,
                                              EmCameraMode.OnCameraModeListener cameraModeListener) {
        if (parameters == null || cameraModeListener == null) {
            return;
        }

        EmCameraMode em = cameraModeListener.getCameraMode();
        String focusMode;
        if (em == EmCameraMode.VIDEO) {
            focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE/*FOCUS_MODE_CONTINUOUS_VIDEO*/;
        } else {
            focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        }
        if (parameters.getFocusMode() != focusMode &&
                parameters.getSupportedFocusModes().contains(focusMode)) {
            parameters.setFocusMode(focusMode);
        }
    }

    public static void updatePreviewFocusMode() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        updatePreviewFocusMode(parameters, getCameraModeListener());
        mCamera.setParameters(parameters);
        printCameraInfo(parameters, "updatePreviewFocusMode");
    }

    private static void updatePreviewSizeParameter(Camera.Parameters parameters,
                                                   EmPreviewSizeMode.OnPreviewSizeListener previewSizeListener) {
        if (previewSizeListener != null && parameters != null) {
            Camera.Size sizeTmp = getWantedCameraSize();
            parameters.setPreviewSize(sizeTmp.width, sizeTmp.height);
            Log.d(TAG, "updatePreviewSizeParameter:  " +
                    "  previewSize:" + sizeTmp.width + "*" + sizeTmp.height);
        }
    }

    /**
     * 耗时约300ms
     *
     * @return
     */
    public static boolean updatePreviewSize() {
        boolean changed = false;
        if (mCamera == null) {
            return false;
        } else {
            try {
                long tm = SystemClock.elapsedRealtime();
                Camera.Parameters parameters = mCamera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                Camera.Size wanted = getWantedCameraSize();
                changed = size.width != wanted.width || size.height != wanted.height;
//                if (changed) {
                parameters.setPreviewSize(wanted.width, wanted.height);
                mCamera.stopPreview();
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                printCameraInfo(parameters, "updatePreviewSize  waste time:"
                        + (SystemClock.elapsedRealtime() - tm));
//                }
                Log.d(TAG, "updatePreviewSize:  changed:" + changed
                        + "  sPreviewParameterListener:" + sPreviewParameterListener.getPreviewSizeMode()
                        + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
                return changed;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "isCameraPreviewSizeAsWanted: " + e);
                return changed;
            }
        }
    }


    public static boolean isCameraPreviewSizeAsWanted() {
        if (mCamera == null) {
            return true;
        } else {
            try {
                Camera.Size setted = mCamera.getParameters().getPreviewSize();
                Camera.Size wanted = getWantedCameraSize();
                Log.d(TAG, "isCameraPreviewSizeAsWanted: setted :" + setted.width + "  " + setted.height
                        + "  sPreviewParameterListener:" + sPreviewParameterListener.getPreviewSizeMode()
                        + "  wanted:" + wanted.width + "  " + wanted.height
                        + "  sSizePre:" + sSizePre.width + " " + sSizePre.height
                        + "  sSizePre1280_720:" + sSizePre1280_720.width + " " + sSizePre1280_720.height
                        + "  sSizePre1920_1080:" + sSizePre1920_1080.width + " " + sSizePre1920_1080.height);
                return setted.width == wanted.width && setted.height == wanted.height;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "isCameraPreviewSizeAsWanted: " + e);
                return true;
            }
        }
    }

    private static Camera.Size getWantedCameraSize() {
        if (mCamera == null) {
            return null;
        }
        Camera.Size sizeTmp = mCamera.new Size(sSizePre.width, sSizePre.height);
        try {
            EmPreviewSizeMode mode = getPreviewSizeListener().getPreviewSizeMode();
            if (mode == EmPreviewSizeMode.VIDEO_BACK) {
                sizeTmp = mCamera.new Size(isSupportBackVideo1440_1080() ?
                        1440 : sSizePre1920_1080.width,
                        sSizePre1920_1080.height);
            } else if (mode == EmPreviewSizeMode.VIDEO_FRONT) {
                sizeTmp = mCamera.new Size(isSupportFrontVideo720_960() ?
                        960 : sSizePre1280_720.width,
                        sSizePre1280_720.height);
            } else if (mode == EmPreviewSizeMode.PIC) {
                sizeTmp = mCamera.new Size(sSizePre.width, sSizePre.height);
                ;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getWantedCameraSize: " + e);
        }
        return sizeTmp;
    }

    public static void updatePreviewSizeParameter() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        EmPreviewSizeMode.OnPreviewSizeListener previewSizeListener = getPreviewSizeListener();
        updatePreviewSizeParameter(parameters, previewSizeListener);
        mCamera.setParameters(parameters);
        printCameraInfo(parameters, "updatePreviewSizeParameter");
    }


    public static void updatePreviewSizeFocusParameter() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        EmPreviewSizeMode.OnPreviewSizeListener previewSizeListener = getPreviewSizeListener();
        updatePreviewSizeParameter(parameters, previewSizeListener);
        EmCameraMode.OnCameraModeListener previewModeListener = getCameraModeListener();
        updatePreviewFocusMode(parameters, previewModeListener);
        mCamera.setParameters(parameters);
        printCameraInfo(parameters, "updatePreviewSizeFocusParameter");
    }

    public static void setParametersPreview(boolean bPreVideo) {
        if (mCamera == null) {
            return;
        }
        if (sSizePre == null || sSizePreVideo == null) {
            return;
        }
        Camera.Parameters parameters = null;
        try {
            parameters = mCamera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getParametersPreview: " + e);
            return;
        }
        Camera.Size tmp = bPreVideo ? sSizePreVideo : sSizePre;
        parameters.setPreviewSize(tmp.width, tmp.height);
        try {
            mCamera.setParameters(parameters);
            printCameraInfo(parameters, "setParametersPreview");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "setParametersPreview: " + e);
        }
        Log.d(TAG, "setParametersPreview: bPreVideo:" + bPreVideo
                + "  width:" + tmp.width + "  height:" + tmp.height);
    }

    private static Camera.Size getProperVideoSize(List<Camera.Size> tmp, int width, int height) {
        List<Camera.Size> list = new ArrayList<>();
        final float radio = width * 1.0f / height;
        for (int i = 0; i < tmp.size(); i++) {
            Camera.Size size = tmp.get(i);
            if (size.width == width && size.height == height) {
                return size;
            }
            if (size.width < width * 0.4f && size.height < height * 0.4f) {
                continue;
            }
            list.add(size);
        }
        if (list.size() == 0) {
            list.addAll(tmp);
            Collections.sort(list, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return (lhs.height + lhs.width) - (rhs.height + rhs.width);
                }
            });
            return list.get(list.size() - 1);
        }
        Collections.sort(list, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return Math.abs(1.0f * lhs.width / lhs.height - radio)
                        < Math.abs(1.0f * rhs.width / rhs.height - radio) ? -1 : 1;
            }
        });
        Camera.Size size = list.get(0);
        Log.d(TAG, "getProperPreviewSize: " + size.width + "*" + size.height
                + "  W:H=" + (size.width * 1.0f / size.height));
        return size;
    }

    private static Camera.Size getProperPreviewSize(List<Camera.Size> tmp, int width, int height) {
        List<Camera.Size> list = new ArrayList<>();
        final float radio = width * 1.0f / height;
        for (int i = 0; i < tmp.size(); i++) {
            Camera.Size size = tmp.get(i);
            if (size.width == width && size.height == height) {
                return size;
            }
            if (size.width < width * 0.4f && size.height < height * 0.4f) {
                continue;
            }
            list.add(size);
        }
        if (list.size() == 0) {
            list.addAll(tmp);
            Collections.sort(list, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return (lhs.height + lhs.width) - (rhs.height + rhs.width);
                }
            });
            return list.get(list.size() - 1);
        }
        Collections.sort(list, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return Math.abs(1.0f * lhs.width / lhs.height - radio)
                        < Math.abs(1.0f * rhs.width / rhs.height - radio) ? -1 : 1;
            }
        });
        Camera.Size size = list.get(0);
        Log.d(TAG, "getProperPreviewSize: " + size.width + "*" + size.height
                + "  W:H=" + (size.width * 1.0f / size.height));
        return size;
    }

    private static Camera.Size getMaxProperSize(List<Camera.Size> tmpPic, float radio) {
        Camera.Size ret = null;
        int size = tmpPic.size();
        for (int i = 0; i < size; i++) {
            Camera.Size size1 = tmpPic.get(i);
            float v = size1.width * 1.0f / size1.height;
            if (Math.abs(radio - v) < 0.15) {
                if (ret == null) {
                    ret = size1;
                    continue;
                }
                if (size1.width * size1.height > ret.width * ret.height) {
                    ret = size1;
                }
            }
        }
        if (ret != null) {
            Log.d(TAG, "getMaxProperSize: " + ret.width + "*" + ret.height + "  W:H=" + (ret.width * 1.0f / ret.height));
        }
        return ret;
    }

    public static Camera.Size getProperSize(List<Camera.Size> tmp, int width, int height) {
        Camera.Size ret = null;
        List<Camera.Size> list = new ArrayList<>();
        for (int i = 0; i < tmp.size(); i++) {
            if (tmp.get(i).width > width || tmp.get(i).height > height) {
                continue;
            }
            list.add(tmp.get(i));
        }
        Collections.sort(list, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return (lhs.height + lhs.width) - (rhs.height + rhs.width);
            }
        });
        return list.get(list.size() - 1);
    }

    public static void setPicFocusMode() {
        setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    public static void setVideoFocusMode() {
        setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    }

    public static void setAutoFocusMode() {
        setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    }

    private static void setFocusMode(String focusMode) {
        if (mCamera == null) {
            return;
        }
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getFocusMode() != focusMode &&
                    parameters.getSupportedFocusModes().contains(focusMode)) {
                parameters.setFocusMode(focusMode);
                mCamera.setParameters(parameters);
                printCameraInfo(parameters, "setFocusMode");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "setFocusMode: " + e);
        }
    }

    public static void setSceneModeSports() {
        if (mCamera == null) return;
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> sceneModes = parameters.getSupportedSceneModes();
        if (sceneModes.contains(Camera.Parameters.SCENE_MODE_SPORTS)) {
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
        }
    }

    private static void printPreviewSize(Camera.Parameters cameraParams) {
        List<Camera.Size> supportedPreviewSizes = cameraParams.getSupportedPreviewSizes();
        if (null == supportedPreviewSizes) return;
        Iterator mIterator = supportedPreviewSizes.iterator();
        Log.d(TAG, "SupportedPreviewSizes:isCameraIdBack:" + CameraEngine.isCameraIdBack() +
                "*******************************************************************\n");
        while (mIterator.hasNext()) {
            Camera.Size size = (Camera.Size) mIterator.next();
            Log.d(TAG, "SupportedPreviewSizes: " + size.width + "*" + size.height + "  W:H=" + (size.width * 1.0f / size.height));
        }
        Log.d(TAG, "SupportedPreviewSizes:*******************************************************************\n");
    }

    private static void printPictureSize(Camera.Parameters cameraParams) {
        List<Camera.Size> supportedPictureSize = cameraParams.getSupportedPictureSizes();
        if (null == supportedPictureSize) return;
        Iterator mIterator = supportedPictureSize.iterator();
        Log.d(TAG, "SupportedPictureSizes:  isCameraIdBack:" + CameraEngine.isCameraIdBack() +
                "*******************************************************************\n");
        while (mIterator.hasNext()) {
            Camera.Size size = (Camera.Size) mIterator.next();
            Log.d(TAG, "SupportedPictureSizes: " + size.width + "*" + size.height + "  W:H=" + (size.width * 1.0f / size.height));
        }
        Log.d(TAG, "SupportedPictureSizes:*******************************************************************\n");
    }

    private static void printVideoSize(Camera.Parameters cameraParams) {
        List<Camera.Size> supportedVideoSize = cameraParams.getSupportedVideoSizes();
        if (null == supportedVideoSize) return;
        Iterator mIterator = supportedVideoSize.iterator();
        Log.d(TAG, "getSupportedVideoSizes:  isCameraIdBack:" + CameraEngine.isCameraIdBack() +
                "*******************************************************************\n");
        while (mIterator.hasNext()) {
            Camera.Size size = (Camera.Size) mIterator.next();
            Log.d(TAG, "getSupportedVideoSizes: " + size.width + "*" + size.height + "  W:H=" + (size.width * 1.0f / size.height));
        }
        Log.d(TAG, "getSupportedVideoSizes:*******************************************************************\n");
    }


    private static void printSupportedFocusModes(Camera.Parameters cameraParams) {
        List<String> focusModes = cameraParams.getSupportedFocusModes();
        for (String mode : focusModes) {
            Log.d(TAG, "SupportedFocusModes: " + mode);
        }
        Log.d(TAG, "*******************************************************************\n");
    }

    private static void setOptimalPreviewSize(Camera.Parameters cameraParams, int targetWidth) {
        List<Camera.Size> supportedPreviewSizes = cameraParams
                .getSupportedPreviewSizes();
        if (null == supportedPreviewSizes) return;
        double minDiff = 1.7976931348623157E308D;
        Iterator mIterator = supportedPreviewSizes.iterator();
        Camera.Size optimalSize = null;
        while (mIterator.hasNext()) {
            Camera.Size size = (Camera.Size) mIterator.next();
            float rate = ((float) size.height / size.width);
            if ((double) Math.abs(size.width - targetWidth) < minDiff) {
                optimalSize = size;
                minDiff = (double) Math.abs(size.width - targetWidth);
            }
        }
        if (optimalSize == null) return;
        cameraParams.setPreviewSize(optimalSize.width, optimalSize.height);
    }

    private static void setOptimalPictureSize(Camera.Parameters cameraParams, int targetWidth) {
        List<Camera.Size> supportedPictureSizes = cameraParams
                .getSupportedPictureSizes();
        if (null == supportedPictureSizes) return;
        Camera.Size optimalSize = null;
        Iterator mIterator = supportedPictureSizes.iterator();
        while (mIterator.hasNext()) {
            Camera.Size size = (Camera.Size) mIterator.next();
            if (size.width - targetWidth < 100) {
                optimalSize = size;
                break;
            }
        }
        if (optimalSize == null) return;
        cameraParams.setPictureSize(optimalSize.width, optimalSize.height);
    }

    private static void setWhiteBalance(Camera.Parameters cameraParams) {
        List<String> whiteBalances = cameraParams.getSupportedWhiteBalance();
        if (cameraParams.isAutoWhiteBalanceLockSupported()) {
            cameraParams.setAutoWhiteBalanceLock(false);
        }
    }

    private static void setExposure(Camera.Parameters cameraParams) {
        //最大曝光补偿指数
        int maxExposure = cameraParams.getMaxExposureCompensation();
        int minExposure = cameraParams.getMinExposureCompensation();
        if (cameraParams.isAutoExposureLockSupported()) {
            cameraParams.setAutoExposureLock(false);
        }
    }

    public static boolean setAutoExposureWhiteBalanceLock(boolean bLockExposure,
                                                          boolean bLockBalance) {
        if (mCamera == null) {
            return false;
        }
        try {
            Camera.Parameters cameraParams = mCamera.getParameters();
            boolean shouldUpdate = false;
            if (cameraParams.isAutoExposureLockSupported()
                    && cameraParams.getAutoExposureLock() != bLockExposure) {
                cameraParams.setAutoExposureLock(bLockExposure);
                shouldUpdate = true;
            }
            if (cameraParams.isAutoWhiteBalanceLockSupported()
                    && cameraParams.getAutoWhiteBalanceLock() != bLockBalance) {
                cameraParams.setAutoWhiteBalanceLock(bLockBalance);
                shouldUpdate = true;
            }
            if (shouldUpdate) {
                mCamera.setParameters(cameraParams);
                printCameraInfo(cameraParams, "setAutoExposureWhiteBalanceLock");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setAutoExposureLock: " + e.toString());
        }
        return true;
    }

    private static void resetExposureCompensationToDefault(Camera.Parameters cameraParams) {
        // Reset the exposure compensation before handing the camera to module.
        cameraParams.setExposureCompensation(0);
    }


    public static int getMaxZoom() {
        return maxZoom;
    }

    public static void setZooM(int progress) {
        if (mCamera == null || progress < 0) return;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setZoom((int) (progress * (maxZoom / 100f)) > maxZoom ? maxZoom : (int) (progress * (maxZoom / 100f)));
        mCamera.setParameters(parameters);
        printCameraInfo(parameters, "setZooM");
    }

    public static void setActivity(Activity act) {
        sAct = act;
    }

    public static void removeActivity(Activity act) {
        if (sAct == act) {//必须先判断
            sAct = null;
        }
    }

    private static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public static int getCameraDisplayOri(Activity activity, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getDisplayRotation(activity);
        int result;
        int orientation = ((Context) activity).getResources().getConfiguration().orientation;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            // compensate the mirror
            //result = (360 - result) % 360;
            result = result % 360;
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static final boolean sbUseFrameBuff = /*false*/true;

    public static void setPreviewCallback(final Camera.PreviewCallback cb) {
        if (mCamera == null) {
            return;
        }
        Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
            public boolean bPrintFrame = true;//前置在帧数大约在15~20，后置在10~13
            long mSec = 0;
            long mMil;
            int framCnt = 0;
            byte[] mdata;

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //long tm = SystemClock.elapsedRealtime();
                //data = new byte[data.length];
                //Log.d(TAG, "setPreviewCallback: onPreviewFrame: initMemorySize:" + buffer.length + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
                if (sbUseFrameBuff) {
                    camera.addCallbackBuffer(data);//采用帧缓存方式必须调用此接口------②
                }
                //camera.addCallbackBuffer(data/*new byte[data.length]*/);
                if (bPrintFrame) {
                    long elapsedRealtime = SystemClock.elapsedRealtime();
                    //Log.d(TAG, "onPreviewFrame: diff mMil:" + (elapsedRealtime - mMil));
                    mMil = elapsedRealtime;
                    long secTmp = elapsedRealtime / 1000;
                    if (mSec == 0) {
                        mSec = secTmp;
                    }
                    if (secTmp != mSec) {
                        String msg = "onPreviewFrame:  isBackCamera:" + isCameraIdBack() +
                                "  frame per sec:" + framCnt +
                                "  len:" + data.length +
                                "  elapsedRealtime:" + elapsedRealtime +
                                "  isSameData:" + (mdata == data);
                        if (mdata != data) {
                            mdata = data;
                        }
                        Log.e(TAG, msg);
                        framCnt = 0;
                        mSec = secTmp;
                    }
                    framCnt += 1;
                }
                if (mCamera == null) {
                    return;
                }
                sOnPreviewFrameData = data;
                if (sOnPreviewFrameListener != null) {
                    sOnPreviewFrameListener.onPreviewFrameListener(data);
                }

                if (cb != null) {
                    cb.onPreviewFrame(data, camera);
                }
            }
        };
        if (sbUseFrameBuff) {
            mCamera.setPreviewCallbackWithBuffer(previewCallback);//采用帧缓存方式必须调用此接口------①
        } else {
            mCamera.setPreviewCallback(previewCallback);
        }
    }

    public static void setOnPreviewFrameListener(OnPreviewFrameListener onPreviewFrameListener) {
        sOnPreviewFrameListener = onPreviewFrameListener;
    }

    public interface OnPreviewFrameListener {
        void onPreviewFrameListener(byte[] data);
    }

    public static byte[] getOnPreviewFrameData() {
        return sOnPreviewFrameData;
    }

    public static void clearOnPreviewFrameData() {
        sOnPreviewFrameData = null;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        int result = getRotationResult(activity, cameraId);
        camera.setDisplayOrientation(result);
        Log.d(TAG, "setCameraDisplayOrientation setDisplayOrientation: result=" + result);
        sCameraRotation = result;
    }

    public static int getRotationResult() {
        return getRotationResult(sAct, getCameraID());
    }

    public static int getRotationResult(Activity activity, int cameraId) {
        if (activity == null) {
            return 0;
        }
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        try {
            //此处相机驱动出现过导致崩溃的bug,估计与getNumberOfCameras接口错误地返回1有关
            Camera.getCameraInfo(cameraId, info);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "getRotationResult: sCameraRotation=" + sCameraRotation
                    + "  isCameraIdBack:" + isCameraIdBack()
                    + "  " + e.toString());
            return sCameraRotation;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.d(TAG, "getRotationResult: result=" + result + "  isCameraIdBack:" + isCameraIdBack());
        return result;
    }

    public static void setPreviewDisplay(SurfaceHolder holder, int orientation) {
        if (mCamera == null) return;
        try {
            Log.d(TAG, "setPreviewDisplay: before setCameraDisplayOrientation");
            long tm = System.currentTimeMillis();
            if (sAct != null) {
                setCameraDisplayOrientation(sAct, mCameraID, mCamera);
            } else {
                Log.d(TAG, "setPreviewDisplay: sAct=null");
            }
            mCamera.setPreviewDisplay(holder);
            printWidthHeight(holder);
            Log.d(TAG, "setPreviewDisplay: after setPreviewDisplay "
                    + (System.currentTimeMillis() - tm));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printWidthHeight(SurfaceHolder holder) {
        Surface s = holder.getSurface();

    }


    public static Camera.CameraInfo getCameraInfo() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, cameraInfo);
        return cameraInfo;
    }


    public static boolean isFlipHorizontal() {
        return CameraEngine.getCameraInfo().facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? true : false;
    }

    public static void setRotation(int rotation) {
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(rotation);
        mCamera.setParameters(params);
    }

    public static void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                                   Camera.PictureCallback jpegCallback) {
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public static Camera.Size getProperVideoSizes() {
        return sMaxVideoSize;
    }

    public static Camera.Size getSizePre() {
        if (mCamera == null || sSizePre == null) {
            return null;
        }
        return mCamera.new Size(sSizePre.width, sSizePre.height);
    }

    public static Camera.Size getSizePic() {
        return sSizePic;
    }

    public static Activity getSettedActivity() {
        return sAct;
    }

    public static Camera.FaceDetectionListener getFaceDetectionListener() {
        if (sFaceDetectionListener == null) {
            sFaceDetectionListener = new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                    Log.d(TAG, "onFaceDetection: ");
                    if (faces != null && faces.length > 0) {
                        EventBus.getDefault().post(faces);
                    }
                }
            };
        }
        return sFaceDetectionListener;
    }

    public static boolean isOrintationLand() {
        return getCameraRotation() % 180 == 0;
    }


    private static EmPreviewSizeMode.OnPreviewSizeListener sPreviewParameterListener;

    public static EmPreviewSizeMode.OnPreviewSizeListener getPreviewSizeListener() {
        return sPreviewParameterListener;
    }

    public static void setPreviewSizeListener(EmPreviewSizeMode.OnPreviewSizeListener previewParameterListener) {
        sPreviewParameterListener = previewParameterListener;
    }

    private static EmCameraMode.OnCameraModeListener sCameraModeListener;

    public static EmCameraMode.OnCameraModeListener getCameraModeListener() {
        return sCameraModeListener;
    }

    public static void setCameraModeListener(EmCameraMode.OnCameraModeListener cameraModeListener) {
        sCameraModeListener = cameraModeListener;
    }

    public static String getFocusMode() {
        try {
            return mCamera.getParameters().getFocusMode();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getFocusMode: " + e);
            return "";
        }
    }


    public static void printCameraInfo(Camera.Parameters cameraPara, String str) {
        if (cameraPara == null) {
            return;
        }
        try {
            List<Camera.Area> focusAreas = cameraPara.getFocusAreas();
            List<Camera.Area> meteringAreas = cameraPara.getMeteringAreas();
            Log.i(TAG, str + "  printCameraInfo: "
                    + "  FocusMode:" + cameraPara.getFocusMode()
                    + "  FocusAreas:" + (focusAreas != null && focusAreas.size() > 0 ? focusAreas.get(0).rect : "null")
                    + "  meteringAreas:" + (meteringAreas != null && meteringAreas.size() > 0 ? meteringAreas.get(0).rect : "null")
                    + "  SceneMode:" + cameraPara.getSceneMode()
                    + "  isAutoWhiteBalanceLocked:" + cameraPara.getAutoWhiteBalanceLock()
                    + "  WhiteBalance:" + cameraPara.getWhiteBalance()
                    + "  isAutoExposureLocked:" + cameraPara.getAutoExposureLock()
                    + "  ExposureCompensationIndex:" + cameraPara.getExposureCompensation()
                    + "  MaxNumFocusAreas:" + cameraPara.getMaxNumFocusAreas()
                    + "  MaxNumMeteringAreas:" + cameraPara.getMaxNumMeteringAreas()
                    + "  MaxZoom:" + cameraPara.getMaxZoom()
                    + "  MaxExposureCompensation:" + cameraPara.getMaxExposureCompensation()
            );
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "printCameraInfo: " + e);
        }
    }

    public static enum EventFocusMeasure {
        FOCUS_ONLY,
        MEASURE_ONLY,
        FOCUS_MEASURE,
        MEASURE_FULL;
    }


    public static boolean isSupportVideoSize(int frameWidth, int frameHeight) {
        if (mCamera == null) {
            return false;
        }
        boolean ret = false;
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedVideoSizes();
        for (Camera.Size s : sizes) {
            if (s.width == frameWidth && s.height == frameHeight) {
                ret = true;
                break;
            }
        }
        Log.d(TAG, "isSupportVideoSize: " + frameHeight + "*" + frameHeight + "  " + ret);
        return ret;
    }


    public static boolean isSupportPreviewSize(int width, int height) {
        if (mCamera == null) {
            return false;
        }
        boolean ret = false;
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        for (Camera.Size s : sizes) {
            if (s.width == width && s.height == height) {
                ret = true;
                break;
            }
        }
        Log.d(TAG, "isSupportPreviewSize: " + width + "*" + height + "  " + ret);
        return ret;
    }


    private static boolean isSupportSize(List<Camera.Size> sizes, int width, int height) {
        if (sizes == null) {
            return false;
        }
        boolean ret = false;
        for (Camera.Size s : sizes) {
            if (s.width == width && s.height == height) {
                ret = true;
                break;
            }
        }
        Log.d(TAG, "isSupportSize: " + height + "*" + height + "  " + ret);
        return ret;
    }

    public static boolean isSupportFrontVideo720_960() {
        return sbSupportFrontVideo960_720;
    }

    public static boolean isSupportBackVideo1440_1080() {
        return sbSupportBackVideo1440_1080;
    }
}
