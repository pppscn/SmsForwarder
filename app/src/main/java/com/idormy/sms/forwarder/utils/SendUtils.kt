package com.idormy.sms.forwarder.utils

import android.os.Looper
import android.util.Log
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
import com.idormy.sms.forwarder.entity.setting.*
import com.idormy.sms.forwarder.utils.sender.*
import com.idormy.sms.forwarder.workers.SendLogicWorker
import com.idormy.sms.forwarder.workers.SendWorker
import com.idormy.sms.forwarder.workers.UpdateLogsWorker
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xutil.XUtil
import java.util.*


object SendUtils {
    private const val TAG = "SendUtils"

    //重新匹配规则并发送消息
    fun rematchSendMsg(item: MsgAndLogs) {
        val msgInfo = MsgInfo(item.msg.type, item.msg.from, item.msg.content, Date(), item.msg.simInfo, item.msg.simSlot, item.msg.subId)
        Log.d(TAG, "msgInfo = $msgInfo")

        val request = OneTimeWorkRequestBuilder<SendWorker>().setInputData(
            workDataOf(
                Worker.sendMsgInfo to Gson().toJson(msgInfo)
            )
        ).build()
        WorkManager.getInstance(XUtil.getContext()).enqueue(request)
    }

    //重试发送消息
    fun retrySendMsg(logId: Long) {
        val item = Core.logs.getOne(logId)

        val msgInfo = MsgInfo(item.msg.type, item.msg.from, item.msg.content, Date(), item.msg.simInfo, item.msg.simSlot, item.msg.subId)
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
    fun sendMsgSender(msgInfo: MsgInfo, rule: Rule, senderIndex: Int = 0, logId: Long = 0L, msgId: Long = 0L) {
        try {
            val sender = rule.senderList[senderIndex]
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
                    updateLogs(logId, 0, "未知发送通道")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateLogs(logId, 0, e.message.toString())
        }
    }

    //发送通道执行逻辑：ALL=全部执行, UntilFail=失败即终止, UntilSuccess=成功即终止, Retry=重试发送
    fun senderLogic(status: Int, msgInfo: MsgInfo, rule: Rule?, senderIndex: Int = 0, msgId: Long = 0L) {
        if (rule == null || rule.senderLogic == SENDER_LOGIC_RETRY) return

        if (senderIndex < rule.senderList.count() - 1 && (rule.senderLogic == SENDER_LOGIC_ALL || (status == 2 && rule.senderLogic == SENDER_LOGIC_UNTIL_FAIL) || (status == 0 && rule.senderLogic == SENDER_LOGIC_UNTIL_SUCCESS))) {
            val request = OneTimeWorkRequestBuilder<SendLogicWorker>().setInputData(
                workDataOf(
                    Worker.sendMsgInfo to Gson().toJson(msgInfo),
                    Worker.ruleId to rule.id,
                    Worker.senderIndex to senderIndex + 1,
                    Worker.msgId to msgId,
                )
            ).build()
            WorkManager.getInstance(XUtil.getContext()).enqueue(request)
        }
    }

    //更新转发日志状态
    fun updateLogs(logId: Long?, status: Int, response: String) {

        //测试的没有记录ID，这里取巧了
        if (logId == null || logId == 0L) {
            if (Looper.myLooper() == null) Looper.prepare()
            if (status == 2) {
                XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
            } else {
                XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
            }
            Looper.loop()
            return
        }

        val sendResponse = SendResponse(logId, status, response)
        val request = OneTimeWorkRequestBuilder<UpdateLogsWorker>().setInputData(
            workDataOf(
                Worker.updateLogs to Gson().toJson(sendResponse)
            )
        ).build()
        WorkManager.getInstance(XUtil.getContext()).enqueue(request)
    }

}