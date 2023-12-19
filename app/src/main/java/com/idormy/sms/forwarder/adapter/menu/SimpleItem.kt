package com.idormy.sms.forwarder.adapter.menu

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.idormy.sms.forwarder.R

class SimpleItem(
    private val icon: Drawable,
    private val title: String,
    private var selectedItemIconTint: Int = 0,
    private var selectedItemTextTint: Int = 0,
    private var normalItemIconTint: Int = 0,
    private var normalItemTextTint: Int = 0
) : DrawerItem<SimpleItem.ViewHolder>() {

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.menu_item_option, parent, false)
        return ViewHolder(v)
    }

    override fun bindViewHolder(holder: ViewHolder) {
        holder.title.text = title
        holder.icon.setImageDrawable(icon)

        holder.title.setTextColor(if (isChecked()) selectedItemTextTint else normalItemTextTint)
        holder.icon.setColorFilter(if (isChecked()) selectedItemIconTint else normalItemIconTint)
    }

    fun withSelectedIconTint(selectedItemIconTint: Int): SimpleItem = apply {
        this.selectedItemIconTint = selectedItemIconTint
    }

    fun withSelectedTextTint(selectedItemTextTint: Int): SimpleItem = apply {
        this.selectedItemTextTint = selectedItemTextTint
    }

    fun withIconTint(normalItemIconTint: Int): SimpleItem = apply {
        this.normalItemIconTint = normalItemIconTint
    }

    fun withTextTint(normalItemTextTint: Int): SimpleItem = apply {
        this.normalItemTextTint = normalItemTextTint
    }

    class ViewHolder(itemView: View) : DrawerAdapter.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val title: TextView = itemView.findViewById(R.id.title)
    }
}
