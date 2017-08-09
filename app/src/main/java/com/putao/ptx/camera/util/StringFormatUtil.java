package com.putao.ptx.camera.util;

import java.text.SimpleDateFormat;

/**
 * Created by Administrator on 2016/4/1.
 */
public class StringFormatUtil {
    public static final SimpleDateFormat sformat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");

    /**
     * @param sec
     * @return 返回00:00或者*:00:00的时间格式
     */
    public static String getTimeStrHms(int sec) {
        if (sec < 3600) {
            return getTimeStr(sec);
        } else {
            return String.format("%d:%02d:%02d", sec / 3600, sec % 3600 / 60, sec % 60);
        }
    }

    //返回00：00格式的时间
    public static String getTimeStr(int sec) {
        return String.format("%02d:%02d", sec % 3600 / 60, sec % 60);
    }

    //返回00格式的时间
    public static String getTimeStr2(int value) {
        String timeStr = "00";
        if (value < 10) {
            timeStr = "0" + value;
        } else {
            timeStr = value + "";
        }
        return timeStr;
    }
}
