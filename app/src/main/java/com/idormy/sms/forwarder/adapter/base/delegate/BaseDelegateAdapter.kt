package com.idormy.sms.forwarder.adapter.base.delegate

import android.view.ViewGroup
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder

/**
 * 通用的DelegateAdapter适配器
 *
 * @author xuexiang
 * @since 2020/3/20 12:44 AM
 */
abstract class BaseDelegateAdapter<T> : XDelegateAdapter<T, RecyclerViewHolder> {
    constructor() : super()
    constructor(list: Collection<T>?) : super(list)
    constructor(data: Array<T>?) : super(data)

    /**
     * 适配的布局
     *
     * @param viewType
     * @return
     */
    protected abstract fun getItemLayoutId(viewType: Int): Int
    override fun getViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        return RecyclerViewHolder(inflateView(parent, getItemLayoutId(viewType)))
    }
}