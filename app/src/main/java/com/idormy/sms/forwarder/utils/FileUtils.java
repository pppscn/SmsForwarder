package com.idormy.sms.forwarder.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/*
 * 文件读写工具类
 * external storage
   外部存储	Environment.getExternalStorageDirectory()	SD根目录:/mnt/sdcard/ (6.0后写入需要用户授权)
            context.getExternalFilesDir(dir)	路径为:/mnt/sdcard/Android/data/< package name >/files/…
            context.getExternalCacheDir()	路径为:/mnt/sdcard//Android/data/< package name >/cach/…
            *
  internal storage
  内部存储
          context.getFilesDir()	路径是:/data/data/< package name >/files/…
          context.getCacheDir()	路径是:/data/data/< package name >/cach/…
*/
@SuppressWarnings({"UnusedReturnValue", "ResultOfMethodCallIgnored"})
public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    /**
     * @return .外部储存sd卡 根路径
     */
    public static String getRootPath() {
        // /storage/emulated/0
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * @param context .
     * @return . 外部储存sd卡 :/mnt/sdcard/Android/data/< package name >/files/…
     */
    public static String getAppRootPth(Context context) {
        // /storage/emulated/0/Android/data/pack_name/files
        return context.getExternalFilesDir("").getAbsolutePath();
    }

    /**
     * @return .内部存储
     */
    public static String getInternalPath() {
        // /data
        return Environment.getDataDirectory().getAbsolutePath();
    }

    /**
     * @param context .
     * @return .内部储存:/data/data/< package name >/files/
     */
    public static String getInternalAppPath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    /**
     * @param path     路径
     * @param fileName 文件名称
     * @return .
     */
    public static boolean createFile(String path, String fileName) {
        File file = new File(path + File.separator + fileName);

        //先创建文件夹 保证文件创建成功
        createDirs(path);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            //
            return false;
        }
    }

    /**
     * @param folder 创建多级文件夹
     * @return .
     */
    public static boolean createDirs(String folder) {
        File file = new File(folder);

        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            Log.i(TAG, "createDirs: 不存在文件夹 开始创建" + mkdirs + "--" + folder);
            return true;
        } else {
            Log.i(TAG, "createDirs: 文件夹已存在");
        }
        return false;
    }

    /**
     * =======================================文件读写=============================================
     *
     * @param content   写入字符串
     * @param path      .    目录
     * @param fileName  .文件名
     * @param isRewrite 是否覆盖
     */
    //1.RandomAccessFile 读写
    public static boolean writeFileR(String content, String path, String fileName, boolean isRewrite) {
        //路径非斜杆结尾
        if (!path.endsWith("/")) path += File.separator;

        File file = new File(path + fileName);

        //文件目录不存在，先创建
        if (!file.exists()) createFile(path, fileName);

        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            if (isRewrite) {
                randomAccessFile.setLength(content.length());
                randomAccessFile.seek(0);
            } else {
                randomAccessFile.seek(randomAccessFile.length());
            }
            randomAccessFile.write(content.getBytes());
            randomAccessFile.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeFileR(String content, String path, String fileName) {
        return writeFileR(content, path, fileName, false);
    }

    /**
     * 读文件
     *
     * @param path     .
     * @param fileName .
     * @return .
     */
    public static String readFileR(String path, String fileName) {
        //路径非斜杆结尾
        if (!path.endsWith("/")) path += File.separator;

        File file = new File(path + fileName);
        if (!file.exists()) {
            Log.i(TAG, "readFileR: return null");
            return null;
        }

        StringBuilder buffer = new StringBuilder();
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(0);
            byte[] buf = new byte[(int) randomAccessFile.length()];
            if (randomAccessFile.read(buf) != -1) {
                buffer.append(new String(buf));
                Log.i(TAG, "readFileR: length" + randomAccessFile.length());
                //buffer.append(new String(buf, StandardCharsets.UTF_8));
            }
            randomAccessFile.close();
        } catch (IOException e) {
            Log.i(TAG, "readFileR: " + e.getMessage());
            e.printStackTrace();
        }

        return buffer.toString();
    }

    /**
     * 读文件
     *
     * @param path .文件路径
     * @param name .名称
     * @return .
     */
    public static String readFileI(String path, String name) {
        //路径非斜杆结尾
        if (!path.endsWith("/")) path += File.separator;

        //默认编码格式 StandardCharsets.UTF_8;
        File file = new File(path, name);
        if (!file.exists()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();

    }

    /**
     * 写入文件
     *
     * @param content  内容.
     * @param path     目录.
     * @param fileName 文件名   .
     */
    public static void writeFileO(String content, String path, String fileName) {
        writeFileO(content, path, fileName, false);
    }

    /**
     * @param content   .
     * @param path      .
     * @param fileName  .
     * @param isReWrite .是否追加
     */
    public static void writeFileO(String content, String path, String fileName, boolean isReWrite) {
        //路径非斜杆结尾
        if (!path.endsWith("/")) path += File.separator;

        File file = new File(path + fileName);

        //文件目录不存在，先创建
        if (!file.exists()) createFile(path, fileName);

        try {
            FileOutputStream ops = new FileOutputStream(file, isReWrite);
            OutputStreamWriter opsw = new OutputStreamWriter(ops, StandardCharsets.UTF_8);
            // byte[] bytes = content.getBytes();
            opsw.write(content);
            opsw.close();
            ops.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}