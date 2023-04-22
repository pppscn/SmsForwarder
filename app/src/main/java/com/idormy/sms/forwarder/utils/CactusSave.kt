package com.idormy.sms.forwarder.utils

object CactusSave {
    //Cactus存活时间
    var timer: Long by SharedPreference(CACTUS_TIMER, 0L)

    //Cactus上次存活时间
    var lastTimer: Long by SharedPreference(CACTUS_LAST_TIMER, 0L)

    //Cactus运行时间
    var date: String by SharedPreference(CACTUS_DATE, "0000-01-01 00:00:00")

    //Cactus结束时间
    var endDate: String by SharedPreference(CACTUS_END_DATE, "0000-01-01 00:00:00")
}