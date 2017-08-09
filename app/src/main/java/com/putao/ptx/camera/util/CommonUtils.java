package com.putao.ptx.camera.util;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.hardware.display.DisplayManager;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.Display;

import com.putao.ptx.camera.AppCommon;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommonUtils {

    public static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                AppCommon.FILE_PARENT_NAME);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = StringFormatUtil.sformat//new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + "_" + (int) (Math.random() * 10) + (int) (Math.random() * 10) + ".jpg");

        return mediaFile;
    }

    public static File getOutputVideoFile() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                AppCommon.FILE_PARENT_NAME);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = StringFormatUtil.sformat//new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + "_" + (int) (Math.random() * 10) + (int) (Math.random() * 10) + ".mp4");

        return mediaFile;
    }


    public static Dialog dialog(Context context, String message, String b1text,
                                String b2text, OnClickListener... listeners) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        return builder.show();
    }

    /**
     * base64加密
     *
     * @param encodeStr
     * @return
     */
    public static String encode(String encodeStr) {
        return new String(Base64.encode(encodeStr.getBytes(), Base64.DEFAULT));
    }

    /**
     * base64解密
     *
     * @param encodeStr
     * @return
     */
    public static String decode(String encodeStr) {
        return new String(Base64.decode(encodeStr.getBytes(), Base64.DEFAULT));
    }


    public static String parseTime(Object time) {
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        if (time instanceof Long) {
            date = new Date((Long) time);
        } else if (time instanceof String) {
            try {
                date = fmt.parse(String.valueOf(time));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return fmt.format(date);
    }

    public static String parseTime(Object time, String type) {
        DateFormat fmt = new SimpleDateFormat(type);
        Date date = null;
        if (time instanceof Long) {
            date = new Date((Long) time);
        } else if (time instanceof String) {
            try {
                date = fmt.parse(String.valueOf(time));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return fmt.format(date);
    }

    public static boolean isServiceRunning(Context mContext, String className) {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断存储卡是否挂载
     *
     * @return
     */
    public static boolean isExternalStorageMounted() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isApkDebugable(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {

        }
        return false;
    }

    public static boolean isZh(Context ctx) {
        Locale locale = ctx.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    /**
     * 判断当前应用程序处于前台还是后台
     */
    public static boolean isForground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        boolean ret = false;

        try {
            if (appProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess.processName.equals(context.getPackageName())) {
                        if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                            Log.i("后台", appProcess.processName);
                            ret = false;
                        } else {
                            Log.i("前台", appProcess.processName);
                            ret = true;
                        }
                    }
                }
            }
        } catch (Exception e) {//此处出现过空指针bug
            e.printStackTrace();
            Log.e(TAG, "isForground: " + e);
        }
        Log.d(TAG, "isForground:   isForground:" + ret);
        return ret;
    }

    private static final String TAG = "CommonUtils";

    public static boolean isScreenOn(Context ctx) {
        DisplayManager server = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
        return server.getDisplay(0).getState() == Display.STATE_ON;
    }

}
