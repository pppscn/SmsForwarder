package com.idormy.sms.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Logs
import com.idormy.sms.forwarder.entity.MsgInfo
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
        val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java)
        val ruleId = inputData.getLong(Worker.ruleId, 0L)
        val senderIndex = inputData.getInt(Worker.senderIndex, 0)
        val msgId = inputData.getLong(Worker.msgId, 0L)

        val rule = Core.rule.getOne(ruleId)
        val sender = rule.senderList[senderIndex]
        val log = Logs(0, rule.type, msgId, rule.id, sender.id)
        val logId = Core.logs.insert(log)
        SendUtils.sendMsgSender(msgInfo, rule, senderIndex, logId, msgId)

        return@withContext Result.success()
    }

}