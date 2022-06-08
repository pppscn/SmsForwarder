package com.idormy.sms.forwarder.adapter.spinner

import android.graphics.drawable.Drawable
import com.xuexiang.xui.utils.ResUtils

@Suppress("unused")
class AppListAdapterItem {

    var name: String = ""
    var icon: Drawable? = null
    var packageName: String? = null
    //var packagePath: String? = null
    //var versionName: String? = null
    //var versionCode: Int = 0
    //var isSystem: Boolean = false


    constructor(name: String, icon: Drawable?, packageName: String?) {
        this.name = name
        this.icon = icon
        this.packageName = packageName
    }

    constructor(name: String) : this(name, null, null)
    constructor(name: String, drawableId: Int, packageName: String) : this(name, ResUtils.getDrawable(drawableId), packageName)

    //注意：自定义实体需要重写对象的toString方法
    override fun toString(): String {
        return name
    }

    companion object {
        fun of(name: String): AppListAdapterItem {
            return AppListAdapterItem(name)
        }

        fun arrayof(title: Array<String>): Array<AppListAdapterItem?> {
            val array = arrayOfNulls<AppListAdapterItem>(title.size)
            for (i in array.indices) {
                array[i] = AppListAdapterItem(title[i])
            }
            return array
        }
    }
}
