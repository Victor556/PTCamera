package com.putao.ptx.camera.eventbus;

import android.graphics.Bitmap;

/**
 * Created by admin on 2016/7/27.
 */
public class EventUpdateAlbum {
    public final Bitmap mBitmap;

    public EventUpdateAlbum(Bitmap bitmap) {
        mBitmap = bitmap;
    }
}
