package cn.ppps.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import cn.ppps.forwarder.core.Core
import cn.ppps.forwarder.database.entity.Logs
import cn.ppps.forwarder.database.entity.Rule
import cn.ppps.forwarder.entity.MsgInfo
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SendUtils
import cn.ppps.forwarder.utils.Worker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendLogicWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val msgInfoJson = inputData.getString(Worker.SEND_MSG_INFO)
        val msgInfo = Gson().fromJson(msgInfoJson, MsgInfo::class.java) ?: return@withContext Result.failure()
        //val ruleId = inputData.getLong(Worker.ruleId, 0L)
        val ruleJson = inputData.getString(Worker.RULE)
        val senderIndex = inputData.getInt(Worker.SENDER_INDEX, 0)
        val msgId = inputData.getLong(Worker.MSG_ID, 0L)
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