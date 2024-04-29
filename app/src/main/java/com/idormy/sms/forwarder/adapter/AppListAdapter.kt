package com.idormy.sms.forwarder.adapter

import android.widget.ImageView
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.base.broccoli.BroccoliRecyclerAdapter
import com.idormy.sms.forwarder.utils.AppInfo
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.PlaceholderHelper
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.widget.imageview.ImageLoader
import me.samlss.broccoli.Broccoli

class AppListAdapter(
    /**
     * 是否是加载占位
     */
    private val mIsAnim: Boolean,
) : BroccoliRecyclerAdapter<AppInfo?>(AppUtils.getAppsInfo()) {

    override fun getItemLayoutId(viewType: Int): Int {
        return R.layout.adapter_app_list_item
    }

    /**
     * 绑定控件
     *
     * @param holder
     * @param model
     * @param position
     */
    override fun onBindData(holder: RecyclerViewHolder?, model: AppInfo?, position: Int) {
        if (holder == null || model == null) return
        val ivAppIcon = holder.findViewById<ImageView>(R.id.iv_app_icon)
        ImageLoader.get().loadImage(ivAppIcon, model.icon)
        holder.text(R.id.tv_app_name, model.name)
        holder.text(R.id.tv_pkg_name, model.packageName)
        holder.text(R.id.tv_ver_name, "VER. " + model.versionName)
        //holder.text(R.id.tv_ver_code, model.versionCode.toString())
        holder.text(R.id.tv_uid, "UID. " + model.uid.toString())
    }

    /**
     * 绑定占位控件
     *
     * @param holder
     * @param broccoli
     */
    override fun onBindBroccoli(holder: RecyclerViewHolder?, broccoli: Broccoli?) {
        if (holder == null || broccoli == null) return
        if (mIsAnim) {
            broccoli.addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.iv_app_icon)))
                .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_app_name)))
                .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_pkg_name)))
                .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_ver_name)))
                //.addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_ver_code)))
                .addPlaceholder(PlaceholderHelper.getParameter(holder.findView(R.id.tv_uid)))
        } else {
            broccoli.addPlaceholders(
                holder.findView(R.id.iv_app_icon),
                holder.findView(R.id.tv_app_name),
                holder.findView(R.id.tv_pkg_name),
                holder.findView(R.id.tv_ver_name),
                //holder.findView(R.id.tv_ver_code),
                holder.findView(R.id.tv_uid)
            )
        }
    }
}