package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.FileChannel;

@SuppressWarnings({"ResultOfMethodCallIgnored", "CommentedOutCode"})
public class BackupDbTask {
    private static final String TAG = "BackupDbTask";
    public static final String COMMAND_BACKUP = "backupDatabase";
    public static final String COMMAND_RESTORE = "restoreDatabase";
    public final static String BACKUP_FOLDER = "SmsForwarder";
    public final static String BACKUP_FILE = "SmsForwarder.zip";
    public String backup_version;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    public BackupDbTask(Context context) {
        mContext = context;
    }

    private static File getExternalStoragePublicDir() {
        // /sdcard/SmsForwarder/
        String path = Environment.getExternalStorageDirectory() + File.separator + BACKUP_FOLDER + File.separator;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public String doInBackground(String command) {
        File dbFile = mContext.getDatabasePath(DbHelper.DATABASE_NAME);// 默认路径是 /data/data/(包名)/databases/*
        File dbFile_shm = mContext.getDatabasePath(DbHelper.DATABASE_NAME + "-journal");// 默认路径是 /data/data/(包名)/databases/*
        //File dbFile_wal = mContext.getDatabasePath("event_database-wal");// 默认路径是 /data/data/(包名)/databases/*

        String bakFolder = mContext.getCacheDir().getPath() + File.separator + BACKUP_FOLDER;
        String zipFile = mContext.getCacheDir().getPath() + File.separator + BACKUP_FILE;
        Log.d(TAG, "备份目录名：" + bakFolder + "，备份文件名：" + zipFile);

        File exportDir = new File(mContext.getCacheDir().getPath(), BACKUP_FOLDER);//直接丢在 cache 目录，可以在在关于目录下清除缓存
        if (!exportDir.exists()) exportDir.mkdirs();

        File backup = new File(bakFolder, dbFile.getName());//备份文件与原数据库文件名一致
        File backup_shm = new File(bakFolder, dbFile_shm.getName());//备份文件与原数据库文件名一致
        //File backup_wal = new File(bakFolder, dbFile_wal.getName());//备份文件与原数据库文件名一致
        if (command.equals(COMMAND_BACKUP)) {
            try {
                //备份文件
                backup.createNewFile();
                backup_shm.createNewFile();
                //backup_wal.createNewFile();
                fileCopy(dbFile, backup);//数据库文件拷贝至备份文件
                fileCopy(dbFile_shm, backup_shm);//数据库文件拷贝至备份文件
                //fileCopy(dbFile_wal, backup_wal);//数据库文件拷贝至备份文件
                //backup.setLastModified(MyTimeUtils.getTimeLong());

                backup_version = TimeUtil.getTimeString("yyyy.MM.dd_HH:mm:ss");
                Log.d(TAG, "backup ok! 备份目录：" + backup.getName() + "\t" + backup_version);

                //打包文件
                ZipUtils.ZipFolder(bakFolder, zipFile);
                Log.d(TAG, "备份Zip包：" + zipFile);

                return backup_version;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "backup fail! 备份文件名：" + backup.getName());
                return null;
            }
        } else if (command.equals(COMMAND_RESTORE)) {
            try {
                //解压文件
                ZipUtils.UnZipFolder(zipFile, bakFolder);
                Log.d(TAG, "解压Zip包：" + zipFile);

                //还原文件
                fileCopy(backup, dbFile);//备份文件拷贝至数据库文件
                fileCopy(backup_shm, dbFile_shm);//备份文件拷贝至数据库文件
                //fileCopy(backup_wal, dbFile_wal);//备份文件拷贝至数据库文件
                backup_version = TimeUtil.getTimeString(backup.lastModified(), "yyyy.MM.dd_HH:mm:ss");
                Log.d(TAG, "restore success! 数据库文件名：" + dbFile.getName() + "\t" + backup_version);

                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException(e.getMessage());
                }

                LogUtil.delLog(null, null);

                return backup_version;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "restore fail! 数据库文件名：" + dbFile.getName());
                return null;
            }
        } else {
            return null;
        }
    }

    private void fileCopy(File dbFile, File backup) {
        try (FileChannel inChannel = new FileInputStream(dbFile).getChannel(); FileChannel outChannel = new FileOutputStream(backup).getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}