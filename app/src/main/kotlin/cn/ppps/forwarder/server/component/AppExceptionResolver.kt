package cn.ppps.forwarder.server.component

import cn.ppps.forwarder.utils.Base64
import cn.ppps.forwarder.utils.HTTP_FAILURE_CODE
import cn.ppps.forwarder.utils.HTTP_FORBIDDEN_CODE
import cn.ppps.forwarder.utils.HTTP_UNAUTHORIZED_CODE
import cn.ppps.forwarder.utils.HttpServerUtils
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.RSACrypt
import cn.ppps.forwarder.utils.SM4Crypt
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

        //鉴权失败返回 401、功能未开启返回 403，其余业务异常沿用 http 200 以兼容历史客户端
        val statusCode = if (e is HttpException) e.statusCode else HTTP_FAILURE_CODE
        val httpStatus = when (statusCode) {
            HTTP_UNAUTHORIZED_CODE -> StatusCode.SC_UNAUTHORIZED
            HTTP_FORBIDDEN_CODE -> StatusCode.SC_FORBIDDEN
            else -> StatusCode.SC_OK
        }
        response.status = httpStatus

        //返回统一结构报文
        var resp = HttpServerUtils.response(e.message.toString(), statusCode)
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