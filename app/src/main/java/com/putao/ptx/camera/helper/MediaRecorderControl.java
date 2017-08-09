package com.putao.ptx.camera.helper;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.putao.ptx.camera.util.FileUtils;
import com.putao.ptx.camera.util.StringFormatUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2016/3/22.
 */
public class MediaRecorderControl {
    private static CamcorderProfile sCamcorderProfileBack;
    private static CamcorderProfile sCamcorderProfileFront;
    private static final String TAG = MediaRecorderControl.class.getSimpleName();
    private static MediaRecorderControl mediaRecorderControl = null;
    private OnSetVideoFrameListener mFrameSizeListener;
    private final static int MIN_TIME_FOR_SOUND_PIC = 1000/*2100*/;

    public boolean isVoiceRecording() {
        return isVoiceRecording;
    }

    private boolean isVoiceRecording = false;
    private MediaRecorder voiceRecorder;

    public boolean isVideoRecording() {
        return isVideoRecording;
    }

    private boolean isVideoRecording = false;
    private MediaRecorder videoRecorder;
    private String voicePath = "";
    private String videoPath;
    private String videoName;
    private boolean isVideoLaunching = false;
    private boolean isVoiceLaunching = false;
    private long videoStartTime = 0;
    private long voiceStartTime = 0;

    private MediaRecorderControl() {

    }

    public static synchronized MediaRecorderControl getInstance() {
        if (mediaRecorderControl == null) {
            mediaRecorderControl = new MediaRecorderControl();
        }
        return mediaRecorderControl;
    }

    /**
     * 耗时30~70ms
     *
     * @param name
     */
    public void startVoiceRecord(String name) {
        if (isVoiceRecording) {
            Log.d(TAG, "startVoiceRecord: 正在录音 isVoiceRecording:" + isVoiceRecording);
            return;
        }
        long tm = SystemClock.elapsedRealtime();
        long tm2 = 0;
        String filePath = FileUtils.getVoicePath() + name + ".aac";
        voicePath = filePath;
        if (voiceRecorder == null) {
            try {
                isVoiceRecording = true;
                isVoiceLaunching = true;
                voiceRecorder = new MediaRecorder();//耗时占用90%以上30+ms
                tm2 = SystemClock.elapsedRealtime();
                voiceRecorder.setAudioSamplingRate(44100);
                voiceRecorder.setAudioEncodingBitRate(256000);
                voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                voiceRecorder.setOutputFile(filePath);
                voiceRecorder.prepare();
                voiceRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
                isVoiceRecording = false;
                voiceRecorder = null;
                isVoiceLaunching = false;
                Log.d(TAG, "startVoiceRecord: 录音失败 " + e.toString());
            }
        }
        isVoiceLaunching = false;
        voiceStartTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "startVoiceRecord: waste time:" + (SystemClock.elapsedRealtime() - tm)
                + "  MediaRecorder:" + (SystemClock.elapsedRealtime() - tm2));
    }

    //保证视频的最短时间为2S
    public boolean isTooShortVoice() {
        if (isVoiceLaunching
                || SystemClock.elapsedRealtime() - voiceStartTime < MIN_TIME_FOR_SOUND_PIC)
            return true;
        return false;
    }

    public String stopVoiceRecord() {
        if (voiceRecorder == null) return "";
        try {
            voiceRecorder.stop();//此处MediaRecorder.stop(Native Method)出现过意外的崩溃异常
            voiceRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "stopVoiceRecord: " + e.toString());
        } finally {
            voiceRecorder = null;
            isVoiceRecording = false;
        }
        return voicePath;
    }

    public void startVideoRecord(Camera camera, int orientation, OnSetVideoFrameListener frameListener) {
        startVideoRecord(camera, orientation, null, frameListener);
    }

    public void startVideoRecord(Camera camera, int orientation, String path, OnSetVideoFrameListener frameListener) {
        if (camera == null) {
            return;
        }
        if (isVideoRecording) {
            return;
        }
        long tm = SystemClock.elapsedRealtime();
        isVideoRecording = true;
        isVideoLaunching = true;
        if (TextUtils.isEmpty(path)) {
            SimpleDateFormat format = StringFormatUtil.sformat;//new SimpleDateFormat("yyyyMMdd_HHmmss");
            videoName = "VID_" + format.format(new Date(System.currentTimeMillis()));
            videoPath = FileUtils.getVideoPath() + videoName + ".mp4";
        } else {
            videoPath = path;
        }
        if (videoRecorder == null) {
            CameraEngine.enableZsd(false);
            videoRecorder = new MediaRecorder();
            Log.d(TAG, "startVideoRecord: new MediaRecorder waste time:"
                    + (SystemClock.elapsedRealtime() - tm));
            camera.unlock();
            videoRecorder.setCamera(camera);
            videoRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            Camera.Size size = CameraEngine.getProperVideoSizes();
            if (size != null) {
                //videoRecorder.setVideoSize(size.width, size.height);//存在IllegalStateException异常
            }
            int degrees = 90;
            int cameraDegree = CameraEngine.getCameraRotation();
            boolean bBackCamera = CameraEngine.getCameraID() == Camera.CameraInfo.CAMERA_FACING_BACK;
            CamcorderProfile profile;//= CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            //有些相机不允许重复调用CamcorderProfile.get（int）方法
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (bBackCamera) {
                    //videoRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                    //videoRecorder.setOrientationHint(90);
                    degrees = 90;
                    profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                } else {
                    //videoRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                    //videoRecorder.setOrientationHint(270);
                    degrees = 270;
                    profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P/*QUALITY_480P*/);
                }
                if (cameraDegree == 270) {
                    degrees += 180;
                    degrees %= 360;
                }

                //videoRecorder.setOrientationHint(degrees);
            } else {
                if (bBackCamera) {
                    //videoRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                    //videoRecorder.setOrientationHint(0);
                    degrees = 0;
                    profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                } else {
                    //videoRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                    //videoRecorder.setOrientationHint(0);
                    degrees = 0;
                    profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P/*QUALITY_480P*/);
                }

                if (cameraDegree == 180) {
                    degrees += 180;
                    degrees %= 360;
                }
                //videoRecorder.setOrientationHint(degrees);
            }
            profile = getCamcorderProfile(bBackCamera);//TODO
            if (profile.quality >= 5) {
                profile.quality -= 3;
            }
            videoRecorder.setProfile(profile);
            videoRecorder.setMaxDuration(Integer.MAX_VALUE);//设置录制时间为无限大
            try {
                //设置录制文件为足够大,实际上一般Android文件的极限大小为4G
                videoRecorder.setMaxFileSize(1024 * 1024 * 1024 * 1024);
            } catch (RuntimeException e) {
                // We are going to ignore failure of setMaxFileSize here, as
                // a) The composer selected may simply not support it, or
                // b) The underlying media framework may not handle 64-bit range
                // on the size restriction.
                e.printStackTrace();
                Log.e(TAG, "startVideoRecord: " + e);
            }
            videoRecorder.setOrientationHint(degrees);
            if (frameListener != null) {
                mFrameSizeListener = frameListener;
                //mFrameSizeListener.onSetVideoFrameListener(profile.videoFrameWidth, profile.videoFrameHeight);
            }


            //自定义参数
//            CamcorderProfile profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_FRONT, CamcorderProfile.QUALITY_LOW);
//            profile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
//            profile.videoCodec = MediaRecorder.VideoEncoder.MPEG_4_SP;
//            profile.videoFrameHeight = 240;
//            profile.videoFrameWidth = 320;
//            profile.videoBitRate = 24;
//            videoRecorder.setPreviewDisplay(sv);
            videoRecorder.setOutputFile(videoPath);
            try {
                videoRecorder.prepare();
                videoRecorder.start();
                videoRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                    @Override
                    public void onError(MediaRecorder mr, int what, int extra) {
                        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
                        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
                            // We may have run out of space on the sdcard.
                            Log.d(TAG, "onError: 视频录制错误！");
                        }
                    }
                });
                videoRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                    @Override
                    public void onInfo(MediaRecorder mr, int what, int extra) {
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                            Log.d(TAG, "onInfo: 视频文件已经达到指定时间！");
                        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                            Log.d(TAG, "onInfo: 视频文件已经达到指定大小！");
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "startVideoRecord: " + e);
                try {
                    videoRecorder.reset();
                    videoRecorder.release();
                    CameraEngine.enableZsd(true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    Log.e(TAG, "startVideoRecord: " + e1);
                }
                isVideoRecording = false;
                videoRecorder = null;
                isVideoLaunching = false;
            }
        }
        videoStartTime = System.currentTimeMillis();
        isVideoLaunching = false;
        Log.d(TAG, "startVideoRecord: waste time:" + (SystemClock.elapsedRealtime() - tm));
    }


    //保证视频的最短时间为2S
    public boolean isTooShortVideo() {
        if (isVideoLaunching || System.currentTimeMillis() - videoStartTime < 2100)
            return true;
        return false;
    }

    public String stopVideoRecord(Context context, MediaScannerConnection.OnScanCompletedListener listener) {
        if (videoRecorder == null) return "";
        try {
            videoRecorder.stop();
            if (mFrameSizeListener != null) {
                //mFrameSizeListener.onReset();
            }

            videoRecorder.release();
            CameraEngine.enableZsd(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        videoRecorder = null;
        isVideoRecording = false;
        MediaScannerConnection.scanFile(context, new String[]{videoPath}, new String[]{"video/mp4"}, listener);
        return videoPath;
    }


    public static interface OnSetVideoFrameListener {
        /**
         * 预览控件的尺寸是否改变
         *
         * @param width
         * @param height
         * @return
         */
        boolean onSetVideoFrameListener(int width, int height);

        /**
         * 预览控件的尺寸是否改变
         *
         * @return
         */
        boolean onReset();
    }

    public static CamcorderProfile getCamcorderProfile(boolean bBack) {
        if (bBack) {
            //if (sCamcorderProfileBack == null) {
            sCamcorderProfileBack = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            if (CameraEngine.isSupportBackVideo1440_1080()) {
                sCamcorderProfileBack.videoFrameWidth = 1440;
                sCamcorderProfileBack.videoFrameHeight = 1080;
            }
//                sCamcorderProfileBack.videoFrameWidth = 1680;
//                sCamcorderProfileBack.videoFrameHeight = 1240;
            //}
            return sCamcorderProfileBack;
        } else {
            //if (sCamcorderProfileFront == null) {
            sCamcorderProfileFront = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            if (CameraEngine.isSupportFrontVideo720_960()) {
                sCamcorderProfileFront.videoFrameWidth = 960;
                sCamcorderProfileFront.videoFrameHeight = 720;
            }
//                sCamcorderProfileFront.videoFrameWidth = 1440;
//                sCamcorderProfileFront.videoFrameHeight = 1080;
            //}
            return sCamcorderProfileFront;
        }
    }

    public String getVideoPath() {
        return videoPath;
    }


    public static void printVideoSize() {
        List<CamcorderProfile> lists = new ArrayList<>();
        List<CamcorderProfile> listTimes = new ArrayList<>();
        List<CamcorderProfile> listSpeed = new ArrayList<>();

        Log.d(TAG, "printVideoSize from CamcorderProfile.get(int):   isCameraIdBack:" + CameraEngine.isCameraIdBack() +
                "********************************************************* ");
        for (int i = CamcorderProfile.QUALITY_LOW; i < CamcorderProfile.QUALITY_2160P; i++) {
            if (CamcorderProfile.hasProfile(i)) {
                try {
                    CamcorderProfile object = CamcorderProfile.get(i);
                    lists.add(object);
                    if (object != null) {
                        Log.d(TAG, "printVideoSize:   QUALITY:" + object.videoFrameWidth + "*" + object.videoFrameHeight + "  w/h:" + (object.videoFrameWidth * 1.0f / object.videoFrameHeight));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "printVideoSize: " + e);
                }
            }
        }
        for (int i = CamcorderProfile.QUALITY_TIME_LAPSE_LOW; i < CamcorderProfile.QUALITY_TIME_LAPSE_2160P; i++) {
            if (CamcorderProfile.hasProfile(i)) {
                try {
                    CamcorderProfile object = CamcorderProfile.get(i);
                    listTimes.add(object);
                    if (object != null) {
                        Log.d(TAG, "printVideoSize:   QUALITY_TIME_LAPSE:" + object.videoFrameWidth + "*" + object.videoFrameHeight + "  w/h:" + (object.videoFrameWidth * 1.0f / object.videoFrameHeight));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "printVideoSize: " + e);
                }
            }
        }
        for (int i = CamcorderProfile.QUALITY_HIGH_SPEED_LOW; i < CamcorderProfile.QUALITY_HIGH_SPEED_2160P; i++) {
            if (CamcorderProfile.hasProfile(i)) {
                try {
                    CamcorderProfile object = CamcorderProfile.get(i);
                    listSpeed.add(object);
                    if (object != null) {
                        Log.d(TAG, "printVideoSize:   QUALITY_HIGH_SPEED:" + object.videoFrameWidth + "*" + object.videoFrameHeight + "  w/h:" + (object.videoFrameWidth * 1.0f / object.videoFrameHeight));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "printVideoSize: " + e);
                }
            }
        }
        Log.d(TAG, "printVideoSize from CamcorderProfile: ********************************************************* ");
    }
}
