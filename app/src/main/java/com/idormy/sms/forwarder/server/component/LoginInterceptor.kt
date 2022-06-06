package com.idormy.sms.forwarder.server.component

import com.yanzhenjie.andserver.error.HttpException
import com.yanzhenjie.andserver.framework.HandlerInterceptor
import com.yanzhenjie.andserver.framework.handler.MethodHandler
import com.yanzhenjie.andserver.framework.handler.RequestHandler
import com.yanzhenjie.andserver.framework.mapping.Addition
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse

//@Interceptor
class LoginInterceptor : HandlerInterceptor {
    override fun onIntercept(
        request: HttpRequest,
        response: HttpResponse,
        handler: RequestHandler,
    ): Boolean {
        if (handler is MethodHandler) {
            val methodHandler: MethodHandler = handler
            val addition: Addition = methodHandler.addition
            if (!isLogin(request, addition)) {
                throw HttpException(401, "You are not logged in yet.")
            }
        }
        return false
    }

    private fun isNeedLogin(addition: Addition?): Boolean {
        if (addition == null) {
            return false
        }
        val stringType = addition.stringType
        if (stringType.isEmpty()) {
            return false
        }
        val booleanType = addition.booleanType
        return if (booleanType.isEmpty()) {
            false
        } else stringType[0].equals("login", ignoreCase = true) && booleanType[0]
    }

    private fun isLogin(request: HttpRequest, addition: Addition): Boolean {
        if (isNeedLogin(addition)) {
            val session = request.session
            if (session != null) {
                val o = session.getAttribute(LOGIN_ATTRIBUTE)
                return o is Boolean && o
            }
            return false
        }
        return true
    }

    companion object {
        const val LOGIN_ATTRIBUTE = "USER.LOGIN.SIGN"
    }
}