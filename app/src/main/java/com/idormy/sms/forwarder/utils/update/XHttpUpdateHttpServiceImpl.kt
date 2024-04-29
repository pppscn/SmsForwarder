package com.idormy.sms.forwarder.utils.update

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.XHttpSDK
import com.xuexiang.xhttp2.callback.DownloadProgressCallBack
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.proxy.IUpdateHttpService.DownloadCallback
import com.xuexiang.xutil.file.FileUtils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResUtils.getString

/**
 * XHttp2实现的请求更新
 *
 * @author xuexiang
 * @since 2018/8/12 上午11:46
 */
class XHttpUpdateHttpServiceImpl : IUpdateHttpService {
    override fun asyncGet(
        url: String,
        params: Map<String, Any>,
        callBack: IUpdateHttpService.Callback,
    ) {
        XHttp.get(url)
            .ignoreHttpsCert()
            .params(params)
            .keepJson(true)
            .execute(object : SimpleCallBack<String>() {
                @Throws(Throwable::class)
                override fun onSuccess(response: String) {
                    callBack.onSuccess(response)
                }

                override fun onError(e: ApiException) {
                    callBack.onError(e)
                }
            })
    }

    override fun asyncPost(
        url: String,
        params: Map<String, Any>,
        callBack: IUpdateHttpService.Callback,
    ) {
        XHttp.post(url)
            .ignoreHttpsCert()
            .upJson(JsonUtil.toJson(params))
            .keepJson(true)
            .execute(object : SimpleCallBack<String>() {
                @Throws(Throwable::class)
                override fun onSuccess(response: String) {
                    callBack.onSuccess(response)
                }

                override fun onError(e: ApiException) {
                    callBack.onError(e)
                }
            })
    }

    override fun download(url: String, path: String, fileName: String, callback: DownloadCallback) {
        XHttpSDK.addRequest(
            url, XHttp.downLoad(url)
                .ignoreHttpsCert()
                .savePath(path)
                .saveName(fileName)
                .isUseBaseUrl(false)
                .execute(object : DownloadProgressCallBack<String?>() {
                    override fun onStart() {
                        callback.onStart()
                    }

                    override fun onError(e: ApiException) {
                        callback.onError(e)
                    }

                    override fun update(downLoadSize: Long, totalSize: Long, done: Boolean) {
                        callback.onProgress(downLoadSize / totalSize.toFloat(), totalSize)
                    }

                    override fun onComplete(path: String) {
                        callback.onSuccess(FileUtils.getFileByPath(path))
                    }
                })
        )
    }

    override fun cancelDownload(url: String) {
        XToastUtils.info(getString(R.string.update_cancelled))
        XHttpSDK.cancelRequest(url)
    }
}