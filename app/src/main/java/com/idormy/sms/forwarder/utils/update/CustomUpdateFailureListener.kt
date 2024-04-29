package com.idormy.sms.forwarder.utils.update

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.update.UpdateTipDialog.Companion.show
import com.xuexiang.xupdate.entity.UpdateError
import com.xuexiang.xupdate.listener.OnUpdateFailureListener
import com.xuexiang.xutil.resource.ResUtils.getString

/**
 * 自定义版本更新提示
 *
 * @author xuexiang
 * @since 2019/4/15 上午12:01
 */
class CustomUpdateFailureListener @JvmOverloads constructor(
    /**
     * 是否需要错误提示
     */
    private val mNeedErrorTip: Boolean = true,
) : OnUpdateFailureListener {
    /**
     * 更新失败
     *
     * @param error 错误
     */
    override fun onFailure(error: UpdateError) {
        if (mNeedErrorTip) {
            if (error.detailMsg.contains("{\"code\":-1,\"msg\":null,\"data\":null}")) {
                XToastUtils.success(getString(R.string.no_new_version))
            } else {
                XToastUtils.error(error)
            }
        }
        if (error.code == UpdateError.ERROR.DOWNLOAD_FAILED) {
            show(String.format(getString(R.string.download_failed_switch_download_url), UpdateTipDialog.DOWNLOAD_TYPE_NAME))
        }
    }
}