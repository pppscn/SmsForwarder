package com.idormy.sms.forwarder.entity.action

import com.idormy.sms.forwarder.utils.SettingUtils
import java.io.Serializable

data class SettingsSetting(
    var description: String = "", //描述
    var enableSms: Boolean = SettingUtils.enableSms, //是否转发短信

    var enablePhone: Boolean = SettingUtils.enablePhone, //是否转发通话
    var enableCallType1: Boolean = SettingUtils.enableCallType1, //是否转发通话——来电挂机
    var enableCallType2: Boolean = SettingUtils.enableCallType2, //是否转发通话——去电挂机
    var enableCallType3: Boolean = SettingUtils.enableCallType3, //是否转发通话——未接来电
    var enableCallType4: Boolean = SettingUtils.enableCallType4, //是否转发通话——来电提醒
    var enableCallType5: Boolean = SettingUtils.enableCallType5, //是否转发通话——来电接通
    var enableCallType6: Boolean = SettingUtils.enableCallType6, //是否转发通话——去电拨出

    var enableAppNotify: Boolean = SettingUtils.enableAppNotify, //是否转发应用通知
    var enableCancelAppNotify: Boolean = SettingUtils.enableCancelAppNotify, //是否转发应用通知——自动消除通知
    var enableNotUserPresent: Boolean = SettingUtils.enableNotUserPresent, //是否转发应用通知——仅锁屏状态

    var enableLocation: Boolean = SettingUtils.enableLocation, //是否启用定位功能
    var locationAccuracy: Int = SettingUtils.locationAccuracy, //设置位置精度
    var locationPowerRequirement: Int = SettingUtils.locationPowerRequirement, //设置电量消耗
    var locationMinInterval: Long = SettingUtils.locationMinInterval, //设置最小更新间隔
    var locationMinDistance: Int = SettingUtils.locationMinDistance, //设置最小更新距离

    var enableSmsCommand: Boolean = SettingUtils.enableSmsCommand, //是否接受短信指令
    var smsCommandSafePhone: String = SettingUtils.smsCommandSafePhone, //短信指令安全手机号

    var enableLoadAppList: Boolean = SettingUtils.enableLoadAppList, //是否加载应用列表
    var enableLoadUserAppList: Boolean = SettingUtils.enableLoadUserAppList, //是否加载应用列表——用户应用
    var enableLoadSystemAppList: Boolean = SettingUtils.enableLoadSystemAppList, //是否加载应用列表——系统应用

    var cancelExtraAppNotify: String = SettingUtils.cancelExtraAppNotify, //是否转发应用通知——自动消除额外APP通知

    var duplicateMessagesLimits: Int = SettingUtils.duplicateMessagesLimits, //重复消息限制
) : Serializable
