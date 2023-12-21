package com.idormy.sms.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Logs
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.Worker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendLogicWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val msgInfoJson = inputData.getString(Worker.sendMsgInfo)
        val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java) ?: return@withContext Result.failure()
        //val ruleId = inputData.getLong(Worker.ruleId, 0L)
        val ruleJson = inputData.getString(Worker.rule)
        val senderIndex = inputData.getInt(Worker.senderIndex, 0)
        val msgId = inputData.getLong(Worker.msgId, 0L)
        Log.d("SendLogicWorker", "msgInfoJson: $msgInfoJson, ruleJson: $ruleJson, senderIndex: $senderIndex, msgId: $msgId")

        val rule = Gson().fromJson(ruleJson, Rule::class.java) ?: return@withContext Result.failure()
        if (senderIndex >= rule.senderList.size) return@withContext Result.failure()
        val sender = rule.senderList[senderIndex]
        var logId = 0L
        if (msgId > 0 && rule.id > 0) {
            val log = Logs(0, rule.type, msgId, rule.id, sender.id)
            logId = Core.logs.insert(log)
        } else if (msgId == -1L) {
            //自动任务的不需要吐司或者更新日志，特殊处理 logId = -1，msgId = -1
            logId = -1L
        }

        SendUtils.sendMsgSender(msgInfo, rule, senderIndex, logId, msgId)
        return@withContext Result.success()
    }

}