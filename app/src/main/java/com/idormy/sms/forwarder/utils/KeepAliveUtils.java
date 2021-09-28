package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import com.idormy.sms.forwarder.R;

public class KeepAliveUtils {

    public static boolean isIgnoreBatteryOptimization(Activity activity) {
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            return powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
        } else {
            return true;
        }
    }

    public static void ignoreBatteryOptimization(Activity activity) {
        if (isIgnoreBatteryOptimization(activity)) {
            return;
        }
        @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo != null) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, R.string.unsupport, Toast.LENGTH_SHORT).show();
        }
    }
}
