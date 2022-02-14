package com.idormy.sms.forwarder.service;

import android.content.ComponentName;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.model.vo.SmsHubVo;
import com.idormy.sms.forwarder.model.vo.SmsVo;
import com.idormy.sms.forwarder.sender.SendUtil;
import com.idormy.sms.forwarder.utils.CommonUtil;
import com.idormy.sms.forwarder.utils.SettingUtil;
import com.idormy.sms.forwarder.utils.SmsHubActionHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        //是否同意隐私协议
        if (!MyApplication.allowPrivacyPolicy) return;

        //未开启转发
        if (!SettingUtil.getSwitchEnableAppNotify()) return;
        //异常通知跳过
        if (sbn.getNotification() == null) return;
        if (sbn.getNotification().extras == null) return;

        //推送通知的应用包名
        String packageName = sbn.getPackageName();
        //自身通知跳过
        if ("com.idormy.sms.forwarder".equals(packageName)) return;

        try {
            //通知标题
            String title = "";
            if (sbn.getNotification().extras.get("android.title") != null) {
                title = sbn.getNotification().extras.get("android.title").toString();
            }
            //通知内容
            String text = "";
            if (sbn.getNotification().extras.get("android.text") != null) {
                text = sbn.getNotification().extras.get("android.text").toString();
            }
            if (text.isEmpty() && sbn.getNotification().tickerText != null) {
                text = sbn.getNotification().tickerText.toString();
            }
            //不处理空消息（标题跟内容都为空）
            if (title.isEmpty() && text.isEmpty()) return;

            //通知时间
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(sbn.getPostTime()));
            Log.d(TAG, String.format(
                    Locale.getDefault(),
                    "onNotificationPosted：\n应用包名：%s\n消息标题：%s\n消息内容：%s\n消息时间：%s\n",
                    packageName, title, text, time)
            );

            //自动关闭通知
            if (SettingUtil.getSwitchCancelAppNotify()) {
                String key = sbn.getKey();
                cancelNotification(key);
            }

            //重复通知不再处理
            String prevHash = SettingUtil.getPrevNoticeHash(packageName);
            String currHash = CommonUtil.MD5(packageName + title + text + time);
            Log.d(TAG, "prevHash=" + prevHash + " currHash=" + currHash);
            if (prevHash != null && prevHash.equals(currHash)) {
                Log.w(TAG, "重复通知不再处理");
                return;
            }
            SettingUtil.setPrevNoticeHash(packageName, currHash);

            SmsVo smsVo = new SmsVo(packageName, text, new Date(), title);
            Log.d(TAG, "send_msg" + smsVo);
            SendUtil.send_msg(this, smsVo, 1, "app");

            //SmsHubApi
            if (SettingUtil.getSwitchEnableSmsHubApi()) {
                SmsHubActionHandler.putData(new SmsHubVo(SmsHubVo.Type.app, null, text, packageName));
            }
        } catch (Exception e) {
            Log.e(TAG, "onNotificationPosted:", e);
        }

    }

    /**
     * 通知已删除
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //未开启转发
        if (!SettingUtil.getSwitchEnableAppNotify()) return;
        //异常通知跳过
        if (sbn.getNotification() == null) return;

        Log.d(TAG, sbn.getPackageName());
    }

    /**
     * 监听断开
     */
    @Override
    public void onListenerDisconnected() {
        //未开启转发
        if (!SettingUtil.getSwitchEnableAppNotify()) return;

        Log.d(TAG, "通知侦听器断开连接 - 请求重新绑定");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(new ComponentName(this, NotificationListenerService.class));
        }
    }
}
