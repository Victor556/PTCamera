package com.putao.ptx.camera.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.putao.ptx.camera.eventbus.MoveSensorEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Administrator on 2016/6/6.
 */
public class AccelerSensorManager {
    public static final float ACC_THREDSHOULD = 10.2f;
    /**
     * 设备转过的角度超过此值则聚焦一次
     */
    private static final double MAGNETIC_ANGLE_LEVEL = 15;//测量误差可能在5~10度
    private final String TAG = AccelerSensorManager.class.getSimpleName();
    private Context mContext;
    private static final int UPTATE_INTERVAL_TIME = 200;
    private long lastUpdateTime;
    private SensorManager sensorMag;
    private Sensor gravitySensor;
    private Sensor mMagneticSensor;
    public final float[] mMagneticLatest = {0, 0, 1};
    public final float[] mMagneticEvent = {0, 0, 1};

    private long lastTime = 0;
    private double mAccelerate;
    private int[] mMagneticAngle = {0, 0, 0};

    private static boolean sEnableEvent = false;
    private static boolean sEnableMagnetic = false;


    /**
     * Invalid heading values.
     */
    public static final int INVALID_HEADING = -1;
    /**
     * Current device heading.
     */
    private int mHeading = INVALID_HEADING;
    /**
     * Accelerometer data.
     */
    private final float[] mGData = new float[3];
    /**
     * Magnetic sensor data.
     */
    private final float[] mMData = new float[3];
    /**
     * Temporary rotation matrix.
     */
    private final float[] mRotationMatrix = new float[16];

    public AccelerSensorManager(Context context) {
        this.mContext = context;
        initGravitySensor();
    }

    private void initGravitySensor() {
        sensorMag = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorMag.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticSensor = sensorMag.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void onPause() {
        if (gravitySensor != null) {
            sensorMag.unregisterListener(sensorLis, gravitySensor);
        }
        if (mMagneticSensor != null) {
            sensorMag.unregisterListener(sensorLis, mMagneticSensor);
        }
    }

    public void onResume() {
        if (gravitySensor != null) {
            sensorMag.registerListener(sensorLis, gravitySensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (mMagneticSensor != null && sEnableMagnetic) {
            sensorMag.registerListener(sensorLis, mMagneticSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }


    private double mAccLargest;
    private double mAccSmallest;
    public boolean mbNeedFocus;

    private SensorEventListener sensorLis = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                onAccSensorChanged(event);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                if (sEnableMagnetic) {
                    onMagneticSensorChanged(event);
                }
            }
        }

        private void onAccSensorChanged(SensorEvent event) {
            if (event == null) {
                return;
            }
            long currentUpdateTime = SystemClock.elapsedRealtime();
            long timeInterval = currentUpdateTime - lastUpdateTime;
            float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            mGData[0] = x;
            mGData[1] = y;
            mGData[2] = z;
            //计算移动速度
            //double accelerate = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeInterval * 10000;
            //计算移动速度
            mAccelerate = Math.sqrt(x * x + y * y + z * z);

            if (timeInterval < UPTATE_INTERVAL_TIME) {
                return;
            }
            lastUpdateTime = currentUpdateTime;

            //if (deltaX > 0.5 || deltaY > 0.5 || deltaZ > 0.5) {
            mAccLargest = Math.max(mAccelerate, mAccLargest);
            mAccSmallest = Math.min(mAccelerate, mAccSmallest);


            if (mAccLargest - mAccSmallest > ACC_THREDSHOULD - SensorManager.STANDARD_GRAVITY) {
                mbNeedFocus = true;
            }

            if (mAccelerate < ACC_THREDSHOULD) {
                long time = SystemClock.elapsedRealtime();
                if (time - lastTime > 800 && mbNeedFocus) {
                    postEvent(time);
                }
            }
        }
    };

    private void updateHeading() {
        if (!sEnableMagnetic) {
            mHeading = INVALID_HEADING;
            return;
        }
        // This is literally the same as the GCamModule implementation.
        float[] orientation = new float[3];
        SensorManager.getRotationMatrix(mRotationMatrix, null, mGData, mMData);
        SensorManager.getOrientation(mRotationMatrix, orientation);
        mHeading = (int) (orientation[0] * 180f / Math.PI) % 360;

        if (mHeading < 0) {
            mHeading += 360;
        }
//        Log.d(TAG, "updateHeading: mHeading :" + mHeading
//                + "  " + (int) (orientation[1] * 180f / Math.PI + 360) % 360
//                + "  " + (int) (orientation[2] * 180f / Math.PI + 360) % 360);
    }

    private void postEvent(long time) {
        if (!sEnableEvent) {
            return;
        }
        if (time - lastTime < 1000) {//聚焦的频率不大于1次/s
            return;
        }

        lastTime = time;
        mbNeedFocus = false;

        //EventBus.getDefault().post(CameraEngine.EventFocusMeasure.MEASURE_ONLY);
        EventBus.getDefault().post(new MoveSensorEvent());
        Log.d(TAG, "postEvent:  ACC_THREDSHOULD:" + ACC_THREDSHOULD +
                "   mAccLargest：" + mAccLargest + "  mAccSmallest:" + mAccSmallest);
        mAccLargest = 0;
        mAccSmallest = ACC_THREDSHOULD;
    }

    private void onMagneticSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double[] angle1 = getAngles(mMagneticEvent);
        double[] angle2 = getAngles(event.values);
        double[] latest = getAngles(mMagneticLatest);
        double maxDiffEvent = getMax(angle1, angle2);
        double maxDiffLatest = getMax(angle2, latest);
//        Log.d(TAG, "onMagneticSensorChanged: maxDiffEvent:"
//                + maxDiffEvent + "  maxDiffLatest:" + maxDiffLatest);
        if (maxDiffEvent > MAGNETIC_ANGLE_LEVEL
                && maxDiffLatest < MAGNETIC_ANGLE_LEVEL / 2
                && (mAccLargest - mAccSmallest < ACC_THREDSHOULD)
                ) {
            postEvent(SystemClock.elapsedRealtime());
            mMagneticEvent[0] = x;
            mMagneticEvent[1] = y;
            mMagneticEvent[2] = z;
            Log.d(TAG, "onMagneticSensorChanged: acosx:"
                    + (int) angle2[0] + "  acosy:" + (int) angle2[1]
                    + "  acosz:" + (int) angle2[2]
                    + "  maxDiffEvent:" + (int) maxDiffEvent
                    + "  maxDiffLatest:" + (int) maxDiffLatest);
        }
        mMagneticLatest[0] = x;
        mMagneticLatest[1] = y;
        mMagneticLatest[2] = z;
        mMData[0] = x;
        mMData[1] = y;
        mMData[2] = z;
    }

    public double getAccelerate() {
        return mAccelerate;
    }


    public double getMaxDiffAngle(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != 3 || v2.length != 3) {
            return 0;
        }
        double[] angles1 = getAngles(v1);
        double[] angles2 = getAngles(v2);
        return getMax(angles1, angles2);
    }

    private double getMax(double[] angles1, double[] angles2) {
        double max = Math.max(Math.max(Math.abs(angles1[0] - angles2[0]), Math.abs(angles1[1] - angles2[1])),
                Math.abs(angles1[2] - angles2[2]));
        return max;
    }

    private double[] getAngles(float[] v) {
        float x = v[SensorManager.DATA_X];
        float y = v[SensorManager.DATA_Y];
        float z = v[SensorManager.DATA_Z];
        double sqrt = Math.sqrt(x * x + y * y + z * z);
        double acosx = Math.toDegrees(Math.acos(x / sqrt));
        double acosy = Math.toDegrees(Math.acos(y / sqrt));
        double acosz = Math.toDegrees(Math.acos(z / sqrt));
        return new double[]{acosx, acosy, acosz};
    }

    public static boolean isEnableEvent() {
        return sEnableEvent;
    }

    public static void setEnableEvent(boolean enableEvent) {
        sEnableEvent = enableEvent;
    }


    /**
     * Returns current device heading.
     *
     * @return current device heading in degrees. INVALID_HEADING if sensors
     * are not available.
     */
    public int getCurrentHeading() {
        updateHeading();
        return mHeading;
    }
}
