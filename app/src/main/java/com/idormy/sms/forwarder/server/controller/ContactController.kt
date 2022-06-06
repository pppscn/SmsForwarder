package com.idormy.sms.forwarder.server.controller

import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.ContactQueryData
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.RestController

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/contact"])
class ContactController {

    //远程查话簿
    @PostMapping("/query")
    fun query(@RequestBody bean: BaseRequest<ContactQueryData>): MutableList<ContactInfo>? {
        val callQueryData = bean.data
        val limit = callQueryData.pageSize
        val offset = (callQueryData.pageNum - 1) * limit
        return PhoneUtils.getContactInfoList(limit, offset, callQueryData.phoneNumber, callQueryData.name)
    }

}