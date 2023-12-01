package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.TaskPagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.databinding.AdapterTasksCardViewListItemBinding

class TaskPagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<Task, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterTasksCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.ivImage.setImageResource(item.imageId)
            holder.binding.ivStatus.setImageResource(item.statusImageId)
            holder.binding.tvName.text = item.name

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