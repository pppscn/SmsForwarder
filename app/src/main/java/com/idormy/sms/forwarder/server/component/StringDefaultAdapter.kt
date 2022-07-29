package com.idormy.sms.forwarder.server.component

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class StringDefaultAdapter : TypeAdapter<String?>() {
    @Throws(IOException::class)
    override fun write(jsonWriter: JsonWriter, s: String?) {
        jsonWriter.value(s)
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): String {
        return if (jsonReader.peek() === JsonToken.NULL) {
            jsonReader.nextNull()
            ""
        } else {
            jsonReader.nextString()
        }
    }
}