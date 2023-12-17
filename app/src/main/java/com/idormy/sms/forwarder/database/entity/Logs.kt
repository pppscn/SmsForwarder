package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(
    tableName = "Logs",
    foreignKeys = [
        ForeignKey(
            entity = Msg::class,
            parentColumns = ["id"],
            childColumns = ["msg_id"],
            onDelete = ForeignKey.CASCADE, //级联操作
            onUpdate = ForeignKey.CASCADE //级联操作
        ),
        ForeignKey(
            entity = Rule::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE, //级联操作
            onUpdate = ForeignKey.CASCADE //级联操作
        ),
        ForeignKey(
            entity = Sender::class,
            parentColumns = ["id"],
            childColumns = ["sender_id"],
            onDelete = ForeignKey.CASCADE, //级联操作
            onUpdate = ForeignKey.CASCADE //级联操作
        ),
    ],
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["msg_id"]),
        Index(value = ["rule_id"]),
        Index(value = ["sender_id"]),
    ]
)
data class Logs(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "type", defaultValue = "sms") var type: String,
    @ColumnInfo(name = "msg_id", defaultValue = "0") var msgId: Long = 0,
    @ColumnInfo(name = "rule_id", defaultValue = "0") var ruleId: Long = 0,
    @ColumnInfo(name = "sender_id", defaultValue = "0") var senderId: Long = 0,
    @ColumnInfo(name = "forward_status", defaultValue = "1") var forwardStatus: Int = 1,
    @ColumnInfo(name = "forward_response", defaultValue = "") var forwardResponse: String = "",
    @ColumnInfo(name = "time") var time: Date = Date(),
) : Parcelable