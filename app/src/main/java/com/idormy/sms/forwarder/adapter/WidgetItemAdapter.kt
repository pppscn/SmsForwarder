package com.idormy.sms.forwarder.adapter

import com.idormy.sms.forwarder.R
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xui.adapter.recyclerview.BaseRecyclerAdapter
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder

class WidgetItemAdapter(list: List<PageInfo>) : BaseRecyclerAdapter<PageInfo>(list) {

    public override fun getItemLayoutId(viewType: Int): Int {
        return R.layout.layout_widget_item
    }

    override fun bindData(holder: RecyclerViewHolder, position: Int, item: PageInfo) {
        holder.text(R.id.item_name, item.name)
        if (item.extra != 0) {
            holder.image(R.id.item_icon, item.extra)
        }
    }
}