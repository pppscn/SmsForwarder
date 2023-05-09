package com.idormy.sms.forwarder

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.gyf.cactus.Cactus
import com.gyf.cactus.callback.CactusCallback
import com.gyf.cactus.ext.cactus
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.repository.*
import com.idormy.sms.forwarder.entity.SimInfo
import com.idormy.sms.forwarder.receiver.CactusReceiver
import com.idormy.sms.forwarder.service.BatteryService
import com.idormy.sms.forwarder.service.ForegroundService
import com.idormy.sms.forwarder.service.HttpService
import com.idormy.sms.forwarder.service.NetworkStateService
import com.idormy.sms.forwarder.utils.*
import com.idormy.sms.forwarder.utils.sdkinit.UMengInit
import com.idormy.sms.forwarder.utils.sdkinit.XBasicLibInit
import com.idormy.sms.forwarder.utils.sdkinit.XUpdateInit
import com.idormy.sms.forwarder.utils.tinker.TinkerLoadLibrary
import com.xuexiang.xutil.app.AppUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class App : Application(), CactusCallback, Configuration.Provider by Core {

    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppDatabase.getInstance(this) }
    val frpcRepository by lazy { FrpcRepository(database.frpcDao()) }
    val msgRepository by lazy { MsgRepository(database.msgDao()) }
    val logsRepository by lazy { LogsRepository(database.logsDao()) }
    val ruleRepository by lazy { RuleRepository(database.ruleDao()) }
    val senderRepository by lazy { SenderRepository(database.senderDao()) }

    companion object {
        const val TAG: String = "SmsForwarder"

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        //已插入SIM卡信息
        var SimInfoList: MutableMap<Int, SimInfo> = mutableMapOf()

        //已安装App信息
        var LoadingAppList = false
        var UserAppList: MutableList<AppUtils.AppInfo> = mutableListOf()
        var SystemAppList: MutableList<AppUtils.AppInfo> = mutableListOf()

        /**
         * @return 当前app是否是调试开发模式
         */
        val isDebug: Boolean
            get() = BuildConfig.DEBUG

        //Cactus结束时间
        val mEndDate = MutableLiveData<String>()

        //Cactus上次存活时间
        val mLastTimer = MutableLiveData<String>()

        //Cactus存活时间
        val mTimer = MutableLiveData<String>()

        //Cactus运行状态
        val mStatus = MutableLiveData<Boolean>().apply { value = true }

        var mDisposable: Disposable? = null
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        //解决4.x运行崩溃的问题
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            context = applicationContext
            initLibs()

            //纯客户端模式
            if (SettingUtils.enablePureClientMode) return

            //动态加载FrpcLib
            val libPath = filesDir.absolutePath + "/libs"
            val soFile = File(libPath)
            if (soFile.exists()) {
                try {
                    TinkerLoadLibrary.installNativeLibraryPath(classLoader, soFile)
                } catch (throwable: Throwable) {
                    Log.e("APP", throwable.message.toString())
                }
            }

            //启动前台服务
            Intent(this, ForegroundService::class.java).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(it)
                } else {
                    startService(it)
                }
            }

            //网络状态监听
            Intent(this, NetworkStateService::class.java).also {
                startService(it)
            }

            //电池状态监听
            Intent(this, BatteryService::class.java).also {
                startService(it)
            }

            //启动HttpServer
            if (HttpServerUtils.enableServerAutorun) {
                Intent(this, HttpService::class.java).also {
                    startService(it)
                }
            }

            //Cactus 集成双进程前台服务，JobScheduler，onePix(一像素)，WorkManager，无声音乐
            if (SettingUtils.enableCactus) {
                //注册广播监听器
                registerReceiver(CactusReceiver(), IntentFilter().apply {
                    addAction(Cactus.CACTUS_WORK)
                    addAction(Cactus.CACTUS_STOP)
                    addAction(Cactus.CACTUS_BACKGROUND)
                    addAction(Cactus.CACTUS_FOREGROUND)
                })
                //设置通知栏点击事件
                val activityIntent = Intent(this, MainActivity::class.java)
                val flags = if (Build.VERSION.SDK_INT >= 30) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, flags)
                cactus {
                    setServiceId(FRONT_NOTIFY_ID) //服务Id
                    setChannelId(FRONT_CHANNEL_ID) //渠道Id
                    setChannelName(FRONT_CHANNEL_NAME) //渠道名
                    setTitle(getString(R.string.app_name))
                    setContent(SettingUtils.notifyContent)
                    setSmallIcon(R.drawable.ic_forwarder)
                    setLargeIcon(R.mipmap.ic_launcher)
                    setPendingIntent(pendingIntent)
                    //无声音乐
                    if (SettingUtils.enablePlaySilenceMusic) {
                        setMusicEnabled(true)
                        setBackgroundMusicEnabled(true)
                        setMusicId(R.raw.silence)
                        //设置音乐间隔时间，时间间隔越长，越省电
                        setMusicInterval(10)
                        isDebug(true)
                    }
                    //是否可以使用一像素，默认可以使用，只有在android p以下可以使用
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && SettingUtils.enableOnePixelActivity) {
                        setOnePixEnabled(true)
                    }
                    //奔溃是否可以重启用户界面
                    setCrashRestartUIEnabled(true)
                    addCallback({
                        Log.d(TAG, "Cactus保活：onStop回调")
                    }) {
                        Log.d(TAG, "Cactus保活：doWork回调")
                    }
                    //切后台切换回调
                    addBackgroundCallback {
                        Log.d(TAG, if (it) "SmsForwarder 切换到后台运行" else "SmsForwarder 切换到前台运行")
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化基础库
     */
    private fun initLibs() {
        Core.init(this)
        // 配置文件初始化
        SharedPreference.init(applicationContext)
        // 转发历史工具类初始化
        HistoryUtils.init(applicationContext)
        // X系列基础库初始化
        XBasicLibInit.init(this)
        // 版本更新初始化
        XUpdateInit.init(this)
        // 运营统计数据
        UMengInit.init(this)
    }

    @SuppressLint("CheckResult")
    override fun doWork(times: Int) {
        Log.d(TAG, "doWork:$times")
        mStatus.postValue(true)
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+00:00")
        var oldTimer = CactusSave.timer
        if (times == 1) {
            CactusSave.lastTimer = oldTimer
            CactusSave.endDate = CactusSave.date
            oldTimer = 0L
        }
        mLastTimer.postValue(dateFormat.format(Date(CactusSave.lastTimer * 1000)))
        mEndDate.postValue(CactusSave.endDate)
        mDisposable = Observable.interval(1, TimeUnit.SECONDS).map {
            oldTimer + it
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { aLong ->
            CactusSave.timer = aLong
            CactusSave.date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).run {
                format(Date())
            }
            mTimer.value = dateFormat.format(Date(aLong * 1000))
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        mStatus.postValue(false)
        mDisposable?.apply {
            if (!isDisposed) {
                dispose()
            }
        }
    }

}