package com.idormy.sms.forwarder.adapter.spinner

import android.graphics.drawable.Drawable

@Suppress("unused")
class FrpcAdapterItem(
    var title: CharSequence,
    var icon: Drawable? = null,
    var uid: String = "",
    var autorun: Int? = 1
) {

    fun setTitle(title: CharSequence): FrpcAdapterItem {
        this.title = title
        return this
    }

    fun setIcon(icon: Drawable?): FrpcAdapterItem {
        this.icon = icon
        return this
    }

    fun setUid(uid: String): FrpcAdapterItem {
        this.uid = uid
        return this
    }

    fun setAutorun(autorun: Int): FrpcAdapterItem {
        this.autorun = autorun
        return this
    }

    // 注意：自定义实体需要重写对象的 toString 方法
    override fun toString(): String {
        return title.toString()
    }

    companion object {
        @JvmStatic
        fun of(title: CharSequence): FrpcAdapterItem {
            return FrpcAdapterItem(title)
        }

        @JvmStatic
        fun arrayOf(vararg titles: CharSequence): Array<FrpcAdapterItem> {
            return titles.map { FrpcAdapterItem(it) }.toTypedArray()
        }
    }
}
