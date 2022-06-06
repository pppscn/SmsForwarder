package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class LogsAndRuleAndSender(
    @Embedded val logs: Logs,

    @Relation(
        entity = Rule::class,
        parentColumn = "rule_id",
        entityColumn = "id"
    )
    val relation: RuleAndSender,
) : Parcelable
