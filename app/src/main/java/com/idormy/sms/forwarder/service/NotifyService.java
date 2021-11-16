package com.idormy.sms.forwarder.service;

import android.content.ComponentName;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.idormy.sms.forwarder.notify.NotifyHelper;
import com.idormy.sms.forwarder.utils.SettingUtil;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotifyService extends NotificationListenerService {

    public static final String TAG = "NotifyService";

    /**
     * 发布通知
     *
     * @param sbn 状态栏通知
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!SettingUtil.getSwitchEnableAppNotify()) {
            return;
        }
        if (sbn.getNotification() == null) return;
        Log.d(TAG, sbn.getPackageName());
        NotifyHelper.getInstance().onReceive(sbn);
    }

    /**
     * 通知已删除
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, sbn.getPackageName());
        if (!SettingUtil.getSwitchEnableAppNotify()) {
            return;
        }
        NotifyHelper.getInstance().onRemoved(sbn);
    }

    /**
     * 监听断开
     */
    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "通知侦听器断开连接 - 请求重新绑定");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(new ComponentName(this, NotificationListenerService.class));
        }
    }
}
