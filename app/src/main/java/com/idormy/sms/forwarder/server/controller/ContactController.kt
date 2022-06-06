package com.idormy.sms.forwarder.server.controller

import android.util.Log
import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.ContactQueryData
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.yanzhenjie.andserver.annotation.*

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/contact"])
class ContactController {

    private val TAG: String = ContactController::class.java.simpleName

    //远程查话簿
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<ContactQueryData>): MutableList<ContactInfo> {
        val contactQueryData = bean.data
        Log.d(TAG, contactQueryData.toString())

        val limit = contactQueryData.pageSize
        val offset = (contactQueryData.pageNum - 1) * limit
        return PhoneUtils.getContactInfoList(limit, offset, contactQueryData.phoneNumber, contactQueryData.name)
    }

}