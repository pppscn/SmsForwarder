package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.RulePagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.RuleAndSender
import com.idormy.sms.forwarder.databinding.AdapterRulesCardViewListItemBinding

class RulePagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<RuleAndSender, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterRulesCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.ivRuleImage.setImageResource(item.rule.imageId)
            holder.binding.ivRuleStatus.setImageResource(item.rule.statusImageId)
            holder.binding.tvRuleMatch.text = item.rule.ruleMatch
            holder.binding.ivSenderImage.setImageResource(item.sender.imageId)
            holder.binding.ivSenderStatus.setImageResource(item.sender.statusImageId)
            holder.binding.tvSenderName.text = item.sender.name

            /*holder.binding.cardView.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }*/
            holder.binding.ivCopy.setImageResource(R.drawable.ic_copy)
            holder.binding.ivEdit.setImageResource(R.drawable.ic_edit)
            holder.binding.ivDelete.setImageResource(R.drawable.ic_delete)
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
        fun onItemClicked(view: View?, item: RuleAndSender)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<RuleAndSender> = object : DiffUtil.ItemCallback<RuleAndSender>() {
            override fun areItemsTheSame(oldItem: RuleAndSender, newItem: RuleAndSender): Boolean {
                return oldItem.rule.id == newItem.rule.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: RuleAndSender, newItem: RuleAndSender): Boolean {
                return oldItem.rule === newItem.rule
            }
        }
    }
}