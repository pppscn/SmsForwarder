package cn.ppps.forwarder.activity

import android.os.Bundle
import androidx.viewbinding.ViewBinding
import cn.ppps.forwarder.core.BaseActivity
import cn.ppps.forwarder.fragment.ClientFragment

class ClientActivity : BaseActivity<ViewBinding?>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPage(ClientFragment::class.java)
    }
}