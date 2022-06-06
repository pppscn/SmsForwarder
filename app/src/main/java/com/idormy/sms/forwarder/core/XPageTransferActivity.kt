package com.idormy.sms.forwarder.core

import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.annotation.Router
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xutil.common.StringUtils

/**
 * https://xuexiangjys.club/xpage/transfer?pageName=xxxxx&....
 * applink的中转
 *
 * @author xuexiang
 * @since 2019-07-06 9:37
 */
@Router(path = "/xpage/transfer")
class XPageTransferActivity : BaseActivity<ViewBinding?>() {

    @JvmField
    @AutoWired(name = "pageName")
    var pageName: Nothing? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        XRouter.getInstance().inject(this)
        if (!StringUtils.isEmpty(pageName)) {
            if (openPage(pageName, intent.extras) == null) {
                XToastUtils.error(getString(R.string.page_not_found))
                finish()
            }
        } else {
            XToastUtils.error(getString(R.string.page_not_found))
            finish()
        }
    }
}