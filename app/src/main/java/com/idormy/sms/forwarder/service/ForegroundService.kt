package com.idormy.sms.forwarder.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.utils.*
import com.idormy.sms.forwarder.utils.task.CronJobScheduler
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
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

@SuppressLint("SimpleDateFormat")
@Suppress("PrivatePropertyName", "DeferredResultUnused", "OPT_IN_USAGE", "DEPRECATION", "LiftReturnOrAssignment")
class ForegroundService : Service() {

    private val TAG: String = ForegroundService::class.java.simpleName
    private var notificationManager: NotificationManager? = null

    private val compositeDisposable = CompositeDisposable()
    private val frpcObserver = Observer { uid: String ->
        if (Frpclib.isRunning(uid)) {
            return@Observer
        }
        Core.frpc.get(uid).flatMap { (uid1, _, config) ->
            val error = Frpclib.runContent(uid1, config)
            Single.just(error)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<String> {
            override fun onSubscribe(d: Disposable) {
                compositeDisposable.add(d)
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "onError: ${e.message}")
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
                val taskList = Core.task.getByType(TASK_CONDITION_CRON)
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
                    val frpcList = Core.frpc.getAutorun()

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
            Log.e(TAG, "startForegroundService: $e")
            isRunning = false
        }

    }

    private fun stopForegroundService() {
        try {
            stopForeground(true)
            stopSelf()
            compositeDisposable.dispose()
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "stopForegroundService: $e")
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