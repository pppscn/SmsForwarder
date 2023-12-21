package com.idormy.sms.forwarder.server.component

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.idormy.sms.forwarder.utils.Log
import java.io.IOException

class IntegerDefaultAdapter : TypeAdapter<Int>() {
    @Throws(IOException::class)
    override fun write(jsonWriter: JsonWriter, value: Int) {
        jsonWriter.value(value.toString())
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): Int {
        return try {
            Integer.valueOf(jsonReader.nextString())
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            Log.e("IntegerDefaultAdapter", "read: ${e.message}")
            -1
        }
    }
}