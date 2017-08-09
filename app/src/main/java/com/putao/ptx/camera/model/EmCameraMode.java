package com.putao.ptx.camera.model;

import com.putao.ptx.camera.R;

public enum EmCameraMode {
    VOICE(0, R.string.camera_mode_voice),
    COMMON(1, R.string.camera_mode_common),
    VIDEO(2, R.string.camera_mode_video),
    PANORAMA(3, R.string.camera_mode_panorama);
    public final int mIdx, mStrId;

    EmCameraMode(int idx, int strId) {
        mIdx = idx;
        mStrId = strId;
    }

    public static EmCameraMode getByIdx(int idx) {
        for (EmCameraMode m : values()) {
            if (idx == m.mIdx) {
                return m;
            }
        }
        return null;
    }

    public boolean isFullScreen() {
        return EmCameraMode.this != VIDEO;
    }

    public static boolean isFullScreen(int idx) {
        EmCameraMode mode = getByIdx(idx);
        return mode != null && mode.isFullScreen();
    }

    public interface OnCameraModeListener {
        EmCameraMode getCameraMode();
    }
}