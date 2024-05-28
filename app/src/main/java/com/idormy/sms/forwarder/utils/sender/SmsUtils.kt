package com.idormy.sms.forwarder.utils.sender

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.SmsSetting
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.resource.ResUtils.getString

class SmsUtils {
    companion object {

        private val TAG: String = SmsUtils::class.java.simpleName

        fun sendMsg(
            setting: SmsSetting,
            msgInfo: MsgInfo,
            rule: Rule? = null,
            senderIndex: Int = 0,
            logId: Long = 0L,
            msgId: Long = 0L
        ) {
            //仅当无网络时启用 && 判断是否真实有网络
            if (setting.onlyNoNetwork && NetworkUtils.isHaveInternet() && NetworkUtils.isAvailableByPing()) {
                SendUtils.updateLogs(logId, 0, getString(R.string.OnlyNoNetwork))
                SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
                return
            }

            if (ActivityCompat.checkSelfPermission(XUtil.getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                SendUtils.updateLogs(logId, 0, getString(R.string.no_sms_sending_permission))
                SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
                return
            }

            val content: String = if (rule != null) {
                msgInfo.getContentForSend(rule.smsTemplate, rule.regexReplace)
            } else {
                msgInfo.getContentForSend(SettingUtils.smsTemplate)
            }

            //【注意】判断卡槽配置：0=原进原出、1=卡槽1、2=卡槽2
            val simSlotIndex = if (setting.simSlot == 0) msgInfo.simSlot else setting.simSlot - 1

            //获取卡槽信息
            if (App.SimInfoList.isEmpty()) {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
            }
            Log.d(TAG, App.SimInfoList.toString())

            //替换 {{来源号码}} 标签
            val mobiles = setting.mobiles.replace(getString(R.string.tag_from), msgInfo.from)

            //TODO：取不到卡槽信息时，采用默认卡槽发送
            val mSubscriptionId: Int = App.SimInfoList[simSlotIndex]?.mSubscriptionId ?: -1
            val res: String? = PhoneUtils.sendSms(mSubscriptionId, mobiles, content)
            if (res == null) {
                SendUtils.updateLogs(logId, 2, getString(R.string.request_succeeded))
                SendUtils.senderLogic(2, msgInfo, rule, senderIndex, msgId)
            } else {
                SendUtils.updateLogs(logId, 0, res)
                SendUtils.senderLogic(0, msgInfo, rule, senderIndex, msgId)
            }
        }

    }
}