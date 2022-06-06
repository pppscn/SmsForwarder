package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.STATUS_ON
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "Frpc")
data class Frpc(
    @PrimaryKey
    @ColumnInfo(name = "uid") var uid: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "config") var config: String,
    @ColumnInfo(name = "autorun", defaultValue = "0") var autorun: Int = 0,
    @ColumnInfo(name = "time") var time: Date = Date(),
    @Ignore var connecting: Boolean = false,
) : Parcelable {
    constructor() : this("", "", "", 0, Date(), false)

    @Ignore
    constructor(config: String) : this("", "", config, 0, Date(), false)

    @Ignore
    constructor(uid: String, name: String, config: String) : this(uid, name, config, 0, Date(), false)

    fun setConnecting(connecting: Boolean): Frpc {
        this.connecting = connecting
        return this
    }

    val autorunImageId: Int
        get() = when (autorun) {
            STATUS_ON -> R.drawable.ic_autorun
            else -> R.drawable.ic_manual
        }

}