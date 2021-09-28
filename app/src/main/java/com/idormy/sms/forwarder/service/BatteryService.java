package com.idormy.sms.forwarder.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;

@SuppressWarnings("deprecation")
public class BatteryService extends Service {

    private static final String TAG = "BatteryReceiver";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate--------------");
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, batteryFilter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand--------------");
        return Service.START_STICKY;  //保证service不被杀死
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy--------------");
        super.onDestroy();
        this.unregisterReceiver(batteryReceiver);

    }

    // 接收电池信息更新的广播
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @SuppressWarnings("unused")
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BatteryReceiver--------------");
            String action = intent.getAction();
            Log.i(TAG, " 0 action:" + action);
            Log.i(TAG, "ACTION_BATTERY_CHANGED");
            int status = intent.getIntExtra("status", 0);
            int health = intent.getIntExtra("health", 0);
            boolean present = intent.getBooleanExtra("present", false);
            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 0);
            int icon_small = intent.getIntExtra("icon-small", 0);
            int plugged = intent.getIntExtra("plugged", 0);
            int voltage = intent.getIntExtra("voltage", 0);
            int temperature = intent.getIntExtra("temperature", 0);
            String technology = intent.getStringExtra("technology");

            String statusString = "";
            switch (status) {
                case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    statusString = "unknown";
                    break;
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    statusString = "charging";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    statusString = "discharging";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    statusString = "not charging";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    statusString = "full";
                    break;
            }
            String acString = "";

            switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    acString = "plugged ac";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    acString = "plugged usb";
                    break;
            }

            @SuppressLint("SimpleDateFormat") SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS ");
            String date = sDateFormat.format(new java.util.Date());

            Log.i(TAG, "battery: date=" + date + ",status " + statusString
                    + ",level=" + level + ",scale=" + scale
                    + ",voltage=" + voltage + ",acString=" + acString);

        }
    };
}
