package com.idormy.sms.forwarder.utils

import android.os.Looper
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.LogsAndRuleAndSender
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.result.SendResponse
import com.idormy.sms.forwarder.entity.setting.*
import com.idormy.sms.forwarder.utils.sender.*
import com.idormy.sms.forwarder.workers.SendWorker
import com.idormy.sms.forwarder.workers.UpdateLogsWorker
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.data.DateUtils
import java.text.SimpleDateFormat
import java.util.*


object SendUtils {
    private const val TAG = "SendUtils"

    //批量发送消息
    /*fun sendMsgList(infoList: List<MsgInfo>, type: String) {
        for (msgInfo in infoList) {
            sendMsg(msgInfo, type)
        }
    }*/

    //发送消息
    fun sendMsg(msgInfo: MsgInfo) {
        val request = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(workDataOf(Worker.sendMsgInfo to Gson().toJson(msgInfo)))
            .build()
        WorkManager.getInstance(XUtil.getContext()).enqueue(request)
    }

    /**
     * 重发消息：从日志获取消息内容并尝试重发
     * 根据当前rule和sender来重发，而不是失败时设置的规则
     */
    fun resendMsg(item: LogsAndRuleAndSender, rematch: Boolean) {
        Log.d(TAG, item.logs.toString())

        val date: Date = try {
            DateUtils.string2Date(item.logs.time.toString(), SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()))
        } catch (e: Exception) {
            e.printStackTrace()
            Date()
        }
        val simInfo: String = item.logs.simInfo
        val simSlot: Int = if (simInfo.startsWith("SIM2")) 2 else 1
        val msgInfo = MsgInfo(item.logs.type, item.logs.from, item.logs.content, date, simInfo, simSlot)
        Log.d(TAG, "resendMsg msgInfo:$msgInfo")

        if (rematch) {
            sendMsg(msgInfo)
            return
        }

        sendMsgSender(msgInfo, item.relation.rule, item.relation.sender, item.logs.id)
    }

    //匹配发送通道发送消息
    fun sendMsgSender(msgInfo: MsgInfo, rule: Rule, sender: Sender, logId: Long) {
        try {
            when (sender.type) {
                TYPE_DINGTALK_GROUP_ROBOT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, DingtalkGroupRobotSetting::class.java)
                    DingtalkGroupRobotUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_EMAIL -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, EmailSetting::class.java)
                    EmailUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_BARK -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, BarkSetting::class.java)
                    BarkUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_WEBHOOK -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, WebhookSetting::class.java)
                    WebhookUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_WEWORK_ROBOT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, WeworkRobotSetting::class.java)
                    WeworkRobotUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_WEWORK_AGENT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, WeworkAgentSetting::class.java)
                    WeworkAgentUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_SERVERCHAN -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, ServerchanSetting::class.java)
                    ServerchanUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_TELEGRAM -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, TelegramSetting::class.java)
                    TelegramUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_SMS -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, SmsSetting::class.java)
                    SmsUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_FEISHU -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, FeishuSetting::class.java)
                    FeishuUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_PUSHPLUS -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, PushplusSetting::class.java)
                    PushplusUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_GOTIFY -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, GotifySetting::class.java)
                    GotifyUtils.sendMsg(settingVo, msgInfo, rule, logId)
                }
                TYPE_DINGTALK_INNER_ROBOT -> {
                    val settingVo = Gson().fromJson(sender.jsonSetting, DingtalkInnerRobotSetting::class.java)
                    DingtalkInnerRobotUtils.sendMsg(settingVo, msgInfo, rule, logId)
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
        val request = OneTimeWorkRequestBuilder<UpdateLogsWorker>()
            .setInputData(
                workDataOf(
                    Worker.updateLogs to Gson().toJson(sendResponse)
                )
            )
            .build()
        WorkManager.getInstance(XUtil.getContext()).enqueue(request)
    }

}