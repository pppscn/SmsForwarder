package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.utils.HTTP_SERVER_PORT
import com.idormy.sms.forwarder.utils.HTTP_SERVER_TIME_OUT
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.king.location.LocationClient
import com.king.location.LocationErrorCode
import com.king.location.OnExceptionListener
import com.king.location.OnLocationListener
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("SimpleDateFormat")
@Suppress("PrivatePropertyName", "DEPRECATION")
class HttpService : Service(), Server.ServerListener {

    private val TAG: String = "HttpService"
    private val server by lazy {
        AndServer.webServer(this).port(HTTP_SERVER_PORT).listener(this).timeout(HTTP_SERVER_TIME_OUT, TimeUnit.SECONDS).build()
    }
    private val locationClient by lazy { LocationClient(App.context) }
    private val geocoder by lazy { Geocoder(App.context) }
    private val simpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss") }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        Log.i(TAG, "onCreate: ")
        server.startup()

        //远程找手机
        if (HttpServerUtils.enableApiLocation) {
            //可根据具体需求设置定位配置参数（这里只列出一些主要的参数）
            val locationOption = locationClient.getLocationOption().setAccuracy(Criteria.ACCURACY_FINE)//设置位置精度：高精度
                .setPowerRequirement(Criteria.POWER_LOW) //设置电量消耗：低电耗
                .setMinTime(10000)//设置位置更新最小时间间隔（单位：毫秒）； 默认间隔：10000毫秒，最小间隔：1000毫秒
                .setMinDistance(0)//设置位置更新最小距离（单位：米）；默认距离：0米
                .setOnceLocation(false)//设置是否只定位一次，默认为 false，当设置为 true 时，则只定位一次后，会自动停止定位
                .setLastKnownLocation(true)//设置是否获取最后一次缓存的已知位置，默认为 true
            //设置定位配置参数
            locationClient.setLocationOption(locationOption)
            locationClient.startLocation()

            //设置定位监听
            locationClient.setOnLocationListener(object : OnLocationListener() {
                override fun onLocationChanged(location: Location) {
                    //位置信息
                    Log.d(TAG, "onLocationChanged(location = ${location})")

                    val locationInfo = LocationInfo(
                        location.longitude,
                        location.latitude,
                        "",
                        simpleDateFormat.format(Date(location.time)),
                        location.provider.toString()
                    )

                    //根据坐标经纬度获取位置地址信息（WGS-84坐标系）
                    val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (list?.isNotEmpty() == true) {
                        locationInfo.address = list[0].getAddressLine(0)
                    }

                    Log.d(TAG, "locationInfo = $locationInfo")
                    HttpServerUtils.apiLocationCache = locationInfo
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
            locationClient.setOnExceptionListener(object : OnExceptionListener {
                override fun onException(@LocationErrorCode errorCode: Int, e: Exception) {
                    //定位出现异常
                    Log.w(TAG, "onException(errorCode = ${errorCode}, e = ${e})")
                }
            })

            if (locationClient.isStarted()) {//如果已经开始定位，则先停止定位
                locationClient.stopLocation()
            }
            locationClient.startLocation()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        Log.i(TAG, "onDestroy: ")
        server.shutdown()

        if (HttpServerUtils.enableApiLocation && locationClient.isStarted()) {//如果已经开始定位，则先停止定位
            locationClient.stopLocation()
        }
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
}