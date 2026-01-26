package cn.ppps.forwarder.database.ext

import androidx.room.TypeConverter
import cn.ppps.forwarder.core.Core
import cn.ppps.forwarder.database.entity.Sender

class ConvertersSenderList {

    @TypeConverter
    fun stringToObject(value: String): List<Sender> {
        return Core.sender.getByIds(value.split(",").map { it.trim().toLong() }, value)
    }

    @TypeConverter
    fun objectToString(list: List<Sender>): String {
        return list.joinToString(",") { it.id.toString() }
    }
}