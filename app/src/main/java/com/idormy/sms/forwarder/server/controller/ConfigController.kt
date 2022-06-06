package com.idormy.sms.forwarder.server.controller

import android.util.Log
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.ConfigData
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.yanzhenjie.andserver.annotation.*

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/config"])
class ConfigController {

    private val TAG: String = CloneController::class.java.simpleName

    //远程查配置
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/query")
    fun test(@RequestBody bean: BaseRequest<*>): ConfigData {
        Log.d(TAG, bean.data.toString())

        return ConfigData(
            HttpServerUtils.enableApiClone,
            HttpServerUtils.enableApiSmsSend,
            HttpServerUtils.enableApiSmsQuery,
            HttpServerUtils.enableApiCallQuery,
            HttpServerUtils.enableApiContactQuery,
            HttpServerUtils.enableApiBatteryQuery,
        )
    }

}