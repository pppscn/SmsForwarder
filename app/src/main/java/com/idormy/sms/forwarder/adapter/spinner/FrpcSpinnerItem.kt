package com.idormy.sms.forwarder.adapter.spinner

import android.graphics.drawable.Drawable

@Suppress("unused")
class FrpcSpinnerItem(
    var title: CharSequence,
    var icon: Drawable? = null,
    var uid: String = "",
    //var autorun: Int? = 1,
    var status: Int? = 1
) {

    fun setTitle(title: CharSequence): FrpcSpinnerItem {
        this.title = title
        return this
    }

    fun setIcon(icon: Drawable?): FrpcSpinnerItem {
        this.icon = icon
        return this
    }

    fun setUid(uid: String): FrpcSpinnerItem {
        this.uid = uid
        return this
    }

    /*fun setAutorun(autorun: Int): FrpcSpinnerItem {
        this.autorun = autorun
        return this
    }*/

    fun setStatus(status: Int): FrpcSpinnerItem {
        this.status = status
        return this
    }

    // 注意：自定义实体需要重写对象的 toString 方法
    override fun toString(): String {
        return title.toString()
    }

    companion object {
        @JvmStatic
        fun of(title: CharSequence): FrpcSpinnerItem {
            return FrpcSpinnerItem(title)
        }

        @JvmStatic
        fun arrayOf(vararg titles: CharSequence): Array<FrpcSpinnerItem> {
            return titles.map { FrpcSpinnerItem(it) }.toTypedArray()
        }
    }
}
