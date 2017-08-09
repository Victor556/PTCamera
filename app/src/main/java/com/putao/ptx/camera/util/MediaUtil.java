package com.putao.ptx.camera.util;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.putao.ptx.camera.PTUIApplication;
import com.putao.ptx.camera.model.ImageInfo;
import com.putao.ptx.camera.model.VideoInfo;

import org.jcodec.api.android.FrameGrab;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Administrator on 2016/5/24.
 */
public class MediaUtil {
    private static long lastVideoClickTime;
    private static long lastVoiceClickTime;

    //保证至少拍摄有声照片的时间
    public static boolean isFastVoicePic() {
        long time = System.currentTimeMillis();
        long timeD = time - lastVoiceClickTime;
        if (0 < timeD && timeD < 3500) return true;
        lastVoiceClickTime = time;
        return false;
    }

    //保证至少拍摄视频的时间
    public static boolean isFastVideo() {
        long time = SystemClock.elapsedRealtime();
        long timeD = time - lastVideoClickTime;
        if (0 < timeD && timeD < 2000) return true;
        lastVideoClickTime = time;
        return false;
    }

    /**
     * 获取最近的一张照片信息
     *
     * @return
     */
    public static ImageInfo getLastImageInfo(Context context) {
        long tm = SystemClock.elapsedRealtime();
        ImageInfo info = new ImageInfo();
        String condition = MediaStore.MediaColumns.DATA + " like '%/" + Environment.DIRECTORY_DCIM + "/%' ";
        String[] projection = {BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.MIME_TYPE};
        String sortString = MediaStore.Images.ImageColumns._ID/*DATE_MODIFIED */ + " desc";
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, condition, null, sortString);//该方法出现过一次ANR
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            info._ID = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
            info._DATE_ADDED = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
            info._SIZE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
            info._TITLE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE));
            info._MIME_TYPE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
            info._DATA = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            Log.d(TAG, "getLastImageInfo:   _ID:" + info._ID + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
        }
        return info;
    }

    /**
     * 获取最近的一张照片信息
     *
     * @return
     */
    public static VideoInfo getLastVideoInfo(Context context) {
        long tm = SystemClock.elapsedRealtime();
        VideoInfo info = new VideoInfo();
        String condition = MediaStore.MediaColumns.DATA + " like '%/" + Environment.DIRECTORY_DCIM + "/%' ";
        String[] projection = {BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.MIME_TYPE};
        String sortString = MediaStore.Video.VideoColumns._ID/*DATE_MODIFIED */ + " desc";
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, condition, null, sortString);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            info._ID = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
            info._DATE_ADDED = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
            info._SIZE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
            info._TITLE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE));
            info._MIME_TYPE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
            info._DATA = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            Log.d(TAG, "getLastVideoInfo:   _ID:" + info._ID + "  waste time:" + (SystemClock.elapsedRealtime() - tm));
        }
        return info;
    }

    public static int getMaxId(Context context) {
        long tm = SystemClock.elapsedRealtime();
        String[] projection = {"MAX(" + BaseColumns._ID + ")"};
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        Cursor cursorImg = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        int id = 0;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String strId = cursor.getString(0);
            if ((!TextUtils.isEmpty(strId)) && TextUtils.isDigitsOnly(strId)) {
                id = Integer.valueOf(strId);
            }
        }
        if (cursorImg != null && cursorImg.getCount() > 0) {
            cursorImg.moveToFirst();
            String strId = cursorImg.getString(0);
            if ((!TextUtils.isEmpty(strId)) && TextUtils.isDigitsOnly(strId)) {
                id = Math.max(Integer.valueOf(strId), id);
            }
        }
        Log.d(TAG, "getMaxId:   _ID:" + id + "  waste time:" + (SystemClock.elapsedRealtime() - tm));//耗时约十几ms
        return id;
    }

    /**
     * @param context
     * @param uri
     * @param type    有声|自拍|探索|全景
     */
    public static void insertPTImage(Context context, Uri uri, String type) {
        insertPTImage(context, uri, type,
                PTUIApplication.getInstance().locationService.getAddress());
    }

    public static void insertPTImage(Context context, Uri uri, String type, String addr) {
        if (uri == null) return;
        String uriStr = uri.toString();
        String ID = uriStr.substring(uriStr.lastIndexOf("/") + 1, uriStr.length());

        ImageInfo info = new ImageInfo();
        String condition = BaseColumns._ID + " = " + ID;
        String[] projection = {BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, condition, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            info._ID = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
            info._DATA = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            info._DATE_ADDED = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
            info._MIME_TYPE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
            info._BUCKET_DISPLAY_NAME = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
        }

        Uri imageUri = Uri.parse("content://com.android.gallery3d.provider.GalleryMediaProvider/pt_image");
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.ImageColumns._ID, Long.parseLong(ID));
        cv.put(MediaStore.Video.Media.DATA, info._DATA);
        cv.put(MediaStore.Images.Media.DATE_TAKEN, info._DATE_ADDED);
        cv.put(MediaStore.Images.Media.MIME_TYPE, info._MIME_TYPE);
        cv.put(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, info._BUCKET_DISPLAY_NAME);
        cv.put("take_photo_type", type);
        cv.put("data_1", TextUtils.isEmpty(addr) ? "" : addr);
        cv.put("collection", 0);
        cr.insert(imageUri, cv);
    }

    public static void insertPTVideo(Context context, Uri uri, String type) {
        insertPTVideo(context, uri, type,
                PTUIApplication.getInstance().locationService.getAddress());
    }

    /**
     * @param context
     * @param uri
     * @param type    有声|自拍|探索|全景
     */
    public static void insertPTVideo(Context context, Uri uri, String type, String addr) {
        if (uri == null) return;
        String uriStr = uri.toString();
        String ID = uriStr.substring(uriStr.lastIndexOf("/") + 1, uriStr.length());

        VideoInfo info = new VideoInfo();
        String condition = BaseColumns._ID + " = " + ID;
        String[] projection = {BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE, MediaStore.Video.Media.BUCKET_DISPLAY_NAME};
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, condition, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            info._ID = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
            info._DATA = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            info._DATE_ADDED = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
            info._MIME_TYPE = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
            info._BUCKET_DISPLAY_NAME = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
        }

        Uri imageUri = Uri.parse("content://com.android.gallery3d.provider.GalleryMediaProvider/pt_image");
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Video.VideoColumns._ID, Long.parseLong(ID));
        cv.put(MediaStore.Video.Media.DATA, info._DATA);
        cv.put(MediaStore.Video.Media.DATE_TAKEN, info._DATE_ADDED);
        cv.put(MediaStore.Video.Media.MIME_TYPE, info._MIME_TYPE);
        cv.put(MediaStore.Video.Media.BUCKET_DISPLAY_NAME, info._BUCKET_DISPLAY_NAME);
        cv.put("take_photo_type", type);
        cv.put("data_1", TextUtils.isEmpty(addr) ? "" : addr);
        cv.put("collection", 0);
        cr.insert(imageUri, cv);
    }

    /**
     * 存储图像并将信息添加入媒体数据库
     */
    public static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    public static Uri insertImage(Context context, String title, String filePath, String filename) {
        long dateTaken = System.currentTimeMillis();
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues(7);
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Video.Media.DATA, filePath);

        return cr.insert(IMAGE_URI, values);
    }

    /**
     * 视频信息添加入媒体数据库
     */
    public static final Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

    public static Uri insertVideo(Context context, String title, String filePath, String filename) {
        long dateTaken = System.currentTimeMillis();
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues(7);
        values.put(MediaStore.Video.Media.TITLE, title);
        values.put(MediaStore.Video.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, filePath);
        return cr.insert(VIDEO_URI, values);
    }

    public static Uri filePathToUri(Context context, String path) {
        if (path != null) {
            path = Uri.decode(path);
            ContentResolver cr = context.getContentResolver();
            StringBuffer buff = new StringBuffer();
            buff.append("(")
                    .append(MediaStore.Images.ImageColumns.DATA)
                    .append("=")
                    .append("'" + path + "'")
                    .append(")");
            Cursor cur = cr.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.ImageColumns._ID},
                    buff.toString(), null, null);
            int index = 0;
            for (cur.moveToFirst(); !cur.isAfterLast(); cur
                    .moveToNext()) {
                index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                // set _id value
                index = cur.getInt(index);
            }
            if (index == 0) {
                //do nothing
            } else {
                Uri uri_temp = Uri
                        .parse("content://media/external/images/media/" + index);
                if (uri_temp != null) {
                    return uri_temp;
                }
            }
        }
        return null;
    }

    public static Uri videoFilePathToUri(Context context, String path) {
        if (path != null) {
            path = Uri.decode(path);
            ContentResolver cr = context.getContentResolver();
            StringBuffer buff = new StringBuffer();
            buff.append("(")
                    .append(MediaStore.Video.VideoColumns.DATA/*Images.ImageColumns.DATA*/)
                    .append("=")
                    .append("'" + path + "'")
                    .append(")");
            Cursor cur = cr.query(
                    MediaStore.Video/*Images*/.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.VideoColumns/*Images.ImageColumns*/._ID},
                    buff.toString(), null, null);
            int index = 0;
            for (cur.moveToFirst(); !cur.isAfterLast(); cur
                    .moveToNext()) {
                index = cur.getColumnIndex(MediaStore.Video.VideoColumns/*Images.ImageColumns*/._ID);
                // set _id value
                index = cur.getInt(index);
            }
            if (index == 0) {
                //do nothing
            } else {
                Uri uri_temp = Uri
                        // .parse("content://media/external/images/media/" + index);
                        .parse("content://media/external/video/media/" + index);
                if (uri_temp != null) {
                    return uri_temp;
                }
            }
        }
        return null;
    }

    /**
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String uriToFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * @param ID
     * @return
     */
    public static Bitmap getThumbnailBitmap(Context context, String ID) {
        if (!TextUtils.isEmpty(ID)) {
            long tm = SystemClock.elapsedRealtime();
            ContentResolver cr = context.getContentResolver();
            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(cr, Long.valueOf(ID), MediaStore.Images.Thumbnails.MINI_KIND, null);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Log.d(TAG, "getThumbnailBitmap:  waste time:" + (SystemClock.elapsedRealtime() - tm) + "  w:" + w + "  h:" + h);
            return bitmap;
        }
        return null;
    }

    /**
     * @param ID
     * @return
     */
    public static Bitmap getVideoThumbnailBitmap(Context context, String ID) {
        if (!TextUtils.isEmpty(ID)) {
            long tm = SystemClock.elapsedRealtime();
            ContentResolver cr = context.getContentResolver();
            Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(cr, Long.valueOf(ID), MediaStore.Video.Thumbnails.MINI_KIND, null);
            if (bitmap == null) {
                return null;
            }
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Log.d(TAG, "getVideoThumbnailBitmap:  waste time:" + (SystemClock.elapsedRealtime() - tm) + "  w:" + w + "  h:" + h);
            return bitmap;
        }
        return null;
    }

    /**
     * @param filePath
     * @return
     */
    public static Bitmap getVideoThumbnail(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getVideoThumbnail: Exception:" + e.toString());
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * 耗时接口，后置摄像头视频耗时在800~1100ms不等,不能放在主线程中
     *
     * @param path 视频文件的路径
     * @return 一帧图片
     */
    public static Bitmap getVideoFramePicture(@NonNull String path) {
        if (path == null) {
            return null;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        FileChannelWrapper ch = null;
        Bitmap frame = null;
        try {
            ch = NIOUtils.readableFileChannel(path);
            FrameGrab frameGrab = new FrameGrab(ch);
            org.jcodec.api.FrameGrab.MediaInfo mi = frameGrab.getMediaInfo();
            frame = Bitmap.createBitmap(mi.getDim().getWidth(), mi.getDim().getHeight(), Bitmap.Config.ARGB_8888);
            frameGrab.getFrame(frame);
            if (frame != null) {
                Log.d(TAG, "getVideoFramePicture: width/height:" + frame.getWidth() + "/" + frame.getHeight());
            }
            return frame;
        } catch (Exception e) {
            Log.e(TAG, "JCodec" + e);
        } finally {
            NIOUtils.closeQuietly(ch);
            Log.d(TAG, "getVideoFramePicture: "
                    + "  frame bitmap:" + frame
                    + "  waste time:" + (SystemClock.elapsedRealtime() - elapsedRealtime));
        }
        return null;
    }

    private static final String TAG = "MediaUtil";

    public static Bitmap getVideoThumbnail(int mOrientation, int cameraID, String videoPath, int width, int height) {
        long tm = System.currentTimeMillis();
        Bitmap tempBmp = null;
        try {
            tempBmp = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MICRO_KIND);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        tempBmp = ThumbnailUtils.extractThumbnail(tempBmp, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        Log.d(TAG, "getVideoThumbnail: waste time:" + (System.currentTimeMillis() - tm));
        return tempBmp;
    }

    public static Bitmap getLastVideoThumbnail(int mOrientation, int cameraID, String videoDir, int width, int height) {
        try {
            File fret = getLastFile(videoDir, ".mp4");
            return getVideoThumbnail(mOrientation, cameraID, fret.getAbsolutePath(), width, height);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isPicName(String name) {
        return name != null && (name.endsWith(".jpg") || name.endsWith(".jpeg"));
    }

    public static boolean isVideoName(String name) {
        return name != null && name.endsWith(".mp4");
    }

    public static File getLastMediaFile(String fileDir) {
        File f = new File(fileDir);
        File[] fs = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return isPicName(filename) || isVideoName(filename);
            }
        });
        File fret = fs[0];
        for (File ff : fs) {
            fret = ff.lastModified() > fret.lastModified() ? ff : fret;
        }
        return fret;
    }

    public static File getLastFile(String fileDir, final String postFix) {
        File f = new File(fileDir);
        File[] fs = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(postFix);
            }
        });
        File fret = fs[0];
        for (File ff : fs) {
            fret = ff.lastModified() > fret.lastModified() ? ff : fret;
        }
        return fret;
    }

    public static File getLastMp4VideoFile() {
        return getLastFile(FileUtils.getVideoPath(), ".mp4");
    }

    /**
     * @param bMute 值为true时为关闭背景音乐。
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static boolean muteAudioFocus(Context context, boolean bMute) {
        if (context == null) {
            Log.d("ANDROID_LAB", "context is null.");
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            //if(!VersionUtils.isrFroyo()){
            // 2.1以下的版本不支持下面的API：requestAudioFocus和abandonAudioFocus
            Log.d("ANDROID_LAB", "Android 2.1 and below can not stop music");
            return false;
        }
        boolean bool = false;
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (bMute) {
                int[] types = {
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                        , AudioManager.AUDIOFOCUS_LOSS_TRANSIENT//听听
                        , AudioManager.AUDIOFOCUS_LOSS//听听
                        , AudioManager.AUDIOFOCUS_GAIN//听听
//                        ,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
//                        ,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
//                        ,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
//                        ,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                };
                int result = AudioManager.AUDIOFOCUS_REQUEST_FAILED;
                for (int t : types) {
                    result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, t);
                    if (bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        break;
                    }
                }
            } else {
                int result = am.abandonAudioFocus(null);
                bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            }
            Log.d("ANDROID_LAB", "pauseMusic bMute=" + bMute + " result=" + bool);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "muteAudioFocus: " + e.toString());
        }
        return bool;
    }

    public static void muteAudioFocus(final Context context, final boolean bMute, int timeDelay) {
        (new android.os.Handler(Looper.getMainLooper())).postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean ret = muteAudioFocus(context, bMute);
                Log.d(TAG, "run: muteAudioFocus:" + ret);
            }
        }, timeDelay);
    }
}
