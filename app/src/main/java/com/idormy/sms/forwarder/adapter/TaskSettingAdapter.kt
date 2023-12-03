@file:Suppress("DEPRECATION")

package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.task.TaskSetting

class TaskSettingAdapter(
    val itemList: MutableList<TaskSetting>,
    private val editClickListener: (Int) -> Unit,
    private val removeClickListener: (Int) -> Unit
) : RecyclerView.Adapter<TaskSettingAdapter.ViewHolder>(), ItemMoveCallback.Listener {

    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_task_setting_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemList.size

    fun setTouchHelper(touchHelper: ItemTouchHelper) {
        this@TaskSettingAdapter.touchHelper = touchHelper
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val icon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val description: TextView = itemView.findViewById(R.id.tv_description)
        private val editIcon: ImageView = itemView.findViewById(R.id.iv_edit)
        private val removeIcon: ImageView = itemView.findViewById(R.id.iv_remove)
        private val dragIcon: ImageView = itemView.findViewById(R.id.iv_drag)

        init {
            editIcon.setOnClickListener(this)
            removeIcon.setOnClickListener(this)

            dragIcon.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this)
                }
                return@setOnTouchListener false
            }
        }

        fun bind(taskSetting: TaskSetting) {
            icon.setImageResource(taskSetting.iconId)
            title.text = taskSetting.title
            description.text = taskSetting.description
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                when (v?.id) {
                    R.id.iv_edit -> editClickListener(position)
                    R.id.iv_remove -> removeClickListener(position)
                }
            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                itemList[i] = itemList.set(i + 1, itemList[i])
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                itemList[i] = itemList.set(i - 1, itemList[i])
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onDragFinished() {
        TODO("Not yet implemented")
    }
}

class ItemMoveCallback(private val listener: Listener) : ItemTouchHelper.Callback() {

    interface Listener {
        fun onItemMove(fromPosition: Int, toPosition: Int)
        fun onDragFinished()
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        listener.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Swiping is not needed for this example
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            listener.onDragFinished()
        }
    }
}
