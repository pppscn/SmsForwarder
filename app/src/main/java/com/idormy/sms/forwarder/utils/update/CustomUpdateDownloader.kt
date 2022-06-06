package com.idormy.sms.forwarder.utils.update

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateDownloader
import com.xuexiang.xupdate.service.OnFileDownloadListener
import com.xuexiang.xutil.app.ActivityUtils

/**
 * 重写DefaultUpdateDownloader，在取消下载时，弹出提示
 *
 * @author xuexiang
 * @since 2019-06-14 23:47
 */
class CustomUpdateDownloader : DefaultUpdateDownloader() {
    private var mIsStartDownload = false
    override fun startDownload(
        updateEntity: UpdateEntity,
        downloadListener: OnFileDownloadListener?,
    ) {
        super.startDownload(updateEntity, downloadListener)
        mIsStartDownload = true
    }

    override fun cancelDownload() {
        super.cancelDownload()
        if (mIsStartDownload) {
            mIsStartDownload = false
            ActivityUtils.startActivity(UpdateTipDialog::class.java)
        }
    }
}