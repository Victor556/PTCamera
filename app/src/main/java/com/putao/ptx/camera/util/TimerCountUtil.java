package com.putao.ptx.camera.util;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by Administrator on 2016/4/27.
 */
public class TimerCountUtil {
    private final static String TAG = TimerCountUtil.class.getSimpleName();
    public final static int DOWNCOUNT_MODE = 1;
    public final static int DEFAULTCOUNT_MODE = 0;
    private static boolean started = false;
    private static boolean isCancelled = false;
    private static boolean takePic = true;
    private static TimerCountCallBack timerCountCallBack;
    private static final Object sObjSyn = new Object();
    public static final int MSG_START = 0x001;
    public static final int MSG_COUNT = 0x002;
    public static final int MSG_END = 0x003;
    public static final int MSG_START_PRE = 0x004;
    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (timerCountCallBack == null)
                return;
            if (msg.what == MSG_START_PRE) {
                timerCountCallBack.onStartPre();
            } else if (msg.what == MSG_START) {
                timerCountCallBack.onStart();
            } else if (msg.what == MSG_COUNT) {
                int count = msg.arg1;//msg.getData().getInt("count");
                boolean ret = timerCountCallBack.onCount(count);
                if (!ret) {
                    handler.sendEmptyMessage(MSG_END);//此行代码出现过ANR？？？
                }
            } else if (msg.what == MSG_END) {
                if (timerCountCallBack != null) {//出现过空指针timerCountCallBack==null的bug
                    timerCountCallBack.onEnd(takePic);
                }
                isCancelled = false;
                started = false;
                takePic = true;
            }
        }
    };
    private static Thread sThread;


    public interface TimerCountCallBack {
        void onStartPre();

        void onStart();

        boolean onCount(int count);

        void onEnd(boolean takePic);
    }

    public static void start(final int mode, final int start, final int end, final TimerCountCallBack callBack) {
        if (started) {
            return;
        }
        timerCountCallBack = callBack;

        sThread = new Thread(new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MSG_START_PRE);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(MSG_START);
                long tmStartMsg = SystemClock.elapsedRealtime();
                int count = start;

                final int cnt = 10;
                if (DOWNCOUNT_MODE == mode) {
                    while (count > 0 && !isCancelled) {
                        Message msg = new Message();
                        msg.what = MSG_COUNT;
                        msg.arg1 = count;
//                        Bundle data = new Bundle();
//                        data.putInt("count", count);
//                        msg.setData(data);
                        handler.sendMessage(msg);
                        try {
                            //Thread.sleep(1000);
                            synchronized (sObjSyn) {
                                if (!isCancelled) {
                                    //sObjSyn.wait(1000);
                                    long waitTime = tmStartMsg + (start - count + 1) * 1000 - SystemClock.elapsedRealtime();
                                    long millis = Math.max(Math.min(1000, waitTime), 0);
                                    Log.d(TAG, "run: waitTime  " + millis + "  count:" + count);
                                    sObjSyn.wait(millis);
                                }
                            }
                            count--;
                        } catch (InterruptedException e) {
                        }
                    }
                } else if (DEFAULTCOUNT_MODE == mode) {
                    while (count < end && !isCancelled) {
                        Message msg = new Message();
                        msg.what = MSG_COUNT;
                        msg.arg1 = count;
//                        Bundle data = new Bundle();
//                        data.putInt("count", count);
//                        msg.setData(data);
                        handler.sendMessage(msg);
                        try {
                            //Thread.sleep(1000);
                            synchronized (sObjSyn) {
                                if (!isCancelled) {
                                    //sObjSyn.wait(1000);
                                    long waitTime = tmStartMsg + (count - start + 1) * 1000 - SystemClock.elapsedRealtime();
                                    long millis = Math.max(Math.min(1000, waitTime), 0);
                                    Log.d(TAG, "run: waitTime  " + millis + "  count:" + count);
                                    sObjSyn.wait(millis);
                                }
                            }
                            count++;
                        } catch (InterruptedException e) {
                        }
                    }
                }
                handler.sendEmptyMessage(MSG_END);
            }
        });
        sThread.setName("---thread for sound pic,count down, record video---");//有声照片，倒计时，录像线程
        sThread.setDaemon(true);
        started = true;
        isCancelled = false;
        sThread.start();
    }

    public static void stop() {
        if (!isCancelled) {
            isCancelled = true;
            //started = false;
            synchronized (sObjSyn) {
                sObjSyn.notifyAll();
            }
        }
    }

    public static void stopNoTakePic() {
        takePic = false;
        stop();
    }

    public static boolean isStarted() {
        return started;
    }

    public static boolean isThreadAlive() {
        return sThread != null && sThread.isAlive();
    }

    public static boolean isCancelled() {
        return isCancelled;
    }
}
