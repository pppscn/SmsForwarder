package com.idormy.sms.forwarder.core.webview

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import com.idormy.sms.forwarder.R
import com.just.agentweb.widget.IWebLayout
import com.scwang.smartrefresh.layout.SmartRefreshLayout

/**
 * 定义支持下来回弹的WebView
 *
 * @author xuexiang
 * @since 2019/1/5 上午2:01
 */
class WebLayout(activity: Activity?) : IWebLayout<WebView?, ViewGroup?> {
    private val mSmartRefreshLayout: SmartRefreshLayout = LayoutInflater.from(activity)
        .inflate(R.layout.fragment_pulldown_web, null) as SmartRefreshLayout
    private val mWebView: WebView = mSmartRefreshLayout.findViewById(R.id.webView)
    override fun getLayout(): ViewGroup {
        return mSmartRefreshLayout
    }

    override fun getWebView(): WebView {
        return mWebView
    }

}