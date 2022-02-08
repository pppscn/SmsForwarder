package com.idormy.sms.forwarder.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import com.hjq.toast.ToastUtils;
import com.idormy.sms.forwarder.R;

public class KeepAliveUtils {

    public static boolean isIgnoreBatteryOptimization(Activity activity) {
        //安卓6.0以下没有忽略电池优化
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return true;
        }
        try {
            PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
            } else {
                return true;
            }
        } catch (Exception e) {
            ToastUtils.show(R.string.unsupport);
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void ignoreBatteryOptimization(Activity activity) {
        try {
            if (isIgnoreBatteryOptimization(activity)) {
                return;
            }
            @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(intent, 0);
            if (resolveInfo != null) {
                activity.startActivity(intent);
            } else {
                ToastUtils.show(R.string.unsupport);
            }
        } catch (Exception e) {
            ToastUtils.show(R.string.unsupport);
        }
    }
}
