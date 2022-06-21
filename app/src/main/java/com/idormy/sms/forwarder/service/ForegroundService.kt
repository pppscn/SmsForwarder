package com.idormy.sms.forwarder.service

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.utils.*
import com.jeremyliao.liveeventbus.LiveEventBus
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

@Suppress("PrivatePropertyName", "DeferredResultUnused", "OPT_IN_USAGE")
class ForegroundService : Service() {
    private val TAG: String = "ForegroundService"
    private val compositeDisposable = CompositeDisposable()
    private val frpcObserver = Observer { uid: String ->
        if (Frpclib.isRunning(uid)) {
            return@Observer
        }
        AppDatabase.getInstance(App.context)
            .frpcDao()
            .get(uid)
            .flatMap { (uid1, _, config) ->
                val error = Frpclib.runContent(uid1, config)
                Single.just(error)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<String> {
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
    private var notificationManager: NotificationManager? = null

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()

        try {
            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            startForeground(FRONT_NOTIFY_ID, createForegroundNotification())

            //开关通知监听服务
            if (SettingUtils.enableAppNotify && CommonUtils.isNotificationListenerServiceEnabled(this)) {
                CommonUtils.toggleNotificationListenerService(this)
            }

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

            isRunning = true
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        //纯客户端模式
        if (SettingUtils.enablePureClientMode) {
            super.onDestroy()
            return
        }

        try {
            stopForeground(true)
            compositeDisposable.dispose()
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createForegroundNotification(): Notification {

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
        val builder = NotificationCompat.Builder(this, FRONT_CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_forwarder)
        builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_menu_frpc))
        // TODO: 部分机型标题会重复待排除
        // if (DeviceUtils.getDeviceBrand().contains("Xiaomi")) {
        builder.setContentTitle(getString(R.string.app_name))
        //}
        builder.setContentText(SettingUtils.notifyContent.toString())
        builder.setWhen(System.currentTimeMillis())
        val activityIntent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= 30) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, flags)
        builder.setContentIntent(pendingIntent)
        return builder.build()
    }

}