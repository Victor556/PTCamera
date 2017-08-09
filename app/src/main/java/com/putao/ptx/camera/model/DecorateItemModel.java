package com.putao.ptx.camera.model;

/**
 * Created by Administrator on 2016/3/21.
 */
public class DecorateItemModel {
    public int width;
    public int height;
    public int distance;
    public int centerX;
    public int centerY;
    public float duration;
    public ImageList imageList;
    public long durationLong = 0;

    public void setDuration(float duration) {
        this.duration = duration;
        durationLong = (long) (this.duration * 1000);
    }


}
