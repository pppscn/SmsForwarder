package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class SocketSetting(
    val method: String? = "MQTT",
    var address: String = "", //IP地址
    val port: Int = 0, //端口号
    val msgTemplate: String = "", //消息模板
    val secret: String? = "", //签名密钥
    val username: String = "", //用户名
    val password: String = "", //密码
    val inCharset: String = "", //输入编码
    val outCharset: String = "", //输出编码
    val inMessageTopic: String = "", //Mqtt专属，输入信息响应主题，即接收对应主题的消息
    val outMessageTopic: String = "", //Mqtt专属，输出信息响应主题，即发送对应主题的消息
    val uriType: String = "tcp", //Mqtt专属，通信方式 默认为tcp
    val path: String = "", //Mqtt专属，通信路径，用于在使用ws进行通信时设置uri，最后的访问结果为"${uriType}://${address}:${port}${path}"
    val clientId: String = "", //Mqtt专属，客户端ID，如果为空则为随机值
) : Serializable {

    fun getMethodCheckId(): Int {
        return when (method) {
            null, "MQTT" -> R.id.rb_method_mqtt
            "TCP" -> R.id.rb_method_tcp
            "UDP" -> R.id.rb_method_udp
            else -> R.id.rb_method_mqtt
        }
    }

}