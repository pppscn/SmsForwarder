package com.idormy.sms.forwarder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.app.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadAppListWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (App.LoadingAppList) {
            Log.d("LoadAppListWorker", "LoadingAppList is true, return")
            return@withContext Result.success()
        }

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
        Log.d("LoadAppListWorker", "LoadAppListWorker finish, App.LoadingAppList=${App.LoadingAppList}")

        return@withContext Result.success()
    }

}