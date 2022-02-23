package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;

@SuppressWarnings({"SynchronizeOnNonFinalField", "unused"})
public class SettingUtil {
    static Boolean hasInit = false;
    private static final String TAG = "SettingUtil";
    private static SharedPreferences sp_setting = null;
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;

    public static void init(Context context1) {
        synchronized (hasInit) {
            if (hasInit) return;
            hasInit = true;
            context = context1;
            Log.d(TAG, "SettingUtil init ");
            sp_setting = PreferenceManager.getDefaultSharedPreferences(context1);
        }
    }

    public static void switchAddExtra(Boolean switchAddExtra) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_SWITCH_ADD_EXTRA, switchAddExtra).apply();
    }

    public static boolean getSwitchAddExtra() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_SWITCH_ADD_EXTRA, false);
    }

    public static void switchAddDeviceName(Boolean switchAddDeviceName) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ADD_EXTRA_DEVICE_NAME, switchAddDeviceName).apply();
    }

    public static boolean getSwitchAddDeviceName() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ADD_EXTRA_DEVICE_NAME, false);
    }

    public static void switchEnableSms(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_SMS, enable).apply();
    }

    public static boolean getSwitchEnableSms() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_SMS, false);
    }

    public static void switchEnablePhone(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_PHONE, enable).apply();
    }

    public static boolean getSwitchEnablePhone() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_PHONE, false);
    }

    public static void switchEnableAppNotify(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_APP_NOTIFY, enable).apply();
    }

    public static boolean getSwitchEnableAppNotify() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_APP_NOTIFY, false);
    }

    public static void switchCancelAppNotify(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_CANCEL_APP_NOTIFY, enable).apply();
    }

    public static boolean getSwitchCancelAppNotify() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_CANCEL_APP_NOTIFY, false);
    }

    public static void switchEnableBatteryReceiver(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_BATTERY_RECEIVER, enable).apply();
    }

    public static boolean getSwitchEnableBatteryReceiver() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_BATTERY_RECEIVER, false);
    }

    public static void switchExcludeFromRecents(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_EXCLUDE_FROM_RECENTS, enable).apply();
    }

    public static boolean getExcludeFromRecents() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_EXCLUDE_FROM_RECENTS, false);
    }

    public static void switchPlaySilenceMusic(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_PLAY_SILENCE_MUSIC, enable).apply();
    }

    public static boolean getPlaySilenceMusic() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_PLAY_SILENCE_MUSIC, false);
    }

    public static void switchOnePixelActivity(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_ONE_PIXEL_ACTIVITY, enable).apply();
    }

    public static boolean getOnePixelActivity() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_ONE_PIXEL_ACTIVITY, false);
    }

    public static void switchSmsTemplate(Boolean switchSmsTemplate) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_SWITCH_SMS_TEMPLATE, switchSmsTemplate).apply();
    }

    public static boolean getSwitchSmsTemplate() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_SWITCH_SMS_TEMPLATE, false);
    }

    public static String getAddExtraDeviceMark() {
        String res = sp_setting.getString(Define.SP_MSG_KEY_STRING_ADD_EXTRA_DEVICE_MARK, "");
        if (res == null || res.equals("")) {
            res = android.os.Build.MODEL;
        }
        return res;
    }

    public static void setAddExtraDeviceMark(String addExtraDeviceMark) {
        sp_setting.edit().putString(Define.SP_MSG_KEY_STRING_ADD_EXTRA_DEVICE_MARK, addExtraDeviceMark).apply();
    }

    public static String getSmsTemplate() {
        return sp_setting.getString(Define.SP_MSG_KEY_STRING_SMS_TEMPLATE,
                getString(R.string.tag_from) + "\n" +
                        getString(R.string.tag_sms) + "\n" +
                        getString(R.string.tag_card_slot) + "\n" +
                        getString(R.string.tag_receive_time) + "\n" +
                        getString(R.string.tag_device_name));
    }

    public static void setSmsTemplate(String textSmsTemplate) {
        sp_setting.edit().putString(Define.SP_MSG_KEY_STRING_SMS_TEMPLATE, textSmsTemplate).apply();
    }

    public static String getAddExtraSim1() {
        String res = sp_setting.getString(Define.SP_MSG_KEY_STRING_ADD_EXTRA_SIM1, "");
        if (res == null || res.equals("")) {
            res = SimUtil.getSimInfo(1);
        }
        return res;
    }

    public static void setAddExtraSim1(String sim1) {
        sp_setting.edit().putString(Define.SP_MSG_KEY_STRING_ADD_EXTRA_SIM1, sim1).apply();
    }

    public static String getAddExtraSim2() {
        String res = sp_setting.getString(Define.SP_MSG_KEY_STRING_ADD_EXTRA_SIM2, "");
        if (res == null || res.equals("")) {
            res = SimUtil.getSimInfo(2);
        }
        return res;
    }

    public static void setAddExtraSim2(String sim2) {
        sp_setting.edit().putString(Define.SP_MSG_KEY_STRING_ADD_EXTRA_SIM2, sim2).apply();
    }

    public static int getBatteryLevelAlarmMin() {
        return sp_setting.getInt(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_ALARM, 0);
    }

    public static void setBatteryLevelAlarmMin(int battery_level) {
        sp_setting.edit().putInt(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_ALARM, battery_level).apply();
    }

    public static int getBatteryLevelAlarmMax() {
        return sp_setting.getInt(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_MAX, 0);
    }

    public static void switchBatteryLevelAlarmOnce(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_ONCE, enable).apply();
    }

    public static boolean getBatteryLevelAlarmOnce() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_ONCE, false);
    }

    public static void setBatteryLevelAlarmMax(int battery_level) {
        sp_setting.edit().putInt(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_MAX, battery_level).apply();
    }

    public static int getBatteryLevelCurrent() {
        return sp_setting.getInt(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_CURRENT, 0);
    }

    public static void setBatteryLevelCurrent(int battery_level) {
        sp_setting.edit().putInt(Define.SP_MSG_KEY_STRING_BATTERY_LEVEL_CURRENT, battery_level).apply();
    }

    public static int getBatteryStatus() {
        return sp_setting.getInt(Define.SP_MSG_KEY_STRING_BATTERY_STATUS, 0);
    }

    public static void setBatteryStatus(int battery_status) {
        sp_setting.edit().putInt(Define.SP_MSG_KEY_STRING_BATTERY_STATUS, battery_status).apply();
    }

    public static boolean saveMsgHistory() {
        return !sp_setting.getBoolean("option_save_history_on", false);
    }

    public static String getPrevNoticeHash(String key) {
        return sp_setting.getString(key, "");
    }

    public static void setPrevNoticeHash(String key, String value) {
        sp_setting.edit().putString(key, value).apply();
    }

    public static void switchEnableSmsHubApi(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_SMSHUB_API, enable).apply();
    }

    public static boolean getSwitchEnableSmsHubApi() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_SMSHUB_API, false);
    }

    public static void switchEnableHttpServer(Boolean enable) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_HTTP_SERVER, enable).apply();
    }

    public static boolean getSwitchEnableHttpServer() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_HTTP_SERVER, false);
    }

    public static void smsHubApiUrl(String url) {
        sp_setting.edit().putString(Define.SP_MSG_KEY_STRING_SMSHUB_API_URL, url).apply();
    }

    public static String getSmsHubApiUrl() {
        return sp_setting.getString(Define.SP_MSG_KEY_STRING_SMSHUB_API_URL, "http://xxx.com/send_api");
    }

    public static void switchCallType1(Boolean switchCallType) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_CALL_TYPE_1, switchCallType).apply();
    }

    public static boolean getSwitchCallType1() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_CALL_TYPE_1, false);
    }

    public static void switchCallType2(Boolean switchCallType) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_CALL_TYPE_2, switchCallType).apply();
    }

    public static boolean getSwitchCallType2() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_CALL_TYPE_2, false);
    }

    public static void switchCallType3(Boolean switchCallType) {
        sp_setting.edit().putBoolean(Define.SP_MSG_KEY_STRING_ENABLE_CALL_TYPE_3, switchCallType).apply();
    }

    public static boolean getSwitchCallType3() {
        return sp_setting.getBoolean(Define.SP_MSG_KEY_STRING_ENABLE_CALL_TYPE_3, true);
    }

    public static int getRetryTimes() {
        return sp_setting.getInt(Define.SP_MSG_KEY_STRING_RETRY_TIMES, 0);
    }

    public static void setRetryTimes(int retry_times) {
        sp_setting.edit().putInt(Define.SP_MSG_KEY_STRING_RETRY_TIMES, retry_times).apply();
    }

    public static int getDelayTime() {
        return sp_setting.getInt(Define.SP_MSG_KEY_STRING_DELAY_TIME, 1);
    }

    public static void setDelayTime(int delay_time) {
        sp_setting.edit().putInt(Define.SP_MSG_KEY_STRING_DELAY_TIME, delay_time).apply();
    }

    //获取当前版本名称
    public static String getVersionName() {
        // 获取PackageManager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    //获取应用的版本号
    public static int getVersionCode() {
        PackageManager manager = context.getPackageManager();
        int code = 0;
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            code = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return code;
    }

    private static String getString(int resId) {
        return MyApplication.getContext().getString(resId);
    }
}
