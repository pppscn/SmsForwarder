package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.DELAY_TIME_AFTER_SIM_READY
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SIM
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.idormy.sms.forwarder.workers.SimWorker
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.concurrent.TimeUnit

@Suppress("PrivatePropertyName")
class SimStateReceiver : BroadcastReceiver() {

    private var TAG = SimStateReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {

        //纯客户端模式
        if (SettingUtils.enablePureClientMode) return

        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return

        // 处理 SIM 卡状态变化的逻辑
        val simStateOld = TaskUtils.simState
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // 获取当前 SIM 卡状态
        val simStateNew = telephonyManager.simState

        // SIM 卡状态未发生变化，避免重复执行
        if (simStateOld == simStateNew) return

        var duration = 10L
        val msg = when (simStateNew) {
            TelephonyManager.SIM_STATE_ABSENT -> {
                Log.d(TAG, "SIM 卡被移除")
                TaskUtils.simState = simStateNew
                getString(R.string.sim_state_absent)
            }

            TelephonyManager.SIM_STATE_READY -> {
                Log.d(TAG, "SIM 卡已准备就绪")
                TaskUtils.simState = simStateNew
                duration = DELAY_TIME_AFTER_SIM_READY
                getString(R.string.sim_state_ready)
            }

            else -> {
                Log.d(TAG, "SIM 卡状态未知")
                TaskUtils.simState = 0
                getString(R.string.sim_state_unknown)
            }

        }

        //注意：SIM卡已准备就绪时，延迟5秒（给够搜索信号时间）才执行任务
        val request = OneTimeWorkRequestBuilder<SimWorker>()
            .setInitialDelay(duration, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    TaskWorker.CONDITION_TYPE to TASK_CONDITION_SIM,
                    TaskWorker.MSG to msg.toString().trimEnd(),
                )
            ).build()
        WorkManager.getInstance(context).enqueue(request)
    }

}