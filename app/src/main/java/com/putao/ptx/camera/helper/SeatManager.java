package com.putao.ptx.camera.helper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.putao.DockingManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.putao.ptx.camera.eventbus.PanorPicEvent;
import com.putao.ptx.camera.model.AudioPlay;
import com.putao.ptx.camera.util.BitmapToVideoUtil;
import com.putao.ptx.camera.util.BitmapUtil;
import com.putao.ptx.camera.util.CustomToastUtils;
import com.putao.ptx.camera.util.FileUtils;
import com.putao.ptx.camera.util.StringFormatUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.R.attr.max;

/**
 * M1：表示点头马达，角度范围0~40度<br/>
 * M2：表示摇头马达，角度范围-90度~90度<br/>
 * M3：表示转身马达，角度范围-160度~160度<br/>
 * 马达速度范围12-200，表示速度为120pps~2000pps<br/>
 * M1和M2马达不能同时转动（可以设置，但是有一个无效）<br/>
 * <br/>
 * void singleMotor(int[] motor)<br/>
 * 单个马达转达命令<br/>
 * Motor[0] : whichMotor，1表示M1，2表示M2，3表示M3<br/>
 * Motor [1] :isRelative，1表示相对角度移动，0表示绝对角度移动<br/>
 * Motor [2] :speed，移动速度<br/>
 * Motor [3] :angle，移动角度<br/>
 * 底座速度30对应300pps也就是每秒300步，步进电机一步18度，减速比底盘和摇头104，点头64<br/>
 * 所以底盘的度数是30*10*18/104   ℃/s
 */
public class SeatManager {
    private static final int ANGLE_UNKOWN = -180;
    private static final int ANGLE_UNKOWN2 = -1;
    private final int LEFT_START_ID = 0x101;
    private final int RIGHT_START_ID = 0x102;
    private final int LEFT_TO_RIGHT_120 = 0x103;
    private final int PART_1 = 0x201;
    private final int PART_2 = 0x202;
    private final int PART_3 = 0x203;
    private final int MSG_WHAT_START = 0x900;
    public static final int angle = 50;
    public static final int offsetAngle = 0/*25*/;
    public static final int motorSpeed = 22;//底座转的速度（度/秒），设置为120时，实测角速度约为22度/秒
    public static final int picCnt = 5;//全景时拍摄的图片数量
    public static final int PROGRESS_MAX = 1000;
    public static final int START_ORDINAL = 1;
    public static /*final */ int SPEED_SLOW = /*12*/15;
    public static /*final */ int SHUTTER_SLEEP = 1400;
    private static final int SPEED_FAST = 20;
    private static final int SPEED_RESTORE= 60;
    private List<String> panoramaPics;

    /**
     * 用于底座电机到达指定位置的通知
     */
    public static final int[] sMotorStatus = new int[3];

    /**
     * 拍照间隔的时间
     */
    public static final int intervalTime = (int) (1000.0f * (angle - offsetAngle) * 2 / motorSpeed / (picCnt - 1));


//    private SeatMoveCallBack mCallBack;
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if (msg.what == MSG_WHAT_START) {
//                if (mCallBack != null)
//                    mCallBack.onStart();
//            }
//        }
//    };

    private static final String TAG = SeatManager.class.getSimpleName();
    private Activity mActivity;
    private int mAnglge2;
    private int[] mAnglgeTmp = {-180, -180, -180};
    private ExecutorService threadExecutor;
    private int mAngleMoveTo = SeatManager.this.angle;
    private long mMillisecondsStartPano = 0;
    private long mTmLastPic;
    /**
     * 从1开始
     */
    private int mOrdinalTake = START_ORDINAL;
    /**
     * 从1开始
     */
    private int mOrdinalName = START_ORDINAL;
    private byte[] mDataLastFrame;
    /**
     * 拍全景照片的方式 true：底座不停取帧，fale:底座转到指定角度暂停取帧
     */
    private static final boolean sbMethodContinue = false/*true*/;
    private AudioPlay mAudioPlay;
    private int mLastHoriAngle;
    public static final boolean sUserInput = /*true*/false;//使用用户输入参数,发布软件时为false
    public static final boolean sUseFramePic = /*false*/true;//获取图片的方式，是否获取帧图片
    private Handler mHandler = new Handler();
    private AccelerSensorManager mAccelerSensor;
    private Thread mSeatThread;

    public DockingManager getDockingManager() {
        return mDockingManager;
    }

    private DockingManager mDockingManager;

    public SeatManager(Activity activity) {
        mActivity = activity;
        mDockingManager = (DockingManager) activity.getSystemService("docking_service");
        init();
    }

    private void init() {
        if (mDockingManager == null) return;
        initLeftStart();
        initLeftToRight();
    }

    private void initLeftStart() {
        int[] m1 = {0, 0, 20, 0};
        int[] m2 = {1, 0, 20, 0};
        int[] m3 = {1, 0, 50, -60};
        mDockingManager.setPreset(LEFT_START_ID, m1, m2, m3);
    }

    private void initLeftToRight() {
        int[] m1 = {0, 0, 20, 0};
        int[] m2 = {1, 0, 20, 0};
        int[] m3 = {1, 0, 12, 60};
        mDockingManager.setPreset(PART_1, m1, m2, m3);
    }


    public void reset() {
        if (mDockingManager != null)
            mDockingManager.Reset();
    }

    /**
     * 底座电量达到10%时
     *
     * @return
     */
    public boolean isLowPower() {
        if (mDockingManager != null) {
            if (mDockingManager.getBatteryLevel() < 10)
                return false;
            else
                return true;

        } else {
            return false;
        }
    }


    /**
     * 水平转动一个绝对角度
     *
     * @param speed
     * @param angle
     * @return
     */
    public boolean moveHorizontalAbsoluteAngle(int speed, int angle) {
        if (mDockingManager != null && isAllowTakePano()) {
            Log.d(TAG, "moveHorizontalAbsoluteAngle: speed:" + speed + "  angle:" + angle);
            return mDockingManager.moveHorizontalAbsoluteAngle(speed, angle);
        } else {
            return false;
        }
    }

    /**
     * 水平转动一个绝对角度
     *
     * @param speed
     * @param angle
     */
    public void moveHorizontalRelativeAngle(int speed, int angle) {
        if (mDockingManager != null) {
            mDockingManager.moveHorizontalRelativeAngle(speed, angle);
        }
    }

    /**
     * 返回马达当前状态，int 0,1,2 分别对应M1,M2,M3马达
     *
     * @return
     */
    private int[] getMotorStatus() {
        if (mDockingManager != null) {
            int[] status = mDockingManager.getMotorStatus();
            Log.d(TAG, "getMotorStatus: " + " M1:" + status[0] + " M2:" + status[1] + " M3:" + status[2]);
            return status;
        } else
            return null;
    }

    /**
     * 判断是否已经连接上底座,耗时1~6ms
     *
     * @return
     */
    public boolean isConnectedToDocking() {
        if (mDockingManager == null) {
            return false;
        }
        try {
            long tm = SystemClock.elapsedRealtime();
            int state = mDockingManager.getDockState();
            boolean b = mDockingManager != null && state == 1;
            Log.d(TAG, "isConnectedToDocking:  " + mDockingManager
                    + "  getDockState:" + state
                    + "  isConnectedToDocking:" + b
                    + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
            return b;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断底座电机是否已开启
     *
     * @return
     */
    public boolean isMotorOn() {
        if (mDockingManager == null) {
            return false;
        }
        long tm = SystemClock.elapsedRealtime();
        //int[] angle = getMotorAngle();
        //返回
        //1：底座为开机模式
        //0：底座为关机模式
        //        -1：超时
        int ret = mDockingManager.getDockingPowerStatus();
        Log.d(TAG, "isMotorOn: waste time:" + (SystemClock.elapsedRealtime() - tm) + "  ret:" + ret);
        //return !isMotorOff(angle);
        return ret == 1;
    }

    private boolean isMotorOff(int[] angles) {
        return angles == null || angles.length < 3 ||
                (angles[0] == angles[1] && angles[1] == angles[2]
                        && (angles[2] == ANGLE_UNKOWN || angles[2] == ANGLE_UNKOWN2))
                || (angles[0] == -13 && angles[1] == -173 && angles[2] == -172);
    }

    /**
     * 返回马达当前角度，int 0,1,2 分别对应M1,M2,M3马达
     * <p/>当两次调用时间太近时，取上一次结果
     * 耗时接口
     *
     * @return
     */
    public int[] getMotorAngle() {
        int[] angle = mLastAngles;
        if (mDockingManager != null) {
            long tm = SystemClock.elapsedRealtime();
            if (tm - mLastTimeGetAngle > 50) {
                //DockingManager.getMotorAngle接口比较脆弱，经不住太过频繁地调用
                angle = mDockingManager.getMotorAngle();
                mLastTimeGetAngle = SystemClock.elapsedRealtime();
                Log.d(TAG, "getMotorAngle: " + " M1:" + angle[0]
                        + " M2:" + angle[1] + " M3:" + angle[2]
                        + "  waste time:" + (mLastTimeGetAngle - tm));
                mLastAngles[0] = angle[0];
                mLastAngles[1] = angle[1];
                mLastAngles[2] = angle[2];
            }
        }
        return angle;
    }

    private long mLastTimeGetAngle = 0;
    private final int[] mLastAngles = {-180, -180, -180};

    public int[] getLastMotorAngle() {
        return mAnglgeTmp;
    }


    /**
     * 得到转身马达当前的角度
     *
     * @return -1表示未知
     */
    private int getMoveStatus() {
        int[] status = mDockingManager.getMotorStatus();
        if (status != null)
            return status[2];
        else return -1;
    }

    /**
     * 得到转身马达当前的状态
     * {@link SeatManager#ANGLE_UNKOWN}
     *
     * @return 表示未知
     */
    private int getMoveAngle() {
        int[] angles = getMotorAngle();
        if (angles != null) {
            mAnglgeTmp = angles;
            return mAnglgeTmp[2];
        } else {
            return ANGLE_UNKOWN;
        }
    }

    /**
     * 点头到0度
     */
    private void pitchToZero() {
        if (mDockingManager != null) {
            int[] m1 = {1, 0, SPEED_SLOW/*SPEED_FAST*/, 0};
            mDockingManager.singleMotor(m1);
        }
    }

    /**
     * 摇头到0度
     */
    private void rowToZero() {
        if (mDockingManager != null) {
            int[] m = {2, 0, SPEED_SLOW, 0};
            mDockingManager.singleMotor(m);
        }
    }

    /**
     * @param speed
     * @param angle
     */
    private void horiMotorToAngle(int speed, int angle) {
        if (mDockingManager != null) {
            int[] m = {3, 0, speed, angle};
            mDockingManager.singleMotor(m);
        }
    }

    public boolean isPrepareOrTaking() {
        boolean bPrepareOrTaking = mStatus == EmPanoStatus.PREPARE || mStatus == EmPanoStatus.TAKING;
        Log.d(TAG, "isPrepareOrTaking: " + bPrepareOrTaking);
        return bPrepareOrTaking;
    }

    public void resetPanPicAngle(final SeatMoveCallBack callBack,
                                 int speed, int sleep, AccelerSensorManager accelerSensor) {
        mAccelerSensor = accelerSensor;
        if (sUserInput) {//采用用户手动输入的参数
            SPEED_SLOW = Math.max(speed / 10, 4);
            SHUTTER_SLEEP = Math.max(sleep, 100);
            if (accelerSensor != null) {
                accelerSensor.onResume();
            }
        }
        mSeatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isAllowTakePano()) return;
                if (mDockingManager != null) {//实测角速度大约为22度每秒
                    EventBus.getDefault().post(CameraEngine.EventFocusMeasure.MEASURE_FULL);
                    setStatus(EmPanoStatus.PREPARE);
                    mOrdinalTake = START_ORDINAL;
                    mOrdinalName = START_ORDINAL;
                    mMillisecondsStartPano = System.currentTimeMillis();
                    try {
                        mLastHoriAngle = getMotorAngle()[2];
                        //经验所得，此处需要休眠一定的时间才能调用getMotorAngle接口，否则返回0,0,0。
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int[] angle = getMotorAngle();
                    Log.d(TAG, "resetPanPicAngle start:angle[0]=" + angle[0] + " angle[1]=" + angle[1] + " angle[2]=" + angle[2]);
                    mAngleMoveTo = (angle[2] > 10 ? -1 : 1) * SeatManager.this.angle;
                    int offsetTime = (int) (offsetAngle * 1.0f / motorSpeed * 1000);
                    if (offsetTime > 0) {
                        try {
                            Thread.sleep(offsetTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (!isAllowTakePano()) {
                        return;
                    }
                    moveSeatPrepare(mAngleMoveTo, callBack);
                    setStatus(EmPanoStatus.TAKING);
                    startThreadTestAngle();
                    if (sbMethodContinue) {
                        startTakingPanoContinue(offsetTime);
                    } else {
                        try {
                            CameraEngine.setPicFocusMode();
                            //AccelerSensorManager.setEnableEvent(false);
                            CameraEngine.setAutoExposureWhiteBalanceLock(true, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "run: " + e.toString());
                        }
                        startTakingPanoStepByStep(-mAngleMoveTo, mAngleMoveTo);
                        if (!isAllowTakePano()) {
                            return;
                        }
                        try {
                            CameraEngine.setAutoExposureWhiteBalanceLock(false, false);
                            //AccelerSensorManager.setEnableEvent(true);
                            //CameraEngine.setPicFocusMode();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "run: " + e.toString());
                        }
                    }
                    try {//等待一定的时间再发送指令让底座归位
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!isAllowTakePano()) {
                        return;
                    }
                    moveHorizontalAbsoluteAngle(SPEED_RESTORE, mLastHoriAngle);
                    Log.d(TAG, "run: resetPanPicAngle:" + mLastHoriAngle);
                    setStatus(EmPanoStatus.FREE);
                }
            }
        });
        mSeatThread.setName("---thread taking panorama pics---");//全景拍照线程
        mSeatThread.setDaemon(true);
        mSeatThread.start();
    }

    public boolean isSeatThreadAlive() {
        return mSeatThread != null && mSeatThread.isAlive();
    }

    public boolean isAllowTakePano() {
        VisibleCallback visible = getVisibleCallback();
        return visible != null && visible.isVisible();
    }

    private void startTakingPanoStepByStep(final int angleFrom, final int angleTo) {
        if (mAudioPlay != null) {
            //mAudioPlay.run();//第一张不给快门声
        }
        onPreviewFrame(mDataLastFrame, CameraEngine.getCamera());
        float diff = (angleTo - angleFrom) * 1.0f / (picCnt - 1);
        int millis, angle;
        long wasteTime;
        for (int i = 1; i < picCnt && isAllowTakePano(); i++) {
            angle = (int) (angleFrom + diff * i);
            if (i <= picCnt / 2) {
                moveHorizontalAbsoluteAngle(SPEED_SLOW, angle);
            } else {//为了减轻震动，在上坡时改方式
                //horiMotorToAngle(SPEED_SLOW, angle);
                moveHorizontalAbsoluteAngle(SPEED_SLOW, angle);
            }
            millis = (int) (Math.abs(diff) / motorSpeed * 1000 + 4000);
            wasteTime = tryWait(millis, "  旋转到角度：" + angle);
            if (wasteTime < millis) {
                try {
                    if (CameraEngine.isCameraIdBack()) {
                        EventBus.getDefault().post(CameraEngine.EventFocusMeasure.FOCUS_ONLY);
                    }
                    Thread.sleep(SHUTTER_SLEEP);//暂停等待时间
                    if (!isAllowTakePano()) {
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "startTakingPanoStepByStep: angle[2]:" + mLastAngles[2]);
            if (mAudioPlay != null && isAllowTakePano()) {
                mAudioPlay.playSound(AudioPlay.EmSound.SHUTTER);
            }
            onPreviewFrame(mDataLastFrame, CameraEngine.getCamera());
            double acc = 0;
            if (mAccelerSensor != null) {
                acc = mAccelerSensor.getAccelerate();
            }
            final double accf = acc;
            if (sUserInput) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CustomToastUtils.showControlToast(mActivity, "加速度：" + String.format("%.2f", accf));
                    }
                });
            }
        }
        if (picCnt == PROGRESS_MAX) {
            moveHorizontalAbsoluteAngle(SPEED_SLOW, (int) (angleTo + diff / 2));//多转一个角度，去除
        }
    }

    private void startTakingPanoContinue(int offsetTime) {
        moveHorizontalAbsoluteAngle(SPEED_SLOW, mAngleMoveTo);
        Log.d(TAG, "run: resetPanPicAngle:" + mAngleMoveTo);
//                    mDockingManager.gotoPreset(LEFT_TO_RIGHT_120);
        try {
            Thread.sleep(7000 - offsetTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 移动底座到指定角度
     *
     * @param angleSet
     * @param callBack
     */
    private void moveSeatPrepare(final int angleSet, final SeatMoveCallBack callBack) {
        int[] angle = getMotorAngle();
//        rowToZero();//摇头到0度
//        Log.d(TAG, "moveSeatPrepare: 摇头角度：" + angle[1]);
//        tryWait((int) Math.max(2000, Math.abs(angle[1]) * 1000f / motorSpeed + 1000),
//                "摇头到0度rowToZero");//500

        angle = getMotorAngle();
//        pitchToZero();//点头到0度
//        tryWait((int) Math.max(1000, Math.abs(angle[0]) * 1000f / motorSpeed),
//                "点头到0度pitchToZero");//500
        //moveHorizontalAbsoluteAngle(SPEED_FAST, -angleSet);
        Log.d(TAG, "run: moveSeatPrepare" + (-angleSet));
        angle = getMotorAngle();
        Log.d(TAG, "run: angle[2]:" + angle[2]);
        //Thread.sleep(1000);
        //tryWait(2000, "moveHorizontalAbsoluteAngle");
        moveHorizontalAbsoluteAngle(SPEED_FAST, -angleSet);//此行代码不可少
        angle = getMotorAngle();
        Log.d(TAG, "run: angle[2]:" + angle[2]);
        int[] pre = new int[]{-360, -360, -360};//当底座没连接上时，有可能返回3个-180度
        int[] cur = pre;
        int abs1 = 0;
        int absPre = -180;
        long startTm = SystemClock.elapsedRealtime();
        long tmDiff = 0;
        boolean speaking = true;
        while (isAllowTakePano()/* && isPrepareOrTaking()*/) {
            try {
                tryWait(1000, "moveHorizontalAbsoluteAngle");
                angle = getMotorAngle();
                pre = cur;
                cur = angle;
                abs1 = Math.abs(angle[2] - (-angleSet));
                absPre = Math.abs(cur[2] - pre[2]);
                speaking = SpeechHelper.instance().isSpeaking();
                tmDiff = SystemClock.elapsedRealtime() - startTm;
                Log.d(TAG, "run: angle[0]:" + angle[0] + " angle[1]:" + angle[1]
                        + " angle[2]:" + angle[2] + "  speaking:" + speaking
                        + "  tmDiff:" + tmDiff);
                if ((abs1 < 5 || absPre < 5)//有bug，可能只旋转到中间位置
                        && Math.abs(cur[0] - pre[0]) < 5
                        && Math.abs(cur[1] - pre[1]) < 5 //底座三个相机稳定时才允许拍摄
                        && !speaking//说话时不允许拍摄
                        && tmDiff > 2000//至少等两秒钟，有可能出现命令迟滞
                        ) {
                    angle = getMotorAngle();
                    Log.d(TAG, "moveSeatPrepare finished angle[0]="
                            + angle[0] + " angle[1]=" + angle[1] + " angle[2]" + angle[2]
                            + "  prepare move to angleSet:" + angleSet);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "moveSeatPrepare: Exception: " + e.toString());
            }
        }
        //handler.sendEmptyMessage(MSG_WHAT_START);
        //if (mStatus != EmPanoStatus.PREPARE) {
        //mDockingManager.moveHorizontalAbsoluteAngle(12, 0);
        //return;
        //}
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (isAllowTakePano()) {
                    callBack.onStart();
                }
            }
        });
        //Thread.sleep(500);
        //tryWait(500,"");
        //moveHorizontalAbsoluteAngle(SPEED_SLOW, angleSet);
    }

    private long tryWait(int millis, String tag) {
        if (millis <= 0) {
            millis = 2000;
        }
        synchronized (sMotorStatus) {
            long tm = SystemClock.elapsedRealtime();
            long wasteTime;
            try {
                sMotorStatus.wait(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "tryWait: " + e);
            } finally {
                wasteTime = SystemClock.elapsedRealtime() - tm;
                Log.d(TAG, "tryWait: sMotorStatus " + tag
                        + "  angle[2]=" + mLastAngles[2]
                        + "  wait:" + millis
                        + "  waste time:" + wasteTime);
                return wasteTime;
            }
        }
    }

    private void startThreadTestAngle() {
        Thread testAngle = new Thread(new Runnable() {
            @Override
            public void run() {
                long tm = SystemClock.elapsedRealtime();
                while (!mActivity.isDestroyed() && isTakingPano() && isAllowTakePano()) {
                    mAnglge2 = getMoveAngle();
                    OnProgressUpdateListener listener = getOnProgressUpdateListener();
                    if (listener != null) {
                        long tm2 = SystemClock.elapsedRealtime();
                        listener.onProgressUpdate(
                                getAngleProgressInt(PROGRESS_MAX), tm2 - tm, PROGRESS_MAX);
                        tm = tm2;
                    }
                    Log.d(TAG, "run: mAnglge2:" + mAnglge2);
                    try {
                        Thread.sleep(/*200*/150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "---thread check docking angle---");//线程 检测底座角度
        testAngle.setDaemon(true);
        testAngle.start();
    }

    public void movePanPicAngle(final SeatMoveCallBack callBack) {
        Log.d(TAG, "movePanPicAngle start");
        if (!moveHorizontalAbsoluteAngle(12, 60)) return;
        if (callBack != null) {
            new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    int angle = 0;
                    while (angle < 60) {
                        try {
                            angle = getMoveAngle();
                            Log.d(TAG, "movePanPicAngle move" + angle);
                            Thread.sleep(500);
                            publishProgress(angle);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    callBack.onStart();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    super.onProgressUpdate(values[0]);
                    callBack.onMoving(values[0], 0);
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    callBack.onEnd();
                }
            }.execute();
        }
    }

    public boolean isTakingPano() {//调用太过频繁，去掉打印
        //Log.d(TAG, "isTakingPano: mStatus:" + mStatus + "  mOrdinalTake:" + mOrdinalTake);
        return mStatus == EmPanoStatus.TAKING;
    }

    public long getMillisecondsStartPano() {
        return mMillisecondsStartPano > 0 ? mMillisecondsStartPano :
                System.currentTimeMillis();
    }

    public byte[] getDataLastFrame() {
        return mDataLastFrame;
    }

    public void setPanoramaPics(List<String> panoramaPics) {
        this.panoramaPics = panoramaPics;
    }

    public void setAudioPlay(AudioPlay audioPlay) {
        mAudioPlay = audioPlay;
    }

    public AudioPlay getAudioPlay() {
        return mAudioPlay;
    }


    public interface SeatMoveCallBack {
        void onStart();

        void onMoving(int angle, int status);

        void onEnd();
    }


    public int getDockingHoriAngle() {
        if (isTakingPano()) {
            return mAnglge2;
        } else {
            long tm = SystemClock.elapsedRealtime();
            int[] angles = getMotorAngle();
            Log.d(TAG, "getDockingHoriAnglge: waste time" + (SystemClock.elapsedRealtime() - tm));
            return angles != null ? angles[2] : ANGLE_UNKOWN;
        }
    }

    private Camera.PreviewCallback mpanoPreviewCallback;

    public synchronized Camera.PreviewCallback getPanoPreviewCallback() {
        if (mpanoPreviewCallback == null) {
            mpanoPreviewCallback = new Camera.PreviewCallback() {
                public long interval;

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    mDataLastFrame = data;
                    if (sbMethodContinue
                            && SystemClock.elapsedRealtime() - interval
                            > SeatManager.intervalTime * (mOrdinalTake != picCnt ? 1 : 1.2)//最后一张图片取静止后的清晰图片，加大了延时时间
                            && (isTakingPano() || mOrdinalTake > START_ORDINAL)
                            && mOrdinalTake <= picCnt/*1500 */) {/*底座22°每秒，转动100度*/
                        SeatManager.this.onPreviewFrame(data, camera);
                        interval = SystemClock.elapsedRealtime();
                        mOrdinalTake++;
                    }
                }
            };
        }
        return mpanoPreviewCallback;
    }

    private void onPreviewFrame(final byte[] data, Camera camera) {
        if (data == null || camera == null) {
            return;
        }
        final Camera.Size size;
        try {
            size = camera.getParameters().getPreviewSize();//有可能相机已释放
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "onPreviewFrame: takePic");
        long tm = SystemClock.elapsedRealtime();
        final long interval = (tm - mTmLastPic) % 10000;
        mTmLastPic = tm;

        final OnTakingPanoPicture panoPictureCallback = getOnTakingPanoPicture();
        final boolean useFrame = sUseFramePic || panoPictureCallback == null;
        Runnable command = new Runnable() {
            @Override
            public void run() {
                double acc = 0;
                if (mAccelerSensor != null) {
                    acc = mAccelerSensor.getAccelerate();
                }
                PanorPicEvent event = new PanorPicEvent();
                String path = FileUtils./*getPhotoPath*/getOtherDirPath() + "PNO_"
                        + StringFormatUtil.sformat.format(new Date(getMillisecondsStartPano()))
                        + "_" + mOrdinalName + "_" + interval + "_" + getDockingHoriAngle()
                        + getStrSpeedSleep() + "_" + ((int) (acc * 100)) + ".jpg";
                mOrdinalName++;

                if (useFrame) {
                    Bitmap temp = BitmapToVideoUtil.yuv2bitmap(data, size.width, size.height);
                    BitmapUtil.saveBitmap(temp, path);
                    temp.recycle();
                } else {
                    panoPictureCallback.onTakingPanoPicture(path);
                }
                event.path = path;
                //if (isTakingPano()) {
                EventBus.getDefault().post(event);
                //}
                Log.d(TAG, "run: EventBus " + event.getClass().getSimpleName());
            }
        };
        if (useFrame) {
            if (threadExecutor == null) {
                threadExecutor = Executors.newSingleThreadExecutor();
            }
            threadExecutor.execute(command);
        } else {
            mHandler.post(command);
        }
    }

    @NonNull
    public static String getStrSpeedSleep() {
        return "_" + SPEED_SLOW
                + "_" + SHUTTER_SLEEP;
    }


    public EmPanoStatus getStatus() {
        return mStatus;
    }

    public long getElapsedStatusTime() {
        return SystemClock.elapsedRealtime() - smTime;
    }

    private long smTime;

    public void setStatus(EmPanoStatus status) {
        mStatus = status;
        smTime = SystemClock.elapsedRealtime();
    }

    public static enum EmPanoStatus {
        FREE, PREPARE, TAKING;
    }

    private EmPanoStatus mStatus = EmPanoStatus.FREE;


    private OnTakingPanoPicture mOnTakingPanoPicture;

    public interface OnTakingPanoPicture {
        String onTakingPanoPicture(String path);
    }

    public OnTakingPanoPicture getOnTakingPanoPicture() {
        return mOnTakingPanoPicture;
    }

    public void setOnTakingPanoPicture(OnTakingPanoPicture onTakingPanoPicture) {
        mOnTakingPanoPicture = onTakingPanoPicture;
    }

    public VisibleCallback getVisibleCallback() {
        return mVisibleCallback;
    }

    public void setVisibleCallback(VisibleCallback visibleCallback) {
        mVisibleCallback = visibleCallback;
    }

    VisibleCallback mVisibleCallback;

    public interface VisibleCallback {
        boolean isVisible();
    }

    public int getAngleMoveTo() {
        return mAngleMoveTo;
    }

    public int getAngleMoveFrom() {
        return -1 * mAngleMoveTo;
    }

    public float getAngleProgress() {
        float v = (getDockingHoriAngle() - getAngleMoveFrom()) * 1.0f / (getAngleMoveTo() - getAngleMoveFrom());
        return v;
    }

    public int getAngleProgressInt(int max) {
        float v = getAngleProgress();
        int round = Math.round(v * max);
        int ret = Math.max(Math.min(round, max), Math.max(round, 0));
        Log.d(TAG, "getAngleProgressInt: " + ret);
        return ret;
    }

    public OnProgressUpdateListener getOnProgressUpdateListener() {
        return mOnProgressUpdateListener;
    }

    public void setOnProgressUpdateListener(OnProgressUpdateListener onProgressUpdateListener) {
        mOnProgressUpdateListener = onProgressUpdateListener;
    }

    OnProgressUpdateListener mOnProgressUpdateListener;

    public interface OnProgressUpdateListener {
        void onProgressUpdate(int cur, long wastetime, int max);
    }

}
