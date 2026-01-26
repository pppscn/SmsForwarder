package cn.ppps.forwarder.entity.action

import cn.ppps.forwarder.database.entity.Task
import java.io.Serializable

data class TaskActionSetting(
    var description: String = "", //描述
    var status: Int = 1, //状态：0-禁用；1-启用
    var taskList: List<Task>, //自动任务列表
) : Serializable
