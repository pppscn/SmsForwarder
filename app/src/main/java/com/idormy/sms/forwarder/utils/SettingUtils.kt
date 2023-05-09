package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.R
import com.xuexiang.xui.utils.ResUtils.getString

class SettingUtils private constructor() {
    companion object {

        //是否是第一次启动
        //var isFirstOpen: Boolean by SharedPreference(IS_FIRST_OPEN_KEY, true)

        //是否同意隐私政策
        var isAgreePrivacy: Boolean by SharedPreference(IS_AGREE_PRIVACY_KEY, false)

        //是否转发短信
        var enableSms: Boolean by SharedPreference(SP_ENABLE_SMS, false)

        //是否转发通话
        var enablePhone: Boolean by SharedPreference(SP_ENABLE_PHONE, false)

        //是否转发通话——来电挂机
        var enableCallType1: Boolean by SharedPreference(SP_ENABLE_CALL_TYPE_1, false)

        //是否转发通话——去电挂机
        var enableCallType2: Boolean by SharedPreference(SP_ENABLE_CALL_TYPE_2, false)

        //是否转发通话——未接来电
        var enableCallType3: Boolean by SharedPreference(SP_ENABLE_CALL_TYPE_3, false)

        //是否转发通话——来电提醒
        var enableCallType4: Boolean by SharedPreference(SP_ENABLE_CALL_TYPE_4, false)

        //是否转发通话——来电接通
        var enableCallType5: Boolean by SharedPreference(SP_ENABLE_CALL_TYPE_5, false)

        //是否转发通话——去电拨出
        var enableCallType6: Boolean by SharedPreference(SP_ENABLE_CALL_TYPE_6, false)

        //是否转发应用通知
        var enableAppNotify: Boolean by SharedPreference(SP_ENABLE_APP_NOTIFY, false)

        //是否接受短信指令
        var enableSmsCommand: Boolean by SharedPreference(SP_ENABLE_SMS_COMMAND, false)
        var smsCommandSafePhone: String by SharedPreference(SP_SMS_COMMAND_SAFE_PHONE, "")

        //是否转发应用通知——自动消除通知
        var enableCancelAppNotify: Boolean by SharedPreference(SP_ENABLE_CANCEL_APP_NOTIFY, false)

        //是否转发应用通知——自动消除额外APP通知
        var cancelExtraAppNotify: String by SharedPreference(SP_CANCEL_EXTRA_APP_NOTIFY, "")

        //是否转发应用通知——仅锁屏状态
        var enableNotUserPresent: Boolean by SharedPreference(SP_ENABLE_NOT_USER_PRESENT, false)

        //是否加载应用列表
        var enableLoadAppList: Boolean by SharedPreference(ENABLE_LOAD_APP_LIST, false)

        //是否加载应用列表——用户应用
        var enableLoadUserAppList: Boolean by SharedPreference(ENABLE_LOAD_USER_APP_LIST, false)

        //是否加载应用列表——系统应用
        var enableLoadSystemAppList: Boolean by SharedPreference(ENABLE_LOAD_SYSTEM_APP_LIST, false)

        //过滤多久内重复消息
        var duplicateMessagesLimits: Int by SharedPreference(SP_DUPLICATE_MESSAGES_LIMITS, 0)

        //免打扰(禁用转发)时间段——开始
        var silentPeriodStart: Int by SharedPreference(SP_SILENT_PERIOD_START, 0)

        //免打扰(禁用转发)时间段——结束
        var silentPeriodEnd: Int by SharedPreference(SP_SILENT_PERIOD_END, 0)

        //自动删除N天前的转发记录
        var autoCleanLogsDays: Int by SharedPreference(SP_AUTO_CLEAN_LOGS_DAYS, 0)

        //是否监听网络状态变化
        var enableNetworkStateReceiver: Boolean by SharedPreference(SP_NET_STATE_RECEIVER, false)

        //是否监听电池状态变化
        var enableBatteryReceiver: Boolean by SharedPreference(SP_BATTERY_RECEIVER, false)

        //电量预警当前状态
        var batteryStatus: Int by SharedPreference(SP_BATTERY_STATUS, 0)

        //电量预警当前值
        var batteryLevelCurrent: Int by SharedPreference(SP_BATTERY_LEVEL_CURRENT, 0)

        //电量预警最低值
        var batteryLevelMin: Int by SharedPreference(SP_BATTERY_LEVEL_MIN, 0)

        //电量预警最高值
        var batteryLevelMax: Int by SharedPreference(SP_BATTERY_LEVEL_MAX, 100)

        //是否持续电量预警
        var batteryLevelOnce: Boolean by SharedPreference(SP_BATTERY_LEVEL_ONCE, false)

        //是否定时推送电池状态
        var enableBatteryCron: Boolean by SharedPreference(SP_BATTERY_CRON, false)

        //是否定时推送电池状态——开始时间
        var batteryCronStartTime: String by SharedPreference(SP_BATTERY_CRON_START_TIME, "00:00")

        //是否定时推送电池状态——间隔时间（分钟）
        var batteryCronInterval: Int by SharedPreference(SP_BATTERY_CRON_INTERVAL, 60)

        //是否不在最近任务列表中显示
        var enableExcludeFromRecents: Boolean by SharedPreference(SP_ENABLE_EXCLUDE_FROM_RECENTS, false)

        //是否转发应用通知
        var enableCactus: Boolean by SharedPreference(SP_ENABLE_CACTUS, false)

        //是否播放静音音乐
        var enablePlaySilenceMusic: Boolean by SharedPreference(SP_ENABLE_PLAY_SILENCE_MUSIC, false)

        //是否启用1像素
        var enableOnePixelActivity: Boolean by SharedPreference(SP_ENABLE_ONE_PIXEL_ACTIVITY, false)

        //请求接口失败重试次数
        var requestRetryTimes: Int by SharedPreference(SP_REQUEST_RETRY_TIMES, 0)

        //请求接口失败重试间隔（秒）
        var requestDelayTime: Int by SharedPreference(SP_REQUEST_DELAY_TIME, 1)

        //请求接口失败超时时间（秒）
        var requestTimeout: Int by SharedPreference(SP_REQUEST_TIMEOUT, 10)

        //通知内容
        var notifyContent: String by SharedPreference(SP_NOTIFY_CONTENT, getString(R.string.notification_content))

        //设备名称
        var extraDeviceMark: String by SharedPreference(SP_EXTRA_DEVICE_MARK, "")

        //SM1主键
        var subidSim1: Int by SharedPreference(SP_SUBID_SIM1, 0)

        //SM2主键
        var subidSim2: Int by SharedPreference(SP_SUBID_SIM2, 0)

        //SM1备注
        var extraSim1: String by SharedPreference(SP_EXTRA_SIM1, "")

        //SM2备注
        var extraSim2: String by SharedPreference(SP_EXTRA_SIM2, "")

        //是否启用自定义模板
        var enableSmsTemplate: Boolean by SharedPreference(SP_ENABLE_SMS_TEMPLATE, false)

        //自定义模板
        var smsTemplate: String by SharedPreference(SP_SMS_TEMPLATE, "")

        //是否显示页面帮助
        var enableHelpTip: Boolean by SharedPreference(SP_ENABLE_HELP_TIP, false)

        //是否纯客户端模式
        var enablePureClientMode: Boolean by SharedPreference(SP_PURE_CLIENT_MODE, false)

    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}