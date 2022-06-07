package com.idormy.sms.forwarder.server.component

import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.yanzhenjie.andserver.annotation.Resolver
import com.yanzhenjie.andserver.error.HttpException
import com.yanzhenjie.andserver.framework.ExceptionResolver
import com.yanzhenjie.andserver.framework.body.JsonBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.StatusCode

@Resolver
class AppExceptionResolver : ExceptionResolver {

    override fun onResolve(request: HttpRequest, response: HttpResponse, e: Throwable) {
        e.printStackTrace()
        if (e is HttpException) {
            //response.status = e.statusCode
            //异常捕获返回 http 200
            response.status = StatusCode.SC_OK
        } else {
            response.status = StatusCode.SC_INTERNAL_SERVER_ERROR
        }

        //返回统一结构报文
        response.setBody(JsonBody(HttpServerUtils.response(e.message.toString())))
    }

}