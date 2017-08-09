package com.putao.ptx.camera.model;

import android.graphics.RectF;

import java.util.Collection;

/**
 * Created by Administrator on 2016/3/15.
 */
public class FaceModel {
    private final String[] emo = {"喜悦", "悲伤", "", "", "惊讶", "厌怒", "正常"};
    public float[] landmarks = {};
    public float[] emotions = {};
    public RectF rectf;

    public boolean isSmile() {
        return emotions != null && emotions.length > 0 && emotions[0] > 0.4;
    }

    public float getSmileLevel() {
        return emotions != null && emotions.length > 0 ? emotions[0] : 0;
    }

    public static FaceModel getSmilest(Collection<FaceModel> faces) {
        if (faces == null && faces.size() == 0) {
            return null;
        }
        FaceModel ret = null;
        for (FaceModel f : faces) {
            if (ret == null) {
                ret = f;
            }
            if (ret.getSmileLevel() < f.getSmileLevel()) {
                ret = f;
            }
        }
        return ret;
    }
}
