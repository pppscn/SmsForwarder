package com.idormy.sms.forwarder.adapter.menu

import android.content.Context
import android.view.View
import android.view.ViewGroup

class SpaceItem(private val spaceDp: Int) : DrawerItem<SpaceItem.ViewHolder>() {

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        val context: Context = parent.context
        val view = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (context.resources.displayMetrics.density * spaceDp).toInt()
            )
        }
        return ViewHolder(view)
    }

    override fun bindViewHolder(holder: ViewHolder) {}

    override fun isSelectable(): Boolean = false

    class ViewHolder(itemView: View) : DrawerAdapter.ViewHolder(itemView)
}
