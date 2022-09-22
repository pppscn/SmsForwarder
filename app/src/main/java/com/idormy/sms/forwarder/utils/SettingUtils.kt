package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.R
import com.xuexiang.xui.utils.ResUtils.getString

class SettingUtils private constructor() {
    companion object {

        //是否是第一次启动
        var isFirstOpen: Boolean
            get() = MMKVUtils.getBoolean(IS_FIRST_OPEN_KEY, true)
            set(isFirstOpen) {
                MMKVUtils.put(IS_FIRST_OPEN_KEY, isFirstOpen)
            }

        //是否同意隐私政策
        @JvmStatic
        var isAgreePrivacy: Boolean
            get() = MMKVUtils.getBoolean(IS_AGREE_PRIVACY_KEY, false)
            set(isAgreePrivacy) {
                MMKVUtils.put(IS_AGREE_PRIVACY_KEY, isAgreePrivacy)
            }

        //是否转发短信
        @JvmStatic
        var enableSms: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_SMS, false)
            set(enableSms) {
                MMKVUtils.put(SP_ENABLE_SMS, enableSms)
            }

        //是否转发通话
        @JvmStatic
        var enablePhone: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_PHONE, false)
            set(enablePhone) {
                MMKVUtils.put(SP_ENABLE_PHONE, enablePhone)
            }

        //是否转发通话——已接来电
        @JvmStatic
        var enableCallType1: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_CALL_TYPE_1, false)
            set(enableCallType1) {
                MMKVUtils.put(SP_ENABLE_CALL_TYPE_1, enableCallType1)
            }

        //是否转发通话——本机去电
        @JvmStatic
        var enableCallType2: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_CALL_TYPE_2, false)
            set(enableCallType2) {
                MMKVUtils.put(SP_ENABLE_CALL_TYPE_2, enableCallType2)
            }

        //是否转发通话——未接来电
        @JvmStatic
        var enableCallType3: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_CALL_TYPE_3, false)
            set(enableCallType3) {
                MMKVUtils.put(SP_ENABLE_CALL_TYPE_3, enableCallType3)
            }

        //是否转发通话——来电提醒
        @JvmStatic
        var enableCallType4: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_CALL_TYPE_4, false)
            set(enableCallType4) {
                MMKVUtils.put(SP_ENABLE_CALL_TYPE_4, enableCallType4)
            }

        //是否转发应用通知
        @JvmStatic
        var enableAppNotify: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_APP_NOTIFY, false)
            set(enableAppNotify) {
                MMKVUtils.put(SP_ENABLE_APP_NOTIFY, enableAppNotify)
            }

        //是否转发应用通知——自动消除通知
        @JvmStatic
        var enableCancelAppNotify: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_CANCEL_APP_NOTIFY, false)
            set(enableCancelAppNotify) {
                MMKVUtils.put(SP_ENABLE_CANCEL_APP_NOTIFY, enableCancelAppNotify)
            }

        //是否转发应用通知——仅锁屏状态
        @JvmStatic
        var enableNotUserPresent: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_NOT_USER_PRESENT, false)
            set(enableNotUserPresent) {
                MMKVUtils.put(SP_ENABLE_NOT_USER_PRESENT, enableNotUserPresent)
            }

        //是否加载应用列表
        @JvmStatic
        var enableLoadAppList: Boolean
            get() = MMKVUtils.getBoolean(ENABLE_LOAD_APP_LIST, false)
            set(enableLoadAppList) {
                MMKVUtils.put(ENABLE_LOAD_APP_LIST, enableLoadAppList)
            }

        //是否加载应用列表——用户应用
        @JvmStatic
        var enableLoadUserAppList: Boolean
            get() = MMKVUtils.getBoolean(ENABLE_LOAD_USER_APP_LIST, false)
            set(enableLoadUserAppList) {
                MMKVUtils.put(ENABLE_LOAD_USER_APP_LIST, enableLoadUserAppList)
            }

        //是否加载应用列表——系统应用
        @JvmStatic
        var enableLoadSystemAppList: Boolean
            get() = MMKVUtils.getBoolean(ENABLE_LOAD_SYSTEM_APP_LIST, false)
            set(enableLoadSystemAppList) {
                MMKVUtils.put(ENABLE_LOAD_SYSTEM_APP_LIST, enableLoadSystemAppList)
            }

        //过滤多久内重复消息
        @JvmStatic
        var duplicateMessagesLimits: Int
            get() = MMKVUtils.getInt(SP_DUPLICATE_MESSAGES_LIMITS, 0)
            set(duplicateMessagesLimits) {
                MMKVUtils.put(SP_DUPLICATE_MESSAGES_LIMITS, duplicateMessagesLimits)
            }

        //免打扰(禁用转发)时间段——开始
        @JvmStatic
        var silentPeriodStart: Int
            get() = MMKVUtils.getInt(SP_SILENT_PERIOD_START, 0)
            set(silentPeriodStart) {
                MMKVUtils.put(SP_SILENT_PERIOD_START, silentPeriodStart)
            }

        //免打扰(禁用转发)时间段——结束
        @JvmStatic
        var silentPeriodEnd: Int
            get() = MMKVUtils.getInt(SP_SILENT_PERIOD_END, 0)
            set(silentPeriodEnd) {
                MMKVUtils.put(SP_SILENT_PERIOD_END, silentPeriodEnd)
            }

        //自动删除N天前的转发记录
        @JvmStatic
        var autoCleanLogsDays: Int
            get() = MMKVUtils.getInt(SP_AUTO_CLEAN_LOGS_DAYS, 0)
            set(autoCleanLogsDays) {
                MMKVUtils.put(SP_AUTO_CLEAN_LOGS_DAYS, autoCleanLogsDays)
            }

        //是否监听电池状态变化
        @JvmStatic
        var enableBatteryReceiver: Boolean
            get() = MMKVUtils.getBoolean(SP_BATTERY_RECEIVER, false)
            set(enableBatteryReceiver) {
                MMKVUtils.put(SP_BATTERY_RECEIVER, enableBatteryReceiver)
            }

        //电量预警当前状态
        @JvmStatic
        var batteryStatus: Int
            get() = MMKVUtils.getInt(SP_BATTERY_STATUS, 0)
            set(batteryStatus) {
                MMKVUtils.put(SP_BATTERY_STATUS, batteryStatus)
            }

        //电量预警当前值
        @JvmStatic
        var batteryLevelCurrent: Int
            get() = MMKVUtils.getInt(SP_BATTERY_LEVEL_CURRENT, 0)
            set(batteryLevelCurrent) {
                MMKVUtils.put(SP_BATTERY_LEVEL_CURRENT, batteryLevelCurrent)
            }

        //电量预警最低值
        @JvmStatic
        var batteryLevelMin: Int
            get() = MMKVUtils.getInt(SP_BATTERY_LEVEL_MIN, 0)
            set(batteryLevelMin) {
                MMKVUtils.put(SP_BATTERY_LEVEL_MIN, batteryLevelMin)
            }

        //电量预警最高值
        @JvmStatic
        var batteryLevelMax: Int
            get() = MMKVUtils.getInt(SP_BATTERY_LEVEL_MAX, 100)
            set(batteryLevelMax) {
                MMKVUtils.put(SP_BATTERY_LEVEL_MAX, batteryLevelMax)
            }

        //是否持续电量预警
        @JvmStatic
        var batteryLevelOnce: Boolean
            get() = MMKVUtils.getBoolean(SP_BATTERY_LEVEL_ONCE, false)
            set(batteryLevelOnce) {
                MMKVUtils.put(SP_BATTERY_LEVEL_ONCE, batteryLevelOnce)
            }

        //是否定时推送电池状态
        @JvmStatic
        var enableBatteryCron: Boolean
            get() = MMKVUtils.getBoolean(SP_BATTERY_CRON, false)
            set(enableBatteryCron) {
                MMKVUtils.put(SP_BATTERY_CRON, enableBatteryCron)
            }

        //是否定时推送电池状态——开始时间
        @JvmStatic
        var batteryCronStartTime: String?
            get() = MMKVUtils.getString(SP_BATTERY_CRON_START_TIME, "00:00")
            set(batteryCronStartTime) {
                MMKVUtils.put(SP_BATTERY_CRON_START_TIME, batteryCronStartTime)
            }

        //是否定时推送电池状态——间隔时间（分钟）
        @JvmStatic
        var batteryCronInterval: Int
            get() = MMKVUtils.getInt(SP_BATTERY_CRON_INTERVAL, 60)
            set(batteryCronInterval) {
                MMKVUtils.put(SP_BATTERY_CRON_INTERVAL, batteryCronInterval)
            }

        //是否不在最近任务列表中显示
        @JvmStatic
        var enableExcludeFromRecents: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_EXCLUDE_FROM_RECENTS, false)
            set(enableExcludeFromRecents) {
                MMKVUtils.put(SP_ENABLE_EXCLUDE_FROM_RECENTS, enableExcludeFromRecents)
            }

        //是否转发应用通知
        @JvmStatic
        var enableCactus: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_CACTUS, false)
            set(enableAppNotify) {
                MMKVUtils.put(SP_ENABLE_CACTUS, enableAppNotify)
            }

        //是否播放静音音乐
        @JvmStatic
        var enablePlaySilenceMusic: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_PLAY_SILENCE_MUSIC, false)
            set(enablePlaySilenceMusic) {
                MMKVUtils.put(SP_ENABLE_PLAY_SILENCE_MUSIC, enablePlaySilenceMusic)
            }

        //是否启用1像素
        @JvmStatic
        var enableOnePixelActivity: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_ONE_PIXEL_ACTIVITY, false)
            set(enableOnePixelActivity) {
                MMKVUtils.put(SP_ENABLE_ONE_PIXEL_ACTIVITY, enableOnePixelActivity)
            }

        //请求接口失败重试次数
        @JvmStatic
        var requestRetryTimes: Int
            get() = MMKVUtils.getInt(SP_REQUEST_RETRY_TIMES, 0)
            set(requestRetryTimes) {
                MMKVUtils.put(SP_REQUEST_RETRY_TIMES, requestRetryTimes)
            }

        //请求接口失败重试间隔（秒）
        @JvmStatic
        var requestDelayTime: Int
            get() = MMKVUtils.getInt(SP_REQUEST_DELAY_TIME, 1)
            set(requestDelayTime) {
                MMKVUtils.put(SP_REQUEST_DELAY_TIME, requestDelayTime)
            }

        //请求接口失败超时时间（秒）
        @JvmStatic
        var requestTimeout: Int
            get() = MMKVUtils.getInt(SP_REQUEST_TIMEOUT, 10)
            set(requestTimeout) {
                MMKVUtils.put(SP_REQUEST_TIMEOUT, requestTimeout)
            }

        //通知内容
        @JvmStatic
        var notifyContent: String?
            get() = MMKVUtils.getString(SP_NOTIFY_CONTENT, getString(R.string.notification_content))
            set(notificationContent) {
                MMKVUtils.put(SP_NOTIFY_CONTENT, notificationContent)
            }

        //设备名称
        @JvmStatic
        var extraDeviceMark: String?
            get() = MMKVUtils.getString(SP_EXTRA_DEVICE_MARK, "")
            set(extraDeviceMark) {
                MMKVUtils.put(SP_EXTRA_DEVICE_MARK, extraDeviceMark)
            }

        //SM1备注
        @JvmStatic
        var extraSim1: String?
            get() = MMKVUtils.getString(SP_EXTRA_SIM1, "")
            set(extraSim1) {
                MMKVUtils.put(SP_EXTRA_SIM1, extraSim1)
            }

        //SM2备注
        @JvmStatic
        var extraSim2: String?
            get() = MMKVUtils.getString(SP_EXTRA_SIM2, "")
            set(extraSim2) {
                MMKVUtils.put(SP_EXTRA_SIM2, extraSim2)
            }

        //是否启用自定义模板
        @JvmStatic
        var enableSmsTemplate: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_SMS_TEMPLATE, false)
            set(enableSmsTemplate) {
                MMKVUtils.put(SP_ENABLE_SMS_TEMPLATE, enableSmsTemplate)
            }

        //自定义模板
        @JvmStatic
        var smsTemplate: String?
            get() = MMKVUtils.getString(SP_SMS_TEMPLATE, "")
            set(smsTemplate) {
                MMKVUtils.put(SP_SMS_TEMPLATE, smsTemplate)
            }

        //是否显示页面帮助
        @JvmStatic
        var enableHelpTip: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_HELP_TIP, false)
            set(enableHelpTip) {
                MMKVUtils.put(SP_ENABLE_HELP_TIP, enableHelpTip)
            }

        //是否纯客户端模式
        @JvmStatic
        var enablePureClientMode: Boolean
            get() = MMKVUtils.getBoolean(SP_PURE_CLIENT_MODE, false)
            set(enablePureClientMode) {
                MMKVUtils.put(SP_PURE_CLIENT_MODE, enablePureClientMode)
            }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}