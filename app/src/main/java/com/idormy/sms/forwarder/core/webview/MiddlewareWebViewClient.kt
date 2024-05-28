package com.idormy.sms.forwarder.core.webview

import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.webview.WebViewInterceptDialog.Companion.show
import com.idormy.sms.forwarder.utils.Log
import com.just.agentweb.core.client.MiddlewareWebClientBase
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import java.util.Locale

/**
 * 【网络请求、加载】
 * WebClient（WebViewClient 这个类主要帮助WebView处理各种通知、url加载，请求时间的）中间件
 *
 *
 *
 *
 * 方法的执行顺序，例如下面用了7个中间件一个 WebViewClient
 *
 *
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 1
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 2
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 3
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 4
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 5
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 6
 * .useMiddlewareWebClient(getMiddlewareWebClient())  // 7
 * DefaultWebClient                                  // 8
 * .setWebViewClient(mWebViewClient)                 // 9
 *
 *
 *
 *
 * 典型的洋葱模型
 * 对象内部的方法执行顺序: 1->2->3->4->5->6->7->8->9->8->7->6->5->4->3->2->1
 *
 *
 *
 *
 * 中断中间件的执行， 删除super.methodName(...) 这行即可
 *
 *
 * 这里主要是做去广告的工作
 */
@Suppress("UNUSED_PARAMETER", "DEPRECATION", "OVERRIDE_DEPRECATION")
open class MiddlewareWebViewClient : MiddlewareWebClientBase() {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        Log.i(
            "Info",
            "MiddlewareWebViewClient -- >  shouldOverrideUrlLoading:" + request.url.toString() + "  c:" + count++
        )
        return if (shouldOverrideUrlLoadingByApp(view, request.url.toString())) {
            true
        } else super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        Log.i(
            "Info",
            "MiddlewareWebViewClient -- >  shouldOverrideUrlLoading:" + url + "  c:" + count++
        )
        return if (shouldOverrideUrlLoadingByApp(view, url)) {
            true
        } else super.shouldOverrideUrlLoading(view, url)
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        val tUrl = url.lowercase(Locale.ROOT)
        return if (!hasAdUrl(tUrl)) {
            //正常加载
            super.shouldInterceptRequest(view, tUrl)
        } else {
            //含有广告资源屏蔽请求
            WebResourceResponse(null, null, null)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url.toString().lowercase(Locale.ROOT)
        return if (!hasAdUrl(url)) {
            //正常加载
            super.shouldInterceptRequest(view, request)
        } else {
            //含有广告资源屏蔽请求
            WebResourceResponse(null, null, null)
        }
    }

    /**
     * 根据url的scheme处理跳转第三方app的业务,true代表拦截，false代表不拦截
     */
    private fun shouldOverrideUrlLoadingByApp(webView: WebView, url: String): Boolean {
        if (url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp")) {
            //不拦截http, https, ftp的请求
            val uri = Uri.parse(url)
            if (uri != null && !(WebViewInterceptDialog.APP_LINK_HOST == uri.host && url.contains("xpage"))) {
                return false
            }
        }
        show(url)
        return true
    }

    companion object {
        private var count = 1

        /**
         * 判断是否存在广告的链接
         *
         * @param url
         * @return
         */
        private fun hasAdUrl(url: String): Boolean {
            val adUrls = getStringArray(R.array.adBlockUrl)
            for (adUrl in adUrls) {
                if (url.contains(adUrl)) {
                    return true
                }
            }
            return false
        }
    }
}