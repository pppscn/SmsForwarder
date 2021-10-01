package com.idormy.sms.forwarder.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.idormy.sms.forwarder.MainActivity;
import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.sender.SenderUtil;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtil;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class FrontService extends Service {
    private static final String TAG = "FrontService";
    private static final String CHANNEL_ONE_ID = "com.idormy.sms.forwarder";
    private static final String CHANNEL_ONE_NAME = "com.idormy.sms.forwarderName";

    @SuppressLint("IconColors")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_forwarder);
        //OSUtils.ROM_TYPE romType = OSUtils.getRomType();
        //Log.d(TAG, "onCreate: " + romType);
        //if (romType == OSUtils.ROM_TYPE.MIUI_ROM) {
        builder.setContentTitle(getString(R.string.app_name));
        //}
        builder.setContentText(getString(R.string.notification_content));
        Intent intent = new Intent(this, MainActivity.class);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity
                (this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //修改安卓8.1以上系统报错
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID, CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_MIN);
            notificationChannel.enableLights(false);//如果使用中的设备支持通知灯，则说明此通知通道是否应显示灯
            notificationChannel.setShowBadge(false);//是否显示角标
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            builder.setChannelId(CHANNEL_ONE_ID);
        }

        Notification notification = builder.build();
        startForeground(1, notification);

        //检查权限是否获取
        //PackageManager pm = getPackageManager();
        //PhoneUtils.CheckPermission(pm, this);

        //Android8.1以下尝试启动主界面，以便动态获取权限
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        // 手机重启，未打开app时，主动获取SIM卡信息
        if (MyApplication.SimInfoList.isEmpty()) {
            PhoneUtils.init(this);
            MyApplication.SimInfoList = PhoneUtils.getSimMultiInfo();
        }

        // 低电量预警
        final int[] alarmTimes = {0}; //通知次数，只通知2次
        Context context1 = this;
        SenderUtil.init(context1);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                int batteryLevel = getBatteryLevel();
                System.out.println("当前剩余电量：" + batteryLevel + "%");
                int batteryLevelAlarm = SettingUtil.getBatteryLevelAlarm();
                if (alarmTimes[0] <= 1 && batteryLevelAlarm > 0 && batteryLevelAlarm <= 100 && (batteryLevel == batteryLevelAlarm || batteryLevel == batteryLevelAlarm - 1)) {
                    try {
                        alarmTimes[0] = alarmTimes[0] + 1;
                        SmsVo smsVo = new SmsVo("88888888",
                                "当前剩余电量：" + batteryLevel + "%，已经到达低电量预警阈值，请及时充电！",
                                new Date(),
                                "低电量预警");
                        Log.d(TAG, "send_msg" + smsVo.toString());
                        SendUtil.send_msg(context1, smsVo, 1);
                    } catch (Exception e) {
                        Log.e(TAG, "getLog e:" + e.getMessage());
                    }
                }

                if (batteryLevelAlarm > 0 && batteryLevelAlarm <= 100 && batteryLevel > batteryLevelAlarm) {
                    alarmTimes[0] = 0;
                }
            }
        }, 0, 10000);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "flags: " + flags + " startId: " + startId);
        return START_STICKY; //保证service不被杀死
    }

    //获取当前电量
    @SuppressLint("ObsoleteSdkInt")
    private int getBatteryLevel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            Intent intent = new ContextWrapper(getApplicationContext()).
                    registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100) /
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        }
    }
}
