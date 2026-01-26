package cn.ppps.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.ppps.forwarder.App
import cn.ppps.forwarder.utils.AppUtils
import cn.ppps.forwarder.utils.EVENT_LOAD_APP_LIST
import cn.ppps.forwarder.utils.Log
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("PrivatePropertyName")
class LoadAppListWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = LoadAppListWorker::class.java.simpleName

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (App.LoadingAppList) {
            Log.d(TAG, "LoadingAppList is true, return")
            return@withContext Result.success()
        }

        try {
            App.LoadingAppList = true
            App.UserAppList.clear()
            App.SystemAppList.clear()
            val appInfoList = AppUtils.getAppsInfo()
            for (appInfo in appInfoList) {
                if (appInfo.isSystem) {
                    App.SystemAppList.add(appInfo)
                } else {
                    App.UserAppList.add(appInfo)
                }
            }
            App.UserAppList.sortBy { appInfo -> appInfo.name }
            App.SystemAppList.sortBy { appInfo -> appInfo.name }

            LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).post("finish")
            App.LoadingAppList = false
            Log.d(TAG, "LoadAppListWorker finish")

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "LoadAppListWorker error: ${e.message}")
            return@withContext Result.failure()
        }
    }

}