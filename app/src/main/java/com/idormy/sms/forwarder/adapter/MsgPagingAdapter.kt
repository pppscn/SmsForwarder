package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.MsgPagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.LogsDetail
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.databinding.AdapterLogsCardViewListItemBinding
import com.xuexiang.xutil.data.DateUtils

@Suppress("EmptyMethod")
class MsgPagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<MsgAndLogs, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterLogsCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.tvFrom.text = item.msg.from
            holder.binding.tvTime.text = DateUtils.getFriendlyTimeSpanByNow(item.msg.time)
            holder.binding.tvContent.text = item.msg.content
            //holder.binding.ivSenderImage.setImageResource(Sender.getImageId(item.sender.type))
            //holder.binding.ivStatusImage.setImageResource(item.msg.statusImageId)
            holder.binding.ivSimImage.setImageResource(item.msg.simImageId)

            holder.binding.layoutLogs.removeAllViews()
            for (logs in item.logsList) {
                val layoutSenderItem = View.inflate(App.context, R.layout.item_logs, null) as LinearLayout
                val ivSenderImage = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_image)
                val ivSenderStatus = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_status)
                val tvSenderName = layoutSenderItem.findViewById<TextView>(R.id.tv_sender_name)
                ivSenderImage.setImageResource(logs.senderImageId)
                ivSenderStatus.setImageResource(logs.statusImageId)
                tvSenderName.text = logs.senderName
                layoutSenderItem.setOnClickListener { view: View? ->
                    itemClickListener.onLogsClicked(view, logs)
                }
                holder.binding.layoutLogs.addView(layoutSenderItem)
            }

            holder.binding.cardView.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
        }
    }

    class MyViewHolder(val binding: AdapterLogsCardViewListItemBinding) : RecyclerView.ViewHolder(binding.root)
    interface OnItemClickListener {
        fun onItemClicked(view: View?, item: MsgAndLogs)
        fun onLogsClicked(view: View?, item: LogsDetail)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<MsgAndLogs> = object : DiffUtil.ItemCallback<MsgAndLogs>() {
            override fun areItemsTheSame(oldItem: MsgAndLogs, newItem: MsgAndLogs): Boolean {
                return oldItem.msg.id == newItem.msg.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: MsgAndLogs, newItem: MsgAndLogs): Boolean {
                return oldItem.msg === newItem.msg
            }
        }
    }
}