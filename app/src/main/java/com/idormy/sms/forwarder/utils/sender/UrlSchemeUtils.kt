package com.idormy.sms.forwarder.utils.sender

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.UrlSchemeSetting
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xutil.XUtil
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class UrlSchemeUtils private constructor() {
    companion object {

        private val TAG: String = UrlSchemeUtils::class.java.simpleName

        fun sendMsg(
            setting: UrlSchemeSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            val from: String = msgInfo.from
            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }
            val timestamp = System.currentTimeMillis()
            val orgContent: String = msgInfo.content
            val deviceMark: String = SettingUtils.extraDeviceMark
            val appVersion: String = AppUtils.getAppVersionName()
            val simInfo: String = msgInfo.simInfo
            @SuppressLint("SimpleDateFormat") val receiveTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()) //smsVo.getDate()

            var urlScheme = setting.urlScheme
            Log.i(TAG, "urlScheme:$urlScheme")

            urlScheme = urlScheme.replace("[from]", URLEncoder.encode(from, "UTF-8"))
                .replace("[content]", URLEncoder.encode(content, "UTF-8"))
                .replace("[msg]", URLEncoder.encode(content, "UTF-8"))
                .replace("[org_content]", URLEncoder.encode(orgContent, "UTF-8"))
                .replace("[device_mark]", URLEncoder.encode(deviceMark, "UTF-8"))
                .replace("[app_version]", URLEncoder.encode(appVersion, "UTF-8"))
                .replace("[title]", URLEncoder.encode(simInfo, "UTF-8"))
                .replace("[card_slot]", URLEncoder.encode(simInfo, "UTF-8"))
                .replace("[receive_time]", URLEncoder.encode(receiveTime, "UTF-8"))
                .replace("[timestamp]", timestamp.toString())
                .replace("\n", "%0A")
            Log.i(TAG, "urlScheme:$urlScheme")

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlScheme))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                XUtil.getContext().startActivity(intent)
                SendUtils.updateLogs(logId, 2, "调用成功")
                SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "sendMsg error:$e")
                SendUtils.updateLogs(logId, 0, e.message.toString())
                SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
            }

        }

    }
}