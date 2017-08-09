package com.putao.ptx.camera.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.ExifInterface;
import android.os.Build;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.Nullable;
import android.util.Log;

import com.putao.ptx.camera.PTUIApplication;
import com.sunnybear.library.util.DensityUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class BitmapUtil {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int heightRatio = (int) (height / (float) reqHeight);
            int widthRatio = (int) (width / (float) reqWidth);
            inSampleSize = (heightRatio < widthRatio) ? widthRatio : heightRatio;
        }
        return inSampleSize;
    }

    private static int calculateInSampleSizebak(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap getBitmapFromPath(String path) {
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setImageFileOrientarionMatrix(String filePath, Matrix matrix) {
        int ori = BitmapUtil.getImageFileOrientation(filePath);
        if (ori != ExifInterface.ORIENTATION_NORMAL) {
            switch (ori) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.preScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.preScale(1, -1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.preScale(1, -1);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(270);
                    matrix.preScale(1, -1);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    break;
            }
        }
    }

    public Bitmap getBitmapFromPathWithSize(String path, int targetwidth, int targetheight) {
        try {
            Bitmap result = BitmapFactory.decodeFile(path, null);
            {
                Matrix matrix = new Matrix();
                float ratio_current = result.getWidth() / (float) result.getHeight();
                float ratio_target = targetwidth / (float) targetheight;
                float scale = 1.0f;
                if (ratio_current < ratio_target) {
                    //以高为准压缩
                    scale = (float) targetheight / result.getHeight();
                    matrix.postScale(scale, scale);
                } else {//以宽为准压缩
                    scale = (float) targetwidth / result.getWidth();
                    matrix.postScale(scale, scale);
                }
                setImageFileOrientarionMatrix(path, matrix);
                result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, false);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String TAG = "BitmapUtil";

    /**
     * @param result
     * @param targetwidth
     * @param targetheight
     * @return <br>前置摄像头比后置快，后置大约花费2~4ms
     */
    public static Bitmap getCenterCropBitmap(Bitmap result, int targetwidth, int targetheight) {
        if (result == null || result.isRecycled()) {
            return null;
        }
        long tm = SystemClock.elapsedRealtime();
        try {
            Matrix matrix = new Matrix();
            float ratio_current = result.getWidth() / (float) result.getHeight();
            float ratio_target = targetwidth / (float) targetheight;
            float scale = 1.0f;
            if (ratio_current >= ratio_target) {
                //以高为准压缩
                scale = (float) targetheight / result.getHeight();
                matrix.postScale(scale, scale);
                result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, false);
                result = Bitmap.createBitmap(result, (result.getWidth() - targetwidth) / 2, 0, targetwidth, targetheight);
            } else {
                //以宽为准压缩
                scale = (float) targetwidth / result.getWidth();
                matrix.postScale(scale, scale);
                result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, false);
                result = Bitmap.createBitmap(result, 0, (result.getHeight() - targetheight) / 2, targetwidth, targetheight);
            }

            Log.d(TAG, "getCenterCropBitmap: waste time:" + (SystemClock.elapsedRealtime() - tm));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap getCenterCropBitmap(String path, int targetwidth, int targetheight) {
        try {
            Bitmap result = BitmapFactory.decodeFile(path, null);
            {
                Matrix matrix = new Matrix();
                float ratio_current = result.getWidth() / (float) result.getHeight();
                float ratio_target = targetwidth / (float) targetheight;
                float scale = 1.0f;
                if (ratio_current >= ratio_target) {
                    //以高为准压缩
                    scale = (float) targetheight / result.getHeight();
                    matrix.postScale(scale, scale);
                    result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, false);
                    result = Bitmap.createBitmap(result, (result.getWidth() - targetwidth) / 2, 0, targetwidth, targetheight);
                } else {
                    //以宽为准压缩
                    scale = (float) targetwidth / result.getWidth();
                    matrix.postScale(scale, scale);
                    result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, false);
                    result = Bitmap.createBitmap(result, 0, (result.getHeight() - targetheight) / 2, targetwidth, targetheight);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static Bitmap drawableToBitmap(Context context, Drawable drawable) {
        // 取 drawable 的长宽
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (DensityUtil.getScreenPixels(context).density < 3) {
            w = drawable.getIntrinsicWidth();
            h = drawable.getIntrinsicHeight();
        } else if (DensityUtil.getScreenPixels(context).density >= 3) {
            w = drawable.getIntrinsicWidth() + 100;
            h = drawable.getIntrinsicHeight() + 100;
        }
        // 取 drawable 的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap;
        bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable getLoadingDrawable(int w, int h) {
        return new BitmapDrawable(getLoadingBitmap(w, h));
    }

    public static Drawable getLoadingDrawable() {
        return new BitmapDrawable(getLoadingBitmap());
    }

    public static Bitmap getLoadingBitmap() {
        int bmpWith = DensityUtil.dp2px(PTUIApplication.getInstance(), 100);
        int bmpHeight = bmpWith;

        return getLoadingBitmap(bmpWith, bmpHeight);
    }

    public static Bitmap getLoadingBitmap(int bmpWith, int bmpHeight) {
        int[] colors = {0xff7a4e6d, 0xffb9cc7d, 0xffce9e8b, 0xff758c4d, 0xffeae989, 0xff9ab9bb, 0xff79bbff, 0xffdd9095, 0xff2b304a, 0xffc7bcb4, 0xffd7d7ce,
                0xffb0eccb, 0xffdabe8e, 0xffad9882, 0xff979eb4, 0xfff6e6db, 0xffe29b3a, 0xff546ea2};
        Bitmap bitmap = Bitmap.createBitmap(bmpWith, bmpHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        int randomIndex = new Random(System.currentTimeMillis()).nextInt(colors.length);
        int color = colors[randomIndex];
        canvas.drawColor(color);
        return bitmap;
    }

    /**
     * 以radius为半径,以color为颜色的实心圆
     *
     * @param color
     * @param radius
     * @return bitmap
     */
    public static Bitmap getCircleBitmap(int color, int radius) {
        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawColor(Color.TRANSPARENT);
        paint.setColor(color);
        canvas.drawCircle(radius, radius, radius, paint);
        return bitmap;
    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    /**
     * ARGB8888 CONVERT TO RGBA8888
     *
     * @param pix
     */
    public static int[] ARGBTORGBA(int[] pix) {
        int[] piexs = pix.clone();
        for (int i = 0; i < piexs.length; i++) {
            int pixel = piexs[i];
            piexs[i] = (pixel << 8) | ((pixel >> 24) & 0xFF);
        }
        return piexs;
    }

    /**
     * 获取图片的旋转角度
     *
     * @param filePath
     * @return
     */
    public static int getImageFileOrientation(String filePath) {
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(filePath);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            return orientation;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ExifInterface.ORIENTATION_NORMAL;
    }

    /**
     * orientationDegree == 180度时，会出错
     * <br>800万像素图片大约耗时200~300毫秒
     */
    public static Bitmap orientBitmap90N(Bitmap bm, final int orientationDegree) {
        if (bm == null) return null;
        long tm = SystemClock.elapsedRealtime();
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX = 0, targetY = 0;
        if (orientationDegree % 360 == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else if (orientationDegree % 360 == 180) {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        } else if (orientationDegree % 360 == 270) {
            targetX = 0;
            targetY = bm.getWidth();
        } else {
            return bm;
        }
        final float[] values = new float[9];
        m.getValues(values);
        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];
        m.postTranslate(targetX - x1, targetY - y1);
        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);

        Log.d(TAG, "orientBitmap90N: waste time:" + (SystemClock.elapsedRealtime() - tm));
        return bm1;
    }

    /**
     * Takes an orientation and a bitmap, and returns the bitmap transformed
     * to that orientation.
     */
    @Deprecated
    public static Bitmap orientBitmap(Bitmap bitmap, int ori) {
        Matrix matrix = new Matrix();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (ori == ExifInterface.ORIENTATION_ROTATE_90 ||
                ori == ExifInterface.ORIENTATION_ROTATE_270 ||
                ori == ExifInterface.ORIENTATION_TRANSPOSE ||
                ori == ExifInterface.ORIENTATION_TRANSVERSE) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        switch (ori) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90, w / 2f, h / 2f);
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270, w / 2f, h / 2f);
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }


    public static Bitmap getBitmapFromPath(String url, BitmapFactory.Options option) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(url, option);
        } catch (Exception e) {

        }
        return bitmap;
    }


    /**
     * 合并两张bitmap为一张
     *
     * @param background
     * @param foreground
     * @return Bitmap
     */
    public static Bitmap combineBitmap(Bitmap background, Bitmap foreground, int posX, int posY) {
        if (background == null) {
            return null;
        }
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        int fgWidth = foreground.getWidth();
        int fgHeight = foreground.getHeight();
        Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(foreground, posX, posY, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        return newmap;
    }

    /**
     * 保存方法
     */
    public static void saveBitmap(Bitmap bitmap, String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Bitmap getScreenSizeBitmap(Context context, Bitmap bitmap) {
        // 把图片缩放成屏幕的大小1:1，方便视频合成的时候调用
        Bitmap scaleImageBmp;
        Bitmap bgImageBitmap = scaleImageBmp = BitmapUtil.resizeBitmap(bitmap, 0.5f);
        // 背景脸图片在屏幕上的缩放
        int screenW = DensityUtil.getDeviceWidth(context);
        int screenH = DensityUtil.getDeviceHeight(context);
        float imageScale = (float) screenW / (float) scaleImageBmp.getWidth();
        int bgImageOffsetX = (int) (screenW - imageScale * scaleImageBmp.getWidth()) / 2;
        int bgImageOffsetY = (int) (screenH - imageScale * scaleImageBmp.getHeight()) / 2;
        scaleImageBmp = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888);
        Bitmap resizedBgImage = BitmapUtil.resizeBitmap(bgImageBitmap, imageScale);
        scaleImageBmp = BitmapUtil.combineBitmap(scaleImageBmp, resizedBgImage, bgImageOffsetX, bgImageOffsetY);
        bgImageBitmap.recycle();
        resizedBgImage.recycle();
        return scaleImageBmp;
    }

    // Remix Blur
    public static Bitmap blur(Context ctx, Bitmap bkg) {
//        RenderScript rs = RenderScript.create(ctx);
//        Allocation overlayAlloc = Allocation.createFromBitmap(rs, bkg);
//        ScriptIntrinsicBlur blur =
//                ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());
//        blur.setInput(overlayAlloc);
//        blur.setRadius(10);
//        blur.forEach(overlayAlloc);
//        overlayAlloc.copyTo(bkg);
//        view.setBackground(new BitmapDrawable(ctx.getResources(), bkg));
//        rs.destroy();
        if (ctx == null || bkg == null) {
            return null;
        }
        Bitmap bitmap = bkg.copy(bkg.getConfig(), true);
        final RenderScript rs = RenderScript.create(ctx);
        final Allocation input = Allocation.createFromBitmap(rs, bkg, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(3.0f /* e.g. 3.f */);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);
        rs.destroy();
        return bitmap;
    }

    public static Bitmap rotation90N(Bitmap bitmap, int rotation, boolean bHoriConvet) {
        if (bitmap == null || rotation == 0 && !bHoriConvet) {
            return bitmap;
        }
        long tm = SystemClock.elapsedRealtime();
        Matrix matrix = new Matrix();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (rotation == 90 ||
                rotation == 270) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        matrix.setRotate(rotation, w / 2f, h / 2f);
        if (bHoriConvet) {
            matrix.preScale(-1, 1);
        }
        Bitmap ret = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        Log.d(TAG, "rotation90N:  rotation:" + rotation + "  bHoriConvet:" + bHoriConvet +
                "  waste time:" + (SystemClock.elapsedRealtime() - tm));
        return ret;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Bitmap getBitmap(VectorDrawable vectorDrawable, int width, int height) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(/*vectorDrawable.getIntrinsicWidth()*/width,
                    /*vectorDrawable.getIntrinsicHeight()*/height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, /*canvas.getWidth()*/width, /*canvas.getHeight()*/height);
            vectorDrawable.draw(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Nullable
    public static Bitmap getRotationModifedBitmap(Bitmap tmp, int rotation, boolean bCameraIdBack) {
        if (tmp == null) {
            return null;
        }
        if (!bCameraIdBack) {
            if (rotation == 270) {
                rotation = 90;
            } else if (rotation == 90) {
                rotation = 270;
            }
        }

        Bitmap btmPost;
        if (rotation == 180) {
            btmPost = BitmapUtil.orientBitmap90N(tmp, 90);
            btmPost = BitmapUtil.orientBitmap90N(btmPost, 90);
        } else {
            btmPost = BitmapUtil.orientBitmap90N(tmp, rotation);
        }
        if (btmPost != tmp && tmp != null && !tmp.isRecycled()) {
            tmp.recycle();
            tmp = null;
        }
        return btmPost;
    }

    /**
     * 从SD卡目录中提取同名缩略图
     *
     * @param videoName
     * @return
     */
    public static Bitmap getVideoThumbnailFromDir(String videoName) {
        if (videoName == null) {
            return null;
        }
        return getBitmapFromPath(FileUtils.getThumbnailPath()
                .concat(videoName.replace(".mp4", ".jpg")));
    }
}
