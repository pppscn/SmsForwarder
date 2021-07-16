package com.idormy.sms.forwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.idormy.sms.forwarder.utils.OSUtils;
import com.idormy.sms.forwarder.utils.PhoneUtils;

import androidx.annotation.Nullable;


public class FrontService extends Service {
    private static final String TAG = "FrontService";
    private static final String CHANNEL_ONE_ID = "com.idormy.sms.forwarder";
    private static final String CHANNEL_ONE_NAME = "com.idormy.sms.forwarderName";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_sms_forwarder);
        OSUtils.ROM_TYPE romType = OSUtils.getRomType();
        //Log.d(TAG, "onCreate: " + romType);
        if (romType == OSUtils.ROM_TYPE.MIUI_ROM) {
            builder.setContentTitle("短信转发器");
        }
        builder.setContentText("根据规则转发到钉钉/微信/邮箱/bark/Server酱/Telegram/webhook等");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity
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
            MyApplication.SimInfoList = PhoneUtils.getSimMultiInfo();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "flags: " + flags + " startId: " + startId);
        return START_STICKY;
    }

}
