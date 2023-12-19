package com.idormy.sms.forwarder.adapter.menu

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

@Suppress("LeakingThis", "UNCHECKED_CAST")
class DrawerAdapter(private val items: List<DrawerItem<out ViewHolder>>) : RecyclerView.Adapter<DrawerAdapter.ViewHolder>() {

    private val viewTypes: MutableMap<Class<out DrawerItem<*>>, Int> = HashMap()
    private val holderFactories = SparseArray<DrawerItem<*>>()

    private var listener: OnItemSelectedListener? = null

    init {
        processViewTypes()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = holderFactories.get(viewType).createViewHolder(parent)
        holder.adapter = this
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (items[position] as DrawerItem<ViewHolder>).bindViewHolder(holder)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = viewTypes[items[position]::class.java] ?: -1

    private fun processViewTypes() {
        var type = 0
        items.forEach { item ->
            if (!viewTypes.containsKey(item::class.java)) {
                viewTypes[item::class.java] = type
                holderFactories.put(type, item)
                type++
            }
        }
    }

    fun setSelected(position: Int) {
        val newChecked = items[position]
        if (!newChecked.isSelectable()) return

        items.forEachIndexed { index, item ->
            if (item.isChecked()) {
                item.setChecked(false)
                notifyItemChanged(index)
                return@forEachIndexed
            }
        }

        newChecked.setChecked(true)
        notifyItemChanged(position)

        listener?.onItemSelected(position)
    }

    fun setListener(listener: OnItemSelectedListener?) {
        this.listener = listener
    }

    @Suppress("DEPRECATION")
    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var adapter: DrawerAdapter? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            adapter?.setSelected(adapterPosition)
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelected(position: Int)
    }
}
