package com.putao.ptx.camera.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by admin on 2016/7/11.
 */
public class DockingBroadcast extends BroadcastReceiver {
    /**
     * 底座电量变化时候主动上报<br/>
     * int batteryLevel = intent.getIntExtra("batteryLevel", 0);
     */
    public static final String ACTION_DOCKING_BATTERY_LEVEL = "com.putao.docking.ACTION_DOCKING_BATTERY_LEVEL";
    /**
     * 马达移动到主动位置的时候主动上报，返回值为3个马达目前状态<br/>
     * int motorStatus[] = new int[3];<br/>
     * motorStatus = intent.getIntArrayExtra("motorStatus");
     */
    public static final String ACTION_DOCKING_MOTOR_STATUS = "com.putao.docking.ACTION_DOCKING_MOTOR_STATUS";
    /**
     * 检测到声音源，并且上报声音位置<br/>
     * int angle = intent.getIntExtra("angle", -180);
     */
    public static final String ACTION_DOCKING_DETECTION_SOUND = "com.putao.docking.ACTION_DOCKING_DETECTION_SOUND";
    /**
     * 马达堵转报警，上报三个马达状态，0表示正常，1表示堵转<br/>
     * int[] motorBlock = new int[3];<br/>
     * motorBlock = intent.getIntArrayExtra("motorBlock");
     */
    public static final String ACTION_DOCKING_MOTOR_BLOCK = "com.putao.docking.ACTION_DOCKING_MOTOR_BLOCK";

    /**
     * padIntent.putExtra("padStatus", newState);<br>
     * 0表示未连接，1表示连接
     */
    public static final String ACTION_DOCKING_PAD_CONNECT_STATUS = "com.putao.docking.ACTION_DOCKING_PAD_CONNECT_STATUS";

    /**
     * 底座电源开关是否打开的，1是打开，0是关闭<br>
     * powerIntent.putExtra("powerstatus", 0);
     */
    public static final String ACTION_DOCKING_POWER_STATUS_CHANGED = "com.putao.docking.ACTION_DOCKING_POWER_STATUS_CHANGED";

    public static IntentFilter getIntentFilter() {
        IntentFilter ret = new IntentFilter();
        ret.addAction(ACTION_DOCKING_BATTERY_LEVEL);
        ret.addAction(ACTION_DOCKING_MOTOR_STATUS);
        ret.addAction(ACTION_DOCKING_DETECTION_SOUND);
        ret.addAction(ACTION_DOCKING_MOTOR_BLOCK);
        ret.addAction(ACTION_DOCKING_PAD_CONNECT_STATUS);
        ret.addAction(ACTION_DOCKING_POWER_STATUS_CHANGED);
        return ret;
    }

    private static final String TAG = "DockingBroadcast";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, DockingBroadcast.class.getSimpleName() + "  onReceive: action：" + action);
        if (action.equalsIgnoreCase(ACTION_DOCKING_BATTERY_LEVEL)) {
            if (mOnDockingLowBatteryListener != null) {
                mOnDockingLowBatteryListener.onDockingLowBattery(intent.getIntExtra("batteryLevel", 0));
            }
        } else if (action.equalsIgnoreCase(ACTION_DOCKING_MOTOR_STATUS)) {
            if (mOnDockingMotorStatusListener != null) {
                mOnDockingMotorStatusListener.onDockingMotorToEnd(intent.getIntArrayExtra("motorStatus"));
            }
        } else if (action.equalsIgnoreCase(ACTION_DOCKING_DETECTION_SOUND)) {
            if (mOnDockingDetectSoundListener != null) {
                mOnDockingDetectSoundListener.onDockingDetectSound(intent.getIntExtra("angle", -180));
            }
        } else if (action.equalsIgnoreCase(ACTION_DOCKING_MOTOR_BLOCK)) {
            if (mOnDockingMotorBlockListener != null) {
                mOnDockingMotorBlockListener.onDockingMotorBlock(intent.getIntArrayExtra("motorBlock"));
            }
        } else if (action.equalsIgnoreCase(ACTION_DOCKING_PAD_CONNECT_STATUS)) {
            boolean padStatus = intent.getIntExtra("padStatus", 0) == 1;
            Log.d(TAG, "onReceive: padStatus:" + padStatus);
            if (mOnDockingPadConnectStatusListener != null) {
                mOnDockingPadConnectStatusListener.onDockingPadConnectStatusListener(padStatus);
            }
        } else if (action.equalsIgnoreCase(ACTION_DOCKING_POWER_STATUS_CHANGED)) {
            boolean powerOn = intent.getIntExtra("powerstatus", 0) == 1;
            Log.d(TAG, "onReceive: powerstatus powerOn:" + powerOn);
            if (mOnDockingPowerStatusChangedListener != null) {
                mOnDockingPowerStatusChangedListener.onDockingPowerStatusChanged(powerOn);
            }
        }
    }

    private OnDockingLowBatteryListener mOnDockingLowBatteryListener;
    private OnDockingMotorStatusListener mOnDockingMotorStatusListener;
    private OnDockingDetectSoundListener mOnDockingDetectSoundListener;
    private OnDockingMotorBlockListener mOnDockingMotorBlockListener;
    private OnDockingPadConnectStatusListener mOnDockingPadConnectStatusListener;
    private OnDockingPowerStatusChangedListener mOnDockingPowerStatusChangedListener;

    public void setOnDockingLowBatteryListener(OnDockingLowBatteryListener onDockingLowBatteryListener) {
        mOnDockingLowBatteryListener = onDockingLowBatteryListener;
    }

    public void setOnDockingMotorStatusListener(OnDockingMotorStatusListener onDockingMotorStatusListener) {
        mOnDockingMotorStatusListener = onDockingMotorStatusListener;
    }

    public void setOnDockingDetectSoundListener(OnDockingDetectSoundListener onDockingDetectSoundListener) {
        mOnDockingDetectSoundListener = onDockingDetectSoundListener;
    }

    public void setOnDockingMotorBlockListener(OnDockingMotorBlockListener onDockingMotorBlockListener) {
        mOnDockingMotorBlockListener = onDockingMotorBlockListener;
    }

    public void setOnDockingPadConnectStatusListener(OnDockingPadConnectStatusListener onDockingPadConnectStatusListener) {
        mOnDockingPadConnectStatusListener = onDockingPadConnectStatusListener;
    }

    public void setOnDockingPowerStatusChangedListener(OnDockingPowerStatusChangedListener onDockingPowerStatusChangedListener) {
        mOnDockingPowerStatusChangedListener = onDockingPowerStatusChangedListener;
    }

    /**
     * 参考{@link DockingBroadcast#ACTION_DOCKING_BATTERY_LEVEL}
     */
    public static interface OnDockingLowBatteryListener {
        void onDockingLowBattery(int level);
    }


    /**
     * 参考{@link DockingBroadcast#ACTION_DOCKING_MOTOR_STATUS}
     */
    public static interface OnDockingMotorStatusListener {
        void onDockingMotorToEnd(int[] motorStatus);
    }


    /**
     * 参考{@link DockingBroadcast#ACTION_DOCKING_DETECTION_SOUND}
     */
    public static interface OnDockingDetectSoundListener {
        void onDockingDetectSound(int soundAngle);
    }

    /**
     * 参考{@link DockingBroadcast#ACTION_DOCKING_MOTOR_BLOCK}
     */
    public static interface OnDockingMotorBlockListener {
        void onDockingMotorBlock(int[] motors);
    }

    /**
     * 参考{@link DockingBroadcast#ACTION_DOCKING_PAD_CONNECT_STATUS}
     */
    public static interface OnDockingPadConnectStatusListener {
        void onDockingPadConnectStatusListener(boolean isConnected);
    }

    /**
     * 参考{@link DockingBroadcast#ACTION_DOCKING_POWER_STATUS_CHANGED}
     */
    public static interface OnDockingPowerStatusChangedListener {
        void onDockingPowerStatusChanged(boolean powerOn);
    }
}
