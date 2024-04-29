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
import com.idormy.sms.forwarder.adapter.base.ItemMoveCallback
import com.idormy.sms.forwarder.database.entity.Rule
import java.util.Collections

@Suppress("DEPRECATION")
class RuleRecyclerAdapter(
    var itemList: MutableList<Rule>,
    private var removeClickListener: ((Int) -> Unit)? = null,
    private var editClickListener: ((Int) -> Unit)? = null,
) : RecyclerView.Adapter<RuleRecyclerAdapter.ViewHolder>(), ItemMoveCallback.Listener {

    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_rule_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemList.size

    fun setTouchHelper(touchHelper: ItemTouchHelper) {
        this@RuleRecyclerAdapter.touchHelper = touchHelper
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val image: ImageView = itemView.findViewById(R.id.iv_image)
        private val status: ImageView = itemView.findViewById(R.id.iv_status)
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val editIcon: ImageView = itemView.findViewById(R.id.iv_edit)
        private val removeIcon: ImageView = itemView.findViewById(R.id.iv_remove)
        private val dragIcon: ImageView = itemView.findViewById(R.id.iv_drag)

        init {
            if (removeClickListener == null) {
                removeIcon.visibility = View.GONE
            } else {
                removeIcon.setOnClickListener(this)
            }

            if (editClickListener == null) {
                editIcon.visibility = View.GONE
            } else {
                editIcon.setOnClickListener(this)
            }

            dragIcon.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this)
                }
                return@setOnTouchListener false
            }
        }

        fun bind(rule: Rule) {
            val icon = when (rule.type) {
                "sms" -> R.drawable.auto_task_icon_sms
                "call" -> R.drawable.auto_task_icon_incall
                "app" -> R.drawable.auto_task_icon_start_activity
                else -> R.drawable.auto_task_icon_sms
            }
            image.setImageResource(icon)
            status.setImageResource(rule.statusImageId)
            title.text = rule.getName()
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                when (v?.id) {
                    R.id.iv_edit -> editClickListener?.let { it(position) }
                    R.id.iv_remove -> removeClickListener?.let { it(position) }
                }
            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(itemList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(itemList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onDragFinished() {}
}

