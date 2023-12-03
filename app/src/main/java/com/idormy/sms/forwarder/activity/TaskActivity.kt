package com.idormy.sms.forwarder.activity

import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.idormy.sms.forwarder.core.BaseActivity
import com.idormy.sms.forwarder.fragment.TasksEditFragment

class TaskActivity : BaseActivity<ViewBinding?>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPage(TasksEditFragment::class.java)
    }
}