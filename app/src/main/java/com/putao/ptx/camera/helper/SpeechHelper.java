package com.putao.ptx.camera.helper;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.putao.ptx.camera.PTUIApplication;

/**
 * Created by Administrator on 2016/6/20.
 */
public class SpeechHelper {
    private static boolean sInited = false;
    private Context mContext;
    private SpeechSynthesizer synthesizer;
    private OnSpeakFinishedListener mOnSpeakFinishedListener;

    private static SpeechHelper sSpeechHelper;

    private String mLastMsg = "";
    private boolean mAllowStop = true;

    public boolean isSpeaking() {
        return speaking;
    }

    private boolean speaking = false;


    private static void initSpeech() {
        if (sInited) {
            return;
        }
        //讯飞初始化
        StringBuffer param = new StringBuffer();
        param.append("appid=" + "56f39a2e");
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(PTUIApplication.getInstance(), param.toString());
        sInited = true;
    }

    public synchronized static SpeechHelper instance() {
        if (sSpeechHelper == null) {
            sSpeechHelper = new SpeechHelper(PTUIApplication.getInstance());
        }
        return sSpeechHelper;
    }


    private SpeechHelper(Context context) {
        if (!sInited) {
            initSpeech();
        }
        this.mContext = context;
        init(context);
    }

    private void init(Context context) {
        try {
            synthesizer = SpeechSynthesizer.createSynthesizer(context, null);
            if (synthesizer == null) return;
            synthesizer.setParameter(SpeechConstant.VOICE_NAME, voicerLocal/*"vinn"*/);//设置发音人
            synthesizer.setParameter(SpeechConstant.SPEED, "70"/*"50"*/);//设置语速
            synthesizer.setParameter(SpeechConstant.VOLUME, "100");//设置音量，范围0~100
            synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL); //设置离线
            synthesizer.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 见assets/tts中的文件
     */
    public static String voicerLocal = "nannan";

    //获取发音人资源路径
    private String getResourcePath(Context context) {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(
                context, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(
                context, ResourceUtil.RESOURCE_TYPE.assets, "tts/" + voicerLocal + ".jet"));
        return tempBuffer.toString();
    }


    public void speakMessage(String msg) {
        speakMessage(msg, null);
    }

    private static final String TAG = "SpeechHelper";

    public void speakMessage(String msg, OnSpeakFinishedListener listener) {
        speakMessage(msg, listener, true);
    }


    public void speakMessage(String msg, boolean allowStop) {
        speakMessage(msg, null, allowStop);
    }

    public void speakMessage(String msg, OnSpeakFinishedListener listener, boolean allowStop) {
        if (speaking) return;
        if (TextUtils.isEmpty(msg)) {
            if (listener != null) {
                listener.onSpeakFinished();
            }
            return;
        }

        speaking = true;
        mOnSpeakFinishedListener = listener;
        if (synthesizer != null) {
            mLastMsg = msg;
            mAllowStop = allowStop;
            synthesizer.startSpeaking(msg, new MySynthesizerListener());
            Log.d(TAG, "speakMessage: " + msg);
        }
    }

    public void stopSpeaking() {
        mOnSpeakFinishedListener = null;
        if (synthesizer != null && synthesizer.isSpeaking()) {
            Log.d(TAG, "stopSpeaking: ");
            synthesizer.stopSpeaking();
            speaking = false;
        }
    }

    public boolean isAllowStop() {
        return mAllowStop;
    }

    private class MySynthesizerListener implements SynthesizerListener {
        @Override
        public void onSpeakBegin() {

        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakPaused() {

        }

        @Override
        public void onSpeakResumed() {

        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            speaking = false;
            if (mOnSpeakFinishedListener != null) {
                mOnSpeakFinishedListener.onSpeakFinished();
                mOnSpeakFinishedListener = null;
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    }


    public static interface OnSpeakFinishedListener {
        void onSpeakFinished();
    }

    public String getLastMsg() {
        return mLastMsg == null ? "" : mLastMsg;
    }
}
