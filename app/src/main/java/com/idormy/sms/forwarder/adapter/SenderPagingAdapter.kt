package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.adapter.SenderPagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.databinding.AdapterSendersCardViewListItemBinding

@Suppress("EmptyMethod")
class SenderPagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<Sender, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterSendersCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.ivImage.setImageResource(item.imageId)
            holder.binding.ivStatus.setImageResource(item.statusImageId)
            holder.binding.tvName.text = item.name

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

    class MyViewHolder(val binding: AdapterSendersCardViewListItemBinding) : RecyclerView.ViewHolder(binding.root)
    interface OnItemClickListener {
        fun onItemClicked(view: View?, item: Sender)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<Sender> = object : DiffUtil.ItemCallback<Sender>() {
            override fun areItemsTheSame(oldItem: Sender, newItem: Sender): Boolean {
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Sender, newItem: Sender): Boolean {
                return oldItem === newItem
            }
        }
    }
}