package com.idormy.sms.forwarder.server.controller

import android.content.Intent
import android.content.IntentFilter
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.entity.BatteryInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.EmptyData
import com.idormy.sms.forwarder.utils.BatteryUtils
import com.yanzhenjie.andserver.annotation.*

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/battery"])
class BatteryController {

    private val TAG: String = BatteryController::class.java.simpleName

    //远程查电量
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<EmptyData>): BatteryInfo {
        Log.d(TAG, bean.data.toString())

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent: Intent? = App.context.registerReceiver(null, intentFilter)
        return BatteryUtils.getBatteryInfo(intent)
    }

}