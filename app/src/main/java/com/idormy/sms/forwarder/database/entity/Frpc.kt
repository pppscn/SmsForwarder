package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.STATUS_ON
import frpclib.Frpclib
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "Frpc")
data class Frpc(
    @PrimaryKey
    @ColumnInfo(name = "uid") var uid: String = "",
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "config") var config: String = "",
    @ColumnInfo(name = "autorun", defaultValue = "0") var autorun: Int = 0,
    @ColumnInfo(name = "time") var time: Date = Date(),
    @Ignore var connecting: Boolean = false,
) : Parcelable {

    val imageId: Int
        get() = R.drawable.ic_menu_frpc

    val autorunImageId: Int
        get() = when (autorun) {
            STATUS_ON -> R.drawable.ic_autorun
            else -> R.drawable.ic_manual
        }

    val status: Int
        get() = if (connecting || (App.FrpclibInited && Frpclib.isRunning(uid))) STATUS_ON else STATUS_OFF

}