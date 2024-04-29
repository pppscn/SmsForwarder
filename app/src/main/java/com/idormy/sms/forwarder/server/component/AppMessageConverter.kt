package com.idormy.sms.forwarder.server.component

import com.idormy.sms.forwarder.utils.Log
import com.google.gson.GsonBuilder
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.RSACrypt
import com.idormy.sms.forwarder.utils.SM4Crypt
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xutil.data.ConvertTools
import com.yanzhenjie.andserver.annotation.Converter
import com.yanzhenjie.andserver.error.HttpException
import com.yanzhenjie.andserver.framework.MessageConverter
import com.yanzhenjie.andserver.framework.body.JsonBody
import com.yanzhenjie.andserver.framework.body.StringBody
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
        var response = HttpServerUtils.response(output)
        Log.d(TAG, "response: $response")

        return when (HttpServerUtils.safetyMeasures) {
            2 -> {
                val privateKey = RSACrypt.getPrivateKey(HttpServerUtils.serverPrivateKey)
                response = Base64.encode(response.toByteArray())
                response = RSACrypt.encryptByPrivateKey(response, privateKey)
                StringBody(response)
            }
            3 -> {
                val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.serverSm4Key)
                //response = Base64.encode(response.toByteArray())
                val encryptCBC = SM4Crypt.encrypt(response.toByteArray(), sm4Key)
                StringBody(ConvertTools.bytes2HexString(encryptCBC))
            }
            else -> JsonBody(response)
        }
    }

    @Throws(IOException::class)
    override fun <T : Any?> convert(stream: InputStream, mediaType: MediaType?, type: Type?): T? {
        val charset: Charset? = mediaType?.charset
        Log.d(TAG, "Charset: $charset")

        var json = if (charset == null) IOUtils.toString(stream) else IOUtils.toString(stream, charset)
        Log.d(TAG, "Json: $json")

        if (HttpServerUtils.safetyMeasures == 2) {
            if (TextUtils.isEmpty(HttpServerUtils.serverPrivateKey)) {
                Log.e(TAG, "RSA解密失败: 私钥为空")
                throw HttpException(500, "服务端未配置私钥")
            }

            val privateKey = RSACrypt.getPrivateKey(HttpServerUtils.serverPrivateKey)
            json = RSACrypt.decryptByPrivateKey(json, privateKey)
            json = String(Base64.decode(json))
            Log.d(TAG, "Json: $json")
        } else if (HttpServerUtils.safetyMeasures == 3) {
            if (TextUtils.isEmpty(HttpServerUtils.serverSm4Key)) {
                Log.e(TAG, "SM4解密失败: SM4密钥为空")
                throw HttpException(500, "服务端未配置SM4密钥")
            }

            val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.serverSm4Key)
            val encryptCBC = ConvertTools.hexStringToByteArray(json)
            val decryptCBC = SM4Crypt.decrypt(encryptCBC, sm4Key)
            //json = String(Base64.decode(decryptCBC.toString()))
            json = String(decryptCBC)
            Log.d(TAG, "Json: $json")
        }

        //修改接口数据中的null、“”为默认值
        val builder = GsonBuilder()
        builder.registerTypeAdapter(Int::class.java, IntegerDefaultAdapter())
        builder.registerTypeAdapter(String::class.java, StringDefaultAdapter())
        val gson = builder.create()
        val t: T? = gson.fromJson(json, type)
        Log.d(TAG, "Bean: $t")

        //校验时间戳（时间误差不能超过1小时）&& 签名
        if (HttpServerUtils.safetyMeasures == 1) {
            HttpServerUtils.checkSign(t as BaseRequest<*>)
        }

        return t
    }

}