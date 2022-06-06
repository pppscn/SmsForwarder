package com.idormy.sms.forwarder.core.http.entity

import androidx.annotation.Keep

/**
 * @author xuexiang
 * @since 2019-08-28 15:35
 */
@Keep
class TipInfo {
    /**
     * title : 小贴士3
     * content :
     *
     *欢迎关注我的微信公众号：我的Android开源之旅。
     *
     *<br></br>
     */
    var title: String? = null
    var content: String? = null
    override fun toString(): String {
        return "TipInfo{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}'
    }
}