package com.idormy.sms.forwarder.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.idormy.sms.forwarder.MainActivity;
import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.R;
import com.idormy.sms.forwarder.database.AppDatabase;
import com.idormy.sms.forwarder.database.Config;
import com.idormy.sms.forwarder.utils.CommonUtils;
import com.idormy.sms.forwarder.utils.Define;
import com.idormy.sms.forwarder.utils.OSUtils;
import com.idormy.sms.forwarder.utils.PhoneUtils;
import com.idormy.sms.forwarder.utils.SettingUtils;
import com.jeremyliao.liveeventbus.LiveEventBus;

import frpclib.Frpclib;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FrontService extends Service {
    private static final String TAG = "FrontService";
    public static final int NOTIFY_ID = 0x1010;
    private static final String CHANNEL_ONE_ID = "com.idormy.sms.forwarder";
    private static final String CHANNEL_ONE_NAME = "com.idormy.sms.forwarderName";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    //Frc相关
    public static final String INTENT_KEY_FILE = "INTENT_KEY_FILE";
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Observer<String> keyObserver = uid -> {
        if (Frpclib.isRunning(uid)) {
            return;
        }

        AppDatabase.getInstance(FrontService.this)
                .configDao()
                .getConfigByUid(uid)
                .flatMap((Function<Config, SingleSource<String>>) config -> {
                    String error = Frpclib.runContent(config.getUid(), config.getCfg());
                    return Single.just(error);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<>() {

                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(String error) {
                        if (!TextUtils.isEmpty(error)) {
                            Toast.makeText(FrontService.this, error, Toast.LENGTH_SHORT).show();
                            LiveEventBus.get(Define.EVENT_RUNNING_ERROR, String.class).post(uid);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    };

    @SuppressLint("IconColors")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        //是否同意隐私协议
        if (!MyApplication.allowPrivacyPolicy) return;

        //开启通知栏
        startForeground(NOTIFY_ID, createNotification());

        //Android8.1以下尝试启动主界面，以便动态获取权限
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        //手机重启，未打开app时，主动获取SIM卡信息
        if (MyApplication.SimInfoList.isEmpty()) {
            PhoneUtils.init(this);
            MyApplication.SimInfoList = PhoneUtils.getSimMultiInfo();
        }

        //开关通知监听服务
        if (SettingUtils.getSwitchEnableAppNotify() && CommonUtils.isNotificationListenerServiceEnabled(this)) {
            CommonUtils.toggleNotificationListenerService(this);
        }

        //Frc内网穿透
        LiveEventBus.get(INTENT_KEY_FILE, String.class).observeStickyForever(keyObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //是否同意隐私协议
        if (!MyApplication.allowPrivacyPolicy) return;

        //进行自动重启
        Intent intent = new Intent(FrontService.this, FrontService.class);
        //重新开启服务
        startService(intent);
        stopForeground(true);

        compositeDisposable.dispose();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY; //保证service不被杀死
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //创建通知栏
    private Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_forwarder);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        if (OSUtils.isMIUI()) {
            builder.setContentTitle(getString(R.string.app_name));
        }
        builder.setContentText(getString(R.string.notification_content));
        Intent intent = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= 30 ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
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

        return builder.build();
    }

}