package com.idormy.sms.forwarder.server.controller

import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.BatteryInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.EmptyData
import com.idormy.sms.forwarder.utils.BatteryUtils
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.RestController

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/battery"])
class BatteryController {

    private val TAG: String = BatteryController::class.java.simpleName

    //远程查电量
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<EmptyData>): BatteryInfo {
        val cloneBean = bean.data
        Log.d(TAG, cloneBean.toString())

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent: Intent? = App.context.registerReceiver(null, intentFilter)
        return BatteryUtils.getBatteryInfo(intent)
    }

}