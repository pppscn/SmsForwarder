package com.idormy.sms.forwarder.adapter.spinner

import android.content.Context
import android.graphics.drawable.Drawable
import com.xuexiang.xui.utils.ResUtils

@Suppress("unused")
class SenderAdapterItem {

    //标题内容
    var title: CharSequence

    //图标
    var icon: Drawable? = null

    //ID
    var id: Long? = 0L

    //状态
    var status: Int? = 1

    constructor(title: CharSequence) {
        this.title = title
    }

    constructor(title: CharSequence, icon: Drawable?) {
        this.title = title
        this.icon = icon
    }

    constructor(title: CharSequence, icon: Drawable?, id: Long?) {
        this.title = title
        this.icon = icon
        this.id = id
    }

    constructor(title: CharSequence, icon: Drawable?, id: Long?, status: Int?) {
        this.title = title
        this.icon = icon
        this.id = id
        this.status = status
    }

    constructor(title: CharSequence, drawableId: Int) : this(title, ResUtils.getDrawable(drawableId))
    constructor(title: CharSequence, drawableId: Int, id: Long) : this(title, ResUtils.getDrawable(drawableId), id)
    constructor(title: CharSequence, drawableId: Int, id: Long, status: Int) : this(title, ResUtils.getDrawable(drawableId), id, status)
    constructor(context: Context?, titleId: Int, drawableId: Int) : this(ResUtils.getString(titleId), ResUtils.getDrawable(context, drawableId))
    constructor(context: Context?, titleId: Int, drawableId: Int, id: Long) : this(ResUtils.getString(titleId), ResUtils.getDrawable(context, drawableId), id)
    constructor(context: Context?, titleId: Int, drawableId: Int, id: Long, status: Int) : this(ResUtils.getString(titleId), ResUtils.getDrawable(context, drawableId), id, status)
    constructor(context: Context?, title: CharSequence, drawableId: Int) : this(title, ResUtils.getDrawable(context, drawableId))
    constructor(context: Context?, title: CharSequence, drawableId: Int, id: Long) : this(title, ResUtils.getDrawable(context, drawableId), id)
    constructor(context: Context?, title: CharSequence, drawableId: Int, id: Long, status: Int) : this(title, ResUtils.getDrawable(context, drawableId), id, status)

    fun setStatus(status: Int): SenderAdapterItem {
        this.status = status
        return this
    }

    fun setId(id: Long): SenderAdapterItem {
        this.id = id
        return this
    }

    fun setTitle(title: CharSequence): SenderAdapterItem {
        this.title = title
        return this
    }

    fun setIcon(icon: Drawable?): SenderAdapterItem {
        this.icon = icon
        return this
    }

    //注意：自定义实体需要重写对象的toString方法
    override fun toString(): String {
        return title.toString()
    }

    companion object {
        fun of(title: CharSequence): SenderAdapterItem {
            return SenderAdapterItem(title)
        }

        fun arrayof(title: Array<CharSequence>): Array<SenderAdapterItem?> {
            val array = arrayOfNulls<SenderAdapterItem>(title.size)
            for (i in array.indices) {
                array[i] = SenderAdapterItem(title[i])
            }
            return array
        }
    }
}
