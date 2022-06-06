package com.idormy.sms.forwarder.utils

object CactusSave {
    //Cactus存活时间
    var timer: Long
        get() = MMKVUtils.getLong(CACTUS_TIMER, 0L)
        set(timer) {
            MMKVUtils.put(CACTUS_TIMER, timer)
        }

    //Cactus上次存活时间
    var lastTimer: Long
        get() = MMKVUtils.getLong(CACTUS_LAST_TIMER, 0L)
        set(timer) {
            MMKVUtils.put(CACTUS_LAST_TIMER, timer)
        }

    //Cactus运行时间
    var date: String?
        get() = MMKVUtils.getString(SP_EXTRA_DEVICE_MARK, "0000-01-01 00:00:00")
        set(extraDeviceMark) {
            MMKVUtils.put(SP_EXTRA_DEVICE_MARK, extraDeviceMark)
        }

    //Cactus结束时间
    var endDate: String?
        get() = MMKVUtils.getString(CACTUS_DATE, "0000-01-01 00:00:00")
        set(extraDeviceMark) {
            MMKVUtils.put(CACTUS_END_DATE, extraDeviceMark)
        }

}