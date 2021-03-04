package com.idormy.sms.forwarder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingUtil {
    static Boolean hasInit = false;
    private static String TAG = "SettingUtil";
    private static SharedPreferences sp_setting = null;
    private static Context context = null;

    public static void init(Context context1) {
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
            Log.d(TAG, "init ");
            sp_setting = PreferenceManager.getDefaultSharedPreferences(context1);

        }
    }

    public static boolean option_withreboot() {
        return sp_setting.getBoolean("option_withreboot", false);
    }

    public static boolean using_dingding() {
        return sp_setting.getBoolean("option_dingding_on", false);
    }

    public static String get_using_dingding_token() {
        return sp_setting.getString("option_dingding_token", "");
    }

    public static String get_using_dingding_secret() {
        return sp_setting.getString("option_dingding_secret", "");
    }

    public static boolean using_email() {
        return sp_setting.getBoolean("option_email_on", false);
    }

    public static void set_send_util_email(String host, String port, String from_add, String psw, String to_add) {
        Log.d(TAG, "set_send_util_email host:" + host + "port" + port + "from_add" + from_add + "psw" + psw + "to_add" + to_add);
        //验证
        if (host.equals("") || port.equals("") || from_add.equals("") || psw.equals("") || to_add.equals("")) {
            return;
        }
        sp_setting.edit()
                .putString(Define.SP_MSG_SEND_UTIL_EMAIL_HOST_KEY, host)
                .putString(Define.SP_MSG_SEND_UTIL_EMAIL_PORT_KEY, port)
                .putString(Define.SP_MSG_SEND_UTIL_EMAIL_FROMADD_KEY, from_add)
                .putString(Define.SP_MSG_SEND_UTIL_EMAIL_PSW_KEY, psw)
                .putString(Define.SP_MSG_SEND_UTIL_EMAIL_TOADD_KEY, to_add)
                .apply();
    }

    public static String get_send_util_email(String key) {
        Log.d(TAG, "get_send_util_email  key" + key);
        String defaultstt = "";
        if (key.equals(Define.SP_MSG_SEND_UTIL_EMAIL_HOST_KEY)) defaultstt = "smtp服务器";
        if (key.equals(Define.SP_MSG_SEND_UTIL_EMAIL_PORT_KEY)) defaultstt = "端口";
        if (key.equals(Define.SP_MSG_SEND_UTIL_EMAIL_FROMADD_KEY)) defaultstt = "发送邮箱";
        if (key.equals(Define.SP_MSG_SEND_UTIL_EMAIL_PSW_KEY)) defaultstt = "密码";
        if (key.equals(Define.SP_MSG_SEND_UTIL_EMAIL_TOADD_KEY)) defaultstt = "接收邮箱";
        return sp_setting.getString(key, defaultstt);
    }

    public static boolean saveMsgHistory() {
        return sp_setting.getBoolean("option_save_history_on", false);
    }
}
