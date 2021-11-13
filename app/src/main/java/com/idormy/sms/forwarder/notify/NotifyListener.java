package com.idormy.sms.forwarder.notify;

import android.service.notification.StatusBarNotification;

public interface NotifyListener {

    /**
     * 接收到通知栏消息
     *
     * @param type
     */
    void onReceiveMessage(int type);

    /**
     * 移除掉通知栏消息
     *
     * @param type
     */
    void onRemovedMessage(int type);

    /**
     * 接收到通知栏消息
     *
     * @param sbn
     */
    void onReceiveMessage(StatusBarNotification sbn);

    /**
     * 移除掉通知栏消息
     *
     * @param sbn
     */
    void onRemovedMessage(StatusBarNotification sbn);
}
