package com.idormy.sms.forwarder.utils.task

import com.idormy.sms.forwarder.utils.SP_ENABLE_SERVER_AUTORUN
import com.idormy.sms.forwarder.utils.SharedPreference

/**
 * 自动任务工具类 —— 用于存储自动任务相关的配置
 */
class TaskUtils private constructor() {

    companion object {

        //是否启用HttpServer开机自启
        var enableServerAutorun: Boolean by SharedPreference(SP_ENABLE_SERVER_AUTORUN, false)
    }
}