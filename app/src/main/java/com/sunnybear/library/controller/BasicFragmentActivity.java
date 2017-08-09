package com.sunnybear.library.controller;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.putao.ptx.camera.util.CommonUtils;
import com.sunnybear.library.util.KeyboardUtils;

import butterknife.ButterKnife;

/**
 * 基础FragmentActivity
 * Created by guchenkai on 2015/11/19.
 */
public abstract class BasicFragmentActivity extends Activity {
    private static final int WHAT_ON_HOME_CLICK = 0x1;
    protected Context mContext;
    protected Bundle args;
    private KeyguardManager mKeyguardManager;
    private boolean mbRegistedShutdownReceiver = false;
    private boolean mSecureCamera = false;
    //private boolean mSecureCamera = false;

    //一直注册锁屏退出广播
    private final boolean mAlwaysRegisteOffScreenRcv = true;

    /**
     * 设置布局id
     *
     * @return 布局id
     */
    protected abstract int getLayoutId();

    /**
     * 布局初始化完成的回调
     *
     * @param saveInstanceState 保存状态
     */
    protected abstract void onViewCreatedFinish(Bundle saveInstanceState);

    /**
     * 收集本Activity请求时的url
     *
     * @return url
     */
    protected abstract String[] getRequestUrls();

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        checkLockedSecurity();
        tryAddDismissKeygardFlag();
        Log.d(TAG, ">>>>>>>>>>>>>>>onCreate: " + BasicFragmentActivity.this.toString()
                + "  isSecureCamera:" + isSecureCamera());
        super.onCreate(savedInstanceState);

//        Window win = getWindow();
//        if (isKeyguardLocked()) {
//            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//        } else {
//            win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//        }
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        int layoutId = getLayoutId();
        if (layoutId == 0)
            throw new RuntimeException("找不到Layout资源,Fragment初始化失败!");
        setContentView(layoutId);
        ButterKnife.bind(this);
        mContext = this;
        this.args = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        //布局初始化完成的回调
        onViewCreatedFinish(savedInstanceState);
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = 0
                //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                //| View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                //| View.SYSTEM_UI_FLAG_LOW_PROFILE
                //
                ;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        if (mAlwaysRegisteOffScreenRcv) {
            if (!mbRegistedShutdownReceiver) {
                IntentFilter filter_screen_off = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                registerReceiver(mShutdownReceiver, filter_screen_off);
                mbRegistedShutdownReceiver = true;
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent: ");
        this.setIntent(intent);
        super.onNewIntent(intent);
        checkLockedSecurity();
        tryAddDismissKeygardFlag();
        Log.d(TAG, "onNewIntent:   Action:" + intent.getAction()
                + "  intent:" + intent);
    }

    private static final String ACTION_STILL_IMAGE_CAMERA_SECURE =
            "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE =
            "android.media.action.IMAGE_CAPTURE_SECURE";

    // The intent extra for camera from secure lock screen. True if the gallery
    // should only show newly captured pictures. sSecureAlbumId does not
    // increment. This is used when switching between camera, camcorder, and
    // panorama. If the extra is not set, it is in the normal camera mode.
    public static final String SECURE_CAMERA_EXTRA = "secure_camera";

    /**
     * 是否处于锁屏状态且有锁屏密码
     *
     * @return
     */
    public boolean isSecureCamera() {
        return /*isKeyguardLockedSecure()*/mSecureCamera;
    }

    @Override
    protected void onResume() {
        super.onResume();
        printLockSecureState();
        Log.d(TAG, ">>>>>>>>>>>>>>>onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, ">>>>>>>>>>>>>>>onPause: ");
    }

    protected void onScreenOffLockedSecure() {
    }

    private void checkLockedSecurity() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if (/*isKeyguardLocked()*/isKeyguardLockedSecure()) {
            getWindow().addFlags(getFlagsShowLocked());
        }

        mSecureCamera = isKeyguardLockedSecure();//isSecurity(intent);
        if (mSecureCamera && !mbRegistedShutdownReceiver/*mSecureCamera*/) {//不管是不是密码锁屏，都注册广播
            // Change the window flags so that secure camera can show when
            // locked

            // Filter for screen off so that we can finish secure camera
            // activity when screen is off.
            IntentFilter filter_screen_off = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mShutdownReceiver, filter_screen_off);
            mbRegistedShutdownReceiver = true;

            // Filter for phone unlock so that we can finish secure camera
            // via this UI path:
            //    1. from secure lock screen, user starts secure camera
            //    2. user presses home button
            //    3. user unlocks phone
            IntentFilter filter_user_unlock = new IntentFilter(Intent.ACTION_USER_PRESENT);
            registerReceiver(mShutdownReceiver, filter_user_unlock);
        }

        Log.d(TAG, "checkLockedSecurity: action:" + action + "    mSecureCamera:" + mSecureCamera);
    }

    private void tryAddDismissKeygardFlag() {
        if (isKeyguardLocked() && !isKeyguardSecure()) {
            getWindow().addFlags(getFlagsDismissKeygard());
        }
    }

    private void tryRemoveDismissKeygardFlag() {
        //if (isKeyguardLocked() && !isKeyguardSecure()) {
        getWindow().clearFlags(getFlagsDismissKeygard());
        //}
    }


    public boolean isFromSpirite(Intent intent) {
        return intent.getBooleanExtra("isFromSprite", false);
    }

    protected int getFlagsShowLocked() {
        return /*WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | */WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                /*| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON*/;
    }


    protected int getFlagsDismissKeygard() {
        Log.d(TAG, "getFlagsDismissKeygard: ");
        return WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
    }


    private boolean isSecurity(Intent intent) {
        if (intent == null) {
            return false;
        }
        boolean mbSecure = false;
        String action = intent.getAction();
        if (ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action)
                || ACTION_IMAGE_CAPTURE_SECURE.equals(action) || isKeyguardLockedSecure()) {
            mbSecure = true;
        } else {
            mbSecure = intent.getBooleanExtra(SECURE_CAMERA_EXTRA, false);
        }
        Log.d(TAG, "isSecurity: action:" + action + "  mbSecure:" + mbSecure);
        return mbSecure;
    }

    private static final String TAG = "BasicFragmentActivity";
    /**
     * Close activity when secure app passes lock screen or screen turns
     * off.
     */
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "received intent, finishing: " + intent.getAction() + "------------------");
            finish();
            //moveTaskToBack(true);
            //if (isKeyguardLockedSecure()) {
            //    clearLockScreenFlag();
            //}
            //onScreenOffLockedSecure();
        }
    };

    protected boolean isKeyguardLocked() {
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (mKeyguardManager != null) {
            return mKeyguardManager.isKeyguardLocked();
        }
        return false;
    }

    protected boolean isKeyguardSecure() {
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (mKeyguardManager != null) {
            return mKeyguardManager.isKeyguardSecure();
        }
        return false;
    }

    /**
     * 注册广播接收器
     *
     * @param receiver 广播接收器
     * @param actions  广播类型
     */
    protected void registerReceiver(BroadcastReceiver receiver, String... actions) {
        IntentFilter intentFilter = new IntentFilter();
        for (String action : actions) {
            intentFilter.addAction(action);
        }
        registerReceiver(receiver, intentFilter);
    }

    /**
     * 跳转目标Activity
     *
     * @param targetClass 目标Activity类型
     */
    public void startActivity(Class<? extends Activity> targetClass) {
        startActivity(targetClass, null);
    }

    /**
     * 跳转目标Activity(传递参数)
     *
     * @param targetClass 目标Activity类型
     * @param args        传递参数
     */
    public void startActivity(Class<? extends Activity> targetClass, Bundle args) {
        Intent intent = new Intent(this, targetClass);
        if (args != null)
            intent.putExtras(args);
        startActivity(intent);
    }


    /**
     * 隐式跳转目标Activity
     *
     * @param action 隐式动作
     */
    public void startActivity(String action) {
        startActivity(action, null);
    }

    /**
     * 隐式跳转目标Activity
     *
     * @param action 隐式动作
     */
    public void startActivity(String action, Bundle args) {
        Intent intent = new Intent(action);
        if (args != null) {
            intent.putExtras(args);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startActivity: " + e);
        }
    }

    /**
     * 启动目标Service
     *
     * @param targetClass 目标Service类型
     * @param args        传递参数
     */
    public void startService(Class<? extends Service> targetClass, Bundle args) {
        Intent intent = new Intent(this, targetClass);
        if (args != null)
            intent.putExtras(args);
        startService(intent);
    }

    /**
     * 启动目标Service
     *
     * @param targetClass 目标Service类型
     */
    public void startService(Class<? extends Service> targetClass) {
        startService(targetClass, null);
    }

    /**
     * 隐式跳转目标Service
     *
     * @param action 隐式动作
     */
    public void startService(String action) {
        startService(action, null);
    }

    /**
     * 隐式跳转目标Service
     *
     * @param action 隐式动作
     */
    protected void startService(String action, Bundle args) {
        Intent intent = new Intent(action);
        if (args != null)
            intent.putExtras(args);
        startService(intent);
    }

    @Override
    public boolean dispatchTouchEvent(@Nullable MotionEvent ev) {
        if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isShouldHideInput(view, ev))
                KeyboardUtils.closeKeyboard(mContext, view);
            return super.dispatchTouchEvent(ev);
        }
        //必不可少,否则所有的组件都不会有TouchEvent了
        return getWindow().superDispatchTouchEvent(ev) || onTouchEvent(ev);
    }

    /**
     * 是否应该隐藏键盘
     *
     * @param view  对应的view
     * @param event 事件
     * @return 是否隐藏
     */
    private boolean isShouldHideInput(View view, MotionEvent event) {
        if (view != null && (view instanceof EditText)) {
            int[] leftTop = {0, 0};
            //获取输入框当前的位置
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + view.getHeight();
            int right = left + view.getWidth();
            return !(event.getX() > left && event.getX() < right && event.getY() > top && event.getY() < bottom);
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, ">>>>>>>>>>>>>>>onStart: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isKeyguardLocked()) {
            //clearLockScreenFlag();
        }
        boolean forground = CommonUtils.isForground(BasicFragmentActivity.this);
        if (isSecureCamera()) {//安全相机中按下锁屏键，则退出相机应用
            if (forground) {
//                finish();
            }
        }
        tryRemoveDismissKeygardFlag();
        Log.d(TAG, ">>>>>>>>>>>>>>>onStop:   isForground:" + forground);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isSecurity(getIntent());
        //if (!isSecureCamera()) {
        //checkLockedSecurity();
        //}
        Log.d(TAG, ">>>>>>>>>>>>>>>onRestart: " + BasicFragmentActivity.this.toString()
                + "  isKeyguardLockedSecure:" + isKeyguardLockedSecure());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, ">>>>>>>>>>>>>>>onDestroy: ");
        if (mbRegistedShutdownReceiver) {
            unregisterReceiver(mShutdownReceiver);
            mbRegistedShutdownReceiver = false;
        }
    }

    private void clearLockScreenFlag() {
        Window win = getWindow();
        win.clearFlags(getFlagsShowLocked()/*WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED*/);
        Log.d(TAG, "clearLockScreenFlag: FLAG_SHOW_WHEN_LOCKED");
    }

    public final boolean printLockSecureState() {
        android.app.KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean inKeyguardRestrictedInputMode = mKeyguardManager.inKeyguardRestrictedInputMode();
        boolean isDeviceLocked = false;
        boolean isDeviceSecure = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                isDeviceLocked = mKeyguardManager.isDeviceLocked();
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                isDeviceSecure = mKeyguardManager.isDeviceSecure();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean isKeyguardLocked = mKeyguardManager.isKeyguardLocked();//X
        boolean isKeyguardSecure = mKeyguardManager.isKeyguardSecure();//

        Log.d(TAG, "printLockSecureState:  inKeyguardRestrictedInputMode:" + inKeyguardRestrictedInputMode
                + "  isDeviceLocked :" + isDeviceLocked
                + "  isDeviceSecure:" + isDeviceSecure
                + "  isKeyguardLocked:" + isKeyguardLocked
                + "  isKeyguardSecure:" + isKeyguardSecure);
        return isDeviceLocked;
    }

    public boolean isFromLockScreen(Intent it) {
        if (it == null) {
            return false;
        }
        String action = it.getAction();
        return (!TextUtils.isEmpty(action)) && (action.equalsIgnoreCase(ACTION_STILL_IMAGE_CAMERA_SECURE)
                || action.equalsIgnoreCase(ACTION_IMAGE_CAPTURE_SECURE));


    }


    protected boolean isKeyguardLockedSecure() {
        return isKeyguardLocked() && isKeyguardSecure();
    }
}
