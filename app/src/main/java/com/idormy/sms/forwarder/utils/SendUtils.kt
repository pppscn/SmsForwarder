package com.idormy.sms.forwarder.utils

import android.annotation.SuppressLint
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.SendResponse
import com.idormy.sms.forwarder.entity.setting.BarkSetting
import com.idormy.sms.forwarder.entity.setting.DingtalkGroupRobotSetting
import com.idormy.sms.forwarder.entity.setting.DingtalkInnerRobotSetting
import com.idormy.sms.forwarder.entity.setting.EmailSetting
import com.idormy.sms.forwarder.entity.setting.FeishuAppSetting
import com.idormy.sms.forwarder.entity.setting.FeishuSetting
import com.idormy.sms.forwarder.entity.setting.GotifySetting
import com.idormy.sms.forwarder.entity.setting.PushplusSetting
import com.idormy.sms.forwarder.entity.setting.ServerchanSetting
import com.idormy.sms.forwarder.entity.setting.SmsSetting
import com.idormy.sms.forwarder.entity.setting.SocketSetting
import com.idormy.sms.forwarder.entity.setting.TelegramSetting
import com.idormy.sms.forwarder.entity.setting.UrlSchemeSetting
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.entity.setting.WeworkAgentSetting
import com.idormy.sms.forwarder.entity.setting.WeworkRobotSetting
import com.idormy.sms.forwarder.utils.sender.BarkUtils
import com.idormy.sms.forwarder.utils.sender.DingtalkGroupRobotUtils
import com.idormy.sms.forwarder.utils.sender.DingtalkInnerRobotUtils
import com.idormy.sms.forwarder.utils.sender.EmailUtils
import com.idormy.sms.forwarder.utils.sender.FeishuAppUtils
import com.idormy.sms.forwarder.utils.sender.FeishuUtils
import com.idormy.sms.forwarder.utils.sender.GotifyUtils
import com.idormy.sms.forwarder.utils.sender.PushplusUtils
import com.idormy.sms.forwarder.utils.sender.ServerchanUtils
import com.idormy.sms.forwarder.utils.sender.SmsUtils
import com.idormy.sms.forwarder.utils.sender.SocketUtils
import com.idormy.sms.forwarder.utils.sender.TelegramUtils
import com.idormy.sms.forwarder.utils.sender.UrlSchemeUtils
import com.idormy.sms.forwarder.utils.sender.WebhookUtils
import com.idormy.sms.forwarder.utils.sender.WeworkAgentUtils
import com.idormy.sms.forwarder.utils.sender.WeworkRobotUtils
import com.idormy.sms.forwarder.workers.SendLogicWorker
import com.idormy.sms.forwarder.workers.SendWorker
import com.idormy.sms.forwarder.workers.UpdateLogsWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.Calendar

object SendUtils {
    private const val TAG = "SendUtils"

    //重新匹配规则并发送消息
    fun rematchSendMsg(item: MsgAndLogs) {
        val msgInfo = MsgInfo(item.msg.type, item.msg.from, item.msg.content, item.msg.time, item.msg.simInfo, item.msg.simSlot, item.msg.subId)
        Log.d(TAG, "msgInfo = $msgInfo")

        val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
            workDataOf(
                Worker.SEND_MSG_INFO to Gson().toJson(msgInfo)
            )
        ).build()
        WorkManager.getInstance(XUtil.getContext()).enqueue(request)
    }

    //重试发送消息
    fun retrySendMsg(logId: Long) {
        val item = Core.logs.getOne(logId)
        val msgInfo = MsgInfo(item.msg.type, item.msg.from, item.msg.content, item.msg.time, item.msg.simInfo, item.msg.simSlot, item.msg.subId)
        Log.d(TAG, "msgInfo = $msgInfo")

        var senderIndex = 0
        for (sender in item.rule.senderList) {
            if (item.logs.senderId == sender.id) {
                Log.d(TAG, "sender = $sender")
                senderIndex = item.rule.senderList.indexOf(sender)
                break
            }
        }

        val rule = item.rule
        rule.senderLogic = SENDER_LOGIC_RETRY
        sendMsgSender(msgInfo, rule, senderIndex, logId, item.msg.id)
    }

    //匹配发送通道发送消息
    @SuppressLint("SimpleDateFormat")
    fun sendMsgSender(msgInfo: MsgInfo, rule: Rule, senderIndex: Int = 0, logId: Long = 0L, msgId: Long = 0L) {
        try {
            val sender = rule.senderList[senderIndex]
            if (sender.status != 1) {
                Log.d(TAG, "sender = $sender is disabled")
                updateLogs(logId, 0, getString(R.string.sender_disabled))
                senderLogic(0, msgInfo, rule, senderIndex, msgId)
                return
            }
            //免打扰(禁用转发)日期段
            Log.d(TAG, "silentDayOfWeek = ${rule.silentDayOfWeek}")
            val silentDayOfWeek = rule.silentDayOfWeek.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
            if (silentDayOfWeek.isNotEmpty()) {
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                if (silentDayOfWeek.contains(dayOfWeek)) {
                    Log.d(TAG, "免打扰(禁用转发)日期段")
                    updateLogs(logId, 0, getString(R.string.silent_time_period))
                    senderLogic(0, msgInfo, rule, senderIndex, msgId)
                    return
                }
            }

            //免打扰(禁用转发)时间段
            Log.d(TAG, "silentPeriodStart = ${rule.silentPeriodStart}, silentPeriodEnd = ${rule.silentPeriodEnd}")
            if (rule.silentPeriodStart != rule.silentPeriodEnd) {
                val isSilentPeriod = DataProvider.isCurrentTimeInPeriod(rule.silentPeriodStart, rule.silentPeriodEnd)
                if (isSilentPeriod) {
                    Log.d(TAG, "免打扰(禁用转发)时间段")
                    updateLogs(logId, 0, getString(R.string.silent_time_period))
                    senderLogic(0, msgInfo, rule, senderIndex, msgId)
                    return
                }
            }
            when (sender.type) {
                TYPE_DINGTALK_GROUP_ROBOT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, DingtalkGroupRobotSetting::class.java)
                    DingtalkGroupRobotUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_EMAIL -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, EmailSetting::class.java)
                    EmailUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_BARK -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, BarkSetting::class.java)
                    BarkUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_WEBHOOK -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, WebhookSetting::class.java)
                    WebhookUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_WEWORK_ROBOT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, WeworkRobotSetting::class.java)
                    WeworkRobotUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_WEWORK_AGENT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, WeworkAgentSetting::class.java)
                    WeworkAgentUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_SERVERCHAN -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, ServerchanSetting::class.java)
                    ServerchanUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_TELEGRAM -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, TelegramSetting::class.java)
                    TelegramUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_SMS -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, SmsSetting::class.java)
                    SmsUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_FEISHU -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, FeishuSetting::class.java)
                    FeishuUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_PUSHPLUS -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, PushplusSetting::class.java)
                    PushplusUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_GOTIFY -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, GotifySetting::class.java)
                    GotifyUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_DINGTALK_INNER_ROBOT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, DingtalkInnerRobotSetting::class.java)
                    DingtalkInnerRobotUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_FEISHU_APP -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, FeishuAppSetting::class.java)
                    FeishuAppUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_URL_SCHEME -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, UrlSchemeSetting::class.java)
                    UrlSchemeUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                TYPE_SOCKET -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, SocketSetting::class.java)
                    SocketUtils.sendMsg(settingVo, msgInfo, rule, senderIndex, logId, msgId)
                }

                else -> {
                    updateLogs(logId, 0, getString(R.string.unknown_sender))
                    senderLogic(0, msgInfo, rule, senderIndex, msgId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "sendMsgSender: ${e.message}")
            updateLogs(logId, 0, e.message.toString())
            senderLogic(0, msgInfo, rule, senderIndex, msgId)
        }
    }

    //发送通道执行逻辑：ALL=全部执行, UntilFail=失败即终止, UntilSuccess=成功即终止, Retry=重试发送
    fun senderLogic(status: Int, msgInfo: MsgInfo, rule: Rule?, senderIndex: Int = 0, msgId: Long = 0L) {
        if (rule == null || rule.senderLogic == SENDER_LOGIC_RETRY) return

        if (senderIndex < rule.senderList.count() - 1 && (rule.senderLogic == SENDER_LOGIC_ALL || (status == 2 && rule.senderLogic == SENDER_LOGIC_UNTIL_FAIL) || (status == 0 && rule.senderLogic == SENDER_LOGIC_UNTIL_SUCCESS))) {
            val request = OneTimeWorkRequestBuilder<SendLogicWorker>().setInputData(
                workDataOf(
                    Worker.SEND_MSG_INFO to Gson().toJson(msgInfo),
                    //Worker.ruleId to rule.id,
                    Worker.RULE to Gson().toJson(rule),
                    Worker.SENDER_INDEX to senderIndex + 1,
                    Worker.MSG_ID to msgId,
                )
            ).build()
            WorkManager.getInstance(XUtil.getContext()).enqueue(request)
        }
    }

    //更新转发日志状态
    fun updateLogs(logId: Long?, status: Int, response: String) {

        //自动任务的不需要吐司或者更新日志
        if (logId == -1L) return

        //测试的没有记录ID，这里取巧了
        if (logId == null || logId == 0L) {
            if (status == 2) {
                LiveEventBus.get(EVENT_TOAST_SUCCESS, String::class.java).post(getString(R.string.request_succeeded))
            } else if (status == 0) {
                LiveEventBus.get(EVENT_TOAST_ERROR, String::class.java).post(getString(R.string.request_failed) + response)
            }
            return
        }

        val sendResponse = SendResponse(logId, status, response)
        val request = OneTimeWorkRequestBuilder<UpdateLogsWorker>().setInputData(
            workDataOf(
                Worker.UPDATE_LOGS to Gson().toJson(sendResponse)
            )
        ).build()
        WorkManager.getInstance(XUtil.getContext()).enqueue(request)
    }

}