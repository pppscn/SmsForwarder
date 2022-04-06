package com.idormy.sms.forwarder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.utils.OnePixelManager;
import com.idormy.sms.forwarder.utils.SettingUtils;

//监听屏幕状态变换广播（开屏、锁屏、解锁）
public class ScreenBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        //1像素透明Activity保活
        if (SettingUtils.getOnePixelActivity()) {
            OnePixelManager manager = new OnePixelManager();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {//如果亮屏，则关闭1像素Activity
                manager.finishOnePixelActivity();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {//如果息屏，则开启1像素Activity
                manager.startOnePixelActivity(context);
            }
        }

        //是否已解锁
        MyApplication.isUserPresent = Intent.ACTION_USER_PRESENT.equals(action);
        Log.d(TAG, String.format("isUserPresent=%s", MyApplication.isUserPresent));
    }
}

