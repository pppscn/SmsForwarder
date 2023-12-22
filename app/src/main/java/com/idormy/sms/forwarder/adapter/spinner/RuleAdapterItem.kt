package com.idormy.sms.forwarder.adapter.spinner

import android.graphics.drawable.Drawable

@Suppress("unused")
class RuleAdapterItem(
    var title: CharSequence,
    var icon: Drawable? = null,
    var id: Long? = 0L,
    var status: Int? = 1
) {

    fun setTitle(title: CharSequence): RuleAdapterItem {
        this.title = title
        return this
    }

    fun setIcon(icon: Drawable?): RuleAdapterItem {
        this.icon = icon
        return this
    }

    fun setId(id: Long): RuleAdapterItem {
        this.id = id
        return this
    }

    fun setStatus(status: Int): RuleAdapterItem {
        this.status = status
        return this
    }

    // 注意：自定义实体需要重写对象的 toString 方法
    override fun toString(): String {
        return title.toString()
    }

    companion object {
        @JvmStatic
        fun of(title: CharSequence): RuleAdapterItem {
            return RuleAdapterItem(title)
        }

        @JvmStatic
        fun arrayOf(vararg titles: CharSequence): Array<RuleAdapterItem> {
            return titles.map { RuleAdapterItem(it) }.toTypedArray()
        }
    }
}
