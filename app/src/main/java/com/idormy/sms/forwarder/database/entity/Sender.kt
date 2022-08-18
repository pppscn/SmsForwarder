package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.*
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "Sender")
data class Sender(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "type", defaultValue = "1") var type: Int = 1,
    @ColumnInfo(name = "name", defaultValue = "") var name: String,
    @ColumnInfo(name = "json_setting", defaultValue = "") var jsonSetting: String,
    @ColumnInfo(name = "status", defaultValue = "1") var status: Int = 1,
    @ColumnInfo(name = "time") var time: Date = Date(),
) : Parcelable {
    companion object {

        fun getImageId(type: Int): Int = when (type) {
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
            else -> R.drawable.icon_sms
        }

    }

    val imageId: Int
        get() = when (type) {
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
            else -> R.drawable.icon_sms
        }

    val statusImageId: Int
        get() = when (status) {
            STATUS_OFF -> R.drawable.icon_off
            else -> R.drawable.icon_on
        }

}