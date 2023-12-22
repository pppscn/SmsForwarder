package com.idormy.sms.forwarder.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.condition.NetworkSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.ConditionUtils
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.xuexiang.xutil.app.ServiceUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

@Suppress("PrivatePropertyName", "DEPRECATION")
class NetworkWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG: String = NetworkWorker::class.java.simpleName
    private val ipv4Pattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    private val ipv6Pattern = Regex("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")

    override suspend fun doWork(): Result {
        try {
            val conditionType = inputData.getInt(TaskWorker.conditionType, -1)
            val taskList = Core.task.getByType(conditionType)
            for (task in taskList) {
                Log.d(TAG, "task = $task")

                // 根据任务信息执行相应操作
                val conditionList = Gson().fromJson(task.conditions, Array<TaskSetting>::class.java).toMutableList()
                if (conditionList.isEmpty()) {
                    Log.d(TAG, "TASK-${task.id}：conditionList is empty")
                    continue
                }
                val firstCondition = conditionList.firstOrNull()
                if (firstCondition == null) {
                    Log.d(TAG, "TASK-${task.id}：firstCondition is null")
                    continue
                }

                val networkSetting = Gson().fromJson(firstCondition.setting, NetworkSetting::class.java)
                if (networkSetting == null) {
                    Log.d(TAG, "TASK-${task.id}：networkSetting is null")
                    continue
                }

                if (TaskUtils.networkState != networkSetting.networkState) {
                    Log.d(TAG, "TASK-${task.id}：networkState is not match, networkSetting = $networkSetting")
                    continue
                }

                //TODO：判断其他条件是否满足，注意：延迟5秒（给够搜索信号时间）才执行任务
                if (!ConditionUtils.checkCondition(task.id, conditionList)) {
                    Log.d(TAG, "TASK-${task.id}：other condition is not satisfied")
                    continue
                }

                var ipv4 = ""
                var ipv6 = ""
                val msg = StringBuilder()
                msg.append(getString(R.string.network_type)).append(": ")
                when (networkSetting.networkState) {
                    //移动网络
                    1 -> {
                        val dataSimSlot = TaskUtils.dataSimSlot
                        if (networkSetting.dataSimSlot != 0 && dataSimSlot != networkSetting.dataSimSlot) {
                            Log.d(TAG, "TASK-${task.id}：dataSimSlot is not match, networkSetting = $networkSetting")
                            continue
                        }
                        msg.append(getString(R.string.net_mobile)).append("\n")

                        if (dataSimSlot != 0) {
                            msg.append(getString(R.string.data_sim_index)).append(": SIM-").append(dataSimSlot).append("\n")
                            // 获取 SIM 卡信息
                            val simIndex = dataSimSlot - 1
                            App.SimInfoList = PhoneUtils.getSimMultiInfo()
                            if (App.SimInfoList[simIndex]?.mCarrierName != null) {
                                //获取网络运营商名称：中国移动、中国联通、中国电信
                                msg.append(getString(R.string.carrier_name)).append(": ").append(App.SimInfoList[simIndex]?.mCarrierName).append("\n")
                            }
                        }

                        ipv4 = getPublicIP(false)
                        ipv6 = getPublicIP(true)
                    }

                    //WiFi
                    2 -> {
                        if (networkSetting.wifiSsid.isNotEmpty() && TaskUtils.wifiSsid != networkSetting.wifiSsid) {
                            Log.d(TAG, "TASK-${task.id}：wifiSsid is not match, networkSetting = $networkSetting")
                            continue
                        }
                        msg.append(getString(R.string.net_wifi)).append("\n")
                        msg.append(getString(R.string.wifi_ssid)).append(": ").append(TaskUtils.wifiSsid).append("\n")

                        ipv4 = getPublicIP(false)
                        ipv6 = getPublicIP(true)
                    }

                    //未知 && 没有网络
                    else -> {
                        msg.append(getString(R.string.no_network)).append("\n")
                    }
                }

                val isHttpServerRunning = ServiceUtils.isServiceRunning("com.idormy.sms.forwarder.service.HttpServerService")
                if (ipv4Pattern.matches(ipv4)) {
                    msg.append(getString(R.string.ipv4)).append(": ").append(ipv4).append("\n")
                    TaskUtils.ipv4 = ipv4
                    if (isHttpServerRunning) {
                        msg.append(getString(R.string.http_server)).append(": ").append("http://${ipv4}:5000").append("\n")
                    }
                } else {
                    TaskUtils.ipv4 = ""
                }

                if (ipv6Pattern.matches(ipv6)) {
                    msg.append(getString(R.string.ipv6)).append(": ").append(ipv6).append("\n")
                    TaskUtils.ipv6 = ipv6
                    if (isHttpServerRunning) {
                        msg.append(getString(R.string.http_server)).append(": ").append("http://[${ipv6}]:5000").append("\n")
                    }
                } else {
                    TaskUtils.ipv6 = ""
                }

                //TODO: 组装消息体 && 执行具体任务
                val msgInfo = MsgInfo("task", task.name, msg.toString().trimEnd(), Date(), task.description)
                val actionData = Data.Builder().putLong(TaskWorker.taskId, task.id).putString(TaskWorker.taskActions, task.actions).putString(TaskWorker.msgInfo, Gson().toJson(msgInfo)).build()
                val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                WorkManager.getInstance().enqueue(actionRequest)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running worker: ${e.message}", e)
            return Result.failure()
        }
    }

    //获取公网IP地址
    private fun getPublicIP(ipv6: Boolean = false): String {
        if (TaskUtils.networkState == 0) return ""

        return try {
            val url = if (ipv6) URL("https://api6.ipify.org/") else URL("https://api.ipify.org/")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            val inputStream = urlConnection.inputStream
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error running worker: ${e.message}", e)
            ""
        }
    }

}