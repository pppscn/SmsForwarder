package com.idormy.sms.forwarder.entity.task

import com.idormy.sms.forwarder.R
import java.io.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationSetting(
    var description: String = "", //描述
    var type: String = "to", //监控类型："to"：到达地点；"leave"：离开地点
    var calcType: String = "distance", //计算方式："distance"：计算距离；"address"：地址匹配
    var longitude: Double = 0.0, //经度
    var latitude: Double = 0.0, //纬度
    var distance: Double = 0.0, //距离
    var address: String = "", //地址
) : Serializable {

    fun getCalcTypeCheckId(): Int {
        return when (calcType) {
            "distance" -> R.id.rb_calc_type_distance
            "address" -> R.id.rb_calc_type_address
            else -> R.id.rb_calc_type_distance
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 地球平均半径，单位：米
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
