package com.idormy.sms.forwarder.core.webview

import android.webkit.JsResult
import android.webkit.WebView
import com.idormy.sms.forwarder.utils.Log
import com.just.agentweb.core.client.MiddlewareWebChromeBase

/**
 * WebChrome（WebChromeClient主要辅助WebView处理JavaScript的对话框、网站图片、网站title、加载进度等）中间件
 * 【浏览器】
 * @author xuexiang
 * @since 2019/1/4 下午11:31
 */
open class MiddlewareChromeClient : MiddlewareWebChromeBase() {
    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        Log.i("Info", "onJsAlert:$url")
        return super.onJsAlert(view, url, message, result)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        Log.i("Info", "onProgressChanged:")
    }
}