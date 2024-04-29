package com.idormy.sms.forwarder.adapter.spinner

import android.graphics.drawable.Drawable

@Suppress("unused")
class TaskSpinnerItem(
    var title: CharSequence,
    var icon: Drawable? = null,
    var id: Long? = 0L,
    var status: Int? = 1
) {

    fun setTitle(title: CharSequence): TaskSpinnerItem {
        this.title = title
        return this
    }

    fun setIcon(icon: Drawable?): TaskSpinnerItem {
        this.icon = icon
        return this
    }

    fun setId(id: Long): TaskSpinnerItem {
        this.id = id
        return this
    }

    fun setStatus(status: Int): TaskSpinnerItem {
        this.status = status
        return this
    }

    // 注意：自定义实体需要重写对象的 toString 方法
    override fun toString(): String {
        return title.toString()
    }

    companion object {
        @JvmStatic
        fun of(title: CharSequence): TaskSpinnerItem {
            return TaskSpinnerItem(title)
        }

        @JvmStatic
        fun arrayOf(vararg titles: CharSequence): Array<TaskSpinnerItem> {
            return titles.map { TaskSpinnerItem(it) }.toTypedArray()
        }
    }
}
