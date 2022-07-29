package com.idormy.sms.forwarder.server.controller

import android.util.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.ConfigData
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xutil.app.AppUtils
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

        //获取卡槽信息
        if (App.SimInfoList.isEmpty()) {
            App.SimInfoList = PhoneUtils.getSimMultiInfo()
        }
        Log.d(TAG, App.SimInfoList.toString())

        return ConfigData(
            HttpServerUtils.enableApiClone,
            HttpServerUtils.enableApiSmsSend,
            HttpServerUtils.enableApiSmsQuery,
            HttpServerUtils.enableApiCallQuery,
            HttpServerUtils.enableApiContactQuery,
            HttpServerUtils.enableApiBatteryQuery,
            HttpServerUtils.enableApiWol,
            SettingUtils.extraDeviceMark.toString(),
            SettingUtils.extraSim1.toString(),
            SettingUtils.extraSim2.toString(),
            App.SimInfoList,
            AppUtils.getAppVersionCode(),
            AppUtils.getAppVersionName(),
        )
    }

}