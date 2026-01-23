@file:OptIn(ExperimentalCoroutinesApi::class)

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
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

            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
            ).launch {
                when (setting.method) {
                    "TCP" -> sendTcpSuspend(setting, message, msgInfo, rule, senderIndex, logId, msgId)
                    "UDP" -> sendUdpAckSuspend(setting, message, msgInfo, rule, senderIndex, logId, msgId)
                    "MQTT" -> sendMqttSuspend(setting, message, msgInfo, rule, senderIndex, logId, msgId)
                }
            }
        }

        //JSON需要转义的字符
        private fun escapeJson(str: String?): String {
            if (str == null) return "null"
            val jsonStr: String = Gson().toJson(str)
            return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
        }

        private suspend fun sendTcpSuspend(
            setting: SocketSetting,
            message: String,
            msgInfo: MsgInfo,
            rule: Rule?,
            senderIndex: Int,
            logId: Long,
            msgId: Long
        ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

            try {
                Socket(setting.address, setting.port).use { socket ->
                    val input = BufferedReader(
                        InputStreamReader(socket.getInputStream(), Charset.forName(setting.inCharset))
                    )
                    val output = BufferedWriter(
                        OutputStreamWriter(socket.getOutputStream(), Charset.forName(setting.outCharset))
                    )

                    output.write(message)
                    output.newLine()
                    output.flush()

                    val response = if (setting.response.isEmpty()) "" else input.readLine() ?: ""
                    val status =
                        if (setting.response.isEmpty() || response.contains(setting.response)) 2 else 0

                    SendUtils.updateLogs(logId, status, response)
                    SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                }
            } catch (e: Exception) {
                SendUtils.updateLogs(logId, 0, e.message ?: "")
                SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
            }
        }

        private suspend fun sendUdpAckSuspend(
            setting: SocketSetting,
            message: String,
            msgInfo: MsgInfo,
            rule: Rule?,
            senderIndex: Int,
            logId: Long,
            msgId: Long
        ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

            val retry = if (SettingUtils.requestRetryTimes > 0) SettingUtils.requestRetryTimes else 3
            val timeout = if (SettingUtils.requestTimeout > 0) SettingUtils.requestTimeout * 1000 else 3000

            val socket = java.net.DatagramSocket()
            socket.soTimeout = timeout

            val address = java.net.InetAddress.getByName(setting.address)
            val uuid = UUID.randomUUID().toString()

            val body = mapOf(
                "id" to uuid,
                "payload" to message,
                "ts" to System.currentTimeMillis()
            )

            val data = Gson().toJson(body)
                .toByteArray(Charset.forName(setting.outCharset))

            val packet = java.net.DatagramPacket(
                data,
                data.size,
                address,
                setting.port
            )

            repeat(retry) { index ->
                try {
                    socket.send(packet)
                    Log.d(TAG, "UDP send ${index + 1}/$retry id=$uuid")

                    val buf = ByteArray(1024)
                    val ackPacket = java.net.DatagramPacket(buf, buf.size)
                    socket.receive(ackPacket)

                    val ackText = String(
                        ackPacket.data,
                        0,
                        ackPacket.length,
                        Charset.forName(setting.inCharset)
                    )

                    val ack = Gson().fromJson(ackText, Map::class.java)
                    if (ack["ack"] == uuid) {
                        SendUtils.updateLogs(logId, 2, "UDP ACK OK")
                        SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
                        socket.close()
                        return@withContext
                    }

                } catch (e: java.net.SocketTimeoutException) {
                    Log.w(TAG, "UDP ACK timeout, retry...: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "UDP error: ${e.message}")
                }
            }

            SendUtils.updateLogs(logId, 0, "UDP ACK timeout")
            SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
            socket.close()
        }

        private suspend fun sendMqttSuspend(
            setting: SocketSetting,
            message: String,
            msgInfo: MsgInfo,
            rule: Rule?,
            senderIndex: Int,
            logId: Long,
            msgId: Long
        ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

            val uriType = if (TextUtils.isEmpty(setting.uriType)) "tcp" else setting.uriType
            var brokerUrl = "$uriType://${setting.address}:${setting.port}"
            if (!TextUtils.isEmpty(setting.path)) {
                brokerUrl += setting.path
            }

            val clientId =
                if (TextUtils.isEmpty(setting.clientId)) UUID.randomUUID().toString()
                else setting.clientId

            val mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            try {
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    if (!TextUtils.isEmpty(setting.username)) {
                        userName = setting.username
                    }
                    if (!TextUtils.isEmpty(setting.password)) {
                        password = setting.password.toCharArray()
                    }
                }

                // 用挂起点等 MQTT 回调，避免忙等
                val result = kotlinx.coroutines.suspendCancellableCoroutine { cont ->

                    mqttClient.setCallback(object : MqttCallbackExtended {

                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            try {
                                if (!TextUtils.isEmpty(setting.inMessageTopic)) {
                                    mqttClient.subscribe(setting.inMessageTopic)
                                }

                                val payload = message.toByteArray(Charset.forName(setting.outCharset))
                                val mqttMessage = MqttMessage(payload).apply { qos = 0 }

                                mqttClient.publish(setting.outMessageTopic, mqttMessage)

                                // 没有 response 期望，直接成功
                                if (setting.response.isEmpty()) {
                                    cont.resume(2 to "MQTT sent", null)
                                }

                            } catch (e: Exception) {
                                cont.resume(0 to e.message.orEmpty(), null)
                            }
                        }

                        override fun messageArrived(topic: String?, msg: MqttMessage?) {
                            val text = msg?.payload?.toString(Charset.forName(setting.inCharset)).orEmpty()
                            val status =
                                if (setting.response.isNotEmpty() && !text.contains(setting.response)) 0
                                else 2

                            cont.resume(status to text, null)
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                            // 如果没有订阅回包，这里兜底
                            if (setting.response.isEmpty()) {
                                cont.resume(2 to "deliveryComplete", null)
                            }
                        }

                        override fun connectionLost(cause: Throwable?) {
                            cont.resume(0 to (cause?.message ?: "MQTT connection lost"), null)
                        }
                    })

                    mqttClient.connect(options)
                }

                SendUtils.updateLogs(logId, result.first, result.second)
                SendUtils.senderLogic(result.first, msgInfo, rule, senderIndex, msgId)

            } catch (e: Exception) {
                SendUtils.updateLogs(logId, 0, e.message ?: "")
                SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)

            } finally {
                try {
                    if (mqttClient.isConnected) {
                        mqttClient.disconnect()
                    }
                } catch (_: Exception) {
                }
            }
        }

    }
}