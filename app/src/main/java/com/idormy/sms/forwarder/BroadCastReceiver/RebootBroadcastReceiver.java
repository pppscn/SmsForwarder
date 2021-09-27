package com.idormy.sms.forwarder.BroadCastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.idormy.sms.forwarder.service.FrontService;
import com.idormy.sms.forwarder.utils.InitUtil;

public class RebootBroadcastReceiver extends BroadcastReceiver {
    private String TAG = "RebootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String receiveAction = intent.getAction();
        Log.d(TAG, "onReceive intent " + receiveAction);
        if (receiveAction.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d(TAG, "BOOT_COMPLETED");

            InitUtil.init(context);
            Intent frontServiceIntent = new Intent(context, FrontService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(frontServiceIntent);
            } else {
                context.startService(frontServiceIntent);
            }
            //监控当前电量
            //Intent batteryServiceIntent = new Intent(context, BatteryService.class);
            //context.startService(batteryServiceIntent);
        }

    }

}
