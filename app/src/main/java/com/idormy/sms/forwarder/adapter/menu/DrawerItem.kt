package com.idormy.sms.forwarder.adapter.menu

import android.view.ViewGroup

abstract class DrawerItem<T : DrawerAdapter.ViewHolder> {
    private var isChecked = false

    abstract fun createViewHolder(parent: ViewGroup): T
    abstract fun bindViewHolder(holder: T)

    fun setChecked(checked: Boolean): DrawerItem<T> {
        isChecked = checked
        return this
    }

    fun isChecked(): Boolean = isChecked

    open fun isSelectable(): Boolean = true
}
