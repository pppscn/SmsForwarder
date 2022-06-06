package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.*
import com.idormy.sms.forwarder.R
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(
    tableName = "Logs",
    foreignKeys = [
        ForeignKey(
            entity = Rule::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE, //级联操作
            onUpdate = ForeignKey.CASCADE //级联操作
        )
    ],
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["rule_id"])
    ]
)
data class Logs(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "type", defaultValue = "sms") var type: String,
    @ColumnInfo(name = "from", defaultValue = "") var from: String,
    @ColumnInfo(name = "content", defaultValue = "") var content: String,
    @ColumnInfo(name = "rule_id", defaultValue = "0") var ruleId: Long = 0,
    @ColumnInfo(name = "sim_info", defaultValue = "") var simInfo: String = "",
    @ColumnInfo(name = "forward_status", defaultValue = "1") var forwardStatus: Int = 1,
    @ColumnInfo(name = "forward_response", defaultValue = "") var forwardResponse: String = "",
    @ColumnInfo(name = "time") var time: Date = Date(),
) : Parcelable {

    val simImageId: Int
        get() {
            if (simInfo.isNotEmpty()) {
                if (simInfo.replace("-", "").startsWith("SIM2")) {
                    return R.drawable.ic_sim2 //mipmap
                } else if (simInfo.replace("-", "").startsWith("SIM1")) {
                    return R.drawable.ic_sim1
                }
            }
            return R.drawable.ic_sim
        }

    val statusImageId: Int
        get() {
            if (forwardStatus == 1) {
                return R.drawable.ic_round_warning
            } else if (forwardStatus == 2) {
                return R.drawable.ic_round_check
            }
            return R.drawable.ic_round_cancel
        }

}