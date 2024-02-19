package com.idormy.sms.forwarder.entity.action

import java.io.Serializable

data class AlarmSetting(
    var description: String = "", //描述
    var action: String = "stop", //动作: start=启动警报, stop=停止警报
    var volume: Int = 80, //播放音量
    var playTimes: Int = 1, //播放次数，0=无限循环
    var music: String = "", //音乐文件
) : Serializable
