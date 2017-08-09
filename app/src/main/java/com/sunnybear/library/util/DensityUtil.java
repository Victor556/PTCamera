package com.sunnybear.library.util;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

/**
 * dp与px转换工具
 * Created by guchenkai on 2015/11/10.
 */
public final class DensityUtil {

    /**
     * dp转px
     *
     * @param context context
     * @param dpValue dp值
     * @return px值
     */
    public static int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }

    /**
     * sp转px
     *
     * @param context context
     * @param spValue sp值
     * @return px值
     */
    public static int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }

    /**
     * px转dp
     *
     * @param context context
     * @param pxValue px值
     * @return dp值
     */
    public static float px2dp(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return pxValue / scale;
    }

    /**
     * px转sp
     *
     * @param context context
     * @param pxValue px值
     * @return dp值
     */
    public static float px2sp(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return pxValue / scale;
    }

    /**
     * 获得设备的屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getDeviceWidth(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        return manager.getDefaultDisplay().getWidth();
    }

    /**
     * 获得设备的屏幕高度
     *
     * @param context
     * @return
     */
    public static int getDeviceHeight(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
//        return manager.getDefaultDisplay().getHeight();
        int heightPixels;
//        WindowManager w = this.getWindowManager();
        Display d = manager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        heightPixels = metrics.heightPixels;
        // includes window decorations (statusbar bar/navigation bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                heightPixels = (Integer) Display.class
                        .getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
            // includes window decorations (statusbar bar/navigation bar)
        else if (Build.VERSION.SDK_INT >= 17)
            try {
                android.graphics.Point realSize = new android.graphics.Point();
                Display.class.getMethod("getRealSize",
                        android.graphics.Point.class).invoke(d, realSize);
                heightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        //Log.e("realHightPixels-heightPixels", heightPixels + "width");
        return heightPixels;
    }

    /**
     * 获得设备状态栏的高度
     *
     * @param context
     * @return
     */
    public static int getDeviceStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(
                    resourceId);
        }
        return result;
    }

    public static class Screen {
        public int widthPixels;
        public int heightPixels;
        public int densityDpi;
        public float density;
    }

    private static final String TAG = "DensityUtil";

    public static Screen getScreenPixels(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        Screen screen = new Screen();
        screen.widthPixels = dm.widthPixels;// e.g. 1080
        screen.heightPixels = dm.heightPixels;// e.g. 1920
        screen.densityDpi = dm.densityDpi;// e.g. 480
        screen.density = dm.density;// e.g. 2.0
        Log.d(TAG, "width=" + screen.widthPixels + ", height=" + screen.heightPixels
                + ", densityDpi=" + screen.densityDpi + ", density=" + screen.density);
        return screen;
    }

}
