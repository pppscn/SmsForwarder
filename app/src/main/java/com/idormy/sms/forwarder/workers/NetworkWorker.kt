package com.idormy.sms.forwarder.workers

import android.content.Context
import android.net.wifi.WifiManager
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
import com.idormy.sms.forwarder.utils.CommonUtils
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
    private val ipv6Pattern = Regex("^((?:[\\da-fA-F]{0,4}:[\\da-fA-F]{0,4}){2,7})(?:[/\\\\%](\\d{1,3}))?$")

    override suspend fun doWork(): Result {
        try {
            //获取公网IP地址
            val ipv4 = getPublicIP(false)
            TaskUtils.ipv4 = if (ipv4Pattern.matches(ipv4)) ipv4 else ""
            val ipv6 = getPublicIP(true)
            TaskUtils.ipv6 = if (ipv6Pattern.matches(ipv6)) ipv6 else ""
            Log.d(TAG, "ipv4 = $ipv4, ipv6 = $ipv6")
            //获取所有IP地址
            val ipList = CommonUtils.getIPAddresses().filter { !isLocalAddress(it) }
            TaskUtils.ipList = if (ipList.isNotEmpty()) ipList.joinToString("\n") else ""

            val conditionType = inputData.getInt(TaskWorker.CONDITION_TYPE, -1)
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
                    }

                    //WiFi
                    2 -> {
                        //获取WiFi名称
                        val wifiManager = App.context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val wifiInfo = wifiManager.connectionInfo
                        TaskUtils.wifiSsid = wifiInfo.ssid.replace("\"", "")

                        if (networkSetting.wifiSsid.isNotEmpty() && TaskUtils.wifiSsid != networkSetting.wifiSsid) {
                            Log.d(TAG, "TASK-${task.id}：wifiSsid is not match, networkSetting = $networkSetting")
                            continue
                        }
                        msg.append(getString(R.string.net_wifi)).append("\n")
                        msg.append(getString(R.string.wifi_ssid)).append(": ").append(TaskUtils.wifiSsid).append("\n")
                    }

                    //未知 && 没有网络
                    else -> {
                        msg.append(getString(R.string.no_network)).append("\n")
                    }
                }

                val isHttpServerRunning = ServiceUtils.isServiceRunning("com.idormy.sms.forwarder.service.HttpServerService")
                if (ipv4Pattern.matches(ipv4)) {
                    msg.append(getString(R.string.ipv4)).append(": ").append(ipv4).append("\n")
                    if (isHttpServerRunning) {
                        msg.append(getString(R.string.http_server)).append(": ").append("http://${ipv4}:5000").append("\n")
                    }
                }

                if (ipv6Pattern.matches(ipv6)) {
                    msg.append(getString(R.string.ipv6)).append(": ").append(ipv6).append("\n")
                    if (isHttpServerRunning) {
                        msg.append(getString(R.string.http_server)).append(": ").append("http://[${ipv6}]:5000").append("\n")
                    }
                }

                //TODO: 组装消息体 && 执行具体任务
                val msgInfo = MsgInfo("task", task.name, msg.toString().trimEnd(), Date(), task.description)
                val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, task.id).putString(TaskWorker.TASK_ACTIONS, task.actions).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
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

    //检查IP地址是否为本地地址
    private fun isLocalAddress(ip: String): Boolean {
        return ip == "127.0.0.1" || ip == "::1" || ip.startsWith("fe80:") || ip.startsWith("fec0:")
    }

}