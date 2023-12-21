package com.idormy.sms.forwarder.server.controller

import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.CallQueryData
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.yanzhenjie.andserver.annotation.*

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/call"])
class CallController {

    private val TAG: String = CallController::class.java.simpleName

    //远程查通话
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<CallQueryData>): List<CallInfo> {
        val callQueryData = bean.data
        Log.d(TAG, callQueryData.toString())

        val limit = callQueryData.pageSize
        val offset = (callQueryData.pageNum - 1) * limit
        return PhoneUtils.getCallInfoList(callQueryData.type, limit, offset, callQueryData.phoneNumber)
    }

}