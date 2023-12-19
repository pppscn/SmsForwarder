package com.idormy.sms.forwarder.core.webview

import android.view.KeyEvent
import androidx.viewbinding.ViewBinding
import com.idormy.sms.forwarder.core.BaseFragment
import com.just.agentweb.core.AgentWeb

/**
 * 基础web
 *
 * @author xuexiang
 * @since 2019/5/28 10:22
 */
abstract class BaseWebViewFragment : BaseFragment<ViewBinding?>() {
    private var mAgentWeb: AgentWeb? = null

    //===================生命周期管理===========================//
    override fun onResume() {
        if (mAgentWeb != null) {
            //恢复
            mAgentWeb!!.webLifeCycle.onResume()
        }
        super.onResume()
    }

    override fun onPause() {
        if (mAgentWeb != null) {
            //暂停应用内所有WebView ， 调用mWebView.resumeTimers();/mAgentWeb.getWebLifeCycle().onResume(); 恢复。
            mAgentWeb!!.webLifeCycle.onPause()
        }
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return mAgentWeb != null && mAgentWeb!!.handleKeyEvent(keyCode, event)
    }

    override fun onDestroyView() {
        if (mAgentWeb != null) {
            mAgentWeb!!.destroy()
        }
        super.onDestroyView()
    }
}