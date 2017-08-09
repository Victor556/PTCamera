package com.putao.ptx.camera.util;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.putao.ptx.camera.R;

import java.lang.reflect.Field;

/**
 */
public final class CustomToastUtils {
    private static View rootView;
    private static TextView tvMsg;
    private static Toast toast;
    private static long lastShowTime;

    public static void showControlToast(Context context, String msg) {
        showControlToast(context, msg, false);
    }

    public static void showControlToast(Context context, String msg, boolean bLong) {
        if (System.currentTimeMillis() - lastShowTime < 2100)
            return;
        if (toast == null)
            toast = new Toast(context);
        if (rootView == null || tvMsg == null) {
            rootView = LayoutInflater.from(context).inflate(R.layout.toast_layout, null);
            tvMsg = (TextView) rootView.findViewById(R.id.tvMsg);
        }
        tvMsg.setText(msg);
        toast.setView(rootView);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 227);/*136dp*/
        toast.setDuration(bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        addFlags(toast, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        toast.show();
        lastShowTime = System.currentTimeMillis();
    }

    private static boolean addFlags(Toast toast, int pflags) {
        long tm = SystemClock.elapsedRealtime();
        try {
            Field mTN = Toast.class.getDeclaredField("mTN");
            mTN.setAccessible(true);
            Field mParams = mTN.getType().getDeclaredField("mParams");
            mParams.setAccessible(true);
            Field flags = mParams.getType().getDeclaredField("flags");
            flags.setAccessible(true);
            Object omTN = mTN.get(toast);
            Object omParams = mParams.get(omTN);
            Object oflags = flags.get(omParams);
            int rst = ((Integer) oflags) | pflags;
            flags.set(omParams, rst);
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Log.e(TAG, "addFlags: " + e.toString());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "addFlags: " + e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "addFlags: " + e.toString());
        }
        Log.d(TAG, "addFlags: waste time:" + (SystemClock.elapsedRealtime() - tm));
        return false;
    }

    private static final String TAG = "CustomToastUtils";
}
