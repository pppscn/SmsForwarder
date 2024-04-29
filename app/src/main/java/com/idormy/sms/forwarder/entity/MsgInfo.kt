package com.idormy.sms.forwarder.entity

import android.annotation.SuppressLint
import android.text.TextUtils
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.App.Companion.CALL_TYPE_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.BatteryUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.enableSmsTemplate
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.extraDeviceMark
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.smsTemplate
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

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

    val titleForSend = getTitleForSend()

    val smsVoForSend = getContentForSend()

    fun getTitleForSend(titleTemplate: String = "", regexReplace: String = ""): String {
        var template = titleTemplate.replace("null", "")
        if (TextUtils.isEmpty(template)) template = getString(R.string.tag_from)

        return replaceTemplate(template, regexReplace)
    }

    fun getContentForSend(ruleSmsTemplate: String = "", regexReplace: String = ""): String {
        var customSmsTemplate: String = getString(R.string.tag_from).toString() + "\n" +
                getString(R.string.tag_sms) + "\n" +
                getString(R.string.tag_card_slot) + "\n" +
                when (type) {
                    "sms", "call" -> "SubId：${getString(R.string.tag_card_subid)}\n"
                    "app" -> "UID：${getString(R.string.tag_uid)}\n"
                    else -> ""
                } +
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

        return replaceTemplate(customSmsTemplate, regexReplace)
    }

    fun getContentFromJson(jsonTemplate: String): String {
        var template = jsonTemplate.replace("null", "")
        if (TextUtils.isEmpty(template)) template = getString(R.string.tag_from)
        return replaceTemplate(template, "", true)
    }

    @SuppressLint("SimpleDateFormat")
    fun replaceTemplate(template: String, regexReplace: String = "", needJson: Boolean = false): String {
        return template.replaceTag(getString(R.string.tag_from), from, needJson)
            .replaceTag(getString(R.string.tag_package_name), from, needJson)
            .replaceTag(getString(R.string.tag_sms), content, needJson)
            .replaceTag(getString(R.string.tag_msg), content, needJson)
            .replaceTag(getString(R.string.tag_card_slot), simInfo, needJson)
            .replaceTag(getString(R.string.tag_card_subid), subId.toString(), needJson)
            .replaceTag(getString(R.string.tag_title), simInfo, needJson)
            .replaceTag(getString(R.string.tag_uid), uid.toString(), needJson)
            .replaceTag(
                getString(R.string.tag_receive_time),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date),
                needJson
            )
            .replaceTag(
                getString(R.string.tag_current_time),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()),
                needJson
            )
            .replaceTag(getString(R.string.tag_device_name), extraDeviceMark.trim(), needJson)
            .replaceTag(getString(R.string.tag_app_version), AppUtils.getAppVersionName(), needJson)
            .replaceTag(
                getString(R.string.tag_call_type),
                CALL_TYPE_MAP[callType.toString()] ?: getString(R.string.unknown_call), needJson
            )
            .replaceTag(getString(R.string.tag_ipv4), TaskUtils.ipv4, needJson)
            .replaceTag(getString(R.string.tag_ipv6), TaskUtils.ipv6, needJson)
            .replaceTag(getString(R.string.tag_ip_list), TaskUtils.ipList, needJson)
            .replaceTag(getString(R.string.tag_battery_pct), "%.0f%%".format(TaskUtils.batteryPct), needJson)
            .replaceTag(getString(R.string.tag_battery_status), BatteryUtils.getStatus(TaskUtils.batteryStatus), needJson)
            .replaceTag(getString(R.string.tag_battery_plugged), BatteryUtils.getPlugged(TaskUtils.batteryPlugged), needJson)
            .replaceTag(getString(R.string.tag_battery_info), TaskUtils.batteryInfo, needJson)
            .replaceTag(
                getString(R.string.tag_battery_info_simple),
                "%.0f%%".format(TaskUtils.batteryPct)
                        + with(BatteryUtils.getPlugged(TaskUtils.batteryPlugged)) {
                    if (this == getString(R.string.battery_unknown)) "" else " - $this"
                },
                needJson
            )
            .replaceTag(
                getString(R.string.tag_net_type), with(NetworkUtils.getNetStateType()) {
                    if (this == NetworkUtils.NetState.NET_NO || this == NetworkUtils.NetState.NET_UNKNOWN)
                        this.name
                    this.name.removePrefix("NET_")
                },
                needJson
            )
            .replaceAppNameTag(from, needJson)
            .replaceLocationTag(needJson)
            .regexReplace(regexReplace)
            .trim()
    }

    //正则替换内容
    private fun String.regexReplace(regexReplace: String): String {
        return if (TextUtils.isEmpty(regexReplace)) this else try {
            var newContent = this
            val lineArray = regexReplace.split("\\n".toRegex()).toTypedArray()
            for (line in lineArray) {
                val lineSplit = line.split("===".toRegex()).toTypedArray()
                if (lineSplit.isNotEmpty()) {
                    val regex = lineSplit[0]
                    val replacement =
                        if (lineSplit.size >= 2)
                            lineSplit[1].replace("\\\\n".toRegex(), "\n") else ""
                    newContent = newContent.replace(regex.toRegex(), replacement)
                }
            }
            newContent
        } catch (e: Exception) {
            Log.e("RegexReplace", "Failed to get the receiving phone number:" + e.message)
            this
        }
    }

    //替换标签（支持正则替换）
    private fun String.replaceTag(tag: String, info: String, needJson: Boolean = false, ignoreCase: Boolean = true): String {
        var result = if (needJson) {
            this.replace(tag, toJsonStr(info), ignoreCase)
        } else {
            this.replace(tag, info, ignoreCase)
        }

        val tagName = tag.removePrefix("{{").removeSuffix("}}")
        val tagRegex = "\\{\\{${tagName}###([^=]+)===(.*?)\\}\\}".toRegex()
        tagRegex.findAll(result).forEach {
            try {
                Log.d("MsgInfo", "tagRegex: ${it.value}, ${it.groupValues}")
                val regex = it.groupValues[1]
                val replacement = it.groupValues[2]
                val temp = info.replace(regex.toRegex(), replacement)
                Log.d("MsgInfo", "tagRegex: regex=$regex, replacement=$replacement, temp=$temp")
                result = if (needJson) {
                    result.replace(it.value, toJsonStr(temp))
                } else {
                    result.replace(it.value, temp)
                }
            } catch (e: Exception) {
                Log.e("MsgInfo", "Failed to replace tagRegex: ${e.message}")
            }
        }

        return result
    }

    //替换{{APP名称}}标签
    private fun String.replaceAppNameTag(packageName: String, needJson: Boolean = false): String {
        if (TextUtils.isEmpty(this)) return this
        if (this.indexOf(getString(R.string.tag_app_name)) == -1) return this

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
        if (needJson) {
            appName = toJsonStr(appName)
        }
        return this.replaceTag(getString(R.string.tag_app_name), appName)
    }

    //替换 {{定位信息}} 标签
    private fun String.replaceLocationTag(needJson: Boolean = false): String {
        if (TextUtils.isEmpty(this)) return this

        val location = HttpServerUtils.apiLocationCache
        var locationStr = location.toString()
        var address = location.address
        if (needJson) {
            locationStr = toJsonStr(locationStr)
            address = toJsonStr(address)
        }
        return this.replaceTag(getString(R.string.tag_location), locationStr)
            .replaceTag(getString(R.string.tag_location_longitude), location.longitude.toString())
            .replaceTag(getString(R.string.tag_location_latitude), location.latitude.toString())
            .replaceTag(getString(R.string.tag_location_address), address)
    }

    //直接插入json字符串需要转义
    private fun toJsonStr(string: String?): String {
        if (string == null) return "null"

        val jsonStr: String = Gson().toJson(string)
        return if (jsonStr.length >= 2) jsonStr.substring(1, jsonStr.length - 1) else jsonStr
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