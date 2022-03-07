package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryUtils {
    private static final String TAG = "BatteryUtils";

    @SuppressLint("DefaultLocale")
    public static String getBatteryInfo(Intent intent) {
        Log.i(TAG, "getBatteryInfo--------------");
        String action = intent.getAction();
        Log.i(TAG, " 0 action:" + action);
        Log.i(TAG, "ACTION_BATTERY_CHANGED");
        int status = intent.getIntExtra("status", 0);
        int health = intent.getIntExtra("health", 0);
        //boolean present = intent.getBooleanExtra("present", false);
        int levelCur = intent.getIntExtra("level", 0);
        int scale = intent.getIntExtra("scale", 0);
        //int icon_small = intent.getIntExtra("icon-small", 0);
        int plugged = intent.getIntExtra("plugged", 0);
        int voltage = intent.getIntExtra("voltage", 0);
        int temperature = intent.getIntExtra("temperature", 0);
        //String technology = intent.getStringExtra("technology");

        String msg = "";
        msg += "\n剩余电量：" + levelCur + "%";

        if (scale > 0) msg += "\n充满电量：" + scale + "%";

        if (voltage > 0) msg += "\n当前电压：" + String.format("%.2f", voltage / 1000F) + "V";

        if (temperature > 0) msg += "\n当前温度：" + String.format("%.2f", temperature / 10F) + "℃";

        msg += "\n电池状态：" + getStatus(status);

        if (health > 0) msg += "\n健康度：" + getHealth(health);

        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                msg += "\n充电器：AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                msg += "\n充电器：USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                msg += "\n充电器：无线";
                break;
        }

        Log.i(TAG, msg);
        return msg;
    }

    //电池状态
    public static String getStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "充电中";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "放电中";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "未充电";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "充满电";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "未知";
        }
    }

    //健康度
    public static String getHealth(int health) {
        switch (health) {
            case 2:
                return "良好";
            case 3:
                return "过热";
            case 4:
                return "没电";
            case 5:
                return "过电压";
            case 6:
                return "未知错误";
            case 7:
                return "温度过低";
            default:
            case 1:
                return "未知";
        }
    }
}
