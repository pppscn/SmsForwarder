package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.entity.SmsInfo
import com.xuexiang.xaop.annotation.MemoryCache

@Suppress("SameParameterValue")
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

    //获取时间段
    @JvmStatic
    @get:MemoryCache
    val timePeriodOption: List<String>
        get() {
            return getTimePeriod(24, 10) //修改时请注意会不会造成旧版下标越界
        }

    /**
     * 获取时间段
     *
     * @param interval 时间间隔（分钟）
     * @return
     */
    private fun getTimePeriod(totalHour: Int, interval: Int): List<String> {
        val list: MutableList<String> = ArrayList()
        var point: Int
        var hour: Int
        var min: Int
        for (i in 0..totalHour * 60 / interval) {
            point = i * interval
            hour = point / 60
            min = point - hour * 60
            list.add((if (hour <= 9) "0$hour" else "" + hour) + ":" + if (min <= 9) "0$min" else "" + min)
        }
        return list
    }
}