package com.idormy.sms.forwarder.core.webview

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xui.widget.dialog.DialogLoader
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.app.ActivityUtils
import java.net.URISyntaxException

/**
 * WebView拦截提示
 *
 * @author xuexiang
 * @since 2019-10-21 9:51
 */
class WebViewInterceptDialog : AppCompatActivity(), DialogInterface.OnDismissListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(KEY_INTERCEPT_URL).toString()
        DialogLoader.getInstance().showConfirmDialog(
            this,
            getOpenTitle(url),
            getString(R.string.lab_yes),
            { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                if (isAppLink(url)) {
                    openAppLink(this, url)
                } else {
                    openApp(url)
                }
            },
            getString(R.string.lab_no)
        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.setOnDismissListener(this)
    }

    private fun getOpenTitle(url: String): String {
        val scheme = getScheme(url)
        return if ("mqqopensdkapi" == scheme) {
            getString(R.string.lab_open_qq_app)
        } else {
            getString(R.string.lab_open_third_app)
        }
    }

    private fun getScheme(url: String): String? {
        try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            return intent.scheme
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            Log.e("WebViewInterceptDialog", e.toString())
        }
        return ""
    }

    private fun isAppLink(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri != null && APP_LINK_HOST == uri.host && (url.startsWith("http") || url.startsWith(
            "https"
        ))
    }

    private fun openApp(url: String) {
        val intent: Intent
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            XUtil.getContext().startActivity(intent)
        } catch (e: Exception) {
            XToastUtils.error(getString(R.string.third_party_app_not_installed))
        }
    }

    private fun openAppLink(context: Context, url: String) {
        try {
            val intent = Intent(APP_LINK_ACTION)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            XToastUtils.error(getString(R.string.third_party_app_not_installed))
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        finish()
    }

    companion object {
        private const val KEY_INTERCEPT_URL = "key_intercept_url"

        // TODO: 修改你的applink
        const val APP_LINK_HOST = "ppps.cn"
        const val APP_LINK_ACTION = "com.idormy.sms.forwarder"

        /**
         * 显示WebView拦截提示
         *
         * @param url 需要拦截处理的url
         */
        @JvmStatic
        fun show(url: String?) {
            ActivityUtils.startActivity(WebViewInterceptDialog::class.java, KEY_INTERCEPT_URL, url)
        }
    }
}