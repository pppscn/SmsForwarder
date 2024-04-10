package com.idormy.sms.forwarder.utils.interceptor

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.entity.result.SendResponse
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.Worker
import com.idormy.sms.forwarder.workers.UpdateLogsWorker
import com.xuexiang.xutil.XUtil
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName")
class NoContentInterceptor(private val logId: Long) : Interceptor {

    private val TAG: String = NoContentInterceptor::class.java.simpleName

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        //HTTP Status 201-299 都算成功
        if (originalResponse.code() in 201..299) {
            val response = "HTTP Status " + originalResponse.code() + " " + originalResponse.message()
            Log.d(TAG, response)
            /*
            // 创建一个空的响应体
            val message = "{\"Code\":0, \"Msg\":\"\", \"Data\":{}}"
            val emptyJsonBody = ResponseBody.create(MediaType.parse("application/json"), message)
            // 使用新的响应体替换原始响应中的响应体
            return originalResponse.newBuilder()
                .body(emptyJsonBody)
                .header("Content-Length", message.length.toString())
                .build()
            */
            //TODO: 暂时特殊处理，更新日志状态为成功
            val sendResponse = SendResponse(logId, 2, response)
            val request = OneTimeWorkRequestBuilder<UpdateLogsWorker>()
                .setInitialDelay(200, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        Worker.UPDATE_LOGS to Gson().toJson(sendResponse)
                    )
                ).build()
            WorkManager.getInstance(XUtil.getContext()).enqueue(request)
        }

        return originalResponse
    }
}

