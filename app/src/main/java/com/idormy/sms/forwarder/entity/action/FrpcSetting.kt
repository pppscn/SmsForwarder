package com.idormy.sms.forwarder.entity.action

import java.io.Serializable

data class FrpcSetting(
    var description: String = "", //描述
    var action: String = "start", //动作: start=启动, stop=停止
    var uids: String = "", //指定配置UID，多个以半角逗号分隔
) : Serializable
