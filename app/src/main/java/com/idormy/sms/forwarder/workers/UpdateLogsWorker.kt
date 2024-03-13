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

class UpdateLogsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sendResponseJson = inputData.getString(Worker.UPDATE_LOGS)
            Log.d("UpdateLogsWorker", "UpdateLogsWorker sendResponseJson: $sendResponseJson")
            val sendResponse = Gson().fromJson(sendResponseJson, SendResponse::class.java)
            if (sendResponse.logId == 0L) {
                Log.e("UpdateLogsWorker", "UpdateLogsWorker error: logId is 0")
                return@withContext Result.failure()
            }
            if (sendResponse.status >= 0) {
                val response = sendResponse.response + "\nAt " + DateUtils.getNowString(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()))
                Thread.sleep(100) //让status=-1的日志先更新
                Core.logs.updateStatus(sendResponse.logId, sendResponse.status, response)
            } else {
                Core.logs.updateResponse(sendResponse.logId, sendResponse.response)
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UpdateLogsWorker", "UpdateLogsWorker error: ${e.message}")
            return@withContext Result.failure()
        }
    }

}