package com.idormy.sms.forwarder.entity

import android.annotation.SuppressLint
import android.text.TextUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.BatteryUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.enableSmsTemplate
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.extraDeviceMark
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.smsTemplate
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
data class MsgInfo(
    var type: String = "sms",
    var from: String,
    var content: String,
    var date: Date,
    var simInfo: String,
    var simSlot: Int = -1, //卡槽id：-1=获取失败、0=卡槽1、1=卡槽2
    var subId: Int = 0, //卡槽主键
    var callType: Int = 0, //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
    var uid: Int = 0, //APP通知的UID
) : Serializable {

    private val titleForSend: String
        get() = getTitleForSend("", "")

    //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
    private val callTypeMap = mapOf(
        //"0" to getString(R.string.unknown_call),
        "1" to getString(R.string.incoming_call_ended),
        "2" to getString(R.string.outgoing_call_ended),
        "3" to getString(R.string.missed_call),
        "4" to getString(R.string.incoming_call_received),
        "5" to getString(R.string.incoming_call_answered),
        "6" to getString(R.string.outgoing_call_started),
    )

    fun getTitleForSend(titleTemplate: String): String {
        return getTitleForSend(titleTemplate, "")
    }

    @SuppressLint("SimpleDateFormat")
    fun getTitleForSend(titleTemplate: String, regexReplace: String): String {
        var template = titleTemplate.replace("null", "")
        if (TextUtils.isEmpty(template)) template = getString(R.string.tag_from)
        val deviceMark = extraDeviceMark.trim()
        val versionName = AppUtils.getAppVersionName()
        val splitSimInfo = simInfo.split("#####")
        val title = splitSimInfo.getOrElse(0) { simInfo }
        val scheme = splitSimInfo.getOrElse(1) { "" }
        val titleForSend: String = template.replace(getString(R.string.tag_from), from)
            .replace(getString(R.string.tag_package_name), from)
            .replace(getString(R.string.tag_sms), content)
            .replace(getString(R.string.tag_msg), content)
            .replace(getString(R.string.tag_card_slot), title)
            .replace(getString(R.string.tag_card_subid), subId.toString())
            .replace(getString(R.string.tag_title), title)
            .replace(getString(R.string.tag_scheme), scheme)
            .replace(getString(R.string.tag_uid), uid.toString())
            .replace(getString(R.string.tag_receive_time), SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
            .replace(getString(R.string.tag_current_time), SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
            .replace(getString(R.string.tag_device_name), deviceMark)
            .replace(getString(R.string.tag_app_version), versionName)
            .replace(getString(R.string.tag_call_type), callTypeMap[callType.toString()] ?: getString(R.string.unknown_call))
            .trim()
        return replaceLocationTag(replaceAppName(regexReplace(titleForSend, regexReplace), from))
    }

    val smsVoForSend: String
        get() = getContentForSend("", "")

    fun getContentForSend(ruleSmsTemplate: String): String {
        return getContentForSend(ruleSmsTemplate, "")
    }

    @SuppressLint("SimpleDateFormat")
    fun getContentForSend(ruleSmsTemplate: String, regexReplace: String): String {
        val deviceMark = extraDeviceMark.trim()
        var customSmsTemplate: String = getString(R.string.tag_from).toString() + "\n" +
                getString(R.string.tag_sms) + "\n" +
                getString(R.string.tag_card_slot) + "\n" +
                (if (type == "app") "" else "SubId：${getString(R.string.tag_card_subid)}\n") +
                getString(R.string.tag_receive_time) + "\n" +
                getString(R.string.tag_device_name)

        //优先取转发规则的自定义模板，留空则取全局设置
        if (ruleSmsTemplate.isNotEmpty()) {
            customSmsTemplate = ruleSmsTemplate.replace("null", "")
        } else {
            val switchSmsTemplate = enableSmsTemplate
            val smsTemplate = smsTemplate.trim()
            if (switchSmsTemplate && smsTemplate.isNotEmpty()) {
                customSmsTemplate = smsTemplate.replace("null", "")
            }
        }
        val versionName = AppUtils.getAppVersionName()
        val splitSimInfo = simInfo.split("#####")
        val title = splitSimInfo.getOrElse(0) { simInfo }
        val scheme = splitSimInfo.getOrElse(1) { "" }
        val smsVoForSend: String = customSmsTemplate.replace(getString(R.string.tag_from), from)
            .replace(getString(R.string.tag_package_name), from)
            .replace(getString(R.string.tag_sms), content)
            .replace(getString(R.string.tag_msg), content)
            .replace(getString(R.string.tag_card_slot), title)
            .replace(getString(R.string.tag_card_subid), subId.toString())
            .replace(getString(R.string.tag_title), title)
            .replace(getString(R.string.tag_scheme), scheme)
            .replace(getString(R.string.tag_uid), uid.toString())
            .replace(getString(R.string.tag_receive_time), SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date))
            .replace(getString(R.string.tag_current_time), SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
            .replace(getString(R.string.tag_device_name), deviceMark)
            .replace(getString(R.string.tag_app_version), versionName)
            .replace(getString(R.string.tag_call_type), callTypeMap[callType.toString()] ?: getString(R.string.unknown_call))
            .replace(getString(R.string.tag_battery_pct), TaskUtils.batteryPct.toString())
            .replace(getString(R.string.tag_battery_status), BatteryUtils.getStatus(TaskUtils.batteryStatus))
            .replace(getString(R.string.tag_battery_plugged), BatteryUtils.getPlugged(TaskUtils.batteryPlugged))
            .replace(getString(R.string.tag_battery_info), TaskUtils.batteryInfo)
            .trim()
        return replaceLocationTag(replaceAppName(regexReplace(smsVoForSend, regexReplace), from))
    }

    //正则替换内容
    private fun regexReplace(content: String, regexReplace: String): String {
        return if (TextUtils.isEmpty(regexReplace)) content else try {
            var newContent = content
            val lineArray = regexReplace.split("\\n".toRegex()).toTypedArray()
            for (line in lineArray) {
                val lineSplit = line.split("===".toRegex()).toTypedArray()
                if (lineSplit.isNotEmpty()) {
                    val regex = lineSplit[0]
                    val replacement = if (lineSplit.size >= 2) lineSplit[1].replace("\\\\n".toRegex(), "\n") else ""
                    newContent = newContent.replace(regex.toRegex(), replacement)
                }
            }
            newContent
        } catch (e: Exception) {
            Log.e("RegexReplace", "Failed to get the receiving phone number:" + e.message)
            content
        }
    }

    //替换{{APP名称}}标签
    private fun replaceAppName(content: String, packageName: String): String {
        if (TextUtils.isEmpty(content)) return content
        if (content.indexOf(getString(R.string.tag_app_name)) == -1) return content

        var appName = ""
        if (SettingUtils.enableLoadUserAppList && App.UserAppList.isNotEmpty()) {
            for (appInfo in App.UserAppList) {
                if (appInfo.packageName == packageName) {
                    appName = appInfo.name
                    break
                }
            }
        }
        if (TextUtils.isEmpty(appName) && SettingUtils.enableLoadSystemAppList && App.SystemAppList.isNotEmpty()) {
            for (appInfo in App.SystemAppList) {
                if (appInfo.packageName == packageName) {
                    appName = appInfo.name
                    break
                }
            }
        }
        return content.replace(getString(R.string.tag_app_name), appName)
    }

    //替换 {{定位信息}} 标签
    private fun replaceLocationTag(content: String): String {
        if (TextUtils.isEmpty(content)) return content
        if (content.indexOf(getString(R.string.tag_location)) == -1) return content

        val location = HttpServerUtils.apiLocationCache.toString()
        return content.replace(getString(R.string.tag_location), location)
    }

    override fun toString(): String {
        return "MsgInfo{" +
                "mobile='" + from + '\'' +
                ", content='" + content + '\'' +
                ", date=" + date +
                ", simInfo=" + simInfo +
                '}'
    }
}