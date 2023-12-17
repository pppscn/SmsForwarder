package com.idormy.sms.forwarder.activity

import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.idormy.sms.forwarder.core.BaseActivity
import com.idormy.sms.forwarder.fragment.ClientFragment

class ClientActivity : BaseActivity<ViewBinding?>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPage(ClientFragment::class.java)
    }
}