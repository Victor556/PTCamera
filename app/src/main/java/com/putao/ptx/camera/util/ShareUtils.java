package com.putao.ptx.camera.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.putao.ptx.camera.PTUIApplication;


/**
 * SharedPreferences的一个工具类，调用setParam就能保存String, Integer, Boolean, Float, Long类型的参数
 * 同样调用getParam就能获取到保存在手机里面的数据
 */
public class ShareUtils {
    /**
     * 保存在手机里面的文件名
     */
    private static final String FILE_NAME = "share_date";


    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param key
     * @param object
     */
    public static boolean setParam(String key, Object object) {
        return setParam(PTUIApplication.getInstance(), key, object);
    }

    private static final String TAG = ShareUtils.class.getSimpleName();

    private static boolean setParam(Context context, String key, Object object) {
        if (context == null || key == null || object == null) {
            return false;
        }

        String type = object.getClass().getSimpleName();
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if ("String".equals(type)) {
            editor.putString(key, (String) object);
        } else if ("Integer".equals(type)) {
            editor.putInt(key, (Integer) object);
        } else if ("Boolean".equals(type)) {
            editor.putBoolean(key, (Boolean) object);
        } else if ("Float".equals(type)) {
            editor.putFloat(key, (Float) object);
        } else if ("Long".equals(type)) {
            editor.putLong(key, (Long) object);
        }
        Log.d(TAG, "setParam:   key:" + key + "  value:" + object);
        return editor.commit();
    }


    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param key
     * @param defaultObject
     * @return
     */

    public static <T> T getParam(String key, T defaultObject) {
        return getParam(PTUIApplication.getInstance(), key, defaultObject);
    }

    private static <T> T getParam(Context context, String key, T defaultObject) {
        if (context == null || key == null) {
            return null;
        }
        String type = defaultObject.getClass().getSimpleName();
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        T value = null;
        if ("String".equals(type)) {
            value = (T) sp.getString(key, (String) defaultObject);
        } else if ("Integer".equals(type)) {
            value = (T) (Integer) sp.getInt(key, (Integer) defaultObject);
        } else if ("Boolean".equals(type)) {
            value = (T) (Boolean) sp.getBoolean(key, (Boolean) defaultObject);
        } else if ("Float".equals(type)) {
            value = (T) (Float) sp.getFloat(key, (Float) defaultObject);
        } else if ("Long".equals(type)) {
            value = (T) (Long) sp.getLong(key, (Long) defaultObject);
        }
        Log.d(TAG, "getParam:   key:" + key + "  value:" + value);
        return value;
    }
}
