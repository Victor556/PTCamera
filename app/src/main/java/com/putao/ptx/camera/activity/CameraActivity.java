package com.putao.ptx.camera.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SoundEffectConstants;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.camera.exif.ExifInterface;
import com.putao.ptx.accountcenter.Account;
import com.putao.ptx.camera.PTUIApplication;
import com.putao.ptx.camera.R;
import com.putao.ptx.camera.eventbus.EventLastMediaFile;
import com.putao.ptx.camera.eventbus.EventList;
import com.putao.ptx.camera.eventbus.EventUpdateAlbum;
import com.putao.ptx.camera.eventbus.FaceDetectEvent;
import com.putao.ptx.camera.eventbus.MoveSensorEvent;
import com.putao.ptx.camera.eventbus.PanorPicComEvent;
import com.putao.ptx.camera.eventbus.PanorPicEvent;
import com.putao.ptx.camera.helper.AccelerSensorManager;
import com.putao.ptx.camera.helper.CameraEngine;
import com.putao.ptx.camera.helper.DetectFacer;
import com.putao.ptx.camera.helper.DockingBroadcast;
import com.putao.ptx.camera.helper.MediaRecorderControl;
import com.putao.ptx.camera.helper.SeatManager;
import com.putao.ptx.camera.helper.SpeechHelper;
import com.putao.ptx.camera.model.AudioPlay;
import com.putao.ptx.camera.model.EmCameraMode;
import com.putao.ptx.camera.model.EmPreviewSizeMode;
import com.putao.ptx.camera.model.EmShareCnt;
import com.putao.ptx.camera.model.FaceModel;
import com.putao.ptx.camera.model.MediaInfo;
import com.putao.ptx.camera.util.AnimUtil;
import com.putao.ptx.camera.util.BitmapToVideoUtil;
import com.putao.ptx.camera.util.BitmapUtil;
import com.putao.ptx.camera.util.CommonUtils;
import com.putao.ptx.camera.util.CustomToastUtils;
import com.putao.ptx.camera.util.FastBlur;
import com.putao.ptx.camera.util.FileUtils;
import com.putao.ptx.camera.util.MediaUtil;
import com.putao.ptx.camera.util.PanoDialog;
import com.putao.ptx.camera.util.PanoramaUtil;
import com.putao.ptx.camera.util.SaveTask;
import com.putao.ptx.camera.util.StringFormatUtil;
import com.putao.ptx.camera.util.TimerCountUtil;
import com.putao.ptx.camera.view.CameraDrawView;
import com.putao.ptx.camera.view.CameraSurfaceView;
import com.putao.ptx.camera.view.CameraTextureZoomListener;
import com.putao.ptx.camera.view.CameraView;
import com.putao.ptx.camera.view.CircleImageView;
import com.putao.ptx.camera.view.CircleMenuLayout;
import com.putao.ptx.camera.view.MenuView;
import com.putao.ptx.camera.view.NonUiThread;
import com.putao.ptx.image.service.IImageProcessService;
import com.sunnybear.library.util.DensityUtil;
import com.sunnybear.library.util.ImageUtils;
import com.sunnybear.library.util.PhoneUtil;
import com.sunnybear.library.util.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

public class CameraActivity extends /*BasicFragmentActivity*/LocationActivity implements View.OnClickListener {
    public static final String QUANJIAN = "quanjian";
    public static final int IPTYPE_NORMAL = 0;//普通 //ip精灵调用，0：拍摄普通照片，1：拍摄全景
    public static final int IPTYPE_PAN = 1;//全景 //ip精灵调用，0：拍摄普通照片，1：拍摄全景
    public static final int LOW_BATTERY_THRESHOLD_FOR_DOCKING = 10;
    public static final String ACTION_PAI_CAMERA = "android.media.action.PAI_CAMERA";
    public static final int MIN_LAST_MEDIA_ID = 1;
    public static Activity instance;
    private final int ACTION_NO_MODE = 50;
    private final int ACTION_IMAGE_MODE = 51;
    private final int ACTION_VIDEO_MODE = 52;

    private final int VOICE_MODE = 0;
    private final int COMMON_MODE = 1;
    private final int VIDEO_MODE = 2;
    private final int PANORAMA_MODE = 3;
    private boolean mbDelayInit = false;
    private Runnable mRunGoneBlackView;
    /**
     * 是否显示电池电量低的对话框<br/>
     * 现在去掉电量低的对话框提醒
     */
    private boolean mbShowBattery = false;
    private PanoDialog mDlgLowStorage;
    private Runnable mRunOnMoveDevice;
    private int mSecurityLastMediaId = MIN_LAST_MEDIA_ID;
    private boolean mbResuming = false;
    private boolean mbVisible = false;
    private boolean mbPreviousSecure = false;

    private Runnable mRunnable;
    private AudioPlay mAudioPlay;
    private Runnable mAction;

    private final int DEFAULT_CONTROL_MODE = 100;
    private final int COUNT_CONTROL_MODE = 98;
    private final int SMAIL_CONTROL_MODE = 97;

    public final int rightWidth = 102;
    private Context mContext;
    final private String TAG = CameraActivity.class.getSimpleName();
    final private String TAG_TAKEPIC = "taking_picture";
    final private String TAG_SMILE = "TAG_SMILE";
    @Bind(R.id.surfaceView)
    CameraSurfaceView cameraSurfaceView;
    @Bind(R.id.blackView)
    View blackView;
    @Bind(R.id.blur)
    ImageView blur;
    @Bind(R.id.pbPanPic)
    SeekBar pbPanPic;
    @Bind(R.id.rlMainlayout)
    RelativeLayout rlMainlayout;
    @Bind(R.id.cameraView)
    CameraView cameraView;
    @Bind(R.id.ivSwitchCamera)
    ImageView ivSwitchCamera;
    @Bind(R.id.ivSmailControl)
    ImageView ivSmailControl;
    @Bind(R.id.tvTime)
    /*TextView*/ MenuView tvTime;
    @Bind(R.id.tvDownCount)
    TextView tvDownCount;
    @Bind(R.id.sbZoom)
    SeekBar sbZoom;
    @Bind(R.id.ivAlbum)
    CircleImageView ivAlbum;
    @Bind(R.id.controlMenu)
    CircleMenuLayout controlMenu;
    @Bind(R.id.rootView)
    ViewGroup rootView;

    @Bind(R.id.focusView)
    ImageView focusView;

    @Bind(R.id.sleep)
    EditText sleep;
    @Bind(R.id.speed)
    EditText speed;
    @Bind(R.id.acc)
    TextView mAcc;

    private CameraDrawView sdvDrawView;
    private ImageView ivVoicePic;
    private boolean mbShowVoicePic = false;

    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private AccelerSensorManager accelerSensor = null;
    private SeatManager seatManager;
    private Configuration configuration;
    private long foIntTime = SystemClock.elapsedRealtime();//识别到人脸自动聚焦时间
    private float[] lastPoint = {0, 0}; //人脸的中心点Apli

    private boolean downCounting = false;//是否在倒计时拍照中
    private int cameraMode = COMMON_MODE; //当前相机使用模式
    private int cameraModeOld = COMMON_MODE; //旧的相机模式
    private int controlMode = DEFAULT_CONTROL_MODE;//当前拍照控制模式
    private String picName = "";
    private DetectFacer detectFacer;
    private boolean videoRecording = false;
    private boolean voiceRecording = false;
    private boolean mbPrepareVideoRecording = false;
    private boolean mbPrepareVoiceRecording = false;
    //private GoogleApiClient client;
    private boolean hasReturn = false;
    private int actionMode = ACTION_NO_MODE;
    private String outputPicPath;
    private String outputVideoPath;
    private String action;
    private boolean takePicing = false;
    private SimpleDateFormat format = StringFormatUtil.sformat;//new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
    private boolean isZoomBarTouch = false;
    private boolean firstSmailControl = true/*false*/;
    private boolean mbFirstSmilePic = true;
    private final float maxPartPan = SeatManager.picCnt;
    private final int maxProgress = SeatManager.PROGRESS_MAX;
    private int ipType = 0;//ip精灵调用，0：拍摄普通照片，1：拍摄全景
    private ServiceConnection mServiceConnection;//unused
    private IImageProcessService mImageProcessService;//unused
    private EventLastMediaFile mLastMediaFile = new EventLastMediaFile(null, null, true);
    private AudioManager mAudioManager;
    private DockingBroadcast mDockBroadcast;
    private BroadcastReceiver mRcvBatteryLevel;

    private DockingBroadcast.OnDockingLowBatteryListener mLowBatteryListener;
    private DockingBroadcast.OnDockingMotorStatusListener mMotorStatusListener;
    private DockingBroadcast.OnDockingPadConnectStatusListener mPadConnectStatusListener;
    private DockingBroadcast.OnDockingPowerStatusChangedListener mDockingPowerStatusChangedListener;
    private int mDockingBatteryLevel = 100;
    private Runnable mRunConfig;
    private OrientationEventListener mOrientationListener;
    private Runnable mRunUpdateRotation;
    private Runnable mRunDelayJumpAlbum;
    private CameraTextureZoomListener mCameraTextureZoomListener;
    private NonUiThread mCameraSwitchThread;
    private boolean mbSwitchingCamera = false;
    private boolean mbSwitchingCameraWithoutData = false;

    private final boolean bPutaoDev = PTUIApplication.isPutaoDev();
//    private LocationManager mLocationManager;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected String[] getRequestUrls() {
        return new String[0];
    }

    @Override
    protected void onScreenOffLockedSecure() {
        if (/*isKeyguardLockedSecure*/isKeyguardSecure()) {
            mSecurityLastMediaId = getLastMediaId();
            tryClearAlbum();
        }
    }

    protected void onViewCreatedFinish(Bundle saveInstanceState) {
        Log.d(TAG, "onViewCreatedFinish: 1");
        ButterKnife.bind(this);
        CameraEngine.setPreviewSizeListener(new EmPreviewSizeMode.OnPreviewSizeListener() {
            @Override
            public EmPreviewSizeMode getPreviewSizeMode() {
                EmPreviewSizeMode ret = EmPreviewSizeMode.PIC;
                if (cameraMode == VIDEO_MODE) {
                    ret = CameraEngine.isCameraIdBack() ? EmPreviewSizeMode.VIDEO_BACK
                            : EmPreviewSizeMode.VIDEO_FRONT;
                }
                return ret;
            }
        });
        CameraEngine.setCameraModeListener(new EmCameraMode.OnCameraModeListener() {
            @Override
            public EmCameraMode getCameraMode() {
                return EmCameraMode.getByIdx(cameraMode);
            }
        });
        cameraView.setFocusView(focusView);
        focusView.setVisibility(View.INVISIBLE);
        cameraView.setRootView(rootView);
        cameraView.setCameraView(cameraSurfaceView);
        cameraView.setFocusViewDisplayImpl(new CameraView.FocusViewDisplayImpl() {
            @Override
            public boolean isAllowShow() {
                return !(voiceRecording || seatManager.isSeatThreadAlive()
                        || SystemClock.elapsedRealtime() - mTmUpdateFace < 500
                        && curFaces != null && curFaces.size() > 0);
            }
        });
        mbPreviousSecure = isKeyguardLockedSecure();
        Log.d(TAG, "onViewCreatedFinish: 2");
        CameraEngine.setActivity(CameraActivity.this);
        blackView.setVisibility(View.GONE);
        ivAlbum.setBorderColor(Color.TRANSPARENT);
        mContext = this;
        if (instance != null && instance != this) {
            EventBus.getDefault().unregister(instance);
            instance.finish();
        }

        if (isSecureCamera()) {
            mSecurityLastMediaId = getLastMediaId();
        }
        instance = this;
        EventBus.getDefault().register(this);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        Log.d(TAG, "onViewCreatedFinish: 3");
        powerManager = (PowerManager) this.getSystemService(this.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "CameraLock");
        wakeLock.setReferenceCounted(false);
        //accelerSensor = new AccelerSensorManager(mContext);

        configuration = this.getResources().getConfiguration();

        sdvDrawView = cameraView.getSdvDrawView();
        final CameraDrawView.OnIsAllowDrawListener onIsAllowDrawListener = new CameraDrawView.OnIsAllowDrawListener() {
            @Override
            public boolean isAllowDraw() {
                return CameraActivity.this.isShouldDetectFace();
            }
        };
        sdvDrawView.setOnIsAllowDrawListener(onIsAllowDrawListener);
        sdvDrawView.setOnIsAllowDrawSmileListener(new CameraDrawView.OnIsAllowDrawSmileListener() {
            @Override
            public boolean isAllowDrawSmile() {
                return isShouldShowSmile();
            }
        });
        ivVoicePic = cameraView.getIvVoicePic();

        Log.d(TAG, "onViewCreatedFinish: 4");

        seatManager = new SeatManager(this);
        seatManager.setPanoramaPics(panoramaPics);
        seatManager.setOnProgressUpdateListener(new SeatManager.OnProgressUpdateListener() {
            private ValueAnimator mOba;

            @Override
            public void onProgressUpdate(final int cur, final long wastetime, final int max) {
                if (pbPanPic == null || !seatManager.isSeatThreadAlive()) {
                    return;
                }
                pbPanPic.post(new Runnable() {
                    @Override
                    public void run() {
                        int curTmp = cur;
                        if (curTmp >= max * 0.92) {
                            curTmp = max;
                        }
                        long wastetimeTmp = (long) (curTmp * 1.0 / cur * wastetime);
                        if (mOba == null) {
                            mOba = ValueAnimator.ofInt(0, curTmp);
                        }
                        mOba.setIntValues(pbPanPic.getProgress(), curTmp);
                        mOba.setDuration(wastetimeTmp);
                        pbPanPic.setMax(max);
                        mOba.removeAllUpdateListeners();
                        mOba.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator anim) {
                                Integer animatedValue = (Integer) anim.getAnimatedValue();
                                pbPanPic.setProgress(animatedValue);
                                Log.d(TAG, "onProgressUpdate: onAnimationUpdate: setProgress" + animatedValue);
                                //pbPanPic.setProgress(cur);
                            }
                        });
                        mOba.start();
                    }
                });
                Log.d(TAG, "onProgressUpdate: " +
                        "cur:" + cur + "  wastetime:" + wastetime + "  max:" + max);
            }
        });
        seatManager.setOnTakingPanoPicture(new SeatManager.OnTakingPanoPicture() {
            @Override
            public String onTakingPanoPicture(String path) {
                return takePic(path);
            }
        });
        seatManager.setVisibleCallback(new SeatManager.VisibleCallback() {
            @Override
            public boolean isVisible() {
                return mbVisible;
            }
        });
        detectFacer = new DetectFacer(mContext, cameraSurfaceView.getCameraID(), getResources().getConfiguration().orientation);
        detectFacer.setOnIsShouldDetectFaceListener(new DetectFacer.OnIsShouldDetectFaceListener() {
            @Override
            public boolean isShouldDetectFace() {
                return onIsAllowDrawListener.isAllowDraw();
            }
        });
        Log.d(TAG, "onViewCreatedFinish: 5");
        detectFacer.setPanoFrameCallback(seatManager.getPanoPreviewCallback());
        cameraSurfaceView.setPreviewCallback(detectFacer.previewCallback);
        mCameraTextureZoomListener = new CameraTextureZoomListener(mContext, new CameraTextureZoomListener.CameraTextureTouchCallBack() {

            @Override
            public void onZoom(int value) {
                if (voiceRecording) return;
                int progress = (int) ((float) value / cameraSurfaceView.getMaxZoom() * 100f);
                sbZoom.setVisibility(View.VISIBLE);
                handler.removeMessages(MSG_DELAY_DISMISS_ZOOM);
                handler.sendEmptyMessageDelayed(MSG_DELAY_DISMISS_ZOOM,
                        TIME_DELAY_DISMISS_ZOOM);
                sbZoom.setProgress(progress);
                cameraSurfaceView.setZooM(progress);
            }

            @Override
            public void onFocus(MotionEvent event) {
                if (event == null || controlMenu == null) {
                    return;
                }
                boolean ret = controlMenu.isTouchOnContent(event);
                if (ret) {
                    return;
                }
                cameraView.focusOnTouch(cameraSurfaceView.getCamera(), event);
                if (isVideoRecording()) {
                    cameraView.cancelReset();
                }
            }

            @Override
            public void onSwitchMode(boolean isUp) {
                if (isStateForbidSwitchMode()) {
                    return;
                }
                if (cameraMode == VOICE_MODE && takePicing) return;
                if (actionMode != ACTION_NO_MODE) return;
                if (isUp) {
                    controlMenu.switchUp();
                } else {
                    controlMenu.switchDown();
                }
                freshSmileMode();
                Log.d(TAG, "onSwitchMode: isUp:" + isUp);
            }

            @Override
            public void onJumpPicView() {
                jumpToAlbum();
            }

            @Override
            public void onActionUp() {
                handler.removeMessages(MSG_DELAY_DISMISS_ZOOM);
                handler.sendEmptyMessageDelayed(MSG_DELAY_DISMISS_ZOOM,
                        TIME_DELAY_DISMISS_ZOOM);
            }
        });
        cameraSurfaceView.setOnTouchListener(mCameraTextureZoomListener);

        sbZoom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isZoomBarTouch = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeMessages(MSG_DELAY_DISMISS_ZOOM);
                        handler.sendEmptyMessageDelayed(MSG_DELAY_DISMISS_ZOOM,
                                TIME_DELAY_DISMISS_ZOOM);
                        isZoomBarTouch = false;
                        break;
                }
                return false;
            }
        });

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cameraSurfaceView.setZooM(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        sbZoom.setOnSeekBarChangeListener(seekBarChangeListener);
        int deviceWidth = DensityUtil.getDeviceWidth(CameraActivity.this);
        int deviceHeight = DensityUtil.getDeviceHeight(CameraActivity.this);
        if (deviceWidth < deviceHeight && false) {
            RelativeLayout.LayoutParams para = (RelativeLayout.LayoutParams)
                    controlMenu.getLayoutParams();
            para.rightMargin = 0;//方便手机测试
            controlMenu.requestLayout();
        }
        //controlMenu.setAnimUpdateListener(getBlurAnimUpdateListener());
        //controlMenu.setAnimListener(getBlurAnimListener());
        controlMenu.setOnSwitchListener(getOnSwitchListener());
        controlMenu.setOnMenuItemClickListener(new CircleMenuLayout.OnMenuItemClickListener() {
            @Override
            public boolean itemClick(View view, int pos) {

                if (isStateForbidSwitchMode()) {
                    Log.d(TAG, "itemCenterClick: 禁止菜单切换");
                    return false;
                } else {
                    return true;
                }
            }

            long mTmLastClick;

            @Override
            public void itemCenterClick(View view) {
                if (isSwitchingCameraAnimRunnig()) {
                    return;
                }
                Log.d(TAG, "itemCenterClick: enter");
                stopSpeak();
                tryWakeLock();
                PhoneUtil.isFastDoubleClick();
                if (controlMenu.isMenuAnimRunning()) {
                    Log.d(TAG, "itemCenterClick: 菜单动画进行中！");
                    return;
                }
                if (cameraMode == VOICE_MODE) {
                    if (takePicing || isPrepareVoiceRecording()) {
                        Log.d(TAG, "itemCenterClick: 正在拍照或准备拍照！");
                        return;
                    }
                    if (voiceRecording) {
                        if (MediaRecorderControl.getInstance().isTooShortVoice() && !isOnStop()) {
                            Log.d(TAG, "itemCenterClick: 有声照片时间太短！");
                            return;
                        }
                        if (CameraEngine.getCamera() == null) {
                            return;
                        }
                        cameraView.setFocusEnable(true);
                        stopMediaCountTime();
                    } else {
                        if (SystemClock.elapsedRealtime() - mTmLastClick < 500) {
                            Log.d(TAG, "itemCenterClick: 拍摄有声照片间隔时间太短！");
                            return;
                        }

                        if (!isMemoryEnoughForPic()) {
                            Log.d(TAG, "itemCenterClick: 没有足够内存！");
                            return;
                        }
                        //正在处理拍照时禁止拍照
                        if (!cameraSurfaceView.isAllowTakePicForTime()) {
                            Log.d(TAG, "itemCenterClick: isAllowTakePicForTime（）正在拍照");
                            return;
                        }
                        if (!testSdcardMemoryWithDlg(10)) {
                            Log.d(TAG, "itemCenterClick: sd卡存储不足！");
                            return;
                        }
                        if (CameraEngine.getCamera() == null) {
                            return;
                        }

                        switch (controlMode) {
                            case DEFAULT_CONTROL_MODE:
                                takePic();
                                break;
                            case SMAIL_CONTROL_MODE:
                                takePic();
                                break;
                        }
                        //voiceRecording = true;
                    }
                    //sdvDrawView.setVisibility(View.INVISIBLE);
                    showDrawView(false);
                    freshSmileMode();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            freshSmileMode();
                        }
                    }, 1000);
                } else if (cameraMode == COMMON_MODE) {
                    if (!isMemoryEnoughForPic()) {
                        Log.d(TAG, "itemCenterClick: 内存不足！");
                        return;//内存不足就返回
                    }
                    //正在处理拍照时禁止拍照
                    if (!cameraSurfaceView.isAllowTakePicForTime()) {
                        Log.d(TAG, "itemCenterClick: 正在拍照！");
                        return;
                    }
                    if (!testSdcardMemoryWithDlg(10)) {
                        Log.d(TAG, "itemCenterClick: sd卡存储不足！");
                        return;
                    }

                    if (SystemClock.elapsedRealtime() - mTmLastClick < 300) {
                        Log.d(TAG, "itemCenterClick: 点击太快！");
                        return;
                    }


                    if (CameraEngine.getCamera() == null) {
                        return;
                    }

                    if (actionMode == ACTION_IMAGE_MODE && (!TextUtils.isEmpty(outputPicPath)
                            && (new File(outputPicPath)).exists()
                            || SaveTask.getTaskCnt() > 0 || cameraSurfaceView.isTakingPic())) {
                        Log.d(TAG, "itemCenterClick: 拍取照片模式！ 已经拍过照片了！");
                        return;
                    }
                    if (actionMode == ACTION_IMAGE_MODE) {
                        ivSwitchCamera.setEnabled(false);
                    }
                    showDrawView(true);
                    Log.d(TAG, "itemCenterClick: actionMode=" + actionMode);
                    //拍摄普通图片
                    switch (controlMode) {
                        case DEFAULT_CONTROL_MODE:
                            takePic();
                            break;
                        case SMAIL_CONTROL_MODE:
                            takePic();
                            break;
                    }
                } else if (cameraMode == VIDEO_MODE) {
                    //拍摄视频
                    if (videoRecording) {
                        if (MediaRecorderControl.getInstance().isTooShortVideo() && !isOnStop()) {
                            return;
                        }
                        stopMediaCountTime();
                    } else {
                        if (!testSdcardMemoryWithDlg(10)) {
                            Log.d(TAG, "itemCenterClick: sd卡存储不足！");
                            return;
                        }
                        if (!mbResuming) {
                            Log.d(TAG, "itemCenterClick: 屏幕已经熄灭！");
                            return;
                        }
                        startMediaCountTime(Integer.MAX_VALUE, cameraMode);
                    }
                } else {
                    onClickMenuPano();
                }
                mTmLastClick = SystemClock.elapsedRealtime();
            }
        });
        controlMenu.setOnTouchListener(mCameraTextureZoomListener);
        Log.d(TAG, "onViewCreatedFinish: 6");
        //initImageService();
        Log.d(TAG, "onViewCreatedFinish: 7");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        trySwitchToFrontCamera();
        Log.d(TAG, "onViewCreatedFinish: 8");
        initOrientationListener();
        blur.setScaleType(CameraSurfaceView.sbFillHeight ?
                ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
//        mLocationManager = new LocationManager(PTUIApplication.getInstance());
        Log.d(TAG, "onViewCreatedFinish: leave");
    }

    private void initImageService() {
        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mImageProcessService = IImageProcessService.Stub.asInterface(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        Intent intent = new Intent("com.putao.ptx.image.service.IMAGE_PROCESS");
        intent.setPackage("com.putao.ptx.image.panorama");

        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void uninitImageService() {
        unbindService(mServiceConnection);
    }


    private boolean isSwitchingCameraAnimRunnig() {
        boolean b = mCameraSwitchThread != null && !mCameraSwitchThread.isSwitchingFinished();
        return b;
    }

    /**
     * 当前状态是否禁止切换菜单
     *
     * @return
     */
    private boolean isStateForbidSwitchMode() {
        return videoRecording || voiceRecording || panoramaTaking
                || isTakeVoicePic
                || isPrepareVoiceRecording()
                || isPrepareVideoRecording()
                || controlMenu.isMenuAnimRunning()
                || TimerCountUtil.isStarted()
                || isSwitchingCameraAnimRunnig();
    }

    private boolean isBlurAnimUnderHalf() {
        boolean bRun = mAnimBlur != null && mAnimBlur.isRunning()
                && mAnimBlur.getAnimatedFraction() < 0.5f;
        Log.d(TAG, "isBlurAnimUnderHalf: " + bRun);
        return bRun;
    }

    private boolean onBlurAnimStart() {
        if (mAnimBlur != null) {
            mAnimBlur.end();
        }
        Bitmap blurBitmap = getBlurBitmap();
        boolean land = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        double ratio = 0;
        boolean ret = true;
        if (blurBitmap != null) {
            int width = blurBitmap.getWidth();
            int height = blurBitmap.getHeight();
            ratio = width * 1.0 / height;
            blur.setImageBitmap(blurBitmap);
            ret = false;
        }
        blur.invalidate();
        blur.setVisibility(View.VISIBLE);
        blur.setAlpha(1.0f);
        ViewGroup.LayoutParams para2 = (ViewGroup.LayoutParams) blur.getLayoutParams();
        Log.d(TAG, "onBlurAnimStart: blur land:" + land
                + "  w/h ratio:" + ratio + ":" + para2.width * 1.0f / para2.height
                + "  " + para2.width + "  " + para2.height
                + "  blur.Visibility:" + blur.getVisibility());
        return ret;
    }

    private void trySwitchToFrontCamera() {
        if ((!isSeatDisable() || isFromSpirite(getIntent()))
                && cameraSurfaceView.getCameraID() == Camera.CameraInfo.CAMERA_FACING_BACK) {
            switchCamera(false);
        }
    }

    private void onClickMenuPano() {
        SeatManager.EmPanoStatus status = getPanoStatus();
        Log.d(TAG, "onClickMenuPano: EmPanoStatus:" + getPanoStatus());

//        startSeatRest();
//        return;//TODO

//        if (status == PanoramaUtil.EmPanoStatus.SYNTHESIS && PanoramaUtil.getElapsedStatusTime() > 10000) {
//            return;
//        }

        if (status != SeatManager.EmPanoStatus.FREE) {
            //onClickPanoStop();
            boolean ret = startSeatRest();
            if (ret) {
                hideRightPart();
            }
        } else {
            boolean ret = startSeatRest();
            if (ret) {
                hideRightPart();
            }
        }
    }

    private void hideRightPart() {
        ivAlbum.setVisibility(View.GONE);
        ivSwitchCamera.setVisibility(View.GONE);
        controlMenu.setVisibility(View.INVISIBLE);
        controlMenu.hideSubItem();
    }

    private void showRightPart() {
        ivAlbum.setVisibility(View.VISIBLE);
        ivSwitchCamera.setVisibility(View.VISIBLE);
        controlMenu.setVisibility(View.VISIBLE);
        controlMenu.showSubItem();
        controlMenu.requestLayout();
    }

    @Override
    protected void onResume() {
        Log.d(TAG_SMILE, "onResume: enter");
        super.onResume();
        boolean ret = cameraSurfaceView.openCamera(true);
        if (!ret) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mbResuming && !isFinishing() && !isDestroyed()) {
                        cameraSurfaceView.openCamera(true);
                    }
                }
            }, 400);
        }
        mbResuming = true;
        if (mPadConnectStatusListener != null) {
            mPadConnectStatusListener.onDockingPadConnectStatusListener(isCnnedToSeat());
        }
        mbVisible = true;
        sdvDrawView.onResume();
        CameraEngine.setActivity(CameraActivity.this);
        //wakeLock.acquire();
        tryWakeLock();
        if (CameraEngine.isCameraIdBack()) {
            sensorResume();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                delayInitContent();
            }
        });
        printSpeed();
        updateIntentCmd();
        //adjustLayout(actionMode);
        //threadUpdateAlbumBtn();
        adjustLayout(actionMode);
        checkSeatStatus();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                freshSmileMode();
            }
        }, 1000);
        Log.d(TAG_SMILE, "onResume: leave action:" + action);
        updateSurfaceSize();
        //showProgressDlg();//TODO
        sdvDrawView.clearData();
        // Enable location recording if the setting is on.
        //final boolean locationRecordingEnabled = true;
        //mLocationManager.recordLocation(locationRecordingEnabled);
        Account.instance(null).registerAccountCenter();
    }

    private void wakeLockRelease() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "wakeLockRelease: " + wakeLock.toString());
        }
    }

    private void wakeLockReset() {
        long tm = SystemClock.elapsedRealtime();
        //wakeLockRelease();
        if (wakeLock != null /*&& !wakeLock.isHeld()*/ && !isDestroyed()) {
            int timeout = 5 * 60 * 1000;//一定时间后无操作就休眠
            wakeLock.acquire(timeout);
            Log.d(TAG, "wakeLockReset:  timeout:" + timeout + "  waste time:"
                    + (SystemClock.elapsedRealtime() - tm));
        } else if (isDestroyed() || isFinishing()) {
            wakeLockRelease();
        }
    }

    /**
     * 耗时1~9ms，平均2ms
     */
    private void tryWakeLock() {
        if (mbResuming) {
            long tm = SystemClock.elapsedRealtime();
            if (videoRecording) {
                //wakeLockForever();
                wakeLockReset();
            } else {
                wakeLockReset();
            }
            Log.d(TAG, "tryWakeLock: waste time:" + (SystemClock.elapsedRealtime() - tm));
        }
    }

    private void wakeLockForever() {
        if (mbResuming) {
            //wakeLockRelease();
            if (wakeLock != null/* && !wakeLock.isHeld()*/) {
                wakeLock.acquire();
                Log.d(TAG, "wakeLockForever:  timeout: forever");
            }
        }
    }

    private void sensorResume() {
        if (accelerSensor != null) {
            accelerSensor.onResume();
        }
    }

    private void sensorPause() {
        if (accelerSensor != null) {
            accelerSensor.onPause();
        }
    }

    private void updateIntentCmd() {
        //判断是否是其他应用调用
        Intent intent = getIntent();
        outputVideoPath = "";
        outputPicPath = "";
        if (intent != null && intent.getAction() != null) {
            action = getIntent().getAction();
            {//TODO 测试
//                action = ACTION_PAI_CAMERA;
//                intent.putExtra("TYPE", 1);
//                outputPicPath = FileUtils.getPhotoPath() + "123456.jpg";
            }
            Bundle data = intent.getExtras();
            SaveTask.sbCapturePic = MediaStore.ACTION_IMAGE_CAPTURE.equals(action);
            if (MediaStore.ACTION_IMAGE_CAPTURE.equals(action)) {
                hasReturn = true;
                actionMode = ACTION_IMAGE_MODE;
                if (data != null && data.containsKey(MediaStore.EXTRA_OUTPUT)) {
                    Uri uri = (Uri) data.getParcelable(MediaStore.EXTRA_OUTPUT);
                    outputPicPath = ImageUtils.getImageAbsolutePath(instance, uri);
                }
            } else if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action)) {
                hasReturn = false;
                actionMode = ACTION_NO_MODE;
            } else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(action) || MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(action)) {
                hasReturn = true;
                actionMode = ACTION_VIDEO_MODE;
                if (data != null && data.containsKey(MediaStore.EXTRA_OUTPUT)) {
                    Uri uri = (Uri) data.getParcelable(MediaStore.EXTRA_OUTPUT);
                    outputVideoPath = ImageUtils.getImageAbsolutePath(instance, uri);
                }
            } else if (ACTION_PAI_CAMERA.equals(action)) {
                if (data != null && data.containsKey("TYPE")) {
                    ipType = data.getInt("TYPE");
                }
                if (data != null && data.containsKey(MediaStore.EXTRA_OUTPUT)) {
                    Uri uri = (Uri) data.getParcelable(MediaStore.EXTRA_OUTPUT);
                    outputPicPath = ImageUtils.getImageAbsolutePath(instance, uri);
                }
                hasReturn = true;
                actionMode = ACTION_IMAGE_MODE;
                //cameraSurfaceView.setCameraID(Camera.CameraInfo.CAMERA_FACING_FRONT);
                if (CameraEngine.getCameraID() != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    switchCamera(false);
                }
                controlMenu.setVisibility(View.INVISIBLE);
                ivAlbum.setVisibility(View.GONE);
                controlMenu.hideSubItem();
                ivSwitchCamera.setVisibility(View.GONE);
                ivSmailControl.setVisibility(View.GONE);
                if (ipType == 0) {
                    speak("");
                    downCounting = true;
                    handler.sendEmptyMessageDelayed(0x105, 1000);
//                    speak(getString(R.string.start_take_tip)/* new SpeechHelper.OnSpeakFinishedListener() {
//                        @Override
//                        public void onSpeakFinished() {
//                            hideRightPart();
//                            downCounting = true;
//                            handler.sendEmptyMessageDelayed(0x105, 1000);
//                        }
//                    }*/);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!TimerCountUtil.isStarted()) {
                                startTakePicDownCount(5);
                            }
                        }
                    }, 2000);
                } else if (ipType == 1) {
//                    if (!mbFirstUpdateIntentCmd) {
                    speak("");
                    if (isSeatMotorDisable()) {//没有底座禁止全景拍照功能
                        String msg = getString(R.string.tips_no_docking);
                        CameraActivity.this.speak(msg, new SpeechHelper.OnSpeakFinishedListener() {
                            @Override
                            public void onSpeakFinished() {
                                finish();
                            }
                        }, false);
                        CustomToastUtils.showControlToast(CameraActivity.this, msg);//TODO
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isDestroyed() && !isFinishing()) {
                                    finish();
                                }
                            }
                        }, 5000);
                        return;
                    }
//                    } else {//避免isSeatMotorDisable()（耗时操作200ms）阻塞主线程
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                updateIntentCmd();
//                            }
//                        }, 1000);
//                    }

                    //handler.sendEmptyMessageDelayed(0x103, 1000);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startSeatRest();
                            Log.d(TAG, "run: " + QUANJIAN + " handler.postDelayed");
                        }
                    }, 1000);
                    //speak(getResString(R.string.start_take_tip));
                    Log.d(TAG, "onResume: " + QUANJIAN);
                }
            } else {
                hasReturn = false;
                actionMode = ACTION_NO_MODE;
            }
        }
    }

    private void postDelayed(Runnable runnable, int mil) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        handler.postDelayed(runnable, mil);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
        }
    }

    //调整显示布局
    private void adjustLayout(int mode) {
        if (mode == ACTION_IMAGE_MODE) {
            switchMode(COMMON_MODE);
            controlMenu.hideSubItem();
            ivSmailControl.setVisibility(View.GONE);
            ivAlbum.setImageResource(R.drawable.icon_quxiao);
            ivAlbum.setBorderColor(Color.TRANSPARENT);
        } else if (mode == ACTION_VIDEO_MODE) {
            switchMode(VIDEO_MODE);
            controlMenu.hideSubItem();
            ivAlbum.setImageResource(R.drawable.icon_quxiao);
            ivAlbum.setBorderColor(Color.TRANSPARENT);
        } else {
            if (mLastMediaFile != null && mLastMediaFile.mMp != null) {
                //ivAlbum.setImageBitmap(bm);
                showTakeedPic(mLastMediaFile.mMp);
                ivAlbum.setBorderColor(Color.WHITE);
            } else {
                ivAlbum.setImageResource(R.drawable.selector_xiance);
                ivAlbum.setBorderColor(Color.TRANSPARENT);
            }
        }
    }

    public void tryClearAlbum() {
        if (mLastMediaFile != null) {
            mLastMediaFile = new EventLastMediaFile(mLastMediaFile.mInfo, null, false);
        }
        adjustLayout(ACTION_NO_MODE);
    }

    @Override
    protected void onPause() {
        Log.d(TAG_SMILE, "onPause: enter");
        super.onPause();
        mbResuming = false;
        wakeLockRelease();
        sensorPause();
        //detectFacer.stopDetect();
        cameraView.clearFocus();
        stopSpeak();
        if (voiceRecording) {
            stopMediaCountTime();
        }

        if (videoRecording) {
            stopMediaCountTime();
        }
        Log.d(TAG_SMILE, "onPause: leave");//从小派进来时，onPause->onStop延时太长
        //mLocationManager.recordLocation(false);
        if (CameraEngine.getSettedActivity() == CameraActivity.this) {
            cameraSurfaceView.closeCamera();
        }
    }

    private long mTimeOnStop = 0;

    private boolean isOnStop() {
        //最后一次调用onStop的时间在两秒之内，则认为Activity不可见，就要关闭录音与录像
        return SystemClock.elapsedRealtime() - mTimeOnStop < 2000;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean fromSpirite = isFromSpirite(intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            action = CameraActivity.this.action;
            intent.setAction(action);
        }
        boolean bSwitch = (fromSpirite
                || (!TextUtils.isEmpty(action)) && action.equalsIgnoreCase(ACTION_PAI_CAMERA))
                && CameraEngine.isCameraIdBack();
        if (bSwitch) {
            switchCamera(true);
        }
        Log.d(TAG, "onNewIntent:   bSwitch:" + bSwitch
                + "  isCameraIdBackOpen:" + CameraEngine.isCameraIdBackOpen()
                + "  action:" + action);
    }

    @Override
    protected void onStop() {
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        Log.d(TAG_SMILE, "onStop: enter");
        mbVisible = false;

        //停止全景拍照
        if (seatManager.isSeatThreadAlive()/* && !isFinishing()*/) {
            setProgressBar(maxProgress);
            pbPanPic.setVisibility(View.GONE);
            pbPanPic.setProgress(0);
            panoramaTaking = false;
            cameraSurfaceView.setVisibility(View.GONE);
            finish();
        }
        //停止自拍
        if (isFromIpSprite() && ipType == 0 && TimerCountUtil.isStarted()) {
            TimerCountUtil.stopNoTakePic();
            finish();
        }
        MediaRecorderControl instance = MediaRecorderControl.getInstance();
        //instance.stopVoiceRecord();
        //instance.stopVideoRecord(this, mOnVideoEndListener);
        boolean fromIpSprite = isFromIpSprite();
        if ((instance.isVoiceRecording() || instance.isVideoRecording()) && !fromIpSprite) {
            mTimeOnStop = SystemClock.elapsedRealtime();
            controlMenu.performClick();//尝试关闭
        }
        sdvDrawView.onPause();
        stopSpeak();
        if (fromIpSprite && !isFinishing()) {
            finish();
        }
        Log.d(TAG_SMILE, "onStop: leave  fromIpSprite:" + fromIpSprite);
        wakeLockRelease();
        sdvDrawView.clearData();
        super.onStop();
        //cameraSurfaceView.getHolder().
        if (CameraEngine.getSettedActivity() == CameraActivity.this) {
            CameraEngine.releaseCamera();
        }
        //rootView.removeView(cameraSurfaceView);
        //rootView.removeView(rlMainlayout);
        //cameraSurfaceView.clearScreen();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: before super.onDestroy()");
        super.onDestroy();
        long tm = SystemClock.elapsedRealtime();
        EventBus.getDefault().unregister(CameraActivity.this);
        unregisterBroadcast();
        if (CameraEngine.getSettedActivity() == CameraActivity.this) {
            CameraEngine.releaseCamera();
            CameraEngine.removeActivity(CameraActivity.this);
            detectFacer.releaseAll();
        } else {
            Log.d(TAG, "onDestroy:   old activity");
        }
        sensorPause();
        sdvDrawView.setStopThread(true);
        Log.d(TAG, "onDestroy: waste time:" + (SystemClock.elapsedRealtime() - tm));
        Log.d(TAG_SMILE, "onDestroy: ");
        wakeLockRelease();
        getWindow().getDecorView().setVisibility(View.GONE);
//        mLocationManager.disconnect();
    }

    private void unregisterBroadcast() {
        try {
            //uninitImageService();
            unregisterDockingBroadCast();
            unregisterAlarm();
            if (mbShowBattery) {
                unregisterReceiver(getBatteryLevelBroadcast(CameraActivity.this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick({R.id.ivSwitchCamera /*,R.id.ivSmailControl*//*, R.id.ivAlbum*/})
    @Override
    public void onClick(View v) {
        //全景的时候禁止操作
        if (isSwitchingCameraAnimRunnig()) {
            return;
        }
        stopSpeak();
        if (panoramaTaking)
            return;
        if (downCounting) return;
        switch (v.getId()) {
            case R.id.ivSwitchCamera:
                if (PhoneUtil.isFastDoubleClick()) {
                    return;
                }
                switchCamera(true);
                break;
            case R.id.ivSmailControl:
                onClickSmile();
                break;
            case R.id.ivAlbum:
                if (actionMode == ACTION_IMAGE_MODE || actionMode == ACTION_VIDEO_MODE) {
                    finish();
                } else {
                    boolean ret = jumpToAlbum();
                    if (ret) {
                        v.removeCallbacks(mRunDelayJumpAlbum);
                    } else {//点击按钮后如果没有立即反应，在之后的一定时间之内会反应
                        Log.d(TAG, "onClick: mRunDelayJumpAlbum  jumpToAlbum failed first time!");
                        v.postDelayed(getRunDelayJumpAlbum(), 250);
                        v.postDelayed(getRunDelayJumpAlbum(), 500);
                        v.postDelayed(getRunDelayJumpAlbum(), 750);
                        v.postDelayed(getRunDelayJumpAlbum(), 1000);
                        v.postDelayed(getRunDelayJumpAlbum(), 1250);
                        v.postDelayed(getRunDelayJumpAlbum(), 1500);
                        //v.postDelayed(getRunDelayJumpAlbum(), 1000);
                    }
                }
                break;
        }
    }

    public Runnable getRunDelayJumpAlbum() {
        if (mRunDelayJumpAlbum == null) {
            mRunDelayJumpAlbum = new Runnable() {
                @Override
                public void run() {
                    boolean ret = jumpToAlbum();
                    if (ret) {
                        ivAlbum.removeCallbacks(mRunDelayJumpAlbum);
                    }
                    Log.d(TAG, "run: mRunDelayJumpAlbum ret:" + ret);
                }
            };
        }
        return mRunDelayJumpAlbum;
    }


    @OnTouch({R.id.ivSmailControl, R.id.ivAlbum})
    public boolean onTouch(View v, MotionEvent event) {
        stopSpeak();
        if (panoramaTaking) {
            return false;
        }
        if (downCounting) {
            return false;
        }
        if (isSwitchingCameraAnimRunnig()) {
            return false;
        }

        int id = v.getId();
        if (id == R.id.ivSmailControl) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setAlpha(1.0f);
                    break;
                case MotionEvent.ACTION_UP:
                    onClickSmile();
                    break;
            }
        } else if (id == R.id.ivAlbum) {
            if (isStateForbidSwitchMode()) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.5f);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setAlpha(1.0f);
                    onClick(ivAlbum);
                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    break;
            }
        }
        return true;
    }

    private void onClickSmile() {
        if (controlMode == SMAIL_CONTROL_MODE) {
            controlMode = DEFAULT_CONTROL_MODE;
        } else {
            controlMode = SMAIL_CONTROL_MODE;
        }
        onSmileContrlChanged(controlMode == SMAIL_CONTROL_MODE);
    }

    private void onSmileContrlChanged(boolean bSmile) {
        ivSmailControl.setImageResource(bSmile ?
                R.drawable./*selector_xiaolian_yes*/weixiao_press
                : R.drawable./*selector_xiaolian_no*/weixiao_normal);
        if (!bSmile) {
            tryFadeSmile();
        }
        sdvDrawView.freshSmileState(/*bSmile*/);
        if (bSmile) {
            //    detectFacer.startDetect();
            //controlMode = SMAIL_CONTROL_MODE;
            if (firstSmailControl) {
                if (!EmShareCnt.SMILE_PHOTO.isCntFull()) {
                    speak(getResString(R.string.smail_take_tip));
                    CustomToastUtils.showControlToast(mContext, getResString(R.string.smail_take_tip));
                }
                firstSmailControl = false;
            }
        }
        //else {
        //    detectFacer.stopDetect();
        //    //controlMode = DEFAULT_CONTROL_MODE;
        //}

        if (detectFacer.isDetect() == bSmile) {
            return;
        }
    }

    private void tryFadeSmile() {
        if (controlMode != SMAIL_CONTROL_MODE && ivSmailControl.getVisibility() == View.VISIBLE) {
            ivSmailControl.animate().alpha(0.5f).setDuration(5000).start();
        }
    }

    //跳转到相册
    private boolean jumpToAlbum() {
        Log.d(TAG, "jumpToAlbum:   mSecurityLastMediaId:" + mSecurityLastMediaId);
        if (!isSecureCamera()) {
            return jumpToAlbumDirectly();
        } else {
            return jumpToAlbumSecuritly();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updatePreviousSecureStatus();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!isKeyguardLockedSecure()) {
                    mSecurityLastMediaId = MIN_LAST_MEDIA_ID;
                    threadUpdateAlbumBtn();
                }
            }
        }, 1000);

        updateSurfaceSize();
        cameraSurfaceView.requestLayout();
        cameraSurfaceView.invalidate(cameraSurfaceView.getLeft(), cameraSurfaceView.getTop(),
                cameraSurfaceView.getRight(), cameraSurfaceView.getBottom());
        cameraSurfaceView.postInvalidate();
        cameraSurfaceView.forceLayout();
        ((View) (cameraSurfaceView.getParent())).forceLayout();
        //cameraSurfaceView.freshCamera();
    }

    private void updatePreviousSecureStatus() {
        boolean bPreSecure = mbPreviousSecure;
        boolean bCurSecure = isKeyguardLockedSecure();
        if (bPreSecure && !bCurSecure) {
            mbPreviousSecure = false;
            mSecurityLastMediaId = MIN_LAST_MEDIA_ID;
        } else if (!bPreSecure && bCurSecure) {
            mbPreviousSecure = true;
            mSecurityLastMediaId = getLastMediaId();
            tryClearAlbum();
        } else if (!bCurSecure && !bCurSecure) {
            mbPreviousSecure = false;
            mSecurityLastMediaId = MIN_LAST_MEDIA_ID;
        } else if (bPreSecure && bCurSecure) {
            //不变
        }
        Log.d(TAG, "onRestart: bPreSecure:" + bPreSecure + "  bCurSecure:" + bCurSecure);
    }

    private boolean jumpToAlbumSecuritly() {
        Log.d(TAG, "jumpToAlbumSecuritly: enter");
        Intent intent = null;

        if (mLastMediaFile == null /*|| mLastMediaFile.mInfo == null ||
                TextUtils.isEmpty(mLastMediaFile.mInfo._DATA) || !(new File(mLastMediaFile.mInfo._DATA).exists())*/) {
            Log.d(TAG, "jumpToAlbumSecuritly: return" + "  mLastMediaFile=" + mLastMediaFile);
            return false;
        }
        boolean ret = false;
        if (mLastMediaFile.uri != null) {
            try {
                intent = new Intent();
                ComponentName info;
                info = new ComponentName("com.putao.ptx.gallery2",
                        "com.android.gallery3d.app.SecurityLockActivity");
                intent.setComponent(info);
//                intent = new Intent("android.intent.action.MAIN_GALLERY", mLastMediaFile.uri);
                intent.setData(mLastMediaFile.uri);
                mSecurityLastMediaId = Math.max(mSecurityLastMediaId, MIN_LAST_MEDIA_ID);
                intent.putExtra("lastId", mSecurityLastMediaId);
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                Log.d(TAG, "jumpToAlbumSecuritly:   mLastMediaFile.uri:" + mLastMediaFile.uri
                        + "  mSecurityLastMediaId:" + mSecurityLastMediaId
                        + "  \n" + mLastMediaFile.mInfo);
                ret = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "jumpToAlbumSecuritly  ERR:   mLastMediaFile.uri:" + mLastMediaFile.uri);
                ret = false;
            }
        }
        return ret;
    }


    private boolean jumpToAlbumDirectly() {
        Log.d(TAG, "jumpToAlbumDirectly: enter");
        Intent intent = null;

//        ImageInfo image = MediaUtil.getLastImageInfo(mContext);
//        Uri uri = MediaUtil.filePathToUri(mContext, image._DATA);
        if (mLastMediaFile == null /*|| mLastMediaFile.mInfo == null ||
                TextUtils.isEmpty(mLastMediaFile.mInfo._DATA) || !(new File(mLastMediaFile.mInfo._DATA).exists())*/) {
            return false;
        }
        boolean ret = false;
        if (mLastMediaFile.uri != null) {
            try {
                //uri = Uri.fromFile(file);//TODO ?
                intent = new Intent(/*Intent.ACTION_VIEW*/"com.android.camera.action.REVIEW", mLastMediaFile.uri);
//                intent.setDataAndType(uri, mLastMediaFile.bImage ? "image/*" : "video/*");
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                Log.d(TAG, "jumpToAlbumDirectly:   mLastMediaFile.uri:" + mLastMediaFile.uri);
                ret = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "jumpToAlbumDirectly  ERR:   mLastMediaFile.uri:" + mLastMediaFile.uri);
                ret = false;
            }
        }
        return ret;
    }

    private long takePicTime = 0;
    private Bitmap takePicBmp = null;
    private String picType;
    private boolean isTakeVoicePic = false;

    private String takePic() {
        return takePic(null);
    }

    //拍照
    private String takePic(@Nullable String mayPanoPath) {
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        if (SystemClock.elapsedRealtime() - takePicTime < 50/*800*/) {
            return "";
        }
        Log.d(TAG, TAG_TAKEPIC + "  start take picture takePic: ");
        isTakeVoicePic = false;
        takePicing = true;
        final File pic;
        picType = "";
        if (actionMode == ACTION_IMAGE_MODE) {
            Log.e(TAG, "takePic: " + outputPicPath);
            if (!TextUtils.isEmpty(outputPicPath)) {
                pic = new File(outputPicPath);
            } else {
                picName = "IMG_" + format.format(new Date(System.currentTimeMillis()));
                pic = new File(FileUtils.getPhotoPath() + picName + ".jpg");
            }
            picType = getPicType();
        } else if (actionMode == ACTION_NO_MODE) {
            picName = "IMG_" + format.format(new Date(System.currentTimeMillis()));
            if (mayPanoPath == null) {
                pic = new File(FileUtils.getPhotoPath() + picName + ".jpg");
            } else {
                pic = new File(mayPanoPath);
            }
            picType = getPicType();
        } else {
            return "";
        }
        if (pic == null) return "";
//        ViewGroup.LayoutParams para = blackView.getLayoutParams();
//        para.width = ViewGroup.LayoutParams.MATCH_PARENT;
//        para.height = ViewGroup.LayoutParams.MATCH_PARENT;
        if (!isTakePanorama()) {
            blackView.setBackgroundColor(Color.BLACK);
            blackView.clearAnimation();
            blackView.setVisibility(View.VISIBLE);
            blackView.setAlpha(1);
            blackView.animate().alpha(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    blackView.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    blackView.setVisibility(View.GONE);
                }
            }).setInterpolator(new AccelerateInterpolator()).start();
        }
//        if (mRunGoneBlackView == null) {
//            mRunGoneBlackView = new Runnable() {
//                @Override
//                public void run() {
//                    blackView.setVisibility(View.GONE);
//                    rlMainlayout.invalidate();
//                    blackView.requestLayout();
//                }
//            };
//        }
//        blackView.removeCallbacks(mRunGoneBlackView);
//        blackView.postDelayed(mRunGoneBlackView, 200);
        if (!isSilentOrVibrate()) {
            if (getAudioPlay() != null && !isTakePanorama()) {
                mAudioPlay.playSound(cameraMode == VOICE_MODE ?
                        AudioPlay.EmSound.AUDIO_START : AudioPlay.EmSound.SHUTTER);
            }
        }
        final String tmp = picType;
        final boolean bSoundMode = (cameraMode == VOICE_MODE);
        ExifInterface exif = getExif();
        cameraSurfaceView.takePic(mContext, CameraEngine.getRotationResult(),
                pic, new CameraSurfaceView.onPictureTakenListener() {
                    @Override
                    public void onSaved(Bitmap bmp, Uri uri) {
                        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        takePicing = false;
                        mLastMediaFile.uri = uri;
                        try {
                            MediaUtil.insertPTImage(mContext, uri, /*picType*/tmp, getAddress());//在其他设备上此处容易发生异常，造成内存泄漏
                            FileUtils.tryAddFirstMediaTime();
                            if (bSoundMode && !mbFirstSoundPicFinished) {
                                mbFirstSoundPicFinished = true;
                                EmShareCnt.VOICE_PHOTO.tryAddCnt();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "onSaved: ", e);
                        }
                        Account.instance(CameraActivity.this).sendEvent();
                        if (actionMode == ACTION_NO_MODE) {
                            takePicBmp = bmp;
                            if (bmp == null) return;
                            if (isTakeVoicePic) {
                                if (ivVoicePic == null) return;
                                ImageUtils.releaseImageViewResouce(ivVoicePic);
                                if (mbShowVoicePic) {
                                    final Bitmap mp;
                                    if (CameraEngine.isCameraIdBack()) {
                                        mp = bmp.copy(Bitmap.Config.ARGB_8888, true);//耗时约60ms
                                    } else {
                                        mp = BitmapUtil.rotation90N(bmp, 0, true);//耗时约100ms
                                    }
                                    ivVoicePic.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ivVoicePic.setVisibility(View.VISIBLE);
                                            if (mp != null && !mp.isRecycled()) {
                                                ivVoicePic.setImageBitmap(mp);
                                            }
                                            //startMediaCountTime(10);
                                        }
                                    });
                                }
                            }
                            mLastMediaFile.uri = uri;
                            //threadUpdateAlbumBtn();
                            //System.gc();

                            if (bmp != null && !bmp.isRecycled()) {
                                //bmp.recycle();
                                //System.gc();
                            }
                            ExifInterface exif = new ExifInterface();
                            try {
                                exif.readExif(pic.getPath());
                                double[] latLon = exif.getLatLongAsDoubles();
                                if (latLon != null) {
                                    Log.d(TAG, "onSaved: exif lat/lon:" + latLon[0] + "/" + latLon[1]);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, TAG_TAKEPIC + "  onSaved:  take pic waste time " + (SystemClock.elapsedRealtime() - cameraSurfaceView.getTmLastTakePic()));
                        } else if (actionMode == ACTION_IMAGE_MODE && hasReturn) {
                            onTakePicFinish(uri.toString());
                            return;
                        }
                    }
                }, exif);
        takePicTime = SystemClock.elapsedRealtime();
        return FileUtils.getPhotoPath() + picName + ".jpg";
    }


    private String getPicType() {
        String picType = "";
        //有声（1：是 0：否）
        if (cameraMode == VOICE_MODE) {
            picType += "1";
            isTakeVoicePic = true;
        } else {
            picType += "0";
            isTakeVoicePic = false;
        }
        //自拍（1：是 0：否）
        if (cameraSurfaceView.getCameraID() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            picType += "1";
        } else {
            picType += "0";
        }
        //探索（1：是 0：否）暂时默认为0
        picType += "0";
        //全景0，只是为了补足4位
        picType += "0";
        return picType;
    }

    private void showTakeedPic(final Bitmap bmp) {
        if (bmp == null || bmp.isRecycled()) {
            return;
        }
        Log.d(TAG, "run: ivAlbum.setImageBitmap(bmp)");

        ImageUtils.releaseImageViewResouce(ivAlbum);
        ivAlbum.setImageBitmap(bmp);
        if (bmp != null) {
            ivAlbum.setBorderColor(Color.WHITE);
        }
        Log.d(TAG, "run: ivAlbum.setImageBitmap(bmp)");
    }

    //人脸识别
    private List<FaceModel> curFaces = null;
    private long mTmUpdateFace;
    private int smailCount = 0;
    private long lastSmailTakePic = 0;
    private long intervalTime = 0;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateLastMediaFile(EventLastMediaFile e) {
        if (e == null && e.mMp != null) {
            e.mMp.recycle();
        }
        mLastMediaFile = e;
        adjustLayout(actionMode);
    }

    @Subscribe
    public void updateLastMediaUri(Uri uri) {
        mLastMediaFile.uri = uri;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventList(EventList event) {
        if (event == null) {
            return;
        }
        switch (event) {
            case TAKED_PIC:
                if (isTakeVoicePic) {
                    CameraEngine.getCamera().stopPreview();
                    cameraView.setFocusEnable(false);
                    startMediaCountTime(10, cameraMode);
                    freshSmileMode();
                } else if (!TextUtils.isEmpty(outputPicPath)) {//当是从小派中跳转过来时拍照完后停止预览
                    CameraEngine.getCamera().stopPreview();
                    cameraView.setFocusEnable(false);
                }
                break;
            case UPDATE_ALBUM:
                threadUpdateAlbumBtn();
                break;
            case UPDATE_SURFACE:
                updateSurfaceSize();//现在不需要
                break;
        }

    }

    private void focusSmileFace(List<FaceModel> curFaces) {
        if (curFaces != null && curFaces.size() > 0) {
            RectF rect = curFaces.get(0).rectf;
            if (rect != null) {
                cameraView.focusOnTouch(CameraEngine.getCamera(),
                        rect.centerX(), rect.centerY() - rect.height() * CameraDrawView.COE_OFFSET);
            }
        }
    }

    private Runnable mRunPostThread = new Runnable() {
        @Override
        public void run() {
            long tm = SystemClock.elapsedRealtime();
            new Thread(mRunThreadUpdateAlbumBtn).start();
            Log.d(TAG, "mRunPostThread  run: waste time:" + (SystemClock.elapsedRealtime() - tm));
        }
    };
    private Runnable mRunThreadUpdateAlbumBtn = new Runnable() {
        @Override
        public void run() {
            if (isDestroyed() || isFinishing()) {
                return;
            }
            try {
                long tm = SystemClock.elapsedRealtime();
                EventLastMediaFile event = getEventLastMediaFile();
                Log.d(TAG, "run: getEventLastMediaFile waste time:" + (SystemClock.elapsedRealtime() - tm)
                        + "  event.mInfo:" + event.mInfo + "  mSecurityLastMediaId:" + mSecurityLastMediaId);

                if (isDestroyed() || isFinishing()) {
                    return;
                }

                if (isKeyguardLockedSecure() && event.mInfo._ID != null
                        && Integer.valueOf(event.mInfo._ID) <= mSecurityLastMediaId) {
                    event = new EventLastMediaFile(event.mInfo, null, false);
                    Log.d(TAG, "run: mSecurityLastMediaId:" + mSecurityLastMediaId);
                }
                EventBus.getDefault().post(event);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "run: " + e.toString());
            }
        }
    };

    public void threadUpdateAlbumBtn() {
        long tm = SystemClock.elapsedRealtime();
        handler.removeCallbacks(mRunPostThread);
        handler.postDelayed(mRunPostThread, 10);//拍一张照片有可能会耗时1.2秒
        Log.d(TAG, "threadUpdateAlbumBtn: waste time:" + (SystemClock.elapsedRealtime() - tm));
    }

    @NonNull
    private EventLastMediaFile getEventLastMediaFile() {
        long tm = SystemClock.elapsedRealtime();
        MediaInfo infoImage = MediaUtil.getLastImageInfo(CameraActivity.this);
        MediaInfo infoVideo = MediaUtil.getLastVideoInfo(CameraActivity.this);
        boolean bImage = true;
        long tmImg = 0;
        long tmVideo = 0;
        try {
            tmImg = Long.valueOf(infoImage._ID/*_DATE_ADDED*/);
        } catch (Exception e) {
        }
        try {
            tmVideo = Long.valueOf(infoVideo._ID/*_DATE_ADDED*/);
        } catch (Exception e) {
        }
        bImage = tmImg >= tmVideo;

        Uri uri;
        Bitmap bm;
        if (bImage) {
            bm = MediaUtil.getThumbnailBitmap(mContext, infoImage._ID);
            uri = MediaUtil.filePathToUri(mContext,
                    infoImage._DATA);
        } else {
            bm = MediaUtil.getVideoThumbnailBitmap(mContext, infoVideo._ID);
            if (bm == null) {
                bm = MediaUtil.getVideoThumbnail(infoVideo._DATA);
                if (bm == null) {
                    bm = BitmapUtil.getVideoThumbnailFromDir(new File(infoVideo._DATA).getName());
                }
                if (bm == null) {
                    Bitmap tmp = MediaUtil.getVideoFramePicture(infoVideo._DATA);
                    if (tmp != null) {
                        long tmpTm = SystemClock.elapsedRealtime();
                        bm = BitmapUtil.resizeBitmap(tmp, 0.2f);
                        Log.d(TAG, "getEventLastMediaFile: resizeBitmap waste time:"
                                + (SystemClock.elapsedRealtime() - tmpTm));
                    }
                }
            }
            uri = MediaUtil.videoFilePathToUri(mContext,
                    infoVideo._DATA);
            //bm = MediaUtil.getVideoThumbnail(configuration.orientation, cameraSurfaceView.getCameraID(), strFile, 50, 50);
        }
        EventLastMediaFile mediaFile = new EventLastMediaFile(bImage ? infoImage : infoVideo, bm, bImage);
        mediaFile.uri = uri;
        Log.d(TAG, "getEventLastMediaFile: wastTime:" + (SystemClock.elapsedRealtime() - tm)
                + "  isSecureCamera:" + isSecureCamera()
                + "  uri:" + uri
                + "  \nmediaFile:" + mediaFile);
        return mediaFile;
    }

    private int getLastMediaId() {
        long tm = SystemClock.elapsedRealtime();
        int id = Math.max(MIN_LAST_MEDIA_ID, MediaUtil.getMaxId(CameraActivity.this));
        Log.d(TAG, "getLastMediaId: waste time:" + (SystemClock.elapsedRealtime() - tm) + "  lastId:" + id);
        return id;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpdateAlbum(EventUpdateAlbum event) {
        if (event != null) {
            mLastMediaFile = new EventLastMediaFile(mLastMediaFile.mInfo, event.mBitmap, mLastMediaFile.bImage);
            adjustLayout(actionMode);
            if (event.mBitmap != null && cameraMode == COMMON_MODE && actionMode == ACTION_NO_MODE) {
                //ivAlbum.animate().cancel();
                ivAlbum.setScaleX(0.01f);
                ivAlbum.setScaleY(0.01f);
                ivAlbum.animate().scaleX(1).scaleY(1).setDuration(400).setInterpolator(new OvershootInterpolator()).start();
            }
            Log.d(TAG, "onEventUpdateAlbum:  taking_picture waste time:"
                    + (SystemClock.elapsedRealtime() - cameraSurfaceView.getTmLastTakePic()));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFaceDetect(FaceDetectEvent event) {
        boolean bShow = isShouldDetectFace();
        if (!detectFacer.isDetect() && event.faces != null || !bShow) {
            event.faces.clear();
        }

        if (event.faces != null && event.faces.size() > 0) {
            boolean bHasSmile = false;
            for (FaceModel face : event.faces) {
                Log.d(TAG, "onFaceDetect: event.faces:" + event.faces.size()
                        + "  face.getSmileLevel:" + face.getSmileLevel());
                if (face.isSmile()) {
                    bHasSmile = true;
                    break;
                }
            }
            curFaces = event.faces;
            mTmUpdateFace = SystemClock.elapsedRealtime();
            focusView.setVisibility(View.GONE);
            //人脸对焦
//            float centerX = faceModel.landmarks[13 * 2];
//            float centerY = faceModel.landmarks[13 * 2 + 1];
//            if (Math.abs(centerX - lastPoint[0]) > 100 && Math.abs(centerY - lastPoint[1]) > 100 && System.currentTimeMillis() - foIntTime > 2500) {
//                if (cameraView == null)
//                    return;
//                if (cameraSurfaceView.getCameraID() == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                    cameraView.focusOnTouch(cameraSurfaceView.getCamera(), centerX, centerY);
//                    foIntTime = System.currentTimeMillis();
//                    lastPoint[0] = centerX;
//                    lastPoint[1] = centerY;
//                }
//            }
            switch (cameraMode) {
                case VOICE_MODE:
                    sdvDrawView.setData(curFaces);
                    //showDrawView(true);
                    //  if (controlMode == SMAIL_CONTROL_MODE && !voiceRecording) {
                    //  sdvDrawView.setData(curFaces);
                    //  showDrawView(true);
                    //  } else {
                    //  showDrawView(false);
                    //  }
                    break;
                case COMMON_MODE:
                    sdvDrawView.setData(curFaces);
                    //showDrawView(true);
                    //  if (controlMode == SMAIL_CONTROL_MODE) {
                    //  sdvDrawView.setData(curFaces);
                    //  showDrawView(true);
                    //  } else {
                    //  showDrawView(false);
                    //  }
                    break;
            }
            //拍照下笑脸识别拍照
            if ((cameraMode == COMMON_MODE || cameraMode == VOICE_MODE)
                    && !controlMenu.isMenuAnimRunning()) {
                if (controlMode == SMAIL_CONTROL_MODE) {
                    //if (faceModel != null && faceModel.emotions != null && faceModel.emotions.length > 0) {
                    //bHasSmile = faceModel.emotions[0] > 0.4;
                    if (bHasSmile) {
                        smailCount++;
                    } else {
                        smailCount = 0;
                    }
                    if (smailCount == 1 && CameraEngine.isCameraIdBack()) {
                        focusSmileFace(curFaces);
                    }
                    //连续监测到3次笑脸开始拍照
                    if (smailCount > 2/*8*/ && !voiceRecording && (SystemClock.elapsedRealtime() - lastSmailTakePic) > 2 * 1000) {
                        lastSmailTakePic = SystemClock.elapsedRealtime();
                        smailCount = 0;
                        //takePic();
                        controlMenu.performClickCenterView();
                        if (mbFirstSmilePic) {
                            EmShareCnt.SMILE_PHOTO.tryAddCnt();
                            mbFirstSmilePic = false;
                        }
                        freshSmileMode();
                        Log.d(TAG, "onFaceDetect: takeSmilePicture");
                    }
                    //}
                }
            }
        } else {
            sdvDrawView.clearData();
        }
    }

    //切换摄像头
    public void switchCamera(boolean bShowAnim) {
        intervalTime = SystemClock.elapsedRealtime();
        cameraView.clearFocus();

        if (bShowAnim) {
            //onBlurAnimStart();//TODO

            Bitmap blurBitmap = getBlurBitmap();
            try {
                if (blurBitmap != null) {
                    mCameraSwitchThread = new NonUiThread(CameraActivity.this,
                            blurBitmap, 700, new NonUiThread.OnCameraSwitchingListener() {
                        @Override
                        public boolean isSwitchingFinished() {
                            Camera camera = CameraEngine.getCamera();
                            return camera != null && !mbSwitchingCamera
                                    && !mbSwitchingCameraWithoutData;
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            sdvDrawView.clearData();
//                            onBlurAnimStart();
//                            startAnimBlurFadeOut(600);
                        }
                    });
                    mCameraSwitchThread.start();
                }
                Thread.sleep(10);//等待NonUiThread线程完全启动
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mbSwitchingCamera = true;
        mbSwitchingCameraWithoutData = true;
        cameraSurfaceView.switchCamera();
        mbSwitchingCamera = false;
        CameraEngine.clearOnPreviewFrameData();
        //cameraSurfaceView.setVisibility(View.VISIBLE);
        sdvDrawView.clearData();
        final long tm = SystemClock.elapsedRealtime();
        CameraEngine.setOnPreviewFrameListener(new CameraEngine.OnPreviewFrameListener() {
            @Override
            public void onPreviewFrameListener(byte[] data) {
                Log.d(TAG, "onPreviewFrameListener: waste time 0:" + (SystemClock.elapsedRealtime() - tm));
                CameraEngine.setOnPreviewFrameListener(null);
                onBlurAnimStart();
                startAnimBlurFadeOut(800);

                detectFacer.update(cameraSurfaceView.getCameraID());//TODO Victor mod
                mbSwitchingCameraWithoutData = false;
                Log.d(TAG, "onPreviewFrameListener: waste time:" + (SystemClock.elapsedRealtime() - tm));
            }
        });

        //cameraSurfaceView.setVisibility(View.INVISIBLE);
        updateSurfaceSize();
        //cameraSurfaceView.setVisibility(View.VISIBLE);
        if (bShowAnim) {
            // startAnimBlurFadeOut(1200);//TODO
        }
        if (CameraEngine.isCameraIdBack()) {
            sensorResume();
        } else {
            sensorPause();
        }
        videoRecording = false;
        voiceRecording = false;
        //showDrawView(false);

        cameraView.postDelayed(new Runnable() {
            @Override
            public void run() {
                freshSmileMode();
                cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
            }
        }, 100);
    }


    //设置DrawView绘制的内容
    private void showDrawView(boolean show) {
        if (show) {
            sdvDrawView.setVisibility(View.VISIBLE);
        } else {
            sdvDrawView.setVisibility(View.INVISIBLE/*GONE*/);
        }
    }

    private boolean firstVoiceMode = true;
    private boolean mbFirstSoundPicFinished = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void switchMode(EmCameraMode newMode) {
        if (newMode == null) {
            return;
        }
        switchMode(newMode.mIdx);
    }

    /**
     * 相机模式切换
     */
    private void switchMode(int newMode) {
        tryWakeLock();
        cameraView.clearFocus();
        boolean bModeChanged = cameraMode != newMode;
        cameraModeOld = cameraMode;
        cameraMode = newMode;

        if (!(firstVoiceMode && VOICE_MODE == cameraMode)) {
            tryStopSpeak();
            Log.d(TAG, "switchMode: speak:tryStopSpeak()");
        }
        if (SeatManager.sUserInput) {//临时设为false
            testPano();
        }
        switch (cameraMode) {
            case VOICE_MODE:
                if (firstVoiceMode) {
                    firstVoiceMode = false;
                    if (!EmShareCnt.VOICE_PHOTO.isCntFull()) {
                        speak(getResString(R.string.voice_pic_tip_tts));
                        CustomToastUtils.showControlToast(mContext, getResString(R.string.voice_pic_tip));
                    }
                }
                //cameraSurfaceView.setPicFocusMode();//TODO
                CameraEngine.updatePreviewSizeFocusParameter();
                ivSmailControl.setVisibility(View.VISIBLE);
                tryFadeSmile();
                setIvShutterIcon(false);
                cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
                freshSmileMode();
                showDrawView(true);
                break;
            case COMMON_MODE:
                //cameraSurfaceView.setPicFocusMode();//TODO
                CameraEngine.updatePreviewSizeFocusParameter();
                ivSmailControl.setVisibility(View.VISIBLE);
                tryFadeSmile();
                setIvShutterIcon(false);
                cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
                freshSmileMode();
                showDrawView(true);
                break;
            case VIDEO_MODE:
                showDrawView(false);
                setIvShutterIcon(false);
                //cameraSurfaceView.setVideoFocusMode();//TODO ?
                if (cameraMode == cameraModeOld) {
                    CameraEngine.updatePreviewSizeFocusParameter();
                }
                ivSmailControl.setVisibility(View.GONE);
                //setIvShutterIcon(false);
                cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
                freshSmileMode();
                break;
            case PANORAMA_MODE:
                showDrawView(false);
                //cameraSurfaceView.setVideoFocusMode();
                //cameraSurfaceView.setPicFocusMode();//TODO ?
                CameraEngine.updatePreviewSizeFocusParameter();
                ivSmailControl.setVisibility(View.GONE);
                setIvShutterIcon(false);
                cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
                freshSmileMode();
                break;
        }
        updateSurfaceSize();
        if (bModeChanged && (cameraModeOld == VIDEO_MODE || cameraMode == VIDEO_MODE)) {
            startAnimBlurFadeOut(1000);
        }
        //showRightPart();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //adjustLayout(actionMode);
                //threadUpdateAlbumBtn();//切换相机模式时不再更新相册按钮
            }
        });
    }

    private void testPano() {
        if (cameraMode != PANORAMA_MODE) {
            sleep.setVisibility(View.GONE);
            speed.setVisibility(View.GONE);
            mAcc.setVisibility(View.GONE);
        } else {
            sleep.setVisibility(View.VISIBLE);
            speed.setVisibility(View.VISIBLE);
            mAcc.setVisibility(View.VISIBLE);
            if (mAction == null) {
                mAction = new Runnable() {
                    @Override
                    public void run() {
                        if (accelerSensor != null) {
                            String text = String.format("% 2.1f", accelerSensor.getAccelerate());
                            mAcc.setText(text);
                            if (cameraMode == PANORAMA_MODE) {
                                mAcc.removeCallbacks(mAction);
                                mAcc.postDelayed(this, 200);
                            }
                        }
                    }
                };
            }
            mAcc.post(mAction);
        }
    }

    /**
     * 耗时可能300ms,190+ms,0~3ms或者100+ms以上
     */
    private void freshSmileMode() {
        long tm = SystemClock.elapsedRealtime();
        boolean bDetectFace = isShouldDetectFace();
        boolean bShowSmile = isShouldShowSmile();
        sdvDrawView.freshSmileState(/*bShowSmile*/);
        if (bDetectFace) {
            //detectFacer.startDetect();
            //sdvDrawView.setAllowDraw(true);
        } else {
            //detectFacer.stopDetect();
            sdvDrawView.clearData();
            //sdvDrawView.setAllowDraw(false);
        }
        //showDrawView(true);
        ((ViewGroup) sdvDrawView.getParent()).invalidate();
        sdvDrawView.setTranslationZ(12);
        CameraEngine.setPreviewCallback(detectFacer.previewCallback);
        Log.d(TAG, "freshSmileMode: bDetectFace:" + bDetectFace
                + "  bShowSmile:" + bShowSmile
                + "  voiceRecording:" + voiceRecording
                + "  isTakeVoicePic:" + isTakeVoicePic
                + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
    }

    private boolean isShouldShowSmile() {
        return isShouldDetectFace() && controlMode == SMAIL_CONTROL_MODE
                && !voiceRecording/* && !isTakeVoicePic*/;
    }

    private boolean isShouldDetectFace() {
        return (cameraMode == VOICE_MODE && !voiceRecording /*&& !isTakeVoicePic*/
                || cameraMode == COMMON_MODE) && !isFromIpSprite()
                && !isSwitchingCameraAnimRunnig() && mbResuming;
    }

    public static final int MSG_DELAY_DISMISS_ZOOM = 0x104;
    final int TIME_DELAY_DISMISS_ZOOM = 1800;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x103) {
                startSeatRest();
            } else if (msg.what == MSG_DELAY_DISMISS_ZOOM) {
                if (!isZoomBarTouch) {
                    sbZoom.setVisibility(View.GONE);
                }
            } else if (msg.what == 0x105) {
                startTakePicDownCount(5);
            }
        }
    };

    private boolean panoramaTaking = false;
    private Dialog progressDialog;
    final List<String> panoramaPics = new ArrayList<String>();

    private boolean startSeatRest() {
        if (!isMotorOn()) {
            String strOpenMotor = getString(R.string.open_motor);
            CustomToastUtils.showControlToast(this, strOpenMotor, true);
            speak(strOpenMotor, new SpeechHelper.OnSpeakFinishedListener() {
                @Override
                public void onSpeakFinished() {
                    if (isFromIpSprite()) {
                        finish();
                    }
                }
            }, false);
            return false;
        }
        String str = getString(R.string.adjust_angle);
        CustomToastUtils.showControlToast(this, str);
        speak(str, false);
        int speedTmp = 0;
        int sleepTmp = 0;
        try {
            speedTmp = Integer.valueOf(speed.getText().toString());
            sleepTmp = Integer.valueOf(sleep.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        seatManager.resetPanPicAngle(new SeatManager.SeatMoveCallBack() {
            @Override
            public void onStart() {
                if (seatManager.isAllowTakePano()) {
                    takePanoramaPic();
                }
            }

            @Override
            public void onMoving(int angle, int status) {
            }

            @Override
            public void onEnd() {
            }
        }, speedTmp, sleepTmp, accelerSensor);
        return true;
    }

    private boolean isSeatDisable() {
        return isSeatMotorDisable();//seatManager == null || seatManager.getDockingManager() == null;
    }

    private boolean isSeatMotorDisable() {
        boolean bDisable = !isCnnedToSeat();
        Log.d(TAG, "isSeatMotorDisable: bDisable = " + bDisable);
        return bDisable;
    }

    private boolean isCnnedToSeat() {
        boolean bCnn = seatManager != null && seatManager.isConnectedToDocking();
        Log.d(TAG, "isCnnedToSeat:  isCnnedToSeat:" + bCnn);
        return bCnn;
    }

    /**
     * 判断底座电机是否已经开启
     *
     * @return
     */
    private boolean isMotorOn() {
        boolean bon = seatManager != null && seatManager.isMotorOn();
        return bon;
    }

    private void takePanoramaPic() {
        if (seatManager.getStatus() != SeatManager.EmPanoStatus.PREPARE) {
            //  return;
        }
        Log.d(TAG, "takePanoramaPic: ");
        if (panoramaTaking) return;
        panoramaTaking = true;
        setIvShutterIcon(panoramaTaking);
        controlMenu.setVisibility(View.INVISIBLE);//拍摄全景照片时，隐藏掉快门按钮；
        controlMenu.hideSubItem();
        String string = getString(R.string.start_shooting);
        speak(string);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        pbPanPic.setVisibility(View.VISIBLE);
        pbPanPic.setProgress(0);
        pbPanPic.setMax(maxProgress);
        AnimUtil.alphaHideView(ivSwitchCamera);
        ivSwitchCamera.setEnabled(false);
        AnimUtil.alphaHideView(ivAlbum);
        ivAlbum.setEnabled(false);
        CustomToastUtils.showControlToast(mContext, string);
        panoramaPics.clear();
    }


    //合成全景图片
    @Subscribe()/*threadMode = ThreadMode.MAIN 主线程影响消息回调效率*/
    public void ontakePanoramaPic(final PanorPicEvent event) {
        Log.d(TAG, "ontakePanoramaPic: EventBus " + event.getClass().getSimpleName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ontakePanoramaPicMainThread(event.path);
            }
        });
    }

    private synchronized void ontakePanoramaPicMainThread(String path) {
        if (panoramaPics.size() >= maxPartPan) {
            return;
        }
        if (!TextUtils.isEmpty(path)) {
            panoramaPics.add(path);
        }
//        if (panoramaPics.size() == 1) {
//            startProgressbar();
//        }
        setProgressBar(/*panoramaPics.size()*/seatManager.getAngleProgressInt(maxProgress));
        //pbPanPic.setProgress((int) (panoramaPics.size() * (100f / maxPartPan)));
        if (panoramaPics.size() == maxPartPan) {
            panoramaTaking = false;
            if (!isFromOtherApp()) {
                setIvShutterIcon(panoramaTaking);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showRightPart();
                        rlMainlayout.invalidate();
                        controlMenu.requestLayout();
                    }
                }, 2000);
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            String name = FileUtils.getPhotoPath()/*getOtherDirPath()*/ + "PNO_" + format.format(new Date(getMillisecondsStartPano())) + ".jpg";
            //String name = "/sdcard/r.jpg";
            long start = SystemClock.elapsedRealtimeNanos();
            showProgressDlg();
            if (!StringUtils.isEmpty(outputPicPath)) {
                name = outputPicPath;
                Log.d(TAG, "ontakePanoramaPicMainThread: onScanCompleted outputPicPath:" + outputPicPath);
            }
            //PanoramaUtil.createPanoramaImage(mImageProcessService, mContext, panoramaPics, name, CameraEngine.isCameraIdBack());
            PanoramaUtil.createPanoramaImage(/*mImageProcessService*/null, mContext, panoramaPics, name, CameraEngine.isCameraIdBack());
            Log.d("myTag", "2222 time ==== " + (SystemClock.elapsedRealtimeNanos() - start));
        }
    }

    private long getMillisecondsStartPano() {
        return seatManager.getMillisecondsStartPano();
    }

    /**
     * 是否从其他APP跳转过来
     *
     * @return
     */
    private boolean isFromOtherApp() {
        return !TextUtils.isEmpty(outputPicPath) || !TextUtils.isEmpty(outputVideoPath);
    }

    /**
     * 是否跳转自IP精灵
     *
     * @return
     */
    private boolean isFromIpSprite() {
        //String action = getIntent().getAction();
        return ACTION_PAI_CAMERA.equals(action);
    }

    private void setProgressBar(int progress) {
        if (!seatManager.isSeatThreadAlive()) {
            return;
        }
        pbPanPic.setProgress(progress);
        if (panoramaPics.size() == 1 && pbPanPic.getVisibility() != View.VISIBLE) {
            pbPanPic.setMax(maxProgress);
            pbPanPic.setVisibility(View.VISIBLE);
        }
        pbPanPic.setClickable(false);
        pbPanPic.setEnabled(false);
        pbPanPic.requestLayout();
        if (/*progress == maxPartPan*/panoramaPics.size() == maxPartPan) {
            pbPanPic.animate().alpha(0).setDuration(1000).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    pbPanPic.setVisibility(View.GONE);
                    pbPanPic.setProgress(0);
                    pbPanPic.setAlpha(1);
                }
            }).start();
        }
    }

    public void stopProgressbar() {
        pbPanPic.setVisibility(View.GONE);
        pbPanPic.setProgress(0);
    }


    private void showProgressDlg() {
        AnimUtil.alphaShowView(ivSwitchCamera);
        AnimUtil.alphaShowView(ivAlbum);
        //pbPanPic.setVisibility(View.GONE);
        //显示进度框
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
        //progressDialog = ProgressDialog.show(mContext, getString(R.string.pano_synthesis), getString(R.string.pano_synthesising), false, false);
//        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
//        builder.setView(R.layout.dlg_pano);
//        builder.setCancelable(false);
//        progressDialog = builder.create();
//        progressDialog .setCanceledOnTouchOutside(false);
//        progressDialog.show();
        View view = LayoutInflater.from(CameraActivity.this).inflate(R.layout.dlg_pano, null);
        progressDialog = new PanoDialog(CameraActivity.this, view, R.style.pano_dialog);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.setCancelable(true);
                    progressDialog.setCanceledOnTouchOutside(true);
                }
            }
        };
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(r);
            }
        });
        rlMainlayout.invalidate();
        handler.postDelayed(r, 15000);
    }

    //全景图片完成
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPanoramaPicComplete(final PanorPicComEvent event) {
        Log.d(TAG, "onPanoramaPicComplete: event.path=" + event.path + " action" + action);
        progressDialog.dismiss();
        boolean bFromOtherApp = isFromOtherApp();
        boolean bFromIpSprite = isFromIpSprite();
        final boolean bSuccess = !TextUtils.isEmpty(event.path);
        if (bSuccess) {
            Account.instance(CameraActivity.this).sendEvent();
        }
        if (!bFromOtherApp && !bFromIpSprite) {
            String msg;
            if (bSuccess) {
                msg = getString(!bFromOtherApp ? R.string.photo_is_taken : R.string.photo_is_taken_chek);
                //getResString(R.string.pano_synthesis_complete);
                CustomToastUtils.showControlToast(mContext, msg);
            } else {
                msg = getResString(R.string.pano_synthesis_err);
                CustomToastUtils.showControlToast(mContext, msg);
            }
            speak(msg);
            CustomToastUtils.showControlToast(mContext, msg);
            Log.d(TAG, "onPanoramaPicComplete: " + msg + "  event.path:" + event.path);
        }
        //TODO 退出应用
        if (bFromOtherApp) {//小派拍摄全景照片退出
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent it = new Intent();
                    it.setData(event.uriObj);
                    it.putExtra("uri", event.uriStr);
                    setResult(RESULT_OK, it);
                    Log.d(TAG, "run: event.uri onPanoramaPicComplete" + event.uriStr + "  event.path:" + event.path);
                    finish();//
//                    Intent itr = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new File(outputPicPath)));
//                    itr.setType("images/jpg");
//                    getBaseContext().startActivity(itr);
//                    onTakePicFinish();
                }
            }, 200);
        } else {
            //adjustLayout(actionMode);
            mLastMediaFile.uri = event.uriObj;
            threadUpdateAlbumBtn();
            Runnable runnable = new Runnable() {//延时刷新相册按钮才有效
                @Override
                public void run() {
                    if (isDestroyed()) {
                        return;
                    }
                    //adjustLayout(actionMode);
                    threadUpdateAlbumBtn();
                }
            };
            handler.postDelayed(runnable, 1000);
            handler.postDelayed(runnable, 3000);
            handler.postDelayed(runnable, 5000);
        }
    }

    // 倒计时拍照
    private void startTakePicDownCount(final int max) {
        if (max <= 0) return;
        sdvDrawView.freshSmileState(/*false*/);
        TimerCountUtil.start(TimerCountUtil.DOWNCOUNT_MODE, max, 0, new TimerCountUtil.TimerCountCallBack() {
            @Override
            public void onStartPre() {

            }

            @Override
            public void onStart() {
                speak(getResString(R.string.start_take_tip), false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                downCounting = true;
                controlMenu.setVisibility(View.INVISIBLE);
                controlMenu.hideSubItem();
            }

            @Override
            public boolean onCount(int count) {
                if (count <= 3) {//3,2,1
                    tvDownCount.setVisibility(View.VISIBLE);
                    tvDownCount.animate().alpha(1).setDuration(0).start();
                    tvDownCount.setText(count + "");
                    tvDownCount.animate().alpha(0).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(800).start();
                    tvDownCount.clearAnimation();
                    if (mbResuming) {
                        if (count == 3) {//避免TTS语音说中文数字
                            speak(getString(R.string.start_take_three_tts));
                        } else if (count == 2) {
                            speak(getString(R.string.start_take_two_tts));
                        } else if (count == 1) {
                            speak(getString(R.string.start_take_one_tts));
                        }
                    }
                }
                return true;
            }

            @Override
            public void onEnd(boolean btakePic) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                downCounting = false;
                tvDownCount.setVisibility(View.GONE);
                if (hasReturn && actionMode == ACTION_IMAGE_MODE) {
                    //
                } else {
                    controlMenu.setVisibility(View.VISIBLE);
                }
                //controlMenu.showSubItem();
                //ivSwitchCamera.setVisibility(View.VISIBLE);
                //ivAlbum.setVisibility(View.VISIBLE);
                if (btakePic) {
                    takePic();
                }
                freshSmileMode();
            }
        });
    }


    //开始录像和录音
    private void startMediaCountTime(final int max, final int cameraMode) {
        if (max <= 0 || videoRecording || voiceRecording) {
            Log.d(TAG, "startMediaCountTime: videoRecording:" + videoRecording
                    + "  voiceRecording:" + voiceRecording);
            return;
        }
        if (cameraMode == VIDEO_MODE) {
            setPrepareVideoRecording(true);
        } else if (cameraMode == VOICE_MODE) {
            setPrepareVoiceRecording(true);
        }
        TimerCountUtil.start(TimerCountUtil.DEFAULTCOUNT_MODE, 0, max, new TimerCountUtil.TimerCountCallBack() {
            @Override
            public void onStartPre() {
                tvTime.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_45sp));
                tvTime.setText(cameraMode == VIDEO_MODE ? "00:00" : "0");//时分秒
                tvTime.requestLayout();
                tvTime.animate().cancel();
                tvTime.setAlpha(1.0f);
                updateTimeLayout();
                tvTime.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStart() {
                long tm = SystemClock.elapsedRealtime();
                //sdvDrawView.setVisibility(View.INVISIBLE);
                showDrawView(false);
                boolean rst = MediaUtil.muteAudioFocus(getApplication(), true);
//                tvTime.setTextSize(getResources().getDimensionPixelSize(R.dimen.text_size_45sp));
//                tvTime.setText(cameraMode == VIDEO_MODE ? "00:00" : "0");//时分秒
//                tvTime.requestLayout();
//                tvTime.setVisibility(View.VISIBLE);
                Log.d(TAG, "onStart: voice 录音与录像" + (SystemClock.elapsedRealtime() - tm) + "  rst:" + rst);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                switch (cameraMode) {
                    case VIDEO_MODE:
                        videoRecording = true;
                        setPrepareVideoRecording(false);
                        setIvShutterIcon(true);
                        if (actionMode == ACTION_NO_MODE) {
                            CameraEngine.setAutoFocusMode();
                            MediaRecorderControl.getInstance().startVideoRecord(cameraSurfaceView.getCamera(), configuration.orientation, getVideoSetListener());
                        } else if (actionMode == ACTION_VIDEO_MODE && !TextUtils.isEmpty(outputVideoPath)) {
                            CameraEngine.setAutoFocusMode();
                            MediaRecorderControl.getInstance().startVideoRecord(cameraSurfaceView.getCamera(),
                                    configuration.orientation, outputVideoPath, getVideoSetListener());
                        } else {
                            return;
                        }
                        controlMenu.hideSubItem();
                        AnimUtil.alphaHideView(ivSwitchCamera);
                        //wakeLockForever();
                        break;
                    case VOICE_MODE:
                        setIvShutterIcon(true);
                        voiceRecording = true;
                        setPrepareVoiceRecording(false);
                        Runnable runnable = new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(500);//需要一定时间的延时
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (voiceRecording == true) {
                                    MediaRecorderControl.getInstance().startVoiceRecord(picName);
                                }
                            }
                        };
                        new Thread(runnable, "---thread delayed start voice recording---").start();
                        //ivVoicePic.setVisibility(View.VISIBLE);//不放照片，直接暂停预览
                        controlMenu.hideSubItem();
                        AnimUtil.alphaHideView(ivSwitchCamera);
                        AnimUtil.alphaHideView(ivSmailControl);
                        //showDrawView(false);
                        break;
                }
                //freshSmileMode();//耗时太长去掉
                if (cameraMode == VOICE_MODE) {
                    controlMenu.startSoundWaves();
                }
                //onstart方法耗时大约在14~20ms
                Log.d(TAG, "onStart: waste time:" + (SystemClock.elapsedRealtime() - tm));
            }

            private long mSpace;

            @Override
            public boolean onCount(int count) {
                long tm = SystemClock.elapsedRealtime();
                String text = "";
                if (cameraMode == VIDEO_MODE) {
                    tryWakeLock();
                    text = StringFormatUtil.getTimeStrHms(count);
                    long availableSpare = FileUtils.getAvailableSpare();
                    long totalSpace = 0;
                    try {
                        File file = new File(MediaRecorderControl.getInstance().getVideoPath());
                        totalSpace = file.length();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "onCount: " + e);
                    }
                    Log.d(TAG, "onCount:  videototalSpace:" + totalSpace / (1024 * 1024));
                    if (count > 10 && availableSpare / 1024 == mSpace / 1024
                            || totalSpace > 4 * 1000 * 1000 * 1000L) {//当文件大约为3.76G接近4G时，停止录制视频
                        //if (count > 35) {
                        //当SD卡空间大小在1s内变化小于1k时，认定录像已停止,
                        // 或者单个文件大小即将到达极限时，重新录像。
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //录制视频文件已到达最大值,通知停止视频录制，1s之后继续录制视频，
                                // android文件的极限大小一般为4G
                                controlMenu.performClickCenterView();//停止录制视频
                                Log.d(TAG, "run: stopMediaCountTime availableSpare:"
                                        + mSpace / (1024 * 1024));
                                if (mSpace > 10 * 1024 * 1024) {
                                    postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //继续录制视频
                                            if (CameraActivity.this.cameraMode == VIDEO_MODE) {
                                                controlMenu.performClickCenterView();
                                            }
                                        }
                                    }, 1000);
                                }
                            }
                        }, 10);
                    }
                    mSpace = availableSpare;
                    if (mSpace <= 10 * 1024 * 1024) {
                        testSdcardMemoryWithDlg(10);
                    }
                } else if (cameraMode == VOICE_MODE) {
                    text = "" + count;/*StringFormatUtil.getTimeStr2(count)*/
                } else {//出现异常
                    text = "" + count;
                    animateDismissTime();
//                    TimerCountUtil.stop();
//                    tvTime.setVisibility(View.GONE);
//                    EventBus.getDefault().post(EmCameraMode.getByIdx(cameraMode));
                    return false;
                }
                tvTime.setText(text);
                sdvDrawView.clearData();
                Log.d(TAG, "onCount: tvTime.setText:" + text
                        + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
                return true;
            }

            private void animateDismissTime() {
                tvTime.animate().alpha(0).setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                tvTime.setVisibility(View.GONE);
                                tvTime.setAlpha(1.0f);
                            }
                        }).start();
            }

            @Override
            public void onEnd(boolean takePic) {
                //tvTime.setVisibility(View.GONE);
                animateDismissTime();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                switch (cameraMode) {
                    case VIDEO_MODE:
                        videoRecording = false;
                        setPrepareVideoRecording(false);
                        setIvShutterIcon(false);
                        String videoPath = MediaRecorderControl.getInstance().stopVideoRecord(mContext, mOnVideoEndListener);
                        CameraEngine.setPicFocusMode();
                        cameraView.resetFocusMeterPara();
                        mLastMediaFile.uri = Uri.fromFile(new File(videoPath));
                        controlMenu.showSubItem();
                        AnimUtil.alphaShowView(ivSwitchCamera);

                        if (!TextUtils.isEmpty(videoPath)) {
                            Bitmap bmp = MediaUtil.getVideoThumbnail(configuration.orientation, cameraSurfaceView.getCameraID(), videoPath, 100, 100);
                            if (bmp == null) {
                                getSaveVideoThumbnailInThread(videoPath);
                            }
                            showTakeedPic(bmp);
                        }
                        break;
                    case VOICE_MODE:
                        setIvShutterIcon(false);
                        Camera camera = CameraEngine.getCamera();
                        if (camera != null) {
                            camera.startPreview();
                            try {
                                // camera.startFaceDetection();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "onEnd: " + e);
                            }
                            cameraView.setFocusEnable(true);
                        }
                        voiceRecording = false;
                        setPrepareVoiceRecording(false);
                        freshSmileMode();
                        MediaRecorderControl.getInstance().stopVoiceRecord();
                        isTakeVoicePic = false;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                freshSmileMode();
                            }
                        }, 1000);
                        //模拟快门声音
                        if (!isSilentOrVibrate()) {
                            getAudioPlay().playSound(AudioPlay.EmSound.AUDIO_END);
                        }
                        ivVoicePic.setVisibility(View.GONE);
                        controlMenu.showSubItem();
                        AnimUtil.alphaShowView(ivSwitchCamera);
                        AnimUtil.alphaShowView(ivSmailControl);

                        //showDrawView(true);
                        showTakeedPic(takePicBmp);
                        break;
                    default:

                }
                MediaUtil.muteAudioFocus(getApplication(), false, 200);
                //sdvDrawView.setVisibility(View.VISIBLE);
                showDrawView(true);
                //wakeLockReset();
            }
        });
    }

    /**
     * 对于Android默认方法获取缩略图失败的视频，可调用该方法
     *
     * @param videoPath
     */
    private void getSaveVideoThumbnailInThread(final String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            return;
        }
        Runnable runnable = new Runnable() {
            public void run() {
                Bitmap bmp = null;
                //耗时操作
                bmp = MediaUtil.getVideoFramePicture(videoPath);
                try {
                    if (bmp != null) {
                        float scale = 512 * 1.0f / bmp.getWidth();
                        bmp = BitmapUtil.resizeBitmap(bmp, scale);
                        bmp = BitmapUtil.getRotationModifedBitmap(bmp,
                                CameraEngine.getCameraRotation(), CameraEngine.isCameraIdBack());
                        File file = new File(videoPath);
                        String name = file.getName();
                        String picName = name.substring(0, name.lastIndexOf(".")) + ".jpg";
                        FileUtils.saveThumbnail(bmp, picName);
                        Log.d(TAG, "getSaveVideoThumbnailInThread: saveThumbnail 已通过第三方库获取了帧图片并保存！  scale:" + scale);
                    } else {
                        new File(videoPath).delete();
                        Log.d(TAG, "getSaveVideoThumbnailInThread: run: file was deleted for get Frame pic failed!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: getSaveVideoThumbnailInThread");
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("---thread saving some video's thumbnails---");//保存个别缩略图的线程
        thread.start();
    }

    @NonNull
    private MediaScannerConnection.OnScanCompletedListener mOnVideoEndListener = new MediaScannerConnection.OnScanCompletedListener() {
        @Override
        public void onScanCompleted(String path, Uri uri) {
            mLastMediaFile.uri = uri;
            if (actionMode == ACTION_VIDEO_MODE) {
                onRecordVideoFinish();
            }
            try {
                boolean bBack = CameraEngine.getCameraID() == Camera.CameraInfo.CAMERA_FACING_BACK;
                String type = "0" + (bBack ? "0" : "1") + "00";
                MediaUtil.insertPTVideo(CameraActivity.this, uri, type);
                FileUtils.tryAddFirstMediaTime();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "onScanCompleted: " + e.toString());
            }
        }
    };

    //停止录像和录音

    private void stopMediaCountTime() {
        //tvTime.setVisibility(View.GONE);
        if (TimerCountUtil.isCancelled()) {
            return;
        }
        TimerCountUtil.stop();
        MediaUtil.muteAudioFocus(getApplication(), false, 1000);//
    }

    //根据相机模式改变快门图标
    private void setIvShutterIcon(boolean isRecording) {
        if (isRecording) {
            AnimUtil.alphaHideView(ivAlbum);
            switch (cameraMode) {
                case VOICE_MODE:
                    controlMenu.setCenterViewRes(R.drawable.icon_yousheng_recording);
                    //controlMenu.startSoundWaves();
                    break;
                case PANORAMA_MODE:
                    controlMenu.setCenterViewRes(R.drawable.icon_qunjin_recording);
                    break;
                case COMMON_MODE:
                    controlMenu.setCenterViewRes(R.drawable.icon_camera_shoot_bg);
                    break;
                case VIDEO_MODE:
                    controlMenu.setCenterViewRes(R.drawable.icon_shipinpaishe_recording);
                    break;
            }
        } else {
            AnimUtil.alphaShowView(ivAlbum);
            switch (cameraMode) {
                case VIDEO_MODE:
                case VOICE_MODE:
                    controlMenu.setCenterViewRes(R.drawable.icon_yousheng_record_bg);
                    controlMenu.stopSoundWaves();
                    break;
                case PANORAMA_MODE:
                case COMMON_MODE:
                    controlMenu.setCenterViewRes(R.drawable.icon_camera_shoot_bg);
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        long tm = SystemClock.elapsedRealtime();
        CameraEngine.setActivity(this);
        Log.d(TAG, "onConfigurationChanged stopPreview: waste Time:" + (SystemClock.elapsedRealtime() - tm));
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "super.onConfigurationChanged: waste Time:" + (SystemClock.elapsedRealtime() - tm));
        configurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: waste Time:" + (SystemClock.elapsedRealtime() - tm));
    }

    private void configurationChanged(final Configuration newConfig) {
        configuration = newConfig;
        cameraSurfaceView.setOrientation(newConfig.orientation);
        Log.d(TAG, "configurationChanged: update before");
        long tm = SystemClock.elapsedRealtime();
        detectFacer.update(cameraSurfaceView.getCameraID());//TODO Victor mod
        Log.d(TAG, "configurationChanged: update after detectFacer.update waste time=" + (SystemClock.elapsedRealtime() - tm));
        mContext = this;
        instance = this;
        blackView.invalidate();
        rlMainlayout.invalidate();
        updateSurfaceSize();
        Log.d(TAG, "configurationChanged: ");
    }

    //自动聚焦
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoveDevice(MoveSensorEvent event) {
        if (mRunOnMoveDevice == null) {
            mRunOnMoveDevice = new Runnable() {
                @Override
                public void run() {
                    if (!cameraView.isFocusing()) {
                        cameraView.focusOnTouch(cameraSurfaceView.getCamera(), DensityUtil.getDeviceWidth(mContext) / 2, DensityUtil.getDeviceHeight(mContext) / 2);
                        if (!CameraEngine.isCameraIdBack()) {
                            sensorPause();
                        }
                    }
                }
            };
        }
        handler.removeCallbacks(mRunOnMoveDevice);
        handler.post(mRunOnMoveDevice);
    }

    @Override
    public void onBackPressed() {
        try {
            super.onBackPressed();//出现过调用此方法崩溃的情况
            setResult(RESULT_CANCELED, getIntent());
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onBackPressed: ");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: ");
    }

    private void onTakePicFinish(String uri) {
        CameraEngine.getCamera().stopPreview();
        Intent inten = getIntent();
        inten.putExtra("uri", uri);
        setResult(RESULT_OK, inten);
        finish();
        Log.d(TAG, TAG_TAKEPIC + "  onTakePicFinish: uri" + uri + "  outputPicPath:" + outputPicPath);
    }

    private void onRecordVideoFinish() {
        Intent inten = getIntent();
        setResult(RESULT_OK, inten);
        finish();
    }

    private String getResString(int id) {
        return mContext.getResources().getString(id);
    }


    private boolean isSilentOrVibrate() {
        int ringerMode = mAudioManager.getRingerMode();
        boolean bSilent = ringerMode == AudioManager.RINGER_MODE_VIBRATE
                || ringerMode == AudioManager.RINGER_MODE_SILENT;
        Log.d(TAG, "isSilentOrVibrate: " + bSilent);
        return bSilent;
    }

    private boolean speak(String str) {
        return speak(str, null);
    }


    private boolean speak(String str, boolean allowStop) {
        return speak(str, null, allowStop);
    }

    private boolean speak(String str, SpeechHelper.OnSpeakFinishedListener listener) {
        return speak(str, listener, true);
    }

    private boolean speak(String str, SpeechHelper.OnSpeakFinishedListener listener, boolean allowStop) {
        stopSpeak();
        if (isSilentOrVibrate()) {
            if (listener != null) {
                listener.onSpeakFinished();
            }
            return false;
        }
        SpeechHelper.instance().speakMessage(str, listener, allowStop);
        Log.d(TAG, "speak: " + str);
        return true;
    }

    private void stopSpeak() {
        SpeechHelper.instance().stopSpeaking();
        Log.d(TAG, "stopSpeak: ");
    }

    private void tryStopSpeak() {
        if (!SpeechHelper.instance().isAllowStop()) {
            return;
        }
        stopSpeak();
    }

    private boolean isSpeaking() {
        return SpeechHelper.instance().isSpeaking();
    }

    public void printSpeed() {
        if (isSeatDisable()) {
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long tm = SystemClock.elapsedRealtime();
                int[] angle = seatManager.getMotorAngle();//耗时20~200ms,通常为30~50ms
                Log.d(TAG, "run: printSpeed ----wate time=" + (SystemClock.elapsedRealtime() - tm) +
                        "--------angle[2]=" + angle[2]);
                handler.removeCallbacks(this);
                handler.postDelayed(this, 5000);
            }
        };
        //handler.postDelayed(runnable, 1000);
    }

    private boolean isTakePanorama() {
        return seatManager.isTakingPano();
    }

    public void initDockingBroadCast() {
        if (mDockBroadcast == null) {
            mDockBroadcast = new DockingBroadcast();
        }
        mLowBatteryListener = new DockingBroadcast.OnDockingLowBatteryListener() {
            @Override
            public void onDockingLowBattery(int level) {
                if (mDockingBatteryLevel >= LOW_BATTERY_THRESHOLD_FOR_DOCKING && level < LOW_BATTERY_THRESHOLD_FOR_DOCKING) {
                    AlertDialog.Builder dlg;
                    dlg = new AlertDialog.Builder(CameraActivity.this);
                    dlg.setCancelable(true);
                    dlg.setPositiveButton(R.string.common_confirm, null);
                    dlg.setMessage(R.string.docking_low_battery_level);
                    dlg.setIcon(android.R.drawable.ic_dialog_alert);
                    AlertDialog dialog = dlg.create();
                    dialog.getWindow().addFlags(getFlagsShowLocked());
                    //dialog.show();//TODO 底座电量不足时，不显示对话框，交由系统显示
                }
                mDockingBatteryLevel = level;
            }
        };
        mMotorStatusListener = new DockingBroadcast.OnDockingMotorStatusListener() {
            @Override
            public void onDockingMotorToEnd(int[] motorStatus) {
                if (seatManager == null || seatManager.getDockingManager() == null) {
                    return;
                }
                long tm = SystemClock.elapsedRealtime();
                try {
                    seatManager.sMotorStatus[0] = motorStatus[0];
                    seatManager.sMotorStatus[1] = motorStatus[1];
                    seatManager.sMotorStatus[2] = motorStatus[2];
                    synchronized (seatManager.sMotorStatus) {
                        seatManager.sMotorStatus.notifyAll();
                    }
                    int[] angles = seatManager.getLastMotorAngle();
                    Log.d(TAG, "onDockingMotorToEnd: sMotorStatus  status:"
                            + motorStatus[0] + "," + motorStatus[1] + "," + motorStatus[2] +
                            "  angles:" + angles[0] + "," + angles[1] + "," + angles[2] +
                            "  waste time:" + (SystemClock.elapsedRealtime() - tm));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        mPadConnectStatusListener = new DockingBroadcast.OnDockingPadConnectStatusListener() {
            @Override
            public void onDockingPadConnectStatusListener(boolean isConnected) {
                if (controlMenu == null) {
                    return;
                }
                //出现过广播状态出错的情况，因而多做一层判断
                boolean isConnected1 = isConnected || isCnnedToSeat();
                controlMenu.setSeatDisable(!isConnected1);
                if (!isConnected1 && cameraMode == PANORAMA_MODE && mbResuming) {
                    controlMenu.getItemPic().performClick();
                }
                Log.d(TAG, "onDockingPadConnectStatusListener: isConnected:"
                        + isConnected + "  isConnected1:" + isConnected1);
            }
        };
        mDockingPowerStatusChangedListener = new DockingBroadcast.OnDockingPowerStatusChangedListener() {
            @Override
            public void onDockingPowerStatusChanged(boolean powerOn) {
                if (mPadConnectStatusListener != null) {
                    boolean cnnedToSeat = isCnnedToSeat();
                    mPadConnectStatusListener.onDockingPadConnectStatusListener(/*powerOn*/cnnedToSeat);
                    Log.d(TAG, "onDockingPowerStatusChanged: cnnedToSeat:"
                            + cnnedToSeat + "  isPowerOn:" + powerOn);
                }
            }
        };
        mDockBroadcast.setOnDockingLowBatteryListener(mLowBatteryListener);
        mDockBroadcast.setOnDockingMotorStatusListener(mMotorStatusListener);
        mDockBroadcast.setOnDockingPadConnectStatusListener(mPadConnectStatusListener);
        mDockBroadcast.setOnDockingPowerStatusChangedListener(mDockingPowerStatusChangedListener);
        registerReceiver(mDockBroadcast, DockingBroadcast.getIntentFilter());//TODO
    }

    private void unregisterDockingBroadCast() {
        unregisterReceiver(mDockBroadcast);
    }


    private void initOrientationListener() {
        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            int last = -123;
            long mTmLastUpdate = 0;

            @Override
            public void onOrientationChanged(int rotation) {
                int displayOrientation = CameraEngine.getCameraRotation();
                displayOrientation -= 90;
                if (displayOrientation < 0) {
                    displayOrientation += 360;
                }
                Log.d(TAG, "onOrientationChanged: last:" + last +
                        "  current:" + rotation
                        + "  CameraEngine.getCameraRotation:" + displayOrientation +
                        "  diff:" + (rotation - last));
                last = rotation;
                if (rotation > 333 || rotation < 33) {
                    rotation = 0;
                }

                int abs = Math.abs(rotation - displayOrientation);
                long time = SystemClock.elapsedRealtime();
                if (abs > 180 - 30 && abs < 180 + 30 /*&& time - mTmLastUpdate > 1000*/) {
                    Log.d(TAG, "initOrientationListener onOrientationChanged: ");
                    onConfigurationChanged(configuration);
                    mTmLastUpdate = time;
                }
            }
        };
        //mOrientationListener.enable();
    }

    private void postRotationRunable() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (mRunUpdateRotation == null) {
            mRunUpdateRotation = new Runnable() {
                int rotationLast = CameraEngine.getRotationResult(CameraActivity.this, CameraEngine.getCameraID());

                @Override
                public void run() {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (mbResuming) {
                        int rst = CameraEngine.getRotationResult(CameraActivity.this, CameraEngine.getCameraID());//耗时7~8ms
                        int orientation = CameraEngine.getCameraRotation();
                        int diff = Math.abs(rst - orientation);
                        SurfaceHolder holder = cameraSurfaceView.getHolder();
                        Log.d(TAG, "postRotationRunable run: rst" + rst
                                + "  orientation" + orientation
                                + " rotationLast" + rotationLast
                                + "  diff" + diff + "  SurfaceFrame:" + holder.getSurfaceFrame()
                                + " tr/ty" + cameraSurfaceView.getX() + " " + cameraSurfaceView.getY());
                        if (diff == 180) {
                            onConfigurationChanged(configuration);
                            rotationLast = rst;
                        }
                        //updateSurfaceSize();
                        //ViewGroup.LayoutParams para = (ViewGroup.LayoutParams) cameraSurfaceView.getLayoutParams();
                        //Log.d(TAG, "onSetVideoFrameListener run: frameWidth:"
                        //        + "  width:" + cameraSurfaceView.getWidth()
                        //        + "  height:" + cameraSurfaceView.getHeight()
                        //        + "  margin:" + cameraSurfaceView.getLeft() + " " + cameraSurfaceView.getTop()
                        //        + " " + cameraSurfaceView.getRight() + " " + cameraSurfaceView.getBottom());
                    }
                    handler.removeCallbacks(mRunUpdateRotation);
                    handler.postDelayed(mRunUpdateRotation, 500);
                }
            };
        }
        handler.postDelayed(mRunUpdateRotation, 1000);
    }

    final String mRcvAlarm = "com.android.alarmclock.ALARM_ALERT";
    private BroadcastReceiver mReceiverAlarm = null;

    private void registerAlarm() {
        if (mReceiverAlarm != null) {
            return;
        }
        mReceiverAlarm = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MediaRecorderControl instance = MediaRecorderControl.getInstance();
                String action = intent.getAction();
                if ((!TextUtils.isEmpty(action)) && action.equalsIgnoreCase(mRcvAlarm)
                        && (instance.isVideoRecording() || instance.isVoiceRecording())) {
                    controlMenu.performClick();
                }
            }
        };
        registerReceiver(mReceiverAlarm, new IntentFilter(mRcvAlarm));
    }

    private void unregisterAlarm() {
        unregisterReceiver(mReceiverAlarm);
    }

    public SeatManager.EmPanoStatus getPanoStatus() {
        return seatManager.getStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        threadUpdateAlbumBtn();
        testSdcardMemoryWithDlg(10);
    }

    private void delayInitContent() {
        if (!mbDelayInit) {
            initDockingBroadCast();
            //startThreadUpdatePanoMenuStatus();
            registerAlarm();
            if (mbShowBattery) {
                registerReceiver(getBatteryLevelBroadcast(CameraActivity.this), Intent.ACTION_BATTERY_CHANGED);
            }
            postRotationRunable();
            detectFacer.update(cameraSurfaceView.getCameraID());
            freshSmileMode();
            mbDelayInit = true;
            tryFadeSmile();

            accelerSensor = new AccelerSensorManager(CameraActivity.this);
            if (CameraEngine.isCameraIdBack()) {
                sensorResume();
            }
            checkSeatStatus();
            showDrawView(true);
            cameraSurfaceView.setIsShouldStopPreview(new CameraSurfaceView.IsShouldStopPreview() {
                @Override
                public boolean isShouldStopPreview() {
                    return isTakeVoicePic || !TextUtils.isEmpty(outputPicPath);//从小派中跳转或者拍摄有声照片
                }
            });
            getAudioPlay();
            cameraView.focusOnTouch(CameraEngine.getCamera(), DensityUtil.getDeviceWidth(CameraActivity.this) / 2,
                    DensityUtil.getDeviceHeight(CameraActivity.this) / 2);
        }
    }

    private void checkSeatStatus() {
        boolean cnnedToSeat = isCnnedToSeat();
        controlMenu.setSeatDisable(!cnnedToSeat);
        if (!cnnedToSeat && cameraMode == PANORAMA_MODE) {
            controlMenu.getItemPic().performClick();
        }
    }

    public long getLeftMemory() {
        long tm = SystemClock.elapsedRealtime();
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long max = runtime.maxMemory();
        //max = Math.max(max, 1204 * 1204 * 180);
        long free = runtime.freeMemory();
        Log.d(TAG, "getLeftMemory:   total:" + total / (1024 * 1024)
                + "  max:" + max / (1024 * 1024)
                + "  free:" + free / (1024 * 1024)
                + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
        return max - total;
    }

    /**
     * 是否有充足的内存可供拍照<br>
     * 前置摄像头21M，后置摄像头31M *裕量系数
     *
     * @return 内存是否充足
     */
    public boolean isMemoryEnoughForPic() {
        long left = getLeftMemory();
        Camera.Size size = CameraEngine.getSizePic();
        if (size == null) {
            return true;
        }
        int min = size.width * size.height * 4;//4为经验值 每个像素两个字节
        boolean bRet = left > Math.max(min, 21 * 1024 * 1024) * (SaveTask.getTaskCnt() + 2);
        if (!bRet) {
            System.gc();
        }
        Log.d(TAG, "isMemoryEnoughForPic:   TaskCnt:" + SaveTask.getTaskCnt());
        return bRet;//*2,加大裕量
    }

    private boolean updateSurfaceSize() {
        //updateSurfaceSize(cameraMode, isVideoRecording());
        return bPutaoDev || updateSurfaceSize(cameraMode, cameraModeOld);
    }

    private boolean isVideoRecording() {
        return videoRecording;
    }

    /**
     * 根据是否录像来改变预览区域大小
     *
     * @param cameraMode
     * @param bRecording 是否录像
     */
    private void updateSurfaceSize(int cameraMode, boolean bRecording) {
        MediaRecorderControl.OnSetVideoFrameListener tmp = getVideoSetListener();
        if (tmp == null) {
            return;
        }
        if (cameraMode == VIDEO_MODE && bRecording) {
            CamcorderProfile profile = MediaRecorderControl.getCamcorderProfile(CameraEngine.isCameraIdBack());
            tmp.onSetVideoFrameListener(profile.videoFrameWidth, profile.videoFrameHeight);
        } else {
            tmp.onReset();
        }
        Log.d(TAG, "updateSurfaceSize: ");
    }

    private boolean updateSurfaceSize(int cameraModeNew, int cameraModeOld) {
        long tm = SystemClock.elapsedRealtime();
        MediaRecorderControl.OnSetVideoFrameListener tmp = getVideoSetListener();
        boolean fullScreenNew = EmCameraMode.isFullScreen(cameraModeNew);
        boolean fullScreenOld = EmCameraMode.isFullScreen(cameraModeOld);
        boolean bHasChangedViewSize = false;
        boolean bCheck = true;
        if (bCheck) {
            if (!fullScreenNew/* && fullScreenOld*/) {
                CamcorderProfile profile = MediaRecorderControl.getCamcorderProfile(CameraEngine.isCameraIdBack());
                bHasChangedViewSize = tmp.onSetVideoFrameListener(profile.videoFrameWidth, profile.videoFrameHeight);
            } else if (fullScreenNew/* && !fullScreenOld*/) {
                bHasChangedViewSize = tmp.onReset();
            }
            if (!bHasChangedViewSize) {//再次检查预览的尺寸是否与预期的一致
                //bHasChangedViewSize = !CameraEngine.isCameraPreviewSizeAsWanted();
                //Log.d(TAG, "updateSurfaceSize: 相机实际设置参数与预期参数不一致！");
            }
            if (bHasChangedViewSize) {
                //cameraSurfaceView.freshCamera();
                CameraEngine.updatePreviewSize();
                //startAnimBlurFadeOut(500);
            }
        } else {
            if (fullScreenNew != fullScreenOld) {
                cameraSurfaceView.freshCamera();
            }
        }

        Log.d(TAG, "updateSurfaceSize:  width:" + cameraSurfaceView.getWidth()
                + "  height:" + cameraSurfaceView.getHeight()
                + "  waste time:" + (SystemClock.elapsedRealtime() - tm)
                + "  bHasChangedViewSize:" + bHasChangedViewSize);
        return bHasChangedViewSize;
    }

    public MediaRecorderControl.OnSetVideoFrameListener getVideoSetListener() {
        return cameraSurfaceView.getVideoSetListener();
    }

    private BroadcastReceiver getBatteryLevelBroadcast(final Context ctx) {
        if (mRcvBatteryLevel == null) {
            mRcvBatteryLevel = new BroadcastReceiver() {
                public boolean mbHasShow15 = false;
                public boolean mbHasShow7 = false;
                AlertDialog dlg15, dlg7;

                private AlertDialog getDlg15() {
                    if (dlg15 == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setMessage(R.string.low_battery_warning15);
                        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //TODO
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CameraActivity.this.finish();
                            }
                        });
                        dlg15 = builder.create();
                        dlg15.getWindow().addFlags(getFlagsShowLocked());
                        dlg15.setCanceledOnTouchOutside(false);
                        dlg15.setCancelable(false);
                    }
                    return dlg15;
                }

                private AlertDialog getDlg7() {
                    if (dlg7 == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setMessage(R.string.low_battery_warning7);
                        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        dlg7 = builder.create();
                        dlg7.getWindow().addFlags(getFlagsShowLocked());
                        dlg7.setCanceledOnTouchOutside(false);
                        dlg7.setCancelable(false);
                    }
                    return dlg7;
                }

                public static final int LEVEL_15 = 15;
                public static final int LEVEL_7 = 7;

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (context == null || intent == null) {
                        return;
                    }
                    String action = intent.getAction();
                    if ((!TextUtils.isEmpty(action)) && action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL;
                        //得到系统当前电量
                        int level = intent.getIntExtra("level", 0);
                        //取得系统总电量
                        int total = intent.getIntExtra("scale", 100);
                        level = (level * 100) / total;

                        boolean debugable = CommonUtils.isApkDebugable(context);
                        Log.d(TAG, "onReceive: ACTION_BATTERY_CHANGED  level:" + level
                                + "  isCharging:" + isCharging + "  debugable:" + debugable);
                        if (isCharging && debugable && level == 1) {//系统出现过电量一直为1%的bug
                            return;
                        }

                        //当从其他应用跳转过来的时候，不弹框
                        if (level >= LEVEL_15 || isFromOtherApp()) {
                            mbHasShow7 = false;
                            mbHasShow15 = false;
                            if (dlg15 != null && dlg15.isShowing()) {
                                dlg15.cancel();
                            }
                            if (dlg7 != null && dlg7.isShowing()) {
                                dlg7.cancel();
                            }
                            return;
                        }

                        if (level < LEVEL_15 && !mbHasShow15) {
                            getDlg15().show();//
                            mbHasShow15 = true;
                        }

                        //textView.setText("当前电量："+(level*100)/total+"%");
                        //当电量小于15%时触发
                        if (level < LEVEL_15 && level > LEVEL_7 && !mbHasShow15) {
                            getDlg15().show();
                            mbHasShow15 = true;
                        } else if (level <= LEVEL_7 && !mbHasShow7) {
                            //getDlg7().show();//TODO 不提醒
                            mbHasShow7 = true;
                        }
                    }
                }
            };
        }
        return mRcvBatteryLevel;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private long mTmLastTestSpace;

    private boolean testSdcardMemoryWithDlg(int sizeMb) {
        boolean bFree = FileUtils.isAvaiableSpace(sizeMb);
        if (bFree) {
            return true;
        }
        if (isVideoRecording()) {
            stopMediaCountTime();
        }

        if (mDlgLowStorage != null && mDlgLowStorage.isShowing()) {
            mDlgLowStorage.cancel();
        }
        View view = LayoutInflater.from(CameraActivity.this).inflate(R.layout.dlg_low_storage, null);
        TextView tv = (TextView) view.findViewById(R.id.tv);
        tv.setText(getString(R.string.sdcard_no_space, sizeMb + ""));
        View btn = view.findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDlgLowStorage != null) {
                    mDlgLowStorage.cancel();
                    finish();
                }
            }
        });

        mDlgLowStorage = new PanoDialog(CameraActivity.this,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                view, R.style.pano_dialog);
        mDlgLowStorage.getWindow().addFlags(getFlagsShowLocked());
        //mDlgLowStorage.show();//启动系统的Activity
        try {
            long timeMillis = SystemClock.elapsedRealtime();
            if (timeMillis - mTmLastTestSpace > 1500) {//间隔在一定时间之后才给系统发送通知
                mTmLastTestSpace = timeMillis;
                startActivity("com.putao.action.lowmemory");
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "testSdcardMemoryWithDlg: " + e.toString());
        }
        return false;
    }

    public boolean isPrepareVideoRecording() {
        Log.d(TAG, "isPrepareVideoRecording: " + mbPrepareVideoRecording);
        return mbPrepareVideoRecording;
    }

    public void setPrepareVideoRecording(boolean bPrepareVideoRecording) {
        mbPrepareVideoRecording = bPrepareVideoRecording;
    }

    public boolean isPrepareVoiceRecording() {
        Log.d(TAG, "isPrepareVoiceRecording: " + mbPrepareVoiceRecording);
        return mbPrepareVoiceRecording;
    }

    public void setPrepareVoiceRecording(boolean bPrepareVoiceRecording) {
        mbPrepareVoiceRecording = bPrepareVoiceRecording;
    }

    public AudioPlay getAudioPlay() {
        if (mAudioPlay == null) {
            mAudioPlay = new AudioPlay(CameraActivity.this);
            cameraSurfaceView.setAudioPlay(mAudioPlay);
            seatManager.setAudioPlay(mAudioPlay);
        }
        return mAudioPlay;
    }

    public Bitmap getFrameBitmap() {
        byte[] frameData = /*seatManager*/CameraEngine.getOnPreviewFrameData();
        if (frameData == null) {
            return null;
        }
        int len = frameData.length;
        Camera.Size size = CameraEngine.getSizePre();
        if (size == null) {
            Log.d(TAG, "getFrameBitmap: return null");
            return null;
        }
        int res = size.width * size.height;
        if (len != res * 1.5f) {
            CamcorderProfile tmp = MediaRecorderControl.getCamcorderProfile(CameraEngine.isCameraIdBack());
            size.width = tmp.videoFrameWidth;
            size.height = tmp.videoFrameHeight;
        }
        res = size.width * size.height;
        if (len != res * 1.5f) {
            Log.d(TAG, "getFrameBitmap: ---尺寸有误--- return null  " + len * 1.0f / res);
            return null;
        }

        Bitmap mp = null;
        try {
            mp = BitmapToVideoUtil.yuv2bitmap(frameData, size.width, size.height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "getFrameBitmap: dataLenght:" + len + "  res:" + res + "  len/res:" + len * 1.0f / res);
        return mp;
    }

    public Bitmap getRotatedFrameBitmap() {
        boolean bBack = CameraEngine.isCameraIdBack();
        int rotationResult = CameraEngine.getRotationResult();
        return BitmapUtil.rotation90N(getFrameBitmap(), rotationResult, !bBack);
    }

    public Bitmap getBlurBitmap() {
        Bitmap mp = getFrameBitmap();
        if (mp == null) {
            return null;
        }
        Bitmap blur = FastBlur.blur(mp, 12, 3);
        boolean bBack = CameraEngine.isCameraIdBack();
        int rotationResult = CameraEngine.getRotationResult();
        blur = BitmapUtil.rotation90N(blur, rotationResult, !bBack);
        return blur;
    }

    private CircleMenuLayout.OnSwitchAnimateStartListener mOnSwitchListener;

    public CircleMenuLayout.OnSwitchAnimateStartListener getOnSwitchListener() {
        if (mOnSwitchListener == null) {
            mOnSwitchListener = new CircleMenuLayout.OnSwitchAnimateStartListener() {
                @Override
                public void onSwitchAnimateStart(EmCameraMode start, EmCameraMode end, int duration) {
                    if (start == end) {
                        return;
                    }
                    boolean videoAbout = (start == EmCameraMode.VIDEO || end == EmCameraMode.VIDEO);
                    //blur.setAlpha(1.0f);
                    onBlurAnimStart();
                    if (!videoAbout) {
                        int duration1 = duration + 1200;
                        startAnimBlurFadeOut(duration1);
                    } else {
                        //startAnimBlurFadeOut(1000);
                    }
                }
            };
        }
        return mOnSwitchListener;
    }

    ValueAnimator mAnimBlur;

    private void startAnimBlurFadeOut(int duration) {
        //mAnimBlur.setDuration();
        if (mAnimBlur != null) {
            mAnimBlur.end();
        }
        mAnimBlur = ValueAnimator.ofFloat(1.0f, 0);
        mAnimBlur.removeAllUpdateListeners();
        mAnimBlur.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (Float) animation.getAnimatedValue();
                blur.setVisibility(View.VISIBLE);
                blur.setAlpha(alpha);
            }
        });
        mAnimBlur.removeAllListeners();
        mAnimBlur.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                blur.setVisibility(View.GONE);
                //updateSurfaceSize();
            }
        });
        mAnimBlur.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimBlur.setDuration(duration);
        mAnimBlur.start();
    }

    public void updateTimeLayout() {
        boolean bLand = CameraEngine.isOrintationLand();
        boolean bVideoMode = cameraMode == VIDEO_MODE;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tvTime.getLayoutParams();
        int marginTop = getResources().getDimensionPixelSize(R.dimen.time_margin_top);
        params.topMargin = marginTop;
        boolean sbFillHeight = Math.abs(cameraSurfaceView.getHeight() -
                DensityUtil.getDeviceHeight(CameraActivity.this)) < 100;
        if ((!sbFillHeight) && bVideoMode && bLand) {
            params.topMargin = (int) (marginTop * 1.5f);
        }
        tvTime.setLayoutParams(params);
        tvTime.requestLayout();
        tvTime.invalidate();
    }
}
