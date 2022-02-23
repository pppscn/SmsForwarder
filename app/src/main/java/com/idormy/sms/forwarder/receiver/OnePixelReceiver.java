package com.idormy.sms.forwarder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.idormy.sms.forwarder.utils.OnePixelManager;


public class OnePixelReceiver extends BroadcastReceiver {
    private static final String TAG = "OnePixelReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        OnePixelManager manager = new OnePixelManager();
        if (Intent.ACTION_SCREEN_ON.equals(action)) {//如果亮屏，则关闭1像素Activity
            manager.finishOnePixelActivity();
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {//如果息屏，则开启1像素Activity
            manager.startOnePixelActivity(context);
        }
    }
}

