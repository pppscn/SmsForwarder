package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class LogsAndRuleAndSender(
    @Embedded val logs: Logs,

    @Relation(
        entity = Msg::class,
        parentColumn = "msg_id",
        entityColumn = "id"
    )
    val msg: Msg,

    @Relation(
        entity = Rule::class,
        parentColumn = "rule_id",
        entityColumn = "id"
    )
    val rule: Rule,

    @Relation(
        entity = Sender::class,
        parentColumn = "sender_id",
        entityColumn = "id"
    )
    val sender: Sender,
) : Parcelable
