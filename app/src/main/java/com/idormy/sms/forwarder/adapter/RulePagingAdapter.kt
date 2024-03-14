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
import com.idormy.sms.forwarder.adapter.RulePagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.databinding.AdapterRulesCardViewListItemBinding

@Suppress("EmptyMethod")
class RulePagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<Rule, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterRulesCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.ivRuleImage.setImageResource(item.imageId)
            holder.binding.ivRuleStatus.setImageResource(item.statusImageId)
            holder.binding.tvRuleMatch.text = item.getName(false)

            holder.binding.layoutSenders.removeAllViews()
            for (sender in item.senderList) {
                val layoutSenderItem = View.inflate(App.context, R.layout.item_sender, null) as LinearLayout
                val ivSenderImage = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_image)
                val ivSenderStatus = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_status)
                val tvSenderName = layoutSenderItem.findViewById<TextView>(R.id.tv_sender_name)
                ivSenderImage.setImageResource(sender.imageId)
                ivSenderStatus.setImageResource(sender.statusImageId)
                tvSenderName.text = sender.name
                holder.binding.layoutSenders.addView(layoutSenderItem)
            }

            holder.binding.ivCopy.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
            holder.binding.ivEdit.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
            holder.binding.ivDelete.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
        }
    }

    class MyViewHolder(val binding: AdapterRulesCardViewListItemBinding) : RecyclerView.ViewHolder(binding.root)
    interface OnItemClickListener {
        fun onItemClicked(view: View?, item: Rule)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<Rule> = object : DiffUtil.ItemCallback<Rule>() {
            override fun areItemsTheSame(oldItem: Rule, newItem: Rule): Boolean {
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Rule, newItem: Rule): Boolean {
                return oldItem === newItem
            }
        }
    }
}