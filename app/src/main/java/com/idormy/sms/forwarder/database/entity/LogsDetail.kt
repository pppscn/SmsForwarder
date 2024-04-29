package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.TYPE_BARK
import com.idormy.sms.forwarder.utils.TYPE_DINGTALK_GROUP_ROBOT
import com.idormy.sms.forwarder.utils.TYPE_DINGTALK_INNER_ROBOT
import com.idormy.sms.forwarder.utils.TYPE_EMAIL
import com.idormy.sms.forwarder.utils.TYPE_FEISHU
import com.idormy.sms.forwarder.utils.TYPE_FEISHU_APP
import com.idormy.sms.forwarder.utils.TYPE_GOTIFY
import com.idormy.sms.forwarder.utils.TYPE_PUSHPLUS
import com.idormy.sms.forwarder.utils.TYPE_SERVERCHAN
import com.idormy.sms.forwarder.utils.TYPE_SMS
import com.idormy.sms.forwarder.utils.TYPE_SOCKET
import com.idormy.sms.forwarder.utils.TYPE_TELEGRAM
import com.idormy.sms.forwarder.utils.TYPE_URL_SCHEME
import com.idormy.sms.forwarder.utils.TYPE_WEBHOOK
import com.idormy.sms.forwarder.utils.TYPE_WEWORK_AGENT
import com.idormy.sms.forwarder.utils.TYPE_WEWORK_ROBOT
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@DatabaseView("SELECT LOGS.id,LOGS.type,LOGS.msg_id,LOGS.rule_id,LOGS.sender_id,LOGS.forward_status,LOGS.forward_response,LOGS.TIME,Rule.filed AS rule_filed,Rule.`check` AS rule_check,Rule.value AS rule_value,Rule.sim_slot AS rule_sim_slot,Sender.type AS sender_type,Sender.NAME AS sender_name FROM LOGS  LEFT JOIN Rule ON LOGS.rule_id = Rule.id LEFT JOIN Sender ON LOGS.sender_id = Sender.id")
data class LogsDetail(
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "type", defaultValue = "sms") var type: String,
    @ColumnInfo(name = "msg_id", defaultValue = "0") var msgId: Long = 0,
    @ColumnInfo(name = "rule_id", defaultValue = "0") var ruleId: Long = 0,
    @ColumnInfo(name = "sender_id", defaultValue = "0") var senderId: Long = 0,
    @ColumnInfo(name = "forward_status", defaultValue = "1") var forwardStatus: Int = 1,
    @ColumnInfo(name = "forward_response", defaultValue = "") var forwardResponse: String = "",
    @ColumnInfo(name = "time") var time: Date = Date(),
    @ColumnInfo(name = "rule_filed", defaultValue = "") var ruleFiled: String,
    @ColumnInfo(name = "rule_check", defaultValue = "") var ruleCheck: String,
    @ColumnInfo(name = "rule_value", defaultValue = "") var ruleValue: String,
    @ColumnInfo(name = "rule_sim_slot", defaultValue = "") var ruleSimSlot: String,
    @ColumnInfo(name = "sender_type", defaultValue = "1") var senderType: Int = 1,
    @ColumnInfo(name = "sender_name", defaultValue = "") var senderName: String,
) : Parcelable {

    val statusImageId: Int
        get() {
            if (forwardStatus == 1) {
                return R.drawable.ic_round_warning
            } else if (forwardStatus == 2) {
                return R.drawable.ic_round_check
            }
            return R.drawable.ic_round_cancel
        }

    val senderImageId: Int
        get() = when (senderType) {
            TYPE_DINGTALK_GROUP_ROBOT -> R.drawable.icon_dingtalk
            TYPE_EMAIL -> R.drawable.icon_email
            TYPE_BARK -> R.drawable.icon_bark
            TYPE_WEBHOOK -> R.drawable.icon_webhook
            TYPE_WEWORK_ROBOT -> R.drawable.icon_wework_robot
            TYPE_WEWORK_AGENT -> R.drawable.icon_wework_agent
            TYPE_SERVERCHAN -> R.drawable.icon_serverchan
            TYPE_TELEGRAM -> R.drawable.icon_telegram
            TYPE_FEISHU -> R.drawable.icon_feishu
            TYPE_PUSHPLUS -> R.drawable.icon_pushplus
            TYPE_GOTIFY -> R.drawable.icon_gotify
            TYPE_SMS -> R.drawable.icon_sms
            TYPE_DINGTALK_INNER_ROBOT -> R.drawable.icon_dingtalk_inner
            TYPE_FEISHU_APP -> R.drawable.icon_feishu_app
            TYPE_URL_SCHEME -> R.drawable.icon_url_scheme
            TYPE_SOCKET -> R.drawable.icon_socket
            else -> R.drawable.icon_sms
        }
}
