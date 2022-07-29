package com.idormy.sms.forwarder.server.component

import android.text.TextUtils
import android.util.Log
import com.google.gson.GsonBuilder
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.yanzhenjie.andserver.annotation.Converter
import com.yanzhenjie.andserver.framework.MessageConverter
import com.yanzhenjie.andserver.framework.body.JsonBody
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.IOUtils
import com.yanzhenjie.andserver.util.MediaType
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.nio.charset.Charset


@Suppress("PrivatePropertyName")
@Converter
class AppMessageConverter : MessageConverter {

    private val TAG: String = "AppMessageConverter"

    override fun convert(output: Any?, mediaType: MediaType?): ResponseBody {
        //返回统一结构报文
        return JsonBody(HttpServerUtils.response(output))
    }

    @Throws(IOException::class)
    override fun <T : Any?> convert(stream: InputStream, mediaType: MediaType?, type: Type?): T? {
        val charset: Charset? = mediaType?.charset
        Log.d(TAG, "Charset: $charset")

        val json = if (charset == null) IOUtils.toString(stream) else IOUtils.toString(stream, charset)
        Log.d(TAG, "Json: $json")

        //修改接口数据中的null、“”为默认值
        val builder = GsonBuilder()
        builder.registerTypeAdapter(Int::class.java, IntegerDefaultAdapter())
        builder.registerTypeAdapter(String::class.java, StringDefaultAdapter())
        val gson = builder.create()
        val t: T? = gson.fromJson(json, type)
        Log.d(TAG, "Bean: $t")

        //校验时间戳（时间误差不能超过1小时）&& 签名
        if (!TextUtils.isEmpty(HttpServerUtils.serverSignKey)) {
            HttpServerUtils.checkSign(t as BaseRequest<*>)
        }

        return t
    }

}