package com.idormy.sms.forwarder.core.http.loader

import android.content.Context
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import com.xuexiang.xhttp2.subsciber.impl.OnProgressCancelListener
import com.xuexiang.xui.widget.dialog.MiniLoadingDialog

/**
 * 默认进度加载
 *
 * @author xuexiang
 * @since 2019-11-18 23:07
 */
class MiniLoadingDialogLoader @JvmOverloads constructor(
    context: Context?,
    msg: String? = "Loading...",
) : IProgressLoader {
    /**
     * 进度loading弹窗
     */
    private val mDialog: MiniLoadingDialog?

    /**
     * 进度框取消监听
     */
    private var mOnProgressCancelListener: OnProgressCancelListener? = null
    override fun isLoading(): Boolean {
        return mDialog != null && mDialog.isShowing
    }

    override fun updateMessage(msg: String) {
        mDialog?.updateMessage(msg)
    }

    override fun showLoading() {
        if (mDialog != null && !mDialog.isShowing) {
            mDialog.show()
        }
    }

    override fun dismissLoading() {
        if (mDialog != null && mDialog.isShowing) {
            mDialog.dismiss()
        }
    }

    override fun setCancelable(flag: Boolean) {
        mDialog!!.setCancelable(flag)
        if (flag) {
            mDialog.setOnCancelListener {
                if (mOnProgressCancelListener != null) {
                    mOnProgressCancelListener!!.onCancelProgress()
                }
            }
        }
    }

    override fun setOnProgressCancelListener(listener: OnProgressCancelListener) {
        mOnProgressCancelListener = listener
    }

    init {
        mDialog = MiniLoadingDialog(context, msg)
    }
}