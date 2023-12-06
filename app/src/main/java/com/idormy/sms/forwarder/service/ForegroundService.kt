package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.utils.*
import com.idormy.sms.forwarder.utils.task.CronJobScheduler
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.location.LocationClient
import com.king.location.LocationErrorCode
import com.king.location.OnExceptionListener
import com.king.location.OnLocationListener
import com.xuexiang.xaop.util.PermissionUtils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.file.FileUtils
import frpclib.Frpclib
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
@Suppress("PrivatePropertyName", "DeferredResultUnused", "OPT_IN_USAGE", "DEPRECATION", "LiftReturnOrAssignment")
class ForegroundService : Service() {

    private val TAG: String = "ForegroundService"
    private var notificationManager: NotificationManager? = null

    private val compositeDisposable = CompositeDisposable()
    private val frpcObserver = Observer { uid: String ->
        if (Frpclib.isRunning(uid)) {
            return@Observer
        }
        AppDatabase.getInstance(App.context).frpcDao().get(uid).flatMap { (uid1, _, config) ->
            val error = Frpclib.runContent(uid1, config)
            Single.just(error)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<String> {
            override fun onSubscribe(d: Disposable) {
                compositeDisposable.add(d)
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                LiveEventBus.get(EVENT_FRPC_RUNNING_ERROR, String::class.java).post(uid)
            }

            override fun onSuccess(msg: String) {
                if (!TextUtils.isEmpty(msg)) {
                    Log.e(TAG, msg)
                    LiveEventBus.get(EVENT_FRPC_RUNNING_ERROR, String::class.java).post(uid)
                } else {
                    LiveEventBus.get(EVENT_FRPC_RUNNING_SUCCESS, String::class.java).post(uid)
                }
            }
        })
    }

    private val locationClient by lazy { LocationClient(App.context) }
    private val geocoder by lazy { Geocoder(App.context) }
    private val simpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss") }

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return START_STICKY

        if (intent != null) {
            when (intent.action) {
                "START" -> {
                    startForegroundService()
                }

                "STOP" -> {
                    stopForegroundService()
                }

                "UPDATE_NOTIFICATION" -> {
                    val updatedContent = intent.getStringExtra("UPDATED_CONTENT")
                    updateNotification(updatedContent ?: "")
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        //非纯客户端模式
        if (!SettingUtils.enablePureClientMode) stopForegroundService()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startForegroundService() {
        val notification = createNotification(SettingUtils.notifyContent)
        startForeground(NOTIFICATION_ID, notification)

        try {
            //开关通知监听服务
            if (SettingUtils.enableAppNotify && CommonUtils.isNotificationListenerServiceEnabled(this)) {
                CommonUtils.toggleNotificationListenerService(this)
            }

            //启动定时任务
            GlobalScope.async(Dispatchers.IO) {
                val taskList = AppDatabase.getInstance(App.context).taskDao().getByType(TASK_CONDITION_CRON)
                taskList.forEach { task ->
                    Log.d(TAG, "task = $task")
                    CronJobScheduler.cancelTask(task.id)
                    CronJobScheduler.scheduleTask(task)
                }
            }

            //异步获取所有已安装 App 信息
            if (SettingUtils.enableLoadAppList) {
                val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                WorkManager.getInstance(XUtil.getContext()).enqueue(request)
            }

            //启动 Frpc
            if (FileUtils.isFileExists(filesDir.absolutePath + "/libs/libgojni.so")) {
                //监听Frpc启动指令
                LiveEventBus.get(INTENT_FRPC_APPLY_FILE, String::class.java).observeStickyForever(frpcObserver)
                //自启动的Frpc
                GlobalScope.async(Dispatchers.IO) {
                    val frpcList = AppDatabase.getInstance(App.context).frpcDao().getAutorun()

                    if (frpcList.isEmpty()) {
                        Log.d(TAG, "没有自启动的Frpc")
                        return@async
                    }

                    for (frpc in frpcList) {
                        val error = Frpclib.runContent(frpc.uid, frpc.config)
                        if (!TextUtils.isEmpty(error)) {
                            Log.e(TAG, error)
                        }
                    }
                }
            }

            //远程找手机 TODO: 判断权限 ACCESS_COARSE_LOCATION ACCESS_FINE_LOCATION
            if ((SettingUtils.enableLocationTag || HttpServerUtils.enableApiLocation)
                && PermissionUtils.isGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                //可根据具体需求设置定位配置参数（这里只列出一些主要的参数）
                val locationOption = locationClient.getLocationOption().setAccuracy(Criteria.ACCURACY_FINE)//设置位置精度：高精度
                    .setPowerRequirement(Criteria.POWER_LOW) //设置电量消耗：低电耗
                    .setMinTime(10000)//设置位置更新最小时间间隔（单位：毫秒）； 默认间隔：10000毫秒，最小间隔：1000毫秒
                    .setMinDistance(0)//设置位置更新最小距离（单位：米）；默认距离：0米
                    .setOnceLocation(false)//设置是否只定位一次，默认为 false，当设置为 true 时，则只定位一次后，会自动停止定位
                    .setLastKnownLocation(false)//设置是否获取最后一次缓存的已知位置，默认为 true
                //设置定位配置参数
                locationClient.setLocationOption(locationOption)
                locationClient.startLocation()

                //设置定位监听
                locationClient.setOnLocationListener(object : OnLocationListener() {
                    override fun onLocationChanged(location: Location) {
                        //位置信息
                        Log.d(TAG, "onLocationChanged(location = ${location})")

                        val locationInfo = LocationInfo(
                            location.longitude, location.latitude, "", simpleDateFormat.format(Date(location.time)), location.provider.toString()
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
            } else if ((!SettingUtils.enableLocationTag && !HttpServerUtils.enableApiLocation) && locationClient.isStarted()) {
                Log.d(TAG, "stopLocation")
                locationClient.stopLocation()
            }

            isRunning = true
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
        }

    }

    private fun stopForegroundService() {
        try {
            //如果已经开始定位，则先停止定位
            if ((SettingUtils.enableLocationTag || HttpServerUtils.enableApiLocation) && locationClient.isStarted()) {
                locationClient.stopLocation()
            }

            stopForeground(true)
            stopSelf()
            compositeDisposable.dispose()
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = true
        }
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(FRONT_CHANNEL_ID, FRONT_CHANNEL_NAME, importance)
            notificationChannel.description = "Frpc Foreground Service"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            if (notificationManager != null) {
                notificationManager!!.createNotificationChannel(notificationChannel)
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= 30) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags)

        return NotificationCompat.Builder(this, FRONT_CHANNEL_ID).setContentTitle(getString(R.string.app_name)).setContentText(content).setSmallIcon(R.drawable.ic_forwarder).setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_menu_frpc)).setContentIntent(pendingIntent).setWhen(System.currentTimeMillis()).build()
    }

    private fun updateNotification(updatedContent: String) {
        val notification = createNotification(updatedContent)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

}