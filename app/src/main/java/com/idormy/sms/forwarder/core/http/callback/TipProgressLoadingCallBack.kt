package com.idormy.sms.forwarder.core.http.callback

import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xhttp2.callback.ProgressLoadingCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xhttp2.model.XHttpRequest
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader
import com.xuexiang.xutil.common.StringUtils
import com.xuexiang.xutil.common.logger.Logger

/**
 * 带错误toast提示和加载进度条的网络请求回调
 *
 * @author xuexiang
 * @since 2019-11-18 23:16
 */
@Suppress("unused")
abstract class TipProgressLoadingCallBack<T> : ProgressLoadingCallBack<T> {
    /**
     * 记录一下请求的url,确定出错的请求是哪个请求
     */
    private var mUrl: String? = null

    constructor(fragment: BaseFragment<*>) : super(fragment.progressLoader)
    constructor(iProgressLoader: IProgressLoader?) : super(iProgressLoader)
    constructor(req: XHttpRequest, iProgressLoader: IProgressLoader?) : this(
        req.url,
        iProgressLoader
    )

    constructor(url: String?, iProgressLoader: IProgressLoader?) : super(iProgressLoader) {
        mUrl = url
    }

    override fun onError(e: ApiException) {
        super.onError(e)
        XToastUtils.error(e)
        if (!StringUtils.isEmpty(mUrl)) {
            Logger.e("Request Url: $mUrl", e)
        } else {
            Logger.e(e)
        }
    }
}