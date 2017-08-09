package com.putao.ptx.camera.model;

/**
 * Created by admin on 2016/7/6.
 */
public class MediaInfo {
    public String _ID;
    public String _DATA;
    public String _DATE_ADDED;
    public String _SIZE;
    public String _TITLE;
    public String _MIME_TYPE;
    public String _BUCKET_DISPLAY_NAME;

    @Override
    public String toString() {
        return "[MediaInfo:  _ID=" + _ID
                + "  _DATA=" + _DATA
                + "  _DATE_ADDED=" + _DATE_ADDED
                + "  _SIZE=" + _SIZE
                + "  _TITLE=" + _TITLE
                + "  _MIME_TYPE=" + _MIME_TYPE
                + "  _BUCKET_DISPLAY_NAME=" + _BUCKET_DISPLAY_NAME
                + "]";
    }
}
