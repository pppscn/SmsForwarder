package com.idormy.sms.forwarder.core.http.api

import com.idormy.sms.forwarder.core.http.entity.TipInfo
import com.xuexiang.xhttp2.model.ApiResult
import io.reactivex.Observable
import retrofit2.http.GET

/**
 * @author xuexiang
 * @since 2021/1/9 7:01 PM
 */
@Suppress("unused")
class ApiService {
    /**
     * 使用的是retrofit的接口定义
     */
    interface IGetService {
        /**
         * 获得小贴士
         */
        @get:GET("/pp/SmsForwarder.wiki/raw/master/tips.json")
        val tips: Observable<ApiResult<List<TipInfo?>?>>
    }
}