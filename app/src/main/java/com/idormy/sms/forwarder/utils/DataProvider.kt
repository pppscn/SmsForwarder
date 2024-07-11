package com.idormy.sms.forwarder.utils

import android.annotation.SuppressLint
import com.idormy.sms.forwarder.entity.CallInfo
import com.idormy.sms.forwarder.entity.ContactInfo
import com.idormy.sms.forwarder.entity.SmsInfo
import com.xuexiang.xaop.annotation.MemoryCache
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

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

    /**
     * 判断当前时间是否在时间段内
     */
    @SuppressLint("SimpleDateFormat")
    fun isCurrentTimeInPeriod(periodStartIndex: Int, periodEndIndex: Int): Boolean {
        val periodStartStr = timePeriodOption[periodStartIndex]
        val periodEndStr = timePeriodOption[periodEndIndex]

        // 定义时间格式
        val formatter = SimpleDateFormat("HH:mm")

        // 解析时间字符串
        val periodStart = Calendar.getInstance().apply {
            time = formatter.parse(periodStartStr) as Date
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val periodEnd = Calendar.getInstance().apply {
            time = formatter.parse(periodEndStr) as Date
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 获取当前时间
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        // 判断是否跨天
        return if (periodEnd.before(periodStart)) {
            // 跨天的情况
            (currentHour > periodStart.get(Calendar.HOUR_OF_DAY) || (currentHour == periodStart.get(Calendar.HOUR_OF_DAY) && currentMinute >= periodStart.get(Calendar.MINUTE))) ||
                    (currentHour < periodEnd.get(Calendar.HOUR_OF_DAY) || (currentHour == periodEnd.get(Calendar.HOUR_OF_DAY) && currentMinute < periodEnd.get(Calendar.MINUTE)))
        } else {
            // 不跨天的情况
            (currentHour > periodStart.get(Calendar.HOUR_OF_DAY) || (currentHour == periodStart.get(Calendar.HOUR_OF_DAY) && currentMinute >= periodStart.get(Calendar.MINUTE))) &&
                    (currentHour < periodEnd.get(Calendar.HOUR_OF_DAY) || (currentHour == periodEnd.get(Calendar.HOUR_OF_DAY) && currentMinute < periodEnd.get(Calendar.MINUTE)))
        }
    }

}