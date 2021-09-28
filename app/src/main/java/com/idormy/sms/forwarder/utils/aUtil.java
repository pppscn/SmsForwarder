package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class aUtil {

    /**
     * 判断是否为MIUI系统，参考http://blog.csdn.net/xx326664162/article/details/52438706
     *
     * @return 返回结果
     */
    @SuppressWarnings("unused")
    public static boolean isMIUI() {
        try {
            String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
            String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
            String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));

            return prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                    || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                    || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null;
        } catch (final IOException e) {
            return false;
        }
    }

    public static String getVersionName(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionName;
    }

    public static Integer getVersionCode(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionCode;
    }

    //友好时间显示
    public static String friendlyTime(String utcTime) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat utcFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));//时区定义并进行时间获取
        Date utcDate;
        try {
            utcDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return utcTime;
        }

        //获取utcDate距离当前的秒数
        assert utcDate != null;
        int ct = (int) ((System.currentTimeMillis() - utcDate.getTime()) / 1000);

        if (ct == 0) {
            return "刚刚";
        }

        if (ct > 0 && ct < 60) {
            return ct + "秒前";
        }

        if (ct >= 60 && ct < 3600) {
            return Math.max(ct / 60, 1) + "分钟前";
        }
        if (ct >= 3600 && ct < 86400) {
            return ct / 3600 + "小时前";
        }
        if (ct >= 86400 && ct < 2592000) { //86400 * 30
            int day = ct / 86400;
            return day + "天前";
        }
        if (ct >= 2592000 && ct < 31104000) { //86400 * 30
            return ct / 2592000 + "月前";
        }

        return ct / 31104000 + "年前";
    }

    /**
     * 函数功能描述:UTC时间转本地时间格式
     *
     * @param utcTime UTC时间
     * @return 本地时间格式的时间
     */
    public static String utc2Local(String utcTime) {
        String utcTimePatten = "yyyy-MM-dd HH:mm:ss";
        String localTimePatten = "yyyy-MM-dd HH:mm:ss";
        @SuppressLint("SimpleDateFormat") SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));//时区定义并进行时间获取

        Date utcDate;
        try {
            utcDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return utcTime;
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        assert utcDate != null;
        return localFormater.format(utcDate.getTime());
    }

}
