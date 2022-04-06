package com.idormy.sms.forwarder.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.BatteryUtils;
import com.idormy.sms.forwarder.utils.SettingUtils;

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
            int levelPre = SettingUtils.getBatteryLevelCurrent();
            if (levelCur != levelPre) {
                String msg = BatteryUtils.getBatteryInfo(intent);
                SettingUtils.setBatteryLevelCurrent(levelCur);

                int levelMin = SettingUtils.getBatteryLevelAlarmMin();
                int levelMax = SettingUtils.getBatteryLevelAlarmMax();
                if (SettingUtils.getBatteryLevelAlarmOnce() && levelMin > 0 && levelPre > levelCur && levelCur <= levelMin) { //电量下降到下限
                    msg = "【电量预警】已低于电量预警下限，请及时充电！" + msg;
                    sendMessage(context, msg);
                    return;
                } else if (SettingUtils.getBatteryLevelAlarmOnce() && levelMax > 0 && levelPre < levelCur && levelCur >= levelMax) { //电量上升到上限
                    msg = "【电量预警】已高于电量预警上限，请拔掉充电器！" + msg;
                    sendMessage(context, msg);
                    return;
                } else if (!SettingUtils.getBatteryLevelAlarmOnce() && levelMin > 0 && levelPre > levelCur && levelCur == levelMin) { //电量下降到下限
                    msg = "【电量预警】已到达电量预警下限，请及时充电！" + msg;
                    sendMessage(context, msg);
                    return;
                } else if (!SettingUtils.getBatteryLevelAlarmOnce() && levelMax > 0 && levelPre < levelCur && levelCur == levelMax) { //电量上升到上限
                    msg = "【电量预警】已到达电量预警上限，请拔掉充电器！" + msg;
                    sendMessage(context, msg);
                    return;
                }
            }

            //充电状态改变
            int status = intent.getIntExtra("status", 0);
            if (SettingUtils.getSwitchEnableBatteryReceiver()) {
                int oldStatus = SettingUtils.getBatteryStatus();
                if (status != oldStatus) {
                    String msg = BatteryUtils.getBatteryInfo(intent);
                    SettingUtils.setBatteryStatus(status);
                    msg = "【充电状态】发生变化：" + BatteryUtils.getStatus(oldStatus) + " → " + BatteryUtils.getStatus(status) + msg;
                    sendMessage(context, msg);
                }
            }
        }
    };

    //发送信息
    private void sendMessage(Context context, String msg) {
        Log.i(TAG, msg);
        try {
            SmsVo smsVo = new SmsVo("88888888", msg, new Date(), "电池状态监听");
            Log.d(TAG, "send_msg" + smsVo);
            SendUtil.send_msg(context, smsVo, 1, "app");
        } catch (Exception e) {
            Log.e(TAG, "getLog e:" + e.getMessage());
        }
    }

}
