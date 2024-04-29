package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.idormy.sms.forwarder.R
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(
    tableName = "Msg",
    indices = [
        Index(value = ["id"], unique = true)
    ]
)
data class Msg(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "type", defaultValue = "sms") var type: String,
    @ColumnInfo(name = "from", defaultValue = "") var from: String,
    @ColumnInfo(name = "content", defaultValue = "") var content: String,
    @ColumnInfo(name = "sim_slot", defaultValue = "-1") var simSlot: Int = -1, //卡槽id：-1=获取失败、0=卡槽1、1=卡槽2
    @ColumnInfo(name = "sim_info", defaultValue = "") var simInfo: String = "",
    @ColumnInfo(name = "sub_id", defaultValue = "0") var subId: Int = 0,
    //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
    @ColumnInfo(name = "call_type", defaultValue = "0") var callType: Int = 0,
    @ColumnInfo(name = "time") var time: Date = Date(),
) : Parcelable {

    val simImageId: Int
        get() {
            return when {
                type == "app" -> R.drawable.ic_app
                simSlot == 0 -> R.drawable.ic_sim1
                simSlot == 1 -> R.drawable.ic_sim2
                simInfo.isNotEmpty() && simInfo.replace("-", "").startsWith("SIM2") -> R.drawable.ic_sim2
                simInfo.isNotEmpty() && simInfo.replace("-", "").startsWith("SIM1") -> R.drawable.ic_sim1
                else -> R.drawable.ic_sim
            }
        }

}