package com.putao.ptx.camera.model;

import java.io.Serializable;

/**
 * Created by Administrator on 2016/3/21.
 */
public class DecorateModel implements Serializable {
    public String icon;
    public DecorateItemModel eye;
    public DecorateItemModel mouth;
    public DecorateItemModel bottom;
    public int distance;
    public int centerX;
    public int centerY;
    public float duration;
    public long durationLong;
    public int animationImageSize;

    public void setDuration(float duration) {
        this.duration = duration;
        durationLong = (long) (duration * 1000);
    }

    @Override
    public String toString() {
        return "Animation{" +
                "icon='" + icon + '\'' +
                ", eye=" + eye +
                ", mouth=" + mouth +
                ", bottom=" + bottom +
                '}';
    }
}
