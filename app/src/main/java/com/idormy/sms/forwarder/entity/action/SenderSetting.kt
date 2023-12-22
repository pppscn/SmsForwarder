package com.idormy.sms.forwarder.entity.action

import com.idormy.sms.forwarder.database.entity.Sender
import java.io.Serializable

data class SenderSetting(
    var description: String = "", //描述
    var status: String = "enable", //状态: enable=启用，disable=禁用
    var senderList: List<Sender>, //发送通道列表
) : Serializable
