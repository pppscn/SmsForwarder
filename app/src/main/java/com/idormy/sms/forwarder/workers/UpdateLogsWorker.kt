package com.idormy.sms.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.result.SendResponse
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.Worker
import com.xuexiang.xutil.data.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class UpdateLogsWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sendResponseJson = inputData.getString(Worker.updateLogs)
            val sendResponse = Gson().fromJson(sendResponseJson, SendResponse::class.java)
            Core.logs.updateStatus(sendResponse.logId, sendResponse.status, sendResponse.response + "\nAt " + DateUtils.getNowString(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())))
            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UpdateLogsWorker", "UpdateLogsWorker error: ${e.message}")
            return@withContext Result.failure()
        }
    }

}