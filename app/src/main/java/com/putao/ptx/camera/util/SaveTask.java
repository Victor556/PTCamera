package com.putao.ptx.camera.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.putao.ptx.camera.eventbus.EventUpdateAlbum;
import com.putao.ptx.camera.helper.CameraEngine;
import com.putao.ptx.camera.view.CameraSurfaceView;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class SaveTask extends AsyncTask<byte[]/*Bitmap*/, Integer, String> {
    private final String TAG = SaveTask.class.getSimpleName();
    private final long mTmCreate;
    public long mTmStartWait;
    private CameraSurfaceView.onPictureTakenListener listener;
    private Context mContext;
    private File mFile;
    private Bitmap bmp = null;
    private int mRotation;
    private static int sTaskCnt = 0;
    public static boolean sbCapturePic = false;
    private final ExifInterface mExif;


    public SaveTask(Context context, int rotation, File file, CameraSurfaceView.onPictureTakenListener listener, ExifInterface exif) {
        this.mContext = context;
        this.mRotation = rotation;
        this.mFile = file;
        this.listener = listener;
        mExif = exif;
        mTmCreate = SystemClock.elapsedRealtime();
        sTaskCnt++;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(final String result) {
        sTaskCnt--;
        if (result != null) {
            MediaScannerConnection.scanFile(mContext, new String[]{result}, new String[]{"image/jpeg"}, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    if (listener != null) {
                        listener.onSaved(bmp, uri);
                    }
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                        Log.d(TAG, "finish take picture onScanCompleted: bmp.isRecycled():" + bmp.isRecycled());
                        bmp = null;
                    }
                    //System.gc();//强制内存回收
                    Log.d(TAG, "onPostExecute  scanFile : taking_picture all wasted time:" + (SystemClock.elapsedRealtime() - mTmCreate));
                }
            });
        } else {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
                Log.d(TAG, "finish take picture onScanCompleted: bmp.isRecycled():" + bmp.isRecycled());
                bmp = null;
            }
        }
        //System.gc();//强制内存回收
        Log.d(TAG, "onPostExecute: taking_picture wasted time:" + (SystemClock.elapsedRealtime() - mTmCreate));

        try {
            ExifInterface exif = new ExifInterface();
            exif.readExif(mFile.getPath());
            exif.getAllTags();
            double[] pos = exif.getLatLongAsDoubles();
            ExifTag tag = exif.getTag(ExifInterface.TAG_IMAGE_DESCRIPTION);
            String str = "onPostExecute:  ExifInterface:";
            if (pos != null) {
                str += (pos[0] + "/" + pos[1]);
            }
            if (tag != null) {
                str += ("  desc:" + new String(tag.getValueAsBytes(), Charset.forName("UTF-8")));
            }
            Log.d(TAG, "onPostExecute: ExifInterface tag:" + str);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Bitmap onPictureTaken(byte[] data) {
        long tm = SystemClock.elapsedRealtime();
        Bitmap bitmap = null;
        //CameraEngine.getCamera().stopPreview();
        //耗时0.12秒,可能会出现内存不足 data.length=19M
        int cnt = 0;
        while (cnt < 10) {
            cnt++;
            try {
                //耗时200~300ms
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.d(TAG, "taking_picture onPictureTaken: data.length=" + data.length
                        + "  decodeByteArray after " + (SystemClock.elapsedRealtime() - tm));
                break;
            } catch (Exception e) {
                System.gc();
                //e.printStackTrace();
                Log.e(TAG, "onPictureTaken: data.length=" + data.length + "拍照出错！" + e.toString());
            }
        }
        if (bitmap == null) {
            return null;
        }

        Bitmap tmp = BitmapUtil.getCenterCropBitmap(bitmap, 100, 100);
        int rotation = mRotation;//CameraEngine.getRotationResult();
        Bitmap btmPost = BitmapUtil.getRotationModifedBitmap(tmp, rotation, CameraEngine.isCameraIdBack());
        EventBus.getDefault().post(new EventUpdateAlbum(btmPost));
        Log.d(TAG, "onPictureTaken waste time  after mSaveTask.execute taking_picture " +
                "  in onPictureTaken wast time " + (SystemClock.elapsedRealtime() - tm));
        return bitmap;
    }

    @Override
    protected String doInBackground(final byte[]... params) {
        Log.d(TAG, "doInBackground: taking_picture asyntask wait time " + (SystemClock.elapsedRealtime() - mTmStartWait));
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);


        long tm = System.currentTimeMillis();
        if (mFile == null)
            return null;

        int rotation = mRotation;//CameraEngine.getRotationResult();
        if (!CameraEngine.isCameraIdBack()) {
            if (rotation == 270) {
                rotation = 90;
            } else if (rotation == 90) {
                rotation = 270;
            }
        }

        Bitmap bitmapMod;
        Bitmap bitmap = null;
        try {
            bitmap = onPictureTaken(params[0]);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "doInBackground: " + e.toString());
        }
        if (bitmap == null) {
            return null;
        }

        if (rotation == 180) {
            Bitmap tmp = BitmapUtil.orientBitmap90N(bitmap, 90);
            bitmapMod = BitmapUtil.orientBitmap90N(tmp, 90);
            tmp.recycle();
            tmp = null;
        } else {
            bitmapMod = BitmapUtil.orientBitmap90N(bitmap, rotation);
        }

        if (bitmapMod != bitmap && bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }


        long bmpTm = System.currentTimeMillis();
        bmp = bitmapMod/*params[0]*/;
        String s = saveBitmap(bmp);//耗时约0.7~1.2秒
        long afterSave = System.currentTimeMillis();
        Log.d(TAG, "SaveTask.doInBackground: wasteTime=" + (afterSave - tm)
                + " saveBitmap waste time:" + (afterSave - bmpTm));
        return s;
    }

    /**
     * @param bitmap
     * @return
     */
    private String saveBitmap(Bitmap bitmap) {
        if (mFile.exists()) {
            mFile.delete();
        }
        try {
            if (mExif == null) {
                FileOutputStream out = new FileOutputStream(mFile);
                //后置摄像头耗时约1.7s 100
                bitmap.compress(Bitmap.CompressFormat.JPEG, sbCapturePic ? 90 : 100, out);
                out.flush();
                out.close();
            } else {
                mExif.writeExif(bitmap, mFile.getPath());
            }

            return mFile.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "saveBitmap: " + e.toString());
        }
        return null;
    }

    public static int getTaskCnt() {
        return sTaskCnt;
    }

}
