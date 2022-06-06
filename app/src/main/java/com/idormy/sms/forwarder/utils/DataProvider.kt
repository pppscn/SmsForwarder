package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.entity.SmsInfo
import com.xuexiang.xaop.annotation.MemoryCache

object DataProvider {

    //用于占位的空信息
    @JvmStatic
    @get:MemoryCache
    val emptySmsInfo: List<SmsInfo>
        get() {
            val list: MutableList<SmsInfo> = ArrayList()
            for (i in 0..5) {
                list.add(SmsInfo())
            }
            return list
        }

    //用于占位的空信息
    @JvmStatic
    @get:MemoryCache
    val emptyCallInfo: List<CallInfo>
        get() {
            val list: MutableList<CallInfo> = ArrayList()
            for (i in 0..5) {
                list.add(CallInfo())
            }
            return list
        }

    //用于占位的空信息
    @JvmStatic
    @get:MemoryCache
    val emptyContactInfo: List<ContactInfo>
        get() {
            val list: MutableList<ContactInfo> = ArrayList()
            for (i in 0..5) {
                list.add(ContactInfo())
            }
            return list
        }
}