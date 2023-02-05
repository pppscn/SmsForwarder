package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class MsgAndLogs(
    @Embedded val msg: Msg,

    @Relation(
        parentColumn = "id",
        entityColumn = "msg_id"
    )
    val logsList: List<LogsDetail>
) : Parcelable
