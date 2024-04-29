package com.idormy.sms.forwarder.adapter.spinner

import android.graphics.drawable.Drawable

@Suppress("unused")
class RuleSpinnerItem(
    var title: CharSequence,
    var icon: Drawable? = null,
    var id: Long? = 0L,
    var status: Int? = 1
) {

    fun setTitle(title: CharSequence): RuleSpinnerItem {
        this.title = title
        return this
    }

    fun setIcon(icon: Drawable?): RuleSpinnerItem {
        this.icon = icon
        return this
    }

    fun setId(id: Long): RuleSpinnerItem {
        this.id = id
        return this
    }

    fun setStatus(status: Int): RuleSpinnerItem {
        this.status = status
        return this
    }

    // 注意：自定义实体需要重写对象的 toString 方法
    override fun toString(): String {
        return title.toString()
    }

    companion object {
        @JvmStatic
        fun of(title: CharSequence): RuleSpinnerItem {
            return RuleSpinnerItem(title)
        }

        @JvmStatic
        fun arrayOf(vararg titles: CharSequence): Array<RuleSpinnerItem> {
            return titles.map { RuleSpinnerItem(it) }.toTypedArray()
        }
    }
}
