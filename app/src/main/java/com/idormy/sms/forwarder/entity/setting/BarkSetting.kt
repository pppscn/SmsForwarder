package com.idormy.sms.forwarder.entity.setting

import java.io.Serializable

data class BarkSetting(
    //推送地址
    var server: String,
    //分组名称
    val group: String? = "",
    //消息图标
    val icon: String? = "",
    //消息声音
    val sound: String? = "",
    //消息角标
    val badge: String? = "",
    //消息链接
    val url: String? = "",
    //通知级别
    val level: String? = "active",
    //标题模板
    val title: String? = "",
) : Serializable