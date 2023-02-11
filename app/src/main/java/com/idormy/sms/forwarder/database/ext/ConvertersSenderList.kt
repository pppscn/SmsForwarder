package com.idormy.sms.forwarder.database.ext

import androidx.room.TypeConverter
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import java.util.*

@Suppress("unused")
class ConvertersSenderList {

    @TypeConverter
    fun stringToObject(value: String): List<Sender> {
        val senderList: MutableList<Sender> = mutableListOf()
        value.split(",").map { it.trim() }.forEach {
            val sender = Core.sender.getOne(it.toLong())
            senderList.add(sender)
        }
        return senderList
    }

    @TypeConverter
    fun objectToString(list: List<Sender>): String {
        val senderList = ArrayList<Long>()
        list.forEach {
            senderList += it.id
        }
        return senderList.joinToString(",")
    }
}