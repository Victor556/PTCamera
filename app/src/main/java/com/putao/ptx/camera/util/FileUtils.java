package com.putao.ptx.camera.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.putao.ptx.camera.AppCommon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 文件操作工具
 */
public final class FileUtils {
    /**
     * 读取文件内容
     *
     * @param file    文件
     * @param charset 文件编码
     * @return 文件内容
     */
    public static String readFile(File file, String charset) {
        String fileContent = "";
        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), charset);
            BufferedReader reader = new BufferedReader(read);
            String line = "";
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if (i == 0)
                    fileContent = line;
                else
                    fileContent = fileContent + "\n" + line;
                i++;
            }
            read.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileContent;
    }

    /**
     * 读取文件内容
     *
     * @param file 文件
     * @return 文件内容
     */
    public static String readFile(File file) {
        return readFile(file, "UTF-8");
    }

    /**
     * 获取文件的SHA1值
     *
     * @param file 目标文件
     * @return 文件的SHA1值
     */
    public static String getSHA1ByFile(File file) {
        if (!file.exists()) return "文件不存在";
        long time = System.currentTimeMillis();
        InputStream in = null;
        String value = null;
        try {
            in = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            int numRead = 0;
            while (numRead != -1) {
                numRead = in.read(buffer);
                if (numRead > 0) digest.update(buffer, 0, numRead);
            }
            byte[] sha1Bytes = digest.digest();
            String t = new String(buffer);
            value = convertHashToString(sha1Bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return value;
    }

    /**
     * @param hashBytes
     * @return
     */
    private static String convertHashToString(byte[] hashBytes) {
        String returnVal = "";
        for (int i = 0; i < hashBytes.length; i++) {
            returnVal += Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }

    /**
     * 获取上传文件的文件名
     *
     * @param filePath
     * @return
     */
    public static String getFileName(String filePath) {
        String filename = new File(filePath).getName();
        if (filename.length() > 80) {
            filename = filename.substring(filename.length() - 80, filename.length());
        }
        return filename;
    }

    /**
     * 创建文件
     *
     * @param file
     */
    public static void createFile(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获得下载文件
     *
     * @param url
     * @return
     */
    public static String getDownloadFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }


    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否刪除成功
     */
    public static boolean delete(String filePath) {
        File file = new File(filePath);
        return delete(file);
    }

    /**
     * 删除文件
     *
     * @param file 文件
     * @return 是否刪除成功
     */
    public static boolean delete(File file) {
        if (file == null || !file.exists()) return false;
        if (file.isFile()) {
            final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
            file.renameTo(to);
            to.delete();
        } else {
            File[] files = file.listFiles();
            if (files != null && files.length > 0)
                for (File innerFile : files) {
                    delete(innerFile);
                }
            final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
            file.renameTo(to);
            return to.delete();
        }
        return false;
    }

    /**
     * 获得文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String getFileContent(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
                String result = null;
                String s = null;
                while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                    result = result + "\n" + s;
                }
                br.close();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return "文件不存在";
        }
        return "";
    }

    /**
     * 保存文本到文件
     *
     * @param fileName 文件名字
     * @param content  内容
     * @param append   是否累加
     * @return 是否成功
     */
    public static boolean saveTextValue(String fileName, String content, boolean append) {
        try {
            File textFile = new File(fileName);
            if (!append && textFile.exists()) textFile.delete();
            FileOutputStream os = new FileOutputStream(textFile);
            os.write(content.getBytes("UTF-8"));
            os.close();
        } catch (Exception ee) {
            ee.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除目录下所有文件
     *
     * @param Path 路径
     */
    public static void deleteAllFile(String Path) {
        // 删除目录下所有文件
        File path = new File(Path);
        File files[] = path.listFiles();
        if (files != null)
            for (File tfi : files) {
                if (tfi.isDirectory())
                    System.out.println(tfi.getName());
                else
                    tfi.delete();
            }
    }

    /**
     * 保存文件
     *
     * @param in       文件输入流
     * @param filePath 文件保存路径
     */
    public static void saveFile(InputStream in, String filePath) {
        File file = new File(filePath);
        byte[] buffer = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try {
            FileUtils.createFile(file);
            fos = new FileOutputStream(file);
            while ((len = in.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取data路径
     */
    public static String getDataPath(Context context) {
        return context.getDir("data", Context.MODE_PRIVATE).getAbsolutePath();
    }

    /**
     * 获取sd卡路径
     */
    public static String getSdcardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * 获得App在SD卡的文件夹路径
     *
     * @return
     */
    public static String getSDPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();

    }

    /**
     * 获得other路径
     *
     * @return
     */
    public static String getOtherDirPath() {
        File file = new File(getSDPath() + File.separator + AppCommon.FILE_OTHER_NAME);
        if (file.exists())
            return file.getAbsolutePath() + File.separator;
        else {
            file.mkdir();
            return file.getAbsolutePath() + File.separator;
        }
    }

    /**
     * 获得 ar sticker路径
     *
     * @return
     */
    public static String getARStickersPath(Context context) {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + AppCommon.FILE_PARENT_NAME + File.separator + AppCommon.FILE_AR_PARENT_NAME);
        if (!dirFile.exists()) dirFile.mkdirs();
        return dirFile.getAbsolutePath() + File.separator;
    }

    /**
     * 获取sd卡 拍摄照片路径
     *
     * @return
     */
    public static String getPhotoPath() {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + Environment.DIRECTORY_DCIM + File.separator + AppCommon.FILE_PHOTO_PARENT_NAME);
        if (!dirFile.exists()) dirFile.mkdirs();
        return dirFile.getAbsolutePath() + File.separator;
    }


    /**
     * 获取sd卡 有声照片声音路径
     *
     * @return
     */
    public static String getVoicePath() {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + Environment.DIRECTORY_DCIM + File.separator + AppCommon.FILE_VIC_PARENT_NAME);
        if (!dirFile.exists()) dirFile.mkdirs();
        return dirFile.getAbsolutePath() + File.separator;
    }

    /**
     * 获取sd卡 缩略图路径
     *
     * @return
     */
    public static String getThumbnailPath() {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + Environment.DIRECTORY_DCIM + File.separator + AppCommon.FILE_PHOTO_THUMBNAIL);
        if (!dirFile.exists()) dirFile.mkdirs();
        return dirFile.getAbsolutePath() + File.separator;
    }

    /**
     * 获取sd卡 自拍照片路径
     *
     * @return
     */
    public static String getSelfPhotoPath() {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + Environment.DIRECTORY_DCIM + File.separator + AppCommon.FILE_SELPIC_PARENT_NAME);
        if (!dirFile.exists()) dirFile.mkdirs();
        return dirFile.getAbsolutePath() + File.separator;
    }

    /**
     * 获取sd卡 探索照片路径
     *
     * @return
     */
    public static String getExplorePhotoPath() {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + Environment.DIRECTORY_DCIM + File.separator + AppCommon.FILE_EXPPIC_PARENT_NAME);
        if (!dirFile.exists()) dirFile.mkdirs();
        return dirFile.getAbsolutePath() + File.separator;
    }

    /**
     * 获取sd卡 视频路径
     *
     * @return
     */
    public static String getVideoPath() {
        File dirFile = new File(FileUtils.getSdcardPath() + File.separator + Environment.DIRECTORY_DCIM + File.separator + AppCommon.FILE_VIDEO_PARENT_NAME);
        if (!dirFile.exists()) dirFile.mkdir();
        return dirFile.getAbsolutePath() + File.separator;
    }

    /**
     * 获取Asset资源管理器
     *
     * @param context
     * @return
     */
    public static AssetManager getAssets(Context context) {
        return context.getResources().getAssets();
    }


    /**
     * 从assets里面获取文件字符串
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String readAssetsFile(Context context, String fileName) {
        String result = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            result = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putDataFileInLocalDir(Context context, int id, File f) {
        try {
            InputStream is = context.getResources().openRawResource(id);
            FileOutputStream os = new FileOutputStream(f);
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static final String TAG = "FileUtils";

    public static boolean isSdCardMemoryEnough(long byteCnt) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        long tm = SystemClock.elapsedRealtime();
        File sdcard = Environment.getExternalStorageDirectory();
        long free = sdcard.getFreeSpace();
        Log.d(TAG, "isSdCardMemoryEnough: waste time:" + (SystemClock.elapsedRealtime() - tm));
        return free > byteCnt;
    }

    /**
     * 耗时3~7ms
     *
     * @param sizeMb
     * @return
     */
    public static boolean isAvaiableSpace(int sizeMb) {
        long availableSpare = getAvailableSpare();
        if (sizeMb > availableSpare / (1024 * 1024)) {
            return false;
        } else {
            return true;
        }
    }

    public static long getAvailableSpare() {
        long availableSpare = 0;
        String externalStorageState = Environment.getExternalStorageState();
        long tm = SystemClock.elapsedRealtime();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            String sdcard = Environment.getExternalStorageDirectory().getPath();
//     File file = new File(sdcard);
            StatFs statFs = new StatFs(sdcard);
            long blockSize = statFs.getBlockSize();
            long blocks = statFs.getAvailableBlocks();
            availableSpare = blocks * blockSize;
        } else {
            Log.d(TAG, "isAvaiableSpace: externalStorageState:" + externalStorageState);
        }

//     long availableSpare = (long) (statFs.getBlockSize()*((long)statFs.getAvailableBlocks()-4))/(1024*1024);//以比特计算 换算成MB
        Log.d(TAG, "getAvaiableSpace: waste time:" + (SystemClock.elapsedRealtime() - tm)//3~7ms
                + "  availableSpare:" + availableSpare / (1024 * 1024));
        return availableSpare;
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
        }
    }


    /**
     * 存储图片到本地
     *
     * @param bitmap
     * @param picName
     */
    public static void saveThumbnail(Bitmap bitmap, String picName) {
        if (bitmap == null || TextUtils.isEmpty(picName)) {
            return;
        }
        File file;
        FileOutputStream fos = null;
        try {
            String picFile = getThumbnailPath().concat(picName);
            file = new File(picFile);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, out);
            out.flush();
//            LogUtil.e("video picture cache path = " + file.getAbsolutePath());
            Log.d(TAG, "saveThumbnail: picName:" + picName);
        } catch (Exception e) {
            Log.e(TAG, "saveThumbnail:" + e);
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "saveThumbnail:" + e);
            }
        }
    }

    /**
     * 当相机第一次添加音视频文件时，尝试添加时间信息，提供给图库使用。<br/>
     * 第一行是一个长整形的字符串，为有用信息
     * 第二行是补充信息。
     *
     * @return
     */
    public static boolean tryAddFirstMediaTime() {
        String picFile = getThumbnailPath().concat(".time");
        File file = new File(picFile);
        if (file.exists() && !file.isDirectory()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(picFile));
                String line = br.readLine();
                long tm = Long.parseLong(line);
                Calendar cld = Calendar.getInstance();
                cld.setTimeInMillis(tm);
                if (cld.get(Calendar.YEAR) > 2014) {//能够获取到时间，且年份在2014之后
                    br.close();
                    return false;
                } else {
                    file.delete();
                }
            } catch (Exception e) {
                Log.e(TAG, "tryAddFirstMediaTime: " + e);
            }
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(picFile));
            bw.write(System.currentTimeMillis() + "\n\n");
            try {
                bw.write("first use time: "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss\n").format(new Date()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            bw.flush();
            bw.close();
        } catch (Exception e) {
            Log.e(TAG, "tryAddFirstMediaTime: " + e);
        }
        return true;
    }
}
