package com.idormy.sms.forwarder.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

/**
 * 权限相关工具类
 */
public class CommonUtil {

    //获取当前版本名称
    public static String getVersionName(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionName;
    }

    //获取当前版本号
    public static Integer getVersionCode(Context context) throws Exception {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionCode;
    }

    // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
    @SuppressLint("InlinedApi")
    public static void CheckPermission(PackageManager pm, Context that) {
        //PackageManager pm = getPackageManager();
        boolean permission_internet = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.INTERNET", that.getPackageName()));
        boolean permission_receive_boot = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", that.getPackageName()));
        boolean permission_foreground_service = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.FOREGROUND_SERVICE", that.getPackageName()));
        boolean permission_read_external_storage = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_EXTERNAL_STORAGE", that.getPackageName()));
        boolean permission_write_external_storage = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", that.getPackageName()));
        boolean permission_receive_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.RECEIVE_SMS", that.getPackageName()));
        boolean permission_read_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_SMS", that.getPackageName()));
        boolean permission_send_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.SEND_SMS", that.getPackageName()));
        boolean permission_read_phone_state = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_PHONE_STATE", that.getPackageName()));
        boolean permission_read_phone_numbers = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_PHONE_NUMBERS", that.getPackageName()));
        boolean permission_read_call_log = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_CALL_LOG", that.getPackageName()));
        boolean permission_read_contacts = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_CONTACTS", that.getPackageName()));
        boolean permission_battery_stats = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.BATTERY_STATS", that.getPackageName()));
        boolean permission_bind_notification_listener_service = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", that.getPackageName()));

        if (!(permission_internet && permission_receive_boot && permission_foreground_service &&
                permission_read_external_storage && permission_write_external_storage &&
                permission_receive_sms && permission_read_sms && permission_send_sms &&
                permission_read_call_log && permission_read_contacts &&
                permission_read_phone_state && permission_read_phone_numbers && permission_battery_stats && permission_bind_notification_listener_service)) {
            ActivityCompat.requestPermissions((Activity) that, new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.BATTERY_STATS,
                    Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            }, 0x01);
        }
    }
}
