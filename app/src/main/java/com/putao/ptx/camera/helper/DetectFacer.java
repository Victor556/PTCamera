package com.putao.ptx.camera.helper;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;

import com.putao.ptx.camera.eventbus.FaceDetectEvent;
import com.putao.ptx.camera.model.FaceModel;
import com.sunnybear.library.util.DensityUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mobile.ReadFace.YMFace;
import mobile.ReadFace.YMFaceTrack;

/**
 * Created by Administrator on 2016/5/15.
 */
public class DetectFacer {
    private final String TAG = DetectFacer.class.getSimpleName();
    private Context context;
    private int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    public YMFaceTrack detector;
    private float mainRadio = 0;
    private float mainRadioY = 0;
    private int screenW, screenH, iw, ih;
    //    private boolean detect = true;
    private OnIsShouldDetectFaceListener mOnIsShouldDetectFaceListener;
    private boolean isDetecting = false;
    private ExecutorService threadExecutor;
    private ExecutorService saveExecutor;
    private long interval;

    private Map<Integer, YMFaceTrack> mMapDetector = new HashMap<>();
    private long mTmLastDetectedFace;
    private Camera.PreviewCallback mPanoFrameCallback;

//    public void startDetect() {
//        detect = true;
//    }
//
//    public void stopDetect() {
//        detect = false;
//    }

    public boolean isDetect() {
        //return detect;
        return mOnIsShouldDetectFaceListener == null
                || mOnIsShouldDetectFaceListener.isShouldDetectFace();
    }

    public void setOnIsShouldDetectFaceListener(OnIsShouldDetectFaceListener onIsShouldDetectFaceListener) {
        mOnIsShouldDetectFaceListener = onIsShouldDetectFaceListener;
    }

    public interface OnIsShouldDetectFaceListener {
        boolean isShouldDetectFace();
    }

    private List<YMFace> faces = new ArrayList<YMFace>();

    private long mtmDetecting;
    public Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {//25 time per sec
//            if (SystemClock.elapsedRealtime() - interval > SeatManager.intervalTime/*1500 */
//                    && takePanorama && PanoramaUtil.getStatus() == PanoramaUtil.EmPanoStatus.TAKING) {/*底座22°每秒，转动100度*/
//                DetectFacer.this.onPreviewFrame(data, camera);
//            }

            if (mPanoFrameCallback != null) {
                mPanoFrameCallback.onPreviewFrame(data, camera);
            }

            if (!isDetect()/*detect*/) {
                FaceDetectEvent event = new FaceDetectEvent();
                event.faces = new ArrayList<>();
                //EventBus.getDefault().post(event);
                return;
            }
            try {
                if (data == null || detector == null) return;
                if (mainRadio == 0 || mainRadioY == 0) {
                    iw = camera.getParameters().getPreviewSize().width;
                    ih = camera.getParameters().getPreviewSize().height;
                    mainRadio = (float) screenW / (float) ih;
                    mainRadioY = (float) screenH / (float) iw;
                }
                long timeInterval = SystemClock.elapsedRealtime() - mtmDetecting;
                if (isDetecting() && timeInterval < 5000 || timeInterval < 400) {
                    return;
                }

                if ((SystemClock.elapsedRealtime() - mTmLastDetectedFace) > 1000 && timeInterval < 1000) {
                    return;//降低CUP 能耗 如果前一秒钟之内没有检测到人脸，则间隔1秒钟再检测人脸
                }

                final YMFaceTrack tmpDetector = detector;
                isDetecting = true;
                mtmDetecting = SystemClock.elapsedRealtime();
                Runnable command = new Runnable() {
                    @Override
                    public void run() {
                        doDetector(tmpDetector, data);
                    }
                };
                threadExecutor.execute(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void setPanoFrameCallback(Camera.PreviewCallback panoFrameCallback) {
        mPanoFrameCallback = panoFrameCallback;
    }

    private synchronized void doDetector(YMFaceTrack tmpDetector, byte[] data) {
        if (tmpDetector == null || data == null) {
            return;
        }
        synchronized (tmpDetector) {
            isDetecting = true;
            faces.clear();
            long tm = SystemClock.elapsedRealtime();
            if (tmpDetector != null && tmpDetector == detector) {
                //faces.add(tmpDetector.onDetector(data, iw, ih));
//                List<YMFace> ymFaces = tmpDetector.onDetectorMultiFace(data, iw, ih);
                List<YMFace> ymFaces = tmpDetector.trackMulti(data, iw, ih);//耗时20~100不等，多数在20~40之间
                FaceDetectEvent event = new FaceDetectEvent();
                if (ymFaces != null && ymFaces.size() > 0 && isDetect()/*detect*/) {
                    faces.addAll(ymFaces);
                    event.faces = handlerFaceData(faces);
                    mTmLastDetectedFace = SystemClock.elapsedRealtime();
                }
                EventBus.getDefault().post(event);
                Log.d(TAG, "doDetector: waste time=" + (SystemClock.elapsedRealtime() - tm) + "  faces.size:" + faces.size());
            }
            //if (tmpDetector != null && tmpDetector != detector) {
            //tmpDetector.onRelease();
            //}
            isDetecting = false;
        }
    }


    public DetectFacer(Context context, int cameraID, int orientation) {
        this.context = context;
        this.cameraID = cameraID;
        init();
    }

    private void init() {
        update(cameraID);
        saveExecutor = Executors.newCachedThreadPool();
        threadExecutor = Executors.newSingleThreadExecutor();
        threadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("----thread detect face--");
            }
        });
    }

    /**
     * 耗时400+ms
     *
     * @param cameraID
     */
    public void update(int cameraID) {
        long tm = SystemClock.elapsedRealtime();
        //release();
        Log.d(TAG, "update: release(); waste time " + (SystemClock.elapsedRealtime() - tm));
        this.cameraID = cameraID;
        int orientation = getOrientation();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            screenW = DensityUtil.getDeviceWidth(context);
            screenH = DensityUtil.getDeviceHeight(context);
        } else {
            screenH = DensityUtil.getDeviceWidth(context);
            screenW = DensityUtil.getDeviceHeight(context);
        }
        try {
            tm = SystemClock.elapsedRealtime();
            int rotation = CameraEngine.getCameraRotation();
            int rotationFace = YMFaceTrack.FACE_270;
            {//实测此方法支持屏幕各种角度的笑脸检测
                if (rotation == 270) {
                    rotationFace = YMFaceTrack.FACE_90;
                    if (cameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        rotationFace = YMFaceTrack.FACE_270;//??
                    }
                } else if (rotation == 90) {
                    rotationFace = YMFaceTrack.FACE_270;
                    if (cameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        rotationFace = YMFaceTrack.FACE_90;//??
                    }
                } else {
                    rotationFace = rotation;
                }
                Log.d(TAG, "update: rotationFace:" + rotationFace);
            }

            detector = mMapDetector.get(rotationFace);
            if (detector == null) {
                detector = new YMFaceTrack();
                Log.d(TAG, "update: befor yuemian 初始化阅面 YMFaceTrack.initTrack");
                detector.initTrack(context, rotationFace, YMFaceTrack.RESIZE_WIDTH_640);
                detector.setRecognitionConfidence(40);
                mMapDetector.put(rotationFace, detector);
                Log.d(TAG, "update: new YMDetector(); waste time " + (SystemClock.elapsedRealtime() - tm)
                        + "  rotationFace:" + rotationFace + "  rotation:" + rotation);
            }
            // detector = new YMDetector(context, CameraEngine.getCameraRotation()/*YMDetector.Config.FACE_0*/, YMDetector.Config.RESIZE_WIDTH_640);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mainRadio = 0;
        mainRadioY = 0;
    }

    public void release() {
        if (detector != null) {
            synchronized (detector) {
                if (detector != null) {
                    if (mMapDetector != null && mMapDetector.size() > 0) {
                        detector.onRelease();//实际上此句根本执行不到
                        detector = null;
                    }
                }
            }
        }
    }

    private void release(YMFaceTrack detector) {
        if (detector != null) {
            synchronized (detector) {
                detector.onRelease();
            }
        }
    }

    public void releaseAll() {
        for (Map.Entry<Integer, YMFaceTrack> entry : mMapDetector.entrySet()) {
            if (entry != null) {
                release(entry.getValue());
            }
        }
        mMapDetector.clear();
        detector = null;
    }

    public boolean isDetecting() {
        return isDetecting;
    }

    private List<FaceModel> handlerFaceData(List<YMFace> faces) {
        List<FaceModel> faceModels = new ArrayList<>();
        if (faces == null) {
            //faceModels.add(null);
            return faceModels;
        }

        int orientation = getOrientation();
        boolean bPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        Log.d(TAG, "handlerFaceData:   bPortrait:" + bPortrait);
        for (YMFace face : faces) {
            if (face != null) {
                Log.d(TAG, "handlerFaceData:  face.getEmotions="
                        + (face.getEmotions() != null ? face.getEmotions()[0] + "" : "null"));
                FaceModel faceModel = null;
                float[] landmarks = face.getLandmarks();
                float[] emotions = face.getEmotions();
                float[] rects = face.getRect();
                float[] points = new float[landmarks.length];

                if (bPortrait) {
                    for (int i = 0; i < landmarks.length / 2; i++) {
                        float x = landmarks[i * 2] * mainRadio;
                        x = (cameraID == Camera.CameraInfo.CAMERA_FACING_FRONT ? screenW - x : x);
                        float y = landmarks[i * 2 + 1] * mainRadioY;
                        points[i * 2] = x;
                        points[i * 2 + 1] = y;
                    }
                    faceModel = new FaceModel();
                    faceModel.landmarks = points;
                    faceModel.emotions = emotions;

                    float x1 = rects[0] * mainRadio;
                    float y1 = rects[1] * mainRadioY;
                    RectF faceRect;
//                                RectF faceRect = new RectF(x1, y1, x1 + rects[2] * mainRadio, y1 + rects[3] * mainRadioY);
                    if (cameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        float left = (float) screenW - (x1 + rects[2] * mainRadio);
                        float right = (float) screenW - x1;
                        faceRect = new RectF(left, y1, right, y1 + rects[3] * mainRadioY);
                    } else {
                        faceRect = new RectF(x1, y1, x1 + rects[2] * mainRadio, y1 + rects[3] * mainRadioY);
                    }

                    faceModel.rectf = faceRect;
                } else {
                    for (int i = 0; i < landmarks.length / 2; i++) {
                        float x = landmarks[i * 2] * mainRadio;
                        x = (cameraID == Camera.CameraInfo.CAMERA_FACING_FRONT ? screenH - x : x);
                        float y = landmarks[i * 2 + 1] * mainRadioY;
                        points[i * 2] = x;
                        points[i * 2 + 1] = y;
                    }
                    faceModel = new FaceModel();
                    faceModel.landmarks = points;
                    faceModel.emotions = emotions;
                    RectF rectf;
                    float x1 = rects[0] * mainRadio;
                    float y1 = rects[1] * mainRadioY;
                    if (cameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        float tempLeft = x1;
                        float top = y1;
                        float temprigth = (x1 + rects[2] * mainRadio);
                        float bottom = (y1 + rects[3] * mainRadioY);
                        float left = screenH - temprigth;
                        float rigth = screenH - tempLeft;
                        rectf = new RectF(left, top, rigth, bottom);
                    } else {
                        rectf = new RectF(x1, y1, x1 + rects[2] * mainRadio, y1 + rects[3] * mainRadioY);
                    }
                    faceModel.rectf = rectf;
                }
                faceModels.add(faceModel);
            }
        }
        return faceModels;
    }

    private int getOrientation() {
        int rotation = CameraEngine.getCameraRotation();
        return (rotation == 0 || rotation == 180) ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT;//context.getResources().getConfiguration().orientation;
    }
}
