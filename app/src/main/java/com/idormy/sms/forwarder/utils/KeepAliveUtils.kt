package com.idormy.sms.forwarder.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.idormy.sms.forwarder.R

@Suppress("DEPRECATION")
class KeepAliveUtils private constructor() {

    companion object {
        fun isIgnoreBatteryOptimization(activity: Activity): Boolean {
            //安卓6.0以下没有忽略电池优化
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                true
            } else try {
                val powerManager: PowerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(activity.packageName)
            } catch (e: Exception) {
                XToastUtils.error(R.string.unsupport)
                false
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        fun ignoreBatteryOptimization(activity: Activity) {
            try {
                if (isIgnoreBatteryOptimization(activity)) {
                    return
                }
                @SuppressLint("BatteryLife") val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:" + activity.packageName)
                val resolveInfo: ResolveInfo? = activity.packageManager.resolveActivity(intent, 0)
                if (resolveInfo != null) {
                    activity.startActivity(intent)
                } else {
                    XToastUtils.error(R.string.unsupport)
                }
            } catch (e: Exception) {
                XToastUtils.error(R.string.unsupport)
            }
        }

    }
}