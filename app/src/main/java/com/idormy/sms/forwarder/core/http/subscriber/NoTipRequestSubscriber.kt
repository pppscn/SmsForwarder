package com.idormy.sms.forwarder.core.http.subscriber

import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xhttp2.model.XHttpRequest
import com.xuexiang.xhttp2.subsciber.BaseSubscriber
import com.xuexiang.xutil.common.StringUtils
import com.xuexiang.xutil.common.logger.Logger

/**
 * 不带错误toast提示的网络请求订阅，只存储错误的日志
 *
 * @author xuexiang
 * @since 2019-11-18 23:11
 */
@Suppress("unused")
abstract class NoTipRequestSubscriber<T> : BaseSubscriber<T> {
    /**
     * 记录一下请求的url,确定出错的请求是哪个请求
     */
    private var mUrl: String? = null

    constructor()
    constructor(req: XHttpRequest) : this(req.url)
    constructor(url: String?) {
        mUrl = url
    }

    public override fun onError(e: ApiException) {
        if (!StringUtils.isEmpty(mUrl)) {
            Logger.e("Request Url: $mUrl", e)
        } else {
            Logger.e(e)
        }
    }
}