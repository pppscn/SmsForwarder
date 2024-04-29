package com.idormy.sms.forwarder.entity.condition

import com.idormy.sms.forwarder.R
import java.io.Serializable

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

}
