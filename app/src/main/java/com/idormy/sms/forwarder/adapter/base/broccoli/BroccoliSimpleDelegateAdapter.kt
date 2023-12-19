@file:Suppress("unused")

package com.idormy.sms.forwarder.adapter.base.broccoli

import android.view.View
import com.alibaba.android.vlayout.LayoutHelper
import com.idormy.sms.forwarder.adapter.base.delegate.SimpleDelegateAdapter
import com.idormy.sms.forwarder.adapter.base.delegate.XDelegateAdapter
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import me.samlss.broccoli.Broccoli

/**
 * 使用Broccoli占位的基础适配器
 *
 * @author xuexiang
 * @since 2021/1/9 4:52 PM
 */
@Suppress("unused")
abstract class BroccoliSimpleDelegateAdapter<T> : SimpleDelegateAdapter<T> {
    /**
     * 是否已经加载成功
     */
    private var mHasLoad = false
    private val mBroccoliMap: MutableMap<View, Broccoli> = HashMap()

    constructor(layoutId: Int, layoutHelper: LayoutHelper) : super(layoutId, layoutHelper)
    constructor(layoutId: Int, layoutHelper: LayoutHelper, list: Collection<T>?) : super(
        layoutId,
        layoutHelper,
        list
    )

    constructor(layoutId: Int, layoutHelper: LayoutHelper?, data: Array<T>?) : super(
        layoutId,
        layoutHelper!!,
        data
    )

    override fun bindData(holder: RecyclerViewHolder, position: Int, item: T) {
        var broccoli = mBroccoliMap[holder.itemView]
        if (broccoli == null) {
            broccoli = Broccoli()
            mBroccoliMap[holder.itemView] = broccoli
        }
        if (mHasLoad) {
            broccoli.removeAllPlaceholders()
            onBindData(holder, item, position)
        } else {
            onBindBroccoli(holder, broccoli)
            broccoli.show()
        }
    }

    /**
     * 绑定控件
     *
     * @param holder
     * @param model
     * @param position
     */
    protected abstract fun onBindData(holder: RecyclerViewHolder, model: T, position: Int)

    /**
     * 绑定占位控件
     *
     * @param holder
     * @param broccoli
     */
    protected abstract fun onBindBroccoli(holder: RecyclerViewHolder, broccoli: Broccoli)

    override fun refresh(collection: Collection<T>?): XDelegateAdapter<*, *> {
        mHasLoad = true
        return super.refresh(collection)
    }

    /**
     * 资源释放，防止内存泄漏
     */
    fun recycle() {
        for (broccoli in mBroccoliMap.values) {
            broccoli.removeAllPlaceholders()
        }
        mBroccoliMap.clear()
        clear()
    }
}