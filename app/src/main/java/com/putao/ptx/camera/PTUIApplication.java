package com.putao.ptx.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.android.camera.util.AndroidContext;
import com.baidu.location.service.LocationService;
import com.baidu.mapapi.SDKInitializer;
import com.putao.ptx.accountcenter.PaibotAccountStateChangedReceiver;
import com.putao.ptx.camera.activity.CameraActivity;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by Administrator on 2016/3/16.
 */
public class PTUIApplication extends /*Basic*/Application {
    private static PTUIApplication globalContext;
    private long mTmLastCreated;

    public static PTUIApplication getInstance() {
        return globalContext;
    }

    private static final String TAG = "timeTest";


    private static List<CameraActivity> sList = new ArrayList<>();


    public LocationService locationService;


    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate() {
        super.onCreate();

        // Android context must be the first item initialized.
        Context context = getApplicationContext();
        AndroidContext.initialize(context);

        Log.d(TAG, "onCreate: ");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        Log.d(TAG, "onCreate:  Thread.getPriority():" + Thread.currentThread().getPriority());
        globalContext = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                initTool();
            }
        }).start();
        registerActivityLifecycleCallbacks(
                new ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        if (sList.size() != 0 && sList.get(0) != activity) {
                            sList.get(0).finish();
                        }
                        if (activity instanceof CameraActivity) {
                            Activity last = sList.size() > 0 ? sList.get(0) : null;
                            Intent it = activity.getIntent();
                            if (SystemClock.elapsedRealtime() - mTmLastCreated < 1000 && last != null
                                    && isIntentEqual(last.getIntent(), it)) {
                                activity.finish();
                            } else {
                                sList.add((CameraActivity) activity);
                                mTmLastCreated = SystemClock.elapsedRealtime();
                            }
                        }
                        Log.d(TAG, "onActivityCreated: \t\t  activitySize=" + sList.size());
                    }

                    private boolean isIntentEqual(Intent last, Intent it) {
                        if (last != it && (it == null || last == null)) {
                            return false;
                        }
                        String lastAction = last.getAction();
                        String itAction = it.getAction();
                        return last == it || TextUtils.equals(lastAction, itAction) && it.getFlags() == last.getFlags();
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                        Log.d(TAG, "onActivityStarted: \t\t  activitySize=" + sList.size());
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        Log.d(TAG, "onActivityResumed: \t\t  activitySize=" + sList.size() + activity.toString());
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        Log.d(TAG, "onActivityPaused: \t\t  activitySize=" + sList.size());
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        Log.d(TAG, "onActivityStopped: \t\t  activitySize=" + sList.size());
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                        Log.d(TAG, "onActivitySaveInstanceState: \t\t  activitySize=" + sList.size());
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        sList.remove(activity);
                        Log.d(TAG, "onActivityDestroyed: \t\t  activitySize=" + sList.size());
                        if (sList.size() > 0) {
                            CameraActivity cameraActivity = (CameraActivity) sList.toArray()[0];
                            //cameraActivity.switchCamera(false);//翻转两次摄像头
                            //cameraActivity.switchCamera(false);//翻转两次摄像头
                            Log.d(TAG, "onActivityDestroyed: 翻转两次摄像头");
                        }
                    }
                }
        );
        Log.d(TAG, "onCreate: leave");


        locationService = new LocationService(getApplicationContext());
        try {
            SDKInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "onCreate: " + e);
        }
        PaibotAccountStateChangedReceiver receiver = new PaibotAccountStateChangedReceiver();
        context.registerReceiver(receiver, new IntentFilter(PaibotAccountStateChangedReceiver.sAction));
    }

    private void initTool() {
        //MultiDex.install(this);
        //ButterKnife的Debug模式
        ButterKnife.setDebug(isDebug());
        //日志输出
        //Logger.init(getLogTag()).hideThreadInfo().setLogLevel(isDebug() ? Logger.LogLevel.FULL : Logger.LogLevel.FULL);
        //Fresco初始化
        //Fresco.initialize(this);//TODO
        //开启bugly
        //CrashReport.initCrashReport(getApplicationContext(), getBuglyKey(), isDebug());
        CrashReport.initCrashReport(getApplicationContext(), getBuglyKey(), isDebug());

        CrashHandler crashHandler = CrashHandler.getInstance();
        //crashHandler.init(getApplicationContext());
        //printDevInfo();
    }

    //@Override
    protected String getBuglyKey() {
        return "900046606";
    }
//

    protected boolean isDebug() {
        return false;
    }

    public static String getVersion() {
        try {
            String pkName = getInstance().getPackageName();
            String versionName = getInstance().getPackageManager().getPackageInfo(
                    pkName, 0).versionName;
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getVersion: " + e);
            return "";
        }
    }

    public static int getVersionCode() {
        try {
            String pkName = getInstance().getPackageName();
            int versionCode = getInstance().getPackageManager().getPackageInfo(
                    pkName, 0).versionCode;
            return versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getVersionCode: " + e);
            return -1;
        }
    }

//
//    @Override
//    public String getPackageName() {
//        return "com.putao.ptx.camera";
//    }
//
//    @Override
//    protected String getLogTag() {
//        return "putao.ptui";
//    }
//
//    @Override
//    protected String getSdCardPath() {
//        return "";
//    }
//
//    @Override
//    protected String getNetworkCacheDirectoryPath() {
//        return "";
//    }
//
//    @Override
//    protected int getNetworkCacheSize() {
//        return 20 * 1024 * 1024;
//    }
//
//    @Override
//    protected int getNetworkCacheMaxAgeTime() {
//        return 0;
//    }

    //    @Override
//    protected void onCrash(Throwable ex) {
//        CameraEngine.releaseCamera();
//        Logger.e("APP崩溃了,错误信息是\n" + ex.getMessage());
//        ex.printStackTrace();
//        ActivityManager.getInstance().killProcess(getApplicationContext());
//    }

    public static boolean isPutaoDev() {
        StringBuffer sb = new StringBuffer();
        sb.append(Build.DEVICE).append(Build.MODEL).append(Build.MANUFACTURER);
        String str = sb.toString().toLowerCase();
        boolean ret = str.contains("paibot") || str.contains("paipad") || str.contains("putao");
        Log.d(TAG, "isPutaoDev: " + ret);
        return ret;
    }
}
