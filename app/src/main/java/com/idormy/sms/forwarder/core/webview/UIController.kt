package com.idormy.sms.forwarder.core.webview

import android.app.Activity
import android.os.Handler
import com.idormy.sms.forwarder.utils.Log
import android.webkit.WebView
import com.just.agentweb.core.web.AgentWebUIControllerImplBase
import java.lang.ref.WeakReference

/**
 * 如果你需要修改某一个AgentWeb 内部的某一个弹窗 ，请看下面的例子
 * 注意写法一定要参照 DefaultUIController 的写法 ，因为UI自由定制，但是回调的方式是固定的，并且一定要回调。
 *
 * @author xuexiang
 * @since 2019-10-30 23:18
 */
@Suppress("unused")
class UIController(activity: Activity) : AgentWebUIControllerImplBase() {
    private val mActivity: WeakReference<Activity> = WeakReference(activity)
    override fun onShowMessage(message: String, from: String) {
        super.onShowMessage(message, from)
        Log.i(TAG, "message:$message")
    }

    override fun onSelectItemsPrompt(
        view: WebView,
        url: String,
        items: Array<String>,
        callback: Handler.Callback,
    ) {
        // 使用默认的UI
        super.onSelectItemsPrompt(view, url, items, callback)
    }

}