package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.FrpcPagingAdapter.MyViewHolder
import com.idormy.sms.forwarder.database.entity.Frpc
import com.idormy.sms.forwarder.databinding.AdapterFrpcsCardViewListItemBinding
import com.xuexiang.xutil.resource.ResUtils.getColors
import frpclib.Frpclib

@Suppress("EmptyMethod")
class FrpcPagingAdapter(private val itemClickListener: OnItemClickListener) : PagingDataAdapter<Frpc, MyViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterFrpcsCardViewListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.binding.ivAutorun.setImageResource(item.autorunImageId)
            holder.binding.tvUid.text = "UID:${item.uid}"
            holder.binding.tvName.text = item.name

            if (item.connecting || (App.FrpclibInited && Frpclib.isRunning(item.uid))) {
                holder.binding.ivPlay.setImageResource(R.drawable.ic_stop)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.binding.ivPlay.imageTintList = getColors(R.color.colorStop)
                }
            } else {
                holder.binding.ivPlay.setImageResource(R.drawable.ic_start)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    holder.binding.ivPlay.imageTintList = getColors(R.color.colorStart)
                }
            }

            holder.binding.ivCopy.setOnClickListener { view: View? ->
                itemClickListener.onItemClicked(view, item)
            }
            holder.binding.ivPlay.setOnClickListener { view: View? ->
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

    class MyViewHolder(val binding: AdapterFrpcsCardViewListItemBinding) : RecyclerView.ViewHolder(binding.root)
    interface OnItemClickListener {
        fun onItemClicked(view: View?, item: Frpc)
        fun onItemRemove(view: View?, id: Int)
    }

    companion object {
        var diffCallback: DiffUtil.ItemCallback<Frpc> = object : DiffUtil.ItemCallback<Frpc>() {
            override fun areItemsTheSame(oldItem: Frpc, newItem: Frpc): Boolean {
                return oldItem.uid == newItem.uid
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Frpc, newItem: Frpc): Boolean {
                return oldItem === newItem
            }
        }
    }
}