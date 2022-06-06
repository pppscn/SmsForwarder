package com.idormy.sms.forwarder.core

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.BuildConfig
import com.idormy.sms.forwarder.database.repository.FrpcRepository
import com.idormy.sms.forwarder.database.repository.LogsRepository
import com.idormy.sms.forwarder.database.repository.RuleRepository
import com.idormy.sms.forwarder.database.repository.SenderRepository
import com.idormy.sms.forwarder.service.ForegroundService
import kotlinx.coroutines.launch

object Core : Configuration.Provider {
    lateinit var app: Application
    val frpc: FrpcRepository by lazy { (app as App).frpcRepository }
    val logs: LogsRepository by lazy { (app as App).logsRepository }
    val rule: RuleRepository by lazy { (app as App).ruleRepository }
    val sender: SenderRepository by lazy { (app as App).senderRepository }
    /*
    val telephonyManager: TelephonyManager by lazy { app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
    val smsManager: SmsManager by lazy { app.getSystemService(SmsManager::class.java) }
    val subscriptionManager: SubscriptionManager by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            SubscriptionManager.from(app)
        } else {
            app.getSystemService(SubscriptionManager::class.java)
        }
    }
    val user by lazy { app.getSystemService<UserManager>()!! }*/


    /*val directBootAware: Boolean get() = directBootSupported && dataStore.canToggleLocked
    val directBootSupported by lazy {
        Build.VERSION.SDK_INT >= 24 && try {
            app.getSystemService<DevicePolicyManager>()?.storageEncryptionStatus ==
                    DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
        } catch (_: RuntimeException) {
            false
        }
    }*/

    fun init(app: Application) {
        this.app = app
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().apply {
            setDefaultProcessName(app.packageName + ":bg")
            setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.INFO)
            setExecutor { (app as App).applicationScope.launch { it.run() } }
            setTaskExecutor { (app as App).applicationScope.launch { it.run() } }
        }.build()
    }

    fun startService() = ContextCompat.startForegroundService(app, Intent(app, ForegroundService::class.java))
}
