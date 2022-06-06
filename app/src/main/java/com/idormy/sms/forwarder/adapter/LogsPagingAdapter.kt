package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.adapter.LogsPagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.LogsAndRuleAndSender
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.databinding.AdapterLogsCardViewListItemBinding
import com.xuexiang.xutil.data.DateUtils

class LogsPagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<LogsAndRuleAndSender, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterLogsCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.tvFrom.text = item.logs.from
            holder.binding.tvTime.text = DateUtils.getFriendlyTimeSpanByNow(item.logs.time)
            holder.binding.tvContent.text = item.logs.content
            holder.binding.ivSenderImage.setImageResource(Sender.getImageId(item.relation.sender.type))
            holder.binding.ivStatusImage.setImageResource(item.logs.statusImageId)
            holder.binding.ivSimImage.setImageResource(item.logs.simImageId)

            holder.binding.cardView.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
        }
    }

    class MyViewHolder(val binding: AdapterLogsCardViewListItemBinding) : RecyclerView.ViewHolder(binding.root)
    interface OnItemClickListener {
        fun onItemClicked(view: View?, item: LogsAndRuleAndSender)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<LogsAndRuleAndSender> = object : DiffUtil.ItemCallback<LogsAndRuleAndSender>() {
            override fun areItemsTheSame(oldItem: LogsAndRuleAndSender, newItem: LogsAndRuleAndSender): Boolean {
                return oldItem.logs.id == newItem.logs.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: LogsAndRuleAndSender, newItem: LogsAndRuleAndSender): Boolean {
                return oldItem.logs === newItem.logs
            }
        }
    }
}