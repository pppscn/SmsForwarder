package com.idormy.sms.forwarder.activity

import android.os.Bundle
import android.view.KeyEvent
import androidx.viewbinding.ViewBinding
import com.idormy.sms.forwarder.core.BaseActivity
import com.idormy.sms.forwarder.fragment.LoginFragment
import com.xuexiang.xui.utils.KeyboardUtils
import com.xuexiang.xui.utils.StatusBarUtils
import com.xuexiang.xutil.display.Colors

class LoginActivity : BaseActivity<ViewBinding?>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPage(LoginFragment::class.java, intent.extras)
    }

    override val isSupportSlideBack: Boolean
        get() = false

    override fun initStatusBarStyle() {
        StatusBarUtils.initStatusBarStyle(this, false, Colors.WHITE)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return KeyboardUtils.onDisableBackKeyDown(keyCode) && super.onKeyDown(keyCode, event)
    }
}