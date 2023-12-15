package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.king.location.LocationErrorCode
import com.king.location.OnExceptionListener
import com.king.location.OnLocationListener
import com.xuexiang.xaop.util.PermissionUtils
import com.yanzhenjie.andserver.Server
import java.util.Date

@SuppressLint("SimpleDateFormat")
@Suppress("PrivatePropertyName", "DEPRECATION")
class LocationService : Service(), Server.ServerListener {

    private val TAG: String = LocationService::class.java.simpleName

    companion object {
        var isRunning = false
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate: ")
        super.onCreate()

        if (!SettingUtils.enableLocation) return
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        if (intent != null) {
            when (intent.action) {
                "START" -> {
                    startForegroundService()
                }

                "STOP" -> {
                    stopForegroundService()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
        super.onDestroy()

        if (!SettingUtils.enableLocation) return
        stopForegroundService()
    }

    override fun onException(e: Exception?) {
        Log.i(TAG, "onException: ")
    }

    override fun onStarted() {
        Log.i(TAG, "onStarted: ")
    }

    override fun onStopped() {
        Log.i(TAG, "onStopped: ")
    }

    private fun startForegroundService() {
        try {
            //远程找手机 TODO: 判断权限 ACCESS_COARSE_LOCATION ACCESS_FINE_LOCATION
            if (SettingUtils.enableLocation && PermissionUtils.isGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                //可根据具体需求设置定位配置参数（这里只列出一些主要的参数）
                val locationOption = App.LocationClient.getLocationOption().setAccuracy(SettingUtils.locationAccuracy)//设置位置精度：高精度
                    .setPowerRequirement(SettingUtils.locationPowerRequirement) //设置电量消耗：低电耗
                    .setMinTime(SettingUtils.locationMinInterval)//设置位置更新最小时间间隔（单位：毫秒）； 默认间隔：10000毫秒，最小间隔：1000毫秒
                    .setMinDistance(SettingUtils.locationMinDistance)//设置位置更新最小距离（单位：米）；默认距离：0米
                    .setOnceLocation(false)//设置是否只定位一次，默认为 false，当设置为 true 时，则只定位一次后，会自动停止定位
                    .setLastKnownLocation(false)//设置是否获取最后一次缓存的已知位置，默认为 true
                //设置定位配置参数
                App.LocationClient.setLocationOption(locationOption)
                App.LocationClient.startLocation()

                //设置定位监听
                App.LocationClient.setOnLocationListener(object : OnLocationListener() {
                    override fun onLocationChanged(location: Location) {
                        //位置信息
                        Log.d(TAG, "onLocationChanged(location = ${location})")

                        val locationInfo = LocationInfo(
                            location.longitude, location.latitude, "", App.DateFormat.format(Date(location.time)), location.provider.toString()
                        )

                        //根据坐标经纬度获取位置地址信息（WGS-84坐标系）
                        val list = App.Geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (list?.isNotEmpty() == true) {
                            locationInfo.address = list[0].getAddressLine(0)
                        }

                        Log.d(TAG, "locationInfo = $locationInfo")
                        HttpServerUtils.apiLocationCache = locationInfo

                        //TODO: 触发自动任务
                    }

                    override fun onProviderEnabled(provider: String) {
                        super.onProviderEnabled(provider)
                        Log.d(TAG, "onProviderEnabled(provider = ${provider})")
                    }

                    override fun onProviderDisabled(provider: String) {
                        super.onProviderDisabled(provider)
                        Log.d(TAG, "onProviderDisabled(provider = ${provider})")
                    }

                })

                //设置异常监听
                App.LocationClient.setOnExceptionListener(object : OnExceptionListener {
                    override fun onException(@LocationErrorCode errorCode: Int, e: Exception) {
                        //定位出现异常
                        Log.w(TAG, "onException(errorCode = ${errorCode}, e = ${e})")

                        //TODO: 重启定位
                        App.LocationClient.startLocation()
                    }
                })

                if (App.LocationClient.isStarted()) {//如果已经开始定位，则先停止定位
                    App.LocationClient.stopLocation()
                }
                App.LocationClient.startLocation()
                isRunning = true
            } else if (!SettingUtils.enableLocation && App.LocationClient.isStarted()) {
                Log.d(TAG, "stopLocation")
                App.LocationClient.stopLocation()
                isRunning = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
        }
    }

    private fun stopForegroundService() {
        isRunning = try {
            //如果已经开始定位，则先停止定位
            if (SettingUtils.enableLocation && App.LocationClient.isStarted()) {
                App.LocationClient.stopLocation()
            }
            stopForeground(true)
            stopSelf()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

}