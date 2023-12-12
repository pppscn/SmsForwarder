package com.idormy.sms.forwarder.activity

import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.idormy.sms.forwarder.core.BaseActivity
import com.idormy.sms.forwarder.fragment.TasksFragment
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.EVENT_TOAST_SUCCESS
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class TaskActivity : BaseActivity<ViewBinding?>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPage(TasksFragment::class.java)

        //替换 Looper.loop() 后再 Toast 形式
        LiveEventBus.get(EVENT_TOAST_SUCCESS, String::class.java).observe(this) { msg: String ->
            XToastUtils.success(msg)
        }
        LiveEventBus.get(EVENT_TOAST_ERROR, String::class.java).observe(this) { msg: String ->
            XToastUtils.error(msg, 15000)
        }
    }
}