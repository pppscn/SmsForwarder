package com.idormy.sms.forwarder.utils.interceptor

import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.xuexiang.xhttp2.interceptor.HttpLoggingInterceptor
import com.xuexiang.xhttp2.utils.HttpUtils
import com.xuexiang.xutil.data.DateUtils
import okhttp3.Connection
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http.HttpHeaders
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("PrivatePropertyName")
class LoggingInterceptor(private val logId: Long) : HttpLoggingInterceptor("custom") {

    private val TAG: String = LoggingInterceptor::class.java.simpleName

    init {
        level = if (App.isDebug) Level.BODY else Level.PARAM
    }

    override fun log(message: String) {
        Log.d(TAG, message)
        //状态=-1，不更新原状态
        SendUtils.updateLogs(logId, -1, message)
    }

    /**
     * 记录请求日志
     *
     * @param request
     * @param connection
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun logForRequest(request: Request, connection: Connection?) {
        if (level != Level.PARAM) {
            log("------REQUEST------" + "\nAt " + DateUtils.getNowString(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())))
        }
        val logBody = level == Level.BODY || level == Level.PARAM
        val logHeaders = level == Level.BODY || level == Level.HEADERS
        val requestBody = request.body()
        val hasRequestBody = requestBody != null
        val protocol = connection?.protocol() ?: Protocol.HTTP_1_1

        try {
            val requestStartMessage = "--> ${request.method()} ${request.url()} $protocol"
            log(requestStartMessage)

            if (logHeaders) {
                val headers = request.headers()
                for (i in 0 until headers.size()) {
                    log("\t${headers.name(i)}: ${headers.value(i)}")
                }
            }

            if (logBody && hasRequestBody) {
                if (HttpUtils.isPlaintext(requestBody?.contentType())) {
                    log("\tbody:" + bodyToString(request))
                } else {
                    log("\tbody: maybe [file part] , too large too print , ignored!")
                }
            }
        } catch (e: Exception) {
            onError(e)
        } finally {
            if (level != Level.PARAM) {
                log("--> END ${request.method()}")
            }
        }
    }

    /**
     * 记录响应日志
     *
     * @param response
     * @param tookMs   请求花费的时间
     * @return
     */
    override fun logForResponse(response: Response, tookMs: Long): Response {
        if (level != Level.PARAM) {
            log("------RESPONSE------" + "\nAt " + DateUtils.getNowString(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())))
        }
        val builder = response.newBuilder()
        val clone = builder.build()
        var responseBody = clone.body()
        val logBody = level == Level.BODY || level == Level.PARAM
        val logHeaders = level == Level.BODY || level == Level.HEADERS

        try {
            log("<-- ${clone.code()} ${clone.message()} ${clone.request().url()} ($tookMs ms）")
            if (logHeaders) {
                log(" ")
                val headers = clone.headers()
                for (i in 0 until headers.size()) {
                    log("\t${headers.name(i)}: ${headers.value(i)}")
                }
                log(" ")
            }

            if (logBody && HttpHeaders.hasBody(clone)) {
                if (HttpUtils.isPlaintext(responseBody?.contentType())) {
                    val body = responseBody?.string()
                    log("\tbody:$body")
                    responseBody = ResponseBody.create(responseBody?.contentType(), body ?: "")
                    return response.newBuilder().body(responseBody).build()
                } else {
                    log("\tbody: maybe [file part] , too large too print , ignored!")
                }
                if (level != Level.PARAM) {
                    log(" ")
                }
            }
        } catch (e: Exception) {
            onError(e)
        } finally {
            if (level != Level.PARAM) {
                log("<-- END HTTP")
            }
        }
        return response
    }

}