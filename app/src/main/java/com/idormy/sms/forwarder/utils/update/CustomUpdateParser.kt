package com.idormy.sms.forwarder.utils.update

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.proxy.impl.AbstractUpdateParser

/**
 * 版本更新信息自定义json解析器
 * 具体使用参见： https://github.com/xuexiangjys/XUpdate/wiki/%E9%AB%98%E9%98%B6%E4%BD%BF%E7%94%A8#%E8%87%AA%E5%AE%9A%E4%B9%89%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0%E8%A7%A3%E6%9E%90%E5%99%A8
 *
 * @author xuexiang
 * @since 2020-02-18 13:01
 */
@Suppress("unused")
class CustomUpdateParser : AbstractUpdateParser() {
    @Throws(Exception::class)
    override fun parseJson(json: String): UpdateEntity? {
        // 填写自定义的json格式，如果使用默认的API就不需要设置
        return null
    }
}