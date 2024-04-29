package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.TaskPagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.databinding.AdapterTasksCardViewListItemBinding
import com.idormy.sms.forwarder.entity.TaskSetting
import com.xuexiang.xutil.data.DateUtils

@Suppress("EmptyMethod")
class TaskPagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<Task, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterTasksCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            // 任务类型：＜1000为任务模板，>=1000为自定义任务
            if (item.type >= 1000) {
                holder.binding.layoutImage.visibility = View.GONE

                holder.binding.tvTime.text = DateUtils.getFriendlyTimeSpanByNow(item.lastExecTime.time)

                //遍历conditions显示图标
                holder.binding.layoutConditionsIcons.removeAllViews()
                if (item.conditions.isNotEmpty()) {
                    val conditionList = Gson().fromJson(item.conditions, Array<TaskSetting>::class.java).toMutableList()
                    for (condition in conditionList) {
                        val layoutConditionItem = View.inflate(App.context, R.layout.item_setting, null) as LinearLayout
                        val ivConditionIcon = layoutConditionItem.findViewById<ImageView>(R.id.iv_setting_icon)
                        if (item.status == 0) {
                            ivConditionIcon.setImageResource(condition.greyIconId)
                        } else {
                            ivConditionIcon.setImageResource(condition.iconId)
                        }
                        holder.binding.layoutConditionsIcons.addView(layoutConditionItem)
                    }
                }

                //遍历actions显示图标
                holder.binding.layoutActionsIcons.removeAllViews()
                if (item.actions.isNotEmpty()) {
                    val actionList = Gson().fromJson(item.actions, Array<TaskSetting>::class.java).toMutableList()
                    for (action in actionList) {
                        val layoutActionItem = View.inflate(App.context, R.layout.item_setting, null) as LinearLayout
                        val ivActionIcon = layoutActionItem.findViewById<ImageView>(R.id.iv_setting_icon)
                        if (item.status == 0) {
                            ivActionIcon.setImageResource(action.greyIconId)
                        } else {
                            ivActionIcon.setImageResource(action.iconId)
                        }
                        holder.binding.layoutActionsIcons.addView(layoutActionItem)
                    }
                }

                holder.binding.ivEdit.setOnClickListener { view: View? ->
                    itemClickListener.onItemClicked(view, item)
                }
                holder.binding.ivDelete.setOnClickListener { view: View? ->
                    itemClickListener.onItemClicked(view, item)
                }

                if (item.status == 0) {
                    holder.binding.ivArrow.setImageResource(R.drawable.auto_task_icon_left_arrow_grey)
                    holder.binding.sbEnable.isChecked = false
                } else {
                    holder.binding.ivArrow.setImageResource(R.drawable.auto_task_icon_left_arrow)
                    holder.binding.sbEnable.isChecked = true
                }
                holder.binding.sbEnable.setOnClickListener { view: View? ->
                    itemClickListener.onItemClicked(view, item)
                }
                //不能用 setOnCheckedChangeListener，否则会导致切换时状态错乱
                /*holder.binding.sbEnable.setOnCheckedChangeListener { view: View, isChecked ->
                    item.status = if (isChecked) 1 else 0
                    itemClickListener.onItemClicked(view, item)
                }*/
            } else {
                holder.binding.layoutImage.visibility = View.VISIBLE
                holder.binding.layoutIcons.visibility = View.GONE
                if (item.status == 0) {
                    holder.binding.ivArrow.setImageResource(R.drawable.auto_task_icon_left_arrow_grey)
                    holder.binding.ivImage.setImageResource(item.greyImageId)
                } else {
                    holder.binding.ivArrow.setImageResource(R.drawable.auto_task_icon_left_arrow)
                    holder.binding.ivImage.setImageResource(item.imageId)
                }
                holder.binding.ivStatus.setImageResource(item.statusImageId)
                holder.binding.ivEdit.visibility = View.GONE
                holder.binding.ivDelete.visibility = View.GONE
                holder.binding.sbEnable.visibility = View.GONE
            }
            holder.binding.tvName.text = item.name
            holder.binding.tvDescription.text = item.description
            holder.binding.ivCopy.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
        }
    }

    class MyViewHolder(val binding: AdapterTasksCardViewListItemBinding) : RecyclerView.ViewHolder(binding.root)
    interface OnItemClickListener {
        fun onItemClicked(view: View?, item: Task)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<Task> = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem === newItem
            }
        }
    }
}