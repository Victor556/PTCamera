package com.putao.ptx.camera.eventbus;

import android.graphics.Bitmap;
import android.net.Uri;

import com.putao.ptx.camera.model.MediaInfo;

/**
 * Created by admin on 2016/7/4.
 */
public class EventLastMediaFile {
    public final MediaInfo mInfo;
    public final Bitmap mMp;
    public final boolean bImage;
    public Uri uri;

    public EventLastMediaFile(MediaInfo info, Bitmap mp, boolean bImage) {
        this.mInfo = info;
        this.mMp = mp;
        this.bImage = bImage;
    }

    @Override
    public String toString() {
        return "[mInfo=" + mInfo
                + "  bImage=" + bImage
                + "  uri=" + uri
                + "]";
    }
}
