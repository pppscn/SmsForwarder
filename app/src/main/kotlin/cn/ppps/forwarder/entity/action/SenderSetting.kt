package cn.ppps.forwarder.entity.action

import cn.ppps.forwarder.database.entity.Sender
import java.io.Serializable

data class SenderSetting(
    var description: String = "", //描述
    var status: Int = 1, //状态：0-禁用；1-启用
    var senderList: List<Sender>, //发送通道列表
) : Serializable
