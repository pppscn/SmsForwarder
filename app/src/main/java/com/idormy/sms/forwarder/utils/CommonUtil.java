package com.idormy.sms.forwarder.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.service.NotifyService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 常用工具类
 */
public class CommonUtil {
    public static final int NOTIFICATION_REQUEST_CODE = 9527;
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    //是否启用通知监听服务
    public static boolean isNotificationListenerServiceEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        return packageNames.contains(context.getPackageName());
    }

    //开关通知监听服务
    public static void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context.getApplicationContext(), NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(context.getApplicationContext(), NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    //跳转通知使用权设置界面
    public static void openNotificationAccess(Activity activity) {
        Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
        activity.startActivityForResult(intent, NOTIFICATION_REQUEST_CODE);
    }

    //获取当前版本名称
    public static String getVersionName(Context context) throws Exception {
        // 获取PackageManager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionName;
    }

    //获取当前版本号
    public static Integer getVersionCode(Context context) throws Exception {
        // 获取PackageManager的实例
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
        boolean permission_query_all_packages = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.QUERY_ALL_PACKAGES", that.getPackageName()));

        if (!(permission_internet && permission_receive_boot && permission_foreground_service &&
                permission_read_external_storage && permission_write_external_storage &&
                permission_receive_sms && permission_read_sms && permission_send_sms &&
                permission_read_call_log && permission_read_contacts &&
                permission_read_phone_state && permission_read_phone_numbers && permission_battery_stats &&
                permission_bind_notification_listener_service && permission_query_all_packages)) {
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
                    Manifest.permission.QUERY_ALL_PACKAGES,
            }, 0x01);
        }
    }

    //计算MD5
    public static String MD5(String input) {
        if (input == null || input.length() == 0) return null;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            byte[] byteArray = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : byteArray) {
                // 一个byte格式化成两位的16进制，不足两位高位补零
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    //屏幕像素转换
    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    //是否合法的url
    public static boolean checkUrl(String urls, boolean emptyResult) {
        if (TextUtils.isEmpty(urls)) return emptyResult;

        String regex = "(ht|f)tp(s?)\\:\\/\\/[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(\\/?)([a-zA-Z0-9\\-\\.\\?\\,\\'\\/\\\\&%\\+\\$#_=]*)?";
        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(urls.trim());
        return mat.matches();
    }

    //焦点位置插入文本
    public static void insertOrReplaceText2Cursor(EditText editText, String str) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

    //计算浮动按钮位置
    public static void calcMarginBottom(Context context, FloatingActionButton btnFloat, ListView viewList, ScrollView scrollView) {

        int marginBottom = MyApplication.showHelpTip ? 85 : 65;
        if (btnFloat != null) {
            RelativeLayout.LayoutParams btnLayoutParams = (RelativeLayout.LayoutParams) btnFloat.getLayoutParams();
            btnLayoutParams.bottomMargin = dp2px(context, marginBottom + 10);
            btnFloat.setLayoutParams(btnLayoutParams);
        }

        if (viewList != null) {
            RelativeLayout.LayoutParams listLayoutParams = (RelativeLayout.LayoutParams) viewList.getLayoutParams();
            listLayoutParams.bottomMargin = dp2px(context, marginBottom);
            viewList.setLayoutParams(listLayoutParams);
        }

        if (scrollView != null) {
            RelativeLayout.LayoutParams listLayoutParams = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
            listLayoutParams.bottomMargin = dp2px(context, marginBottom);
            scrollView.setLayoutParams(listLayoutParams);
        }
    }
}
