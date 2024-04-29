package com.idormy.sms.forwarder.server.controller

import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.CloneInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import com.yanzhenjie.andserver.annotation.*

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/clone"])
class CloneController {

    private val TAG: String = CloneController::class.java.simpleName

    //客户端从服务端拉取克隆信息
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/pull")
    fun pull(@RequestBody bean: BaseRequest<CloneInfo>): CloneInfo {
        val cloneBean = bean.data
        Log.d(TAG, cloneBean.toString())

        HttpServerUtils.compareVersion(cloneBean)

        val cloneInfo = HttpServerUtils.exportSettings()
        Log.d(TAG, cloneInfo.toString())
        return cloneInfo
    }

    //客户端向服务端推送克隆信息
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/push")
    fun push(@RequestBody bean: BaseRequest<CloneInfo>): String {
        val cloneInfo = bean.data
        Log.d(TAG, cloneInfo.toString())

        HttpServerUtils.compareVersion(cloneInfo)

        return if (HttpServerUtils.restoreSettings(cloneInfo)) "success" else getString(R.string.restore_failed)
    }

}