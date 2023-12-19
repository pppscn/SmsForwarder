package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.task.TaskUtils
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "Task")
data class Task(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @ColumnInfo(name = "type", defaultValue = "1") var type: Int = 1, // 任务类型：＜1000为任务模板，>=1000为自定义任务
    @ColumnInfo(name = "name", defaultValue = "") val name: String = "", // 任务名称
    @ColumnInfo(name = "description", defaultValue = "") val description: String = "", // 任务描述
    @ColumnInfo(name = "conditions", defaultValue = "") val conditions: String = "", // 触发条件
    @ColumnInfo(name = "actions", defaultValue = "") val actions: String = "", // 执行动作
    @ColumnInfo(name = "status", defaultValue = "1") var status: Int = 1, // 任务状态
    @ColumnInfo(name = "last_exec_time") var lastExecTime: Date = Date(), // 上次执行时间
    @ColumnInfo(name = "next_exec_time") var nextExecTime: Date = Date(), // 下次执行时间
) : Parcelable {

    val imageId: Int
        get() = TaskUtils.getTypeImageId(type)

    val greyImageId: Int
        get() = TaskUtils.getTypeGreyImageId(type)

    val statusImageId: Int
        get() = when (status) {
            STATUS_OFF -> R.drawable.ic_stop
            else -> R.drawable.ic_start
        }

}