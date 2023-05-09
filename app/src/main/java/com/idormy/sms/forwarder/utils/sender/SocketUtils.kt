package com.idormy.sms.forwarder.utils.sender

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Base64
import com.gitee.xuankaicat.kmnkt.socket.MqttQuality
import com.gitee.xuankaicat.kmnkt.socket.dsl.mqtt
import com.gitee.xuankaicat.kmnkt.socket.dsl.tcp
import com.gitee.xuankaicat.kmnkt.socket.dsl.udp
import com.gitee.xuankaicat.kmnkt.socket.open
import com.gitee.xuankaicat.kmnkt.socket.utils.Charset
import com.google.gson.Gson
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.SocketSetting
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xutil.app.AppUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
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
                mac.init(SecretKeySpec(setting.secret?.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
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
                var isReceived = false
                var isConnected = false
                val socket = if (setting.method == "TCP") {
                    tcp {
                        address = setting.address//设置ip地址
                        port = setting.port//设置端口号
                        if (!TextUtils.isEmpty(setting.inCharset)) inCharset = Charset.forName(setting.inCharset)//设置输入编码
                        if (!TextUtils.isEmpty(setting.outCharset)) outCharset = Charset.forName(setting.outCharset)//设置输出编码
                    }
                } else {
                    udp {
                        address = setting.address//设置ip地址
                        port = setting.port//设置端口号
                        if (!TextUtils.isEmpty(setting.inCharset)) inCharset = Charset.forName(setting.inCharset)//设置输入编码
                        if (!TextUtils.isEmpty(setting.outCharset)) outCharset = Charset.forName(setting.outCharset)//设置输出编码
                    }
                }

                socket.open {
                    success {
                        //开启连接成功时执行
                        isConnected = true
                        SendUtils.updateLogs(logId, 1, "TCP连接成功")
                        socket.send(message)
                        socket.startReceive { str, data ->
                            isReceived = true
                            android.util.Log.d(TAG, "str=$str,data=$data")
                            SendUtils.updateLogs(logId, 2, "收到订阅消息：str=$str,data=$data")
                            SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
                            return@startReceive false
                        }
                    }
                    failure {
                        //开启连接失败时执行
                        val status = 0
                        SendUtils.updateLogs(logId, status, "TCP连接失败")
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                        return@failure false//是否继续尝试连接
                    }
                    loss {
                        //失去连接时执行
                        return@loss false//是否尝试重连
                    }
                }

                //延时5秒关闭连接
                if (isConnected) {
                    Thread.sleep(5000)
                    socket.stopReceive()
                    socket.close()
                    if (!isReceived) {
                        SendUtils.updateLogs(logId, 0, "未收到订阅消息")
                        SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
                    }
                }
                return

            } else if (setting.method == "MQTT") {
                val mqtt = mqtt {
                    address = setting.address//设置ip地址
                    port = setting.port//设置端口号
                    if (!TextUtils.isEmpty(setting.inCharset)) inCharset = Charset.forName(setting.inCharset)//设置输入编码
                    if (!TextUtils.isEmpty(setting.outCharset)) outCharset = Charset.forName(setting.outCharset)//设置输出编码
                    if (!TextUtils.isEmpty(setting.username)) username = setting.username
                    if (!TextUtils.isEmpty(setting.password)) password = setting.password
                    if (!TextUtils.isEmpty(setting.inMessageTopic)) inMessageTopic = setting.inMessageTopic
                    if (!TextUtils.isEmpty(setting.outMessageTopic)) outMessageTopic = setting.outMessageTopic
                    //自定义配置
                    qos = MqttQuality.ExactlyOnce // 服务质量 详见MqttQuality
                    if (!TextUtils.isEmpty(setting.uriType)) uriType = setting.uriType //通信方式 默认为tcp
                    if (!TextUtils.isEmpty(setting.clientId)) clientId = setting.clientId //客户端ID，如果为空则为随机值
                    timeOut = 10 //设置超时时间
                    cleanSession = true //断开连接后是否清楚缓存，如果清除缓存则在重连后需要手动恢复订阅。
                    keepAliveInterval = 20 //检测连接是否中断的间隔
                    //行为配置
                    threadLock = false //是否启用线程同步锁 默认false
                }

                mqtt.open {
                    success {
                        //开启连接成功时执行
                        SendUtils.updateLogs(logId, 1, "MQTT连接成功")
                        // 订阅并发布后等待至拿到响应消息并赋值给result
                        // 如果超过10秒没有收到消息则将result设为"消息响应超时"，并取消订阅topic
                        val response = mqtt.sendAndReceiveSync(setting.outMessageTopic, setting.inMessageTopic, message, 10000L) ?: "消息响应超时"
                        mqtt.close()

                        val status = if (response == "消息响应超时") 0 else 2
                        SendUtils.updateLogs(logId, status, "收到订阅消息：$response")
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                        return@success
                    }
                    failure {
                        //开启连接失败时执行
                        val status = 0
                        SendUtils.updateLogs(logId, status, "MQTT连接失败")
                        SendUtils.senderLogic(status, msgInfo, rule, senderIndex, msgId)
                        return@failure false//是否继续尝试连接
                    }
                    loss {
                        //失去连接时执行
                        return@loss false//是否尝试重连
                    }
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