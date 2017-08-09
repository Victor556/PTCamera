package com.putao.ptx.camera.model;

public enum EmPreviewSizeMode {
    /**
     * 拍照满屏预览4:3
     */
    PIC,
    /**
     * 前置，视频预览1280*720，宽高比5:3
     */
    VIDEO_FRONT,
    /**
     * 后置，视频预览2048*1080，5:3
     */
    VIDEO_BACK;

    public interface OnPreviewSizeListener {
        EmPreviewSizeMode getPreviewSizeMode();
    }
}