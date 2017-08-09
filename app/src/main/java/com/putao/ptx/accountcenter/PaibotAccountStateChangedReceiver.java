package com.putao.ptx.accountcenter;

import android.util.Log;

import com.putao.ptx.authenticator.AccountStateChangedReceiver;

/**
 * Created by admin on 2016/11/23.
 */

public class PaibotAccountStateChangedReceiver extends AccountStateChangedReceiver {
    private static final String TAG = PaibotAccountStateChangedReceiver.class.getSimpleName();
    public static final String sAction = "com.putao.ptx.accountcenter.ACCOUNT_STATE";

    @Override
    public void onAccountLogin(String s) {
        Account.instance(null).registerAccountCenter();
        Log.d(TAG, "onAccountLogin: " + s);
    }

    @Override
    protected void onAccountLogout(String s) {
        Account.instance(null).unregisterAccountCenter();
        Log.d(TAG, "onAccountLogout: " + s);
    }

    @Override
    protected void onAccountUpdate(String s) {
        Log.d(TAG, "onAccountUpdate: " + s);
    }
}
