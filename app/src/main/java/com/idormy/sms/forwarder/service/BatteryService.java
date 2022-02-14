package com.idormy.sms.forwarder.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import java.util.Date;

@SuppressWarnings("deprecation")
public class BatteryService extends Service {

    private static final String TAG = "BatteryReceiver";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate--------------");

        //是否同意隐私协议
        if (!MyApplication.allowPrivacyPolicy) return;

        IntentFilter batteryfilter = new IntentFilter();
        batteryfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, batteryfilter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand--------------");
        return Service.START_STICKY; //
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy--------------");
        super.onDestroy();

        //是否同意隐私协议
        if (!MyApplication.allowPrivacyPolicy) return;

        this.unregisterReceiver(batteryReceiver);
    }

    // 接收电池信息更新的广播
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(Context context, Intent intent) {

            //电量发生变化
            int levelCur = intent.getIntExtra("level", 0);
            int levelPre = SettingUtil.getBatteryLevelCurrent();
            if (levelCur != levelPre) {
                String msg = batteryReceiver(intent);
                SettingUtil.setBatteryLevelCurrent(levelCur);

                int levelMin = SettingUtil.getBatteryLevelAlarmMin();
                int levelMax = SettingUtil.getBatteryLevelAlarmMax();
                if (SettingUtil.getBatteryLevelAlarmOnce() && levelMin > 0 && levelPre > levelCur && levelCur <= levelMin) { //电量下降到下限
                    msg = "【电量预警】已低于电量预警下限，请及时充电！" + msg;
                    sendMessage(context, msg);
                    return;
                } else if (SettingUtil.getBatteryLevelAlarmOnce() && levelMax > 0 && levelPre < levelCur && levelCur >= levelMax) { //电量上升到上限
                    msg = "【电量预警】已高于电量预警上限，请拔掉充电器！" + msg;
                    sendMessage(context, msg);
                    return;
                } else if (!SettingUtil.getBatteryLevelAlarmOnce() && levelMin > 0 && levelPre > levelCur && levelCur == levelMin) { //电量下降到下限
                    msg = "【电量预警】已到达电量预警下限，请及时充电！" + msg;
                    sendMessage(context, msg);
                    return;
                } else if (!SettingUtil.getBatteryLevelAlarmOnce() && levelMax > 0 && levelPre < levelCur && levelCur == levelMax) { //电量上升到上限
                    msg = "【电量预警】已到达电量预警上限，请拔掉充电器！" + msg;
                    sendMessage(context, msg);
                    return;
                }
            }

            //充电状态改变
            int status = intent.getIntExtra("status", 0);
            if (SettingUtil.getSwitchEnableBatteryReceiver()) {
                int oldStatus = SettingUtil.getBatteryStatus();
                if (status != oldStatus) {
                    String msg = batteryReceiver(intent);
                    SettingUtil.setBatteryStatus(status);
                    msg = "【充电状态】发生变化：" + getStatus(oldStatus) + " → " + getStatus(status) + msg;
                    sendMessage(context, msg);
                }
            }
        }
    };

    @SuppressLint("DefaultLocale")
    private String batteryReceiver(Intent intent) {
        Log.i(TAG, "BatteryReceiver--------------");
        String action = intent.getAction();
        Log.i(TAG, " 0 action:" + action);
        Log.i(TAG, "ACTION_BATTERY_CHANGED");
        int status = intent.getIntExtra("status", 0);
        int health = intent.getIntExtra("health", 0);
        //boolean present = intent.getBooleanExtra("present", false);
        int levelCur = intent.getIntExtra("level", 0);
        int scale = intent.getIntExtra("scale", 0);
        //int icon_small = intent.getIntExtra("icon-small", 0);
        int plugged = intent.getIntExtra("plugged", 0);
        int voltage = intent.getIntExtra("voltage", 0);
        int temperature = intent.getIntExtra("temperature", 0);
        //String technology = intent.getStringExtra("technology");

        String msg = "";
        msg += "\n剩余电量：" + levelCur + "%";

        if (scale > 0) msg += "\n充满电量：" + scale + "%";

        if (voltage > 0) msg += "\n当前电压：" + String.format("%.2f", voltage / 1000F) + "V";

        if (temperature > 0) msg += "\n当前温度：" + String.format("%.2f", temperature / 10F) + "℃";

        msg += "\n电池状态：" + getStatus(status);

        if (health > 0) msg += "\n健康度：" + getHealth(health);

        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                msg += "\n充电器：AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                msg += "\n充电器：USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                msg += "\n充电器：无线";
                break;
        }

        Log.i(TAG, msg);
        return msg;
    }

    //电池状态
    private String getStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "充电中";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "放电中";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "未充电";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "充满电";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "未知";
        }
    }

    //健康度
    private String getHealth(int health) {
        switch (health) {
            case 2:
                return "良好";
            case 3:
                return "过热";
            case 4:
                return "没电";
            case 5:
                return "过电压";
            case 6:
                return "未知错误";
            case 7:
                return "温度过低";
            default:
            case 1:
                return "未知";
        }
    }

    //发送信息
    private void sendMessage(Context context, String msg) {
        Log.i(TAG, msg);
        try {
            SmsVo smsVo = new SmsVo("88888888", msg, new Date(), "电池状态监听");
            Log.d(TAG, "send_msg" + smsVo);
            SendUtil.send_msg(context, smsVo, 1, "app");

            //SmsHubApi
            if (SettingUtil.getSwitchEnableSmsHubApi()) {
                SmsHubActionHandler.putData(new SmsHubVo(SmsHubVo.Type.phone, null, msg, "电池状态监听"));
            }
        } catch (Exception e) {
            Log.e(TAG, "getLog e:" + e.getMessage());
        }
    }

}
