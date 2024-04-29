package com.idormy.sms.forwarder.server.component

import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.RSACrypt
import com.idormy.sms.forwarder.utils.SM4Crypt
import com.xuexiang.xutil.data.ConvertTools
import com.yanzhenjie.andserver.annotation.Resolver
import com.yanzhenjie.andserver.error.HttpException
import com.yanzhenjie.andserver.framework.ExceptionResolver
import com.yanzhenjie.andserver.framework.body.JsonBody
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.StatusCode

@Suppress("PrivatePropertyName")
@Resolver
class AppExceptionResolver : ExceptionResolver {

    private val TAG: String = "AppExceptionResolver"

    override fun onResolve(request: HttpRequest, response: HttpResponse, e: Throwable) {
        Log.e(TAG, "onResolve: ${e.message}")
        if (e is HttpException) {
            //response.status = e.statusCode
            //异常捕获返回 http 200
            response.status = StatusCode.SC_OK
        } else {
            response.status = StatusCode.SC_INTERNAL_SERVER_ERROR
        }

        //返回统一结构报文
        var resp = HttpServerUtils.response(e.message.toString())
        Log.d(TAG, "resp: $resp")
        when (HttpServerUtils.safetyMeasures) {
            2 -> {
                val privateKey = RSACrypt.getPrivateKey(HttpServerUtils.serverPrivateKey)
                resp = Base64.encode(resp.toByteArray())
                resp = RSACrypt.encryptByPrivateKey(resp, privateKey)
                response.setBody(StringBody(resp))
            }

            3 -> {
                val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.serverSm4Key)
                //response = Base64.encode(response.toByteArray())
                val encryptCBC = SM4Crypt.encrypt(resp.toByteArray(), sm4Key)
                response.setBody(StringBody(ConvertTools.bytes2HexString(encryptCBC)))
            }

            else -> {
                response.setBody(JsonBody(resp))
            }
        }
    }

}