package com.idormy.sms.forwarder.utils.sender

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.SmsSetting
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.net.NetworkUtils

@Suppress("PrivatePropertyName", "UNUSED_PARAMETER", "unused")
class SmsUtils {
    companion object {

        private val TAG: String = SmsUtils::class.java.simpleName

        fun sendMsg(
            setting: SmsSetting,
            msgInfo: MsgInfo,
            rule: Rule?,
            logId: Long?,
        ) {
            //仅当无网络时启用 && 判断是否真实有网络
            if (setting.onlyNoNetwork == true && NetworkUtils.isHaveInternet() && NetworkUtils.isAvailableByPing()) {
                SendUtils.updateLogs(logId, 0, ResUtils.getString(R.string.OnlyNoNetwork))
                return
            }

            if (ActivityCompat.checkSelfPermission(XUtil.getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                SendUtils.updateLogs(logId, 0, ResUtils.getString(R.string.no_sms_sending_permission))
                return
            }

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate.toString())
            }

            //【注意】判断卡槽配置：0=原进原出、1=卡槽1、2=卡槽2
            val simSlotIndex = if (setting.simSlot == 0) msgInfo.simSlot else setting.simSlot - 1

            //获取卡槽信息
            if (App.SimInfoList.isEmpty()) {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
            }
            Log.d(TAG, App.SimInfoList.toString())

            //TODO：取不到卡槽信息时，采用默认卡槽发送
            val mSubscriptionId: Int = App.SimInfoList[simSlotIndex]?.mSubscriptionId ?: -1
            val res: String? = PhoneUtils.sendSms(mSubscriptionId, setting.mobiles, content)
            if (res == null) {
                SendUtils.updateLogs(logId, 2, ResUtils.getString(R.string.request_succeeded))
            } else {
                SendUtils.updateLogs(logId, 0, res)
            }
        }

        fun sendMsg(setting: SmsSetting, msgInfo: MsgInfo) {
            sendMsg(setting, msgInfo, null, null)
        }
    }
}