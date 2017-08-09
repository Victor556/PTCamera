package com.putao.ptx.camera.model;

import com.putao.ptx.camera.PTUIApplication;
import com.putao.ptx.camera.util.ShareUtils;

/**
 * Created by admin on 2016/11/24.
 */

public enum EmShareCnt {
    VOICE_PHOTO(5),
    PANO(5),
    SMILE_PHOTO(5);
    final int maxCnt;

    static final String APP_VERSION = "app_version";

    EmShareCnt(int maxCnt) {
        this.maxCnt = maxCnt;
    }

    public int getMaxCnt() {
        return maxCnt;
    }

    public int getCnt() {
        String versionCur = PTUIApplication.getVersion();
        String version = getSavedAppVersion();
        if (version.equals(versionCur)) {
            return ShareUtils.getParam(name(), 0);
        } else {
            setSavedAppVersion(versionCur);
            resetCnt();
            return 0;
        }
    }

    private static boolean setSavedAppVersion(String versionCur) {
        return ShareUtils.setParam(APP_VERSION, versionCur);
    }

    private static String getSavedAppVersion() {
        return ShareUtils.getParam(APP_VERSION, "");
    }

    public boolean isCntFull() {
        return getCnt() >= maxCnt;
    }

    private void addCnt() {
        ShareUtils.setParam(name(), getCnt() + 1);
    }

    private void resetCnt() {
        ShareUtils.setParam(name(), 0);
    }

    public boolean tryAddCnt() {
        if (isCntFull()) {
            return false;
        } else {
            addCnt();
            return true;
        }
    }
}
