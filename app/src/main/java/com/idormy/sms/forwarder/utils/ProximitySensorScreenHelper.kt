package com.idormy.sms.forwarder.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager


object ProximitySensorScreenHelper {
    private var mProximityWakeLock: PowerManager.WakeLock? = null
    private var sensorEventListener: SensorEventListener? = null

    fun refresh(context: Context) {
        if (SettingUtils.enableCloseToEarpieceTurnOffScreen) {
            if (mProximityWakeLock == null) {
                start(context)
            }
        } else {
            if (mProximityWakeLock != null) {
                stop(context)
            }
        }
    }

    fun isEnable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    private fun start(context: Context) {
        if (!isEnable()) {
            return
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mProximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "smsForwarder:ProximitySensorScreenHelper"
            )
        } else {
            mProximityWakeLock = null
        }
        mProximityWakeLock?.also { wakeLock ->
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return
            sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val its = event?.values ?: return
                    if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                        //经过测试，当手贴近距离感应器的时候its[0]返回值为0.0，当手离开时返回1.0
                        if (its[0] == 0.0f) { // 贴近手机
                            if (wakeLock.isHeld) {
                                return
                            }
                            wakeLock.acquire() // 申请设备电源锁
                        } else { // 远离手机
                            if (!wakeLock.isHeld) {
                                return
                            }
                            wakeLock.release() // 释放设备电源锁
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

                }
            }
            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun stop(context: Context) {
        sensorEventListener?.also {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(it)
        }
        sensorEventListener = null
        mProximityWakeLock?.also { wakeLock ->
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
        mProximityWakeLock = null
    }
}