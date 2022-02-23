package com.idormy.sms.forwarder.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.idormy.sms.forwarder.OnePixelActivity;
import com.idormy.sms.forwarder.receiver.OnePixelReceiver;

import java.lang.ref.WeakReference;

public class OnePixelManager {
    private static final String TAG = "OnePixelManager";
    private WeakReference<Activity> mActivity;
    private OnePixelReceiver onePixelReceiver;

    /**
     * 一像素广播接收者注册方法。该方法中初始化OnePixelReceiver，并添加了过滤条件
     * 屏幕息屏和亮屏。然后注册该广播接收者
     */
    public void registerOnePixelReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        onePixelReceiver = new OnePixelReceiver();
        context.registerReceiver(onePixelReceiver, filter);
    }

    /**
     * 对广播接收者进行解注册
     */
    public void unregisterOnePixelReceiver(Context context) {
        if (null != onePixelReceiver) {
            context.unregisterReceiver(onePixelReceiver);
        }
    }

    /**
     * 开启一像素Activity
     */
    public void startOnePixelActivity(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, OnePixelActivity.class);
        context.startActivity(intent);
    }

    /**
     * 关闭一像素Activity
     */
    public void finishOnePixelActivity() {
        if (null != mActivity) {
            Activity activity = mActivity.get();
            if (null != activity) {
                activity.finish();
            }
            mActivity = null;
        }
    }

    /**
     * 使用弱引用获取一像素的上下文
     */
    public void setKeepAliveReference(OnePixelActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

}



