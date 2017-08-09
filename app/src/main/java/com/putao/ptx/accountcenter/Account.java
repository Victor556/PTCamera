package com.putao.ptx.accountcenter;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.putao.ptx.authenticator.AccountCallback;
import com.putao.ptx.authenticator.AccountManager;
import com.putao.ptx.camera.PTUIApplication;
import com.putao.ptx.statistics.collector.PTAgent;

/**
 * Created by admin on 2016/7/13.
 */
public class Account {
    public static final String APPID = "1101";
    public static boolean USE_ACCOUNT_CENTER = true; // 是否使用个人中心
    private String mUid = "";
    private String mToken = "";
    private static Account instance;

    public String getToken() {
        return mToken;
    }

    public String getUid() {
        return mUid;
    }

    private Account(Context ctx) {
    }

    public static synchronized Account instance(@Nullable Context ctx) {
        if (instance == null) {
            instance = new Account(ctx);
        }
        return instance;
    }

    /**
     * 认证账户信息供统计使用<br>
     * 耗时1~23ms
     */
    public void registerAccountCenter() {
        if (!USE_ACCOUNT_CENTER) {
            return;
        }
        long tm = SystemClock.elapsedRealtime();
        try {
            AccountManager.init(PTUIApplication.getInstance(), APPID).auth(new AccountCallback() {
                @Override
                public void onSuccess(com.putao.ptx.authenticator.Account account) {
                    if (null != account) {
                        // 个人中心初始化
                        mUid = account.uid;
                        mToken = account.token;
                        PTAgent.getInstance().init(PTUIApplication.getInstance(), mUid, mToken);
                        Log.d(TAG, "onAuthResponse uid : " + mUid + "  deviceToken:" + mToken);
                    }
                    //如果账户重新登录后，账户有变化，在这里处理新数据
                    Log.d(TAG, "onLoginChangedListener uid : " + mUid);
                    Log.d(TAG, "onAuthResponse deviceToken : " + mToken);
                }

                @Override
                public void onFailed(int errorCode, String s) {
                    String string = "";
                    switch (errorCode) {
                        case AccountManager.ERROR_INVALID_TOKEN:
                            string = "ERROR_INVALID_TOKEN";
                            break;
                        case AccountManager.ERROR_NO_PERMISSION:
                            string = "签名不一致";
                            break;
                        case AccountManager.ERROR_PARAMETERS:
                            string = "ERROR_PARAMETERS";
                            break;
                        case AccountManager.ERROR_SDK_VERSION:
                            string = "ERROR_SDK_VERSION";
                            break;
                        case AccountManager.ERROR_USER_CANCEL:
                            string = "ERROR_USER_CANCEL";
                            break;
                        case AccountManager.ERROR_UNKNOWN:
                            string = "ERROR_UNKNOWN";
                            break;
                        default:
                            break;
                    }
                    Log.d(TAG, "onFailed  : " + string + "  errorCode:" + errorCode);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "registerAccountCenter: " + e);
        }
        Log.d(TAG, "registerAccountCenter:  waste time= " + (SystemClock.elapsedRealtime() - tm));
    }

    public static void unregisterAccountCenter() {
        if (!USE_ACCOUNT_CENTER) {
            return;
        }
        try {
            PTAgent.getInstance().unInit();
        } catch (Exception e) {
            Log.e(TAG, "unregisterAccountCenter: " + e);
        }
        Log.d(TAG, "unregisterAccountCenter: ");
    }

    private static final String TAG = Account.class.getSimpleName();

    public void sendEvent() {
        if (TextUtils.isEmpty(mUid) || TextUtils.isEmpty(mToken)) {
            return;
        }
        try {
            PTAgent.getInstance().onEvent("com.putao.ptx.camera", "pkg", "ac_cus_picture", 1);
            Log.d(TAG, "sendEvent: ");
        } catch (Exception e) {
            Log.e(TAG, "sendEvent: " + e);
        }
    }
}
