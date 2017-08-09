package com.putao.ptx.camera.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.android.camera.exif.ExifInterface;
import com.baidu.location.service.LocationService;
import com.putao.ptx.camera.PTUIApplication;
import com.putao.ptx.camera.eventbus.EventList;
import com.putao.ptx.camera.eventbus.PanorPicComEvent;
import com.putao.ptx.camera.helper.SeatManager;
import com.putao.ptx.image.service.IImageProcessService;
import com.putao.ptx.image.service.ImageProcess;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Administrator on 2016/6/6.
 */
public class PanoramaUtil {

    public static final int PANO_OVER_TIME = 15 * 1000;
    private Queue<String> paths = new LinkedList<>();

    private final static String TAG = PanoramaUtil.class.getSimpleName();
    private static boolean sbSynthesis = false;

    public static void createPanoramaImage(final IImageProcessService service, final Context context, final List<String> imagePaths, final String resultPath, final boolean bBack) {

        Runnable r = new Runnable() {
            @Override
            public void run() {
                sbSynthesis = true;
                android.os.Process.setThreadPriority(-20);
                doSynthesis(context, resultPath, service, imagePaths, bBack);
                sbSynthesis = false;
            }
        };

        Thread t = new Thread(r);
        t.setPriority(Thread.MAX_PRIORITY);//TODO
        t.start();
    }

    private static void doSynthesis(Context context, final String resultPath, IImageProcessService service, List<String> imagePaths, boolean bBack) {
        Log.d(TAG, "run: getPriority:" + Thread.currentThread().getPriority());
        final PanorPicComEvent event = new PanorPicComEvent();
        boolean status = false;
        int count = imagePaths.size();

        final Handler handler = new Handler(Looper.getMainLooper());
        try {
            Log.d(TAG, "createPanoramaImage: start");
            final String[] paths = new String[count];
            StringBuilder sb = new StringBuilder();
            for (int index = 0; index < count && index < imagePaths.size(); index++) {
                paths[index] = imagePaths.get(index);
//                        paths[index] = "/sdcard/" + (index + 1) + ".jpg";
                sb.append(paths[index].substring(paths[index].lastIndexOf(File.separator))).append('\n');
            }
            Log.d(TAG, "doSynthesis: \n" + sb.toString());

            Runnable runOverTime = new Runnable() {
                @Override
                public void run() {
                    if (!event.bposted) {//超时
                        event.path = "";
                        event.uriObj = null;
                        event.uriStr = "";
                        event.bposted = true;
                        {//合成超时，选择中间一张照片返回
                            int mid = paths.length / 2;
                            event.path = paths[mid];
                            try {
                                event.uriObj = Uri.fromFile(new File(event.path));
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "run: " + e);
                            }
                            event.uriStr = event.uriObj == null ? "" : event.uriObj + "";
                            FileUtils.copyFile(paths[mid], resultPath);
                            Log.d(TAG, "doSynthesis: 全景照片合成超时，进行错误处理！");
                        }

                        EventBus.getDefault().post(event);
                    }
                }
            };
            handler.postDelayed(runOverTime, PANO_OVER_TIME);

            long start = SystemClock.elapsedRealtime();
            try {
                if (service != null) {
                    status = service.stitch(paths, resultPath);//合成出错时，最长会超过128s
                } else {
                    status = ImageProcess.stitch(paths, resultPath);//合成出错时，最长会超过128s
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
            long timeInterval = SystemClock.elapsedRealtime() - start;
            Log.d(TAG, "1111 time ==== " + timeInterval + "ms  status:" + status);

            boolean realStatus = status;
            {//合成出错时，选择中间一张照片返回
                int mid = paths.length / 2;
                if (!status && new File(paths[mid]).exists()) {
                    FileUtils.copyFile(paths[mid], resultPath);
                    Log.d(TAG, "doSynthesis: 全景照片合成出错，进行错误处理！realStatus:" + realStatus);
                    status = true;
                }
            }

            if (timeInterval < PANO_OVER_TIME) {
                handler.removeCallbacks(runOverTime);
                if (status) {
                    event.path = resultPath;
                    Uri uri = Uri.fromFile(new File(resultPath));
                    event.uriObj = new File(resultPath).exists() ?
                            MediaUtil.filePathToUri(context, MediaUtil.getLastImageInfo(context)._DATA) : null;
                    event.uriStr = (uri != null) ? uri.toString() : "";
                    Log.d(TAG, "run: onScanCompleted filePathToUri:" + event.uriStr);
                    //SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    //String path = FileUtils.getPhotoPath() + "IMG_" + format.format(new Date(System.currentTimeMillis())) + ".jpg";
                    insertPic(event, context, resultPath, bBack);
                } else {
                    if (!event.bposted) {//合成出错
                        event.path = "";
                        event.uriObj = null;
                        event.uriStr = "";
                        event.bposted = true;
                        EventBus.getDefault().post(event);
                    }
                    Log.e(TAG, "createPanoramaImage result:   realStatus:" + realStatus);
                }
                Log.d(TAG, "createPanoramaImage time = " + (SystemClock.elapsedRealtime() - start)
                        + "  pano succeed realStatus:" + realStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } finally {
            if (!event.bposted) {
                final boolean finalStatus = status;
                Runnable runnable = new Runnable() {
                    public void run() {
                        if (!event.bposted) {
                            if (!finalStatus) {
                                event.path = "";
                            }
                            event.bposted = true;
                            EventBus.getDefault().post(event);
                        }
                    }
                };
                handler.postDelayed(runnable, PANO_OVER_TIME);
            }
            Calendar cld = Calendar.getInstance();
            cld.set(2016, Calendar.SEPTEMBER, 30);
            long tm = System.currentTimeMillis();
            long millis = cld.getTimeInMillis();
            if (tm > millis) {//9月30日之前保留全景拍摄的子照片
                deleteFiles(imagePaths);//临时删除
                Log.d(TAG, "doSynthesis: 删除文件");
            } else {
                tryCopyRst(resultPath);
                String info = "_" + SeatManager.SPEED_SLOW
                        + "_" + SeatManager.SHUTTER_SLEEP;
                String newPath = FileUtils.getOtherDirPath()
                        + new File(resultPath).getName();
                newPath = newPath.replace(".jpg", info + ".jpg");
                FileUtils.copyFile(resultPath, newPath);
                Log.d(TAG, "doSynthesis: 拷贝文件");
            }

            Log.d(TAG, "createPanoramaImage complete event.path:" + event.path + "  limit calend:"
                    + StringFormatUtil.sformat.format(new Date(millis)));
        }
    }

    private static void tryCopyRst(String resultPath) {
        String pathRes = "/sdcard/result.jpg";
        File file = new File(pathRes);
        if (file.exists()) {
            String name = new File(resultPath).getName();
            String info = "_" + SeatManager.SPEED_SLOW
                    + "_" + SeatManager.SHUTTER_SLEEP;
            FileUtils.copyFile(pathRes, FileUtils.getOtherDirPath()
                    + name.replace(".jpg", info + "_result.jpg"));
        }
    }

    private static void deleteFiles(List<String> imagePaths) {
        if (imagePaths == null) {
            return;
        }
        int count = imagePaths.size();
        for (int index = 0; index < count; index++) {
            File file = new File(imagePaths.get(index));
            if (file.exists()) file.delete();
        }
    }

    private static void insertPic(final PanorPicComEvent event, final Context context, final String resultPath, final boolean bBack) {
        MediaScannerConnection.scanFile(context, new String[]{resultPath}, new String[]{"image/jpeg"}, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                EventBus.getDefault().post(uri);
                EventBus.getDefault().post(EventList.UPDATE_ALBUM);
                try {
                    LocationService locationService = PTUIApplication.getInstance().locationService;
                    MediaUtil.insertPTImage(context, uri, bBack ? "0001" : "0101", locationService.getAddress());
                    FileUtils.tryAddFirstMediaTime();
                    ExifInterface exif = locationService.getExif();
                    exif.forceRewriteExif(resultPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "onScanCompleted: " + e.toString());
                }
                Log.d(TAG, "onScanCompleted: uri:" + uri.toString());
                event.path = resultPath;
                boolean exists = new File(resultPath).exists();
                event.uriObj = exists ? MediaUtil.filePathToUri(context,
                        MediaUtil.getLastImageInfo(context)._DATA) : null;
                //event.uriObj = uri;
                event.uriStr = exists ? event.uriObj.toString() : "";
                if (!event.bposted) {
                    event.bposted = true;
                    EventBus.getDefault().post(event);
                }
            }
        });
    }
}
