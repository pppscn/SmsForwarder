package com.idormy.sms.forwarder.core

import android.content.Context
import android.view.View
import android.widget.TextView
import com.idormy.sms.forwarder.R
import com.xuexiang.xui.adapter.listview.BaseListAdapter
import com.xuexiang.xutil.common.StringUtils

/**
 * 主副标题显示适配器
 *
 * @author xuexiang
 * @since 2018/12/19 上午12:19
 */
class SimpleListAdapter(context: Context?, data: List<Map<String?, String?>?>?) :
    BaseListAdapter<Map<String?, String?>, SimpleListAdapter.ViewHolder>(context, data) {
    override fun newViewHolder(convertView: View): ViewHolder {
        val holder = ViewHolder()
        holder.mTvTitle = convertView.findViewById(R.id.tv_title)
        holder.mTvSubTitle = convertView.findViewById(R.id.tv_sub_title)
        return holder
    }

    override fun getLayoutId(): Int {
        return R.layout.adapter_item_simple_list
    }

    override fun convert(holder: ViewHolder, item: Map<String?, String?>, position: Int) {
        holder.mTvTitle!!.text =
            item[KEY_TITLE]
        if (!StringUtils.isEmpty(item[KEY_SUB_TITLE])) {
            holder.mTvSubTitle!!.text =
                item[KEY_SUB_TITLE]
            holder.mTvSubTitle!!.visibility = View.VISIBLE
        } else {
            holder.mTvSubTitle!!.visibility = View.GONE
        }
    }

    class ViewHolder {
        /**
         * 标题
         */
        var mTvTitle: TextView? = null

        /**
         * 副标题
         */
        var mTvSubTitle: TextView? = null
    }

    companion object {
        const val KEY_TITLE = "key_title"
        const val KEY_SUB_TITLE = "key_sub_title"
    }
}