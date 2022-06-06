package com.idormy.sms.forwarder.utils.sender

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException

class RetryInterceptor internal constructor(builder: Builder) : Interceptor {
    //重试的间隔
    private val retryInterval: Long

    //更新记录ID
    private val logId: Long

    //最大重试次数
    private val executionCount: Int

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var retryTimes = 0
        val request = chain.request()
        var response: Response
        do {
            if (retryTimes > 0 && retryInterval > 0) {
                val delayTime = retryTimes * retryInterval
                try {
                    Log.w(TAG, "第 $retryTimes 次重试，休眠 $delayTime 秒")
                    Thread.sleep(delayTime * 1000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException(e.message)
                }
            }
            response = doRequest(chain, request, retryTimes)!!
            retryTimes++
        } while ((!response.isSuccessful) && retryTimes <= executionCount)

        return response
    }

    private fun doRequest(chain: Interceptor.Chain, request: Request, retryTimes: Int): Response? {
        var response: Response? = null
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val resp = if (retryTimes > 0) "第" + retryTimes + "次重试：" + e.message else e.message!!
            //LogUtils.updateLog(logId, 1, resp);
            Log.w(TAG, resp)
        }
        return response
    }

    class Builder {
        var executionCount = 3
        var retryInterval: Long = 1000
        var logId: Long = 0
        fun executionCount(executionCount: Int): Builder {
            this.executionCount = executionCount
            return this
        }

        fun retryInterval(retryInterval: Long): Builder {
            this.retryInterval = retryInterval
            return this
        }

        fun logId(logId: Long): Builder {
            this.logId = logId
            return this
        }

        fun build(): RetryInterceptor {
            return RetryInterceptor(this)
        }
    }

    companion object {
        const val TAG = "RetryInterceptor"
    }

    init {
        executionCount = builder.executionCount
        retryInterval = builder.retryInterval
        logId = builder.logId
    }
}