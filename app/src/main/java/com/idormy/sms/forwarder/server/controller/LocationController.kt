package com.idormy.sms.forwarder.server.controller

import android.annotation.SuppressLint
import com.idormy.sms.forwarder.entity.LocationInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.EmptyData
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.yanzhenjie.andserver.annotation.CrossOrigin
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.RequestMethod
import com.yanzhenjie.andserver.annotation.RestController

@SuppressLint("SimpleDateFormat")
@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/location"])
class LocationController {

    private val TAG: String = LocationController::class.java.simpleName

    //远程找手机
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<EmptyData>): LocationInfo {
        Log.d(TAG, bean.data.toString())
        return HttpServerUtils.apiLocationCache
    }

}