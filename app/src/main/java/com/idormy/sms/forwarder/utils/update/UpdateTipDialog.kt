package com.idormy.sms.forwarder.utils.update

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.goWeb
import com.xuexiang.xui.widget.dialog.DialogLoader
import com.xuexiang.xupdate.XUpdate

/**
 * 版本更新提示弹窗
 *
 * @author xuexiang
 * @since 2019-06-15 00:06
 */
class UpdateTipDialog : AppCompatActivity(), DialogInterface.OnDismissListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var content = intent.getStringExtra(KEY_CONTENT)
        if (TextUtils.isEmpty(content)) {
            content = String.format(getString(R.string.download_slow_switch_download_url), DOWNLOAD_TYPE_NAME)
        }
        DialogLoader.getInstance()
            .showConfirmDialog(this, content, getString(R.string.yes), { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                goWeb(this@UpdateTipDialog, DOWNLOAD_URL)
            }, getString(R.string.no))
            .setOnDismissListener(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        finish()
    }

    companion object {
        const val KEY_CONTENT = "com.idormy.sms.forwarder.utils.update.KEY_CONTENT"

        // 填写你应用下载类型名
        const val DOWNLOAD_TYPE_NAME = "GitHub"

        // 填写你应用下载页面的链接
        private const val DOWNLOAD_URL = "https://github.com/pppscn/SmsForwarder/releases"

        /**
         * 显示版本更新重试提示弹窗
         *
         * @param content
         */
        @JvmStatic
        fun show(content: String?) {
            val intent = Intent(XUpdate.getContext(), UpdateTipDialog::class.java)
            intent.putExtra(KEY_CONTENT, content)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            XUpdate.getContext().startActivity(intent)
        }
    }
}