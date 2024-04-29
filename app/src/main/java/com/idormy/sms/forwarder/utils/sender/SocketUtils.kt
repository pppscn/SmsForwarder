package com.idormy.sms.forwarder.utils.sender

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.SocketSetting
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SocketUtils {
    companion object {

        private val TAG: String = SocketUtils::class.java.simpleName

        fun sendMsg(
            setting: SocketSetting, msgInfo: MsgInfo, rule: Rule? = null, senderIndex: Int = 0, logId: Long = 0L, msgId: Long = 0L
        ) {
            val from: String = msgInfo.from
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            val timestamp = System.currentTimeMillis()
            val orgContent: String = msgInfo.content
            val deviceMark: String = SettingUtils.extraDeviceMark
            val appVersion: String = AppUtils.getAppVersionName()
            val simInfo: String = msgInfo.simInfo
            @SuppressLint("SimpleDateFormat") val receiveTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()) //smsVo.getDate()
            var sign = ""
            if (!TextUtils.isEmpty(setting.secret)) {
                val stringToSign = "$timestamp\n" + setting.secret
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(setting.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
                val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
                sign = URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
            }

            var message = if (TextUtils.isEmpty(setting.msgTemplate)) "{\"msg\": \"[msg]\"}" else setting.msgTemplate
            message = if (message.startsWith("{")) {
                message.replace("[from]", from).replace("[content]", escapeJson(content)).replace("[msg]", escapeJson(content)).replace("[org_content]", escapeJson(orgContent)).replace("[device_mark]", escapeJson(deviceMark)).replace("[app_version]", appVersion).replace("[title]", escapeJson(simInfo)).replace("[card_slot]", escapeJson(simInfo)).replace("[receive_time]", receiveTime).replace("[timestamp]", timestamp.toString()).replace("[sign]", sign)
            } else {
                message.replace("[from]", URLEncoder.encode(from, "UTF-8")).replace("[content]", URLEncoder.encode(content, "UTF-8")).replace("[msg]", URLEncoder.encode(content, "UTF-8")).replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8")).replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8")).replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8")).replace("[title]", URLEncoder.encode(simInfo, "UTF-8")).replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8")).replace("[receive_time]", URLEncoder.encode(receiveTime, "UTF-8")).replace("\n", "%0A").replace("[timestamp]", timestamp.toString()).replace("[sign]", sign)
            }

            if (setting.method == "TCP" || setting.method == "UDP") {
                // 创建套接字并连接到服务器
                val socket = Socket(setting.address, setting.port)
                Log.d(TAG, "连接到服务器: ${setting.address}:${setting.port}")
                try {
                    // 获取输入流和输出流，设置字符集为UTF-8
                    val input = BufferedReader(InputStreamReader(socket.getInputStream(), Charset.forName(setting.inCharset)))
                    val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charset.forName(setting.outCharset)))

                    // 向服务器发送数据
                    output.write(message)
                    output.newLine() // 添加换行符以便服务器使用readLine()来读取
                    output.flush()
                    Log.d(TAG, "发送到服务器的消息: $message")

                    // 从服务器接收响应
                    val response = input.readLine()
                    Log.d(TAG, "从服务器接收的响应: $response")
                    val status = if (setting.response.isNotEmpty() && !response.contains(setting.response)) 0 else 2
                    SendUtils.updateLogs(logId, status, response)
                    SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "An error occurred: ${e.message}")
                    val status = 0
                    SendUtils.updateLogs(logId, status, e.message.toString())
                    SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                } finally {
                    // 关闭套接字
                    socket.close()
                    Log.d(TAG, "Disconnected from MQTT broker")
                }
            } else if (setting.method == "MQTT") {
                // MQTT 连接参数
                val uriType = if (TextUtils.isEmpty(setting.uriType)) "tcp" else setting.uriType
                var brokerUrl = "${uriType}://${setting.address}:${setting.port}"
                if (!TextUtils.isEmpty(setting.path)) {
                    brokerUrl += setting.path
                }
                Log.d(TAG, "MQTT brokerUrl: $brokerUrl")
                val clientId = if (TextUtils.isEmpty(setting.clientId)) UUID.randomUUID().toString() else setting.clientId
                val mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())
                try {
                    val options = MqttConnectOptions()
                    if (!TextUtils.isEmpty(setting.username)) {
                        options.userName = setting.username
                    }
                    if (!TextUtils.isEmpty(setting.password)) {
                        options.password = setting.password.toCharArray()
                    }
                    options.isCleanSession = true
                    mqttClient.connect(options)
                    Log.d(TAG, "Connected to MQTT broker: ${mqttClient.serverURI}")

                    mqttClient.subscribe(setting.inMessageTopic)
                    Log.d(TAG, "Subscribed to topic: $setting.inMessageTopic")

                    val outMessage = message.toByteArray(Charset.forName(setting.outCharset))
                    val mqttMessage = MqttMessage(outMessage)
                    mqttMessage.qos = 0 // 设置消息质量服务等级
                    //异步发布消息
                    mqttClient.publish(setting.outMessageTopic, mqttMessage)
                    Log.d(TAG, "Published message to topic: $setting.outMessageTopic")
                    mqttClient.setCallback(object : MqttCallbackExtended {
                        override fun connectionLost(cause: Throwable?) {
                            val response = "Connection to MQTT broker lost: ${cause?.message}"
                            Log.d(TAG, response)
                            SendUtils.updateLogs(logId, 0, response)
                            SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
                        }

                        override fun messageArrived(topic: String?, inMessage: MqttMessage?) {
                            val payload = inMessage?.payload?.toString(Charset.forName(setting.inCharset))
                            Log.d(TAG, "Received message on topic $topic: $payload")
                            val status = if (setting.response.isNotEmpty() && !payload?.contains(setting.response)!!) 0 else 2
                            SendUtils.updateLogs(logId, status, payload.toString())
                            SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                            Log.d(TAG, "deliveryComplete")
                            SendUtils.updateLogs(logId, 2, "deliveryComplete")
                            SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
                        }

                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            Log.d(TAG, "connectComplete")
                        }
                    })
                } catch (e: MqttException) {
                    Log.d(TAG, "An error occurred: ${e.message}")
                } finally {
                    mqttClient.disconnect()
                    Log.d(TAG, "Disconnected from MQTT broker")
                }
            }
        }

        //JSON需要转义的字符
        private fun escapeJson(str: String?): String {
            if (str == null) return "null"
            val jsonStr: String = Gson().toJson(str)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

    }
}