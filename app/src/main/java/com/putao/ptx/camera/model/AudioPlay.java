package com.putao.ptx.camera.model;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.putao.ptx.camera.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by admin on 2016/8/30.
 */
public class AudioPlay {
    final Context mContext;
    private SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_SYSTEM, 0);

    Map<EmSound, Integer> mMapId = new HashMap<>();

    public AudioPlay(Context ctx) {
        this.mContext = ctx;
        initPool(ctx);
    }

    private void initPool(Context ctx) {
        for (EmSound r : EmSound.values()) {
            int id = soundPool.load(ctx, r.getSoundRes(), 1);
            mMapId.put(r, id);
        }
    }


    public void playSound(EmSound em) {
        try {
            soundPool.play(mMapId.get(em), 1, 1, 0, 0, 1);
        } catch (Exception e) {
            e.printStackTrace();
            soundPool.play(mMapId.get(EmSound.SHUTTER), 1, 1, 0, 0, 1);
        }
    }

    public static enum EmSound {
        SHUTTER(R.raw.shutter_sound),
        AUDIO_START(R.raw.audio_start),
        AUDIO_END(R.raw.audio_end);

        private final int mSoundRes;

        EmSound(int soundRes) {
            mSoundRes = soundRes;
        }

        public int getSoundRes() {
            return mSoundRes;
        }
    }
}
