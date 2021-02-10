package com.idormy.sms.forwarder.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class aUtil {
    private static String TAG = "aUtil";

    private static Context context = null;

    /**
     * 判断是否为MIUI系统，参考http://blog.csdn.net/xx326664162/article/details/52438706
     *
     * @return
     */
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
        String version = packInfo.versionName;
        return version;
    }

    public static Integer getVersionCode(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        Integer versionCode = packInfo.versionCode;
        return versionCode;
    }
}
