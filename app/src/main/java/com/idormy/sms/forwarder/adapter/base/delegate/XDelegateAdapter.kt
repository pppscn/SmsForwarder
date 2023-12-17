package com.idormy.sms.forwarder.adapter.base.delegate

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.vlayout.DelegateAdapter

/**
 * 基础DelegateAdapter
 *
 * @author xuexiang
 * @since 2020/3/20 12:17 AM
 */
@Suppress("unused")
abstract class XDelegateAdapter<T, V : RecyclerView.ViewHolder> : DelegateAdapter.Adapter<V> {
    /**
     * 数据源
     */
    private val mData: MutableList<T> = ArrayList()
    /**
     * @return 当前列表的选中项
     */
    /**
     * 当前点击的条目
     */
    private var selectPosition = -1

    constructor()
    constructor(list: Collection<T>?) {
        if (list != null) {
            mData.addAll(list)
        }
    }

    constructor(data: Array<T>?) {
        if (!data.isNullOrEmpty()) {
            mData.addAll(listOf(*data))
        }
    }

    /**
     * 构建自定义的ViewHolder
     *
     * @param parent
     * @param viewType
     * @return
     */
    protected abstract fun getViewHolder(parent: ViewGroup, viewType: Int): V

    /**
     * 绑定数据
     *
     * @param holder
     * @param position 索引
     * @param item     列表项
     */
    protected abstract fun bindData(holder: V, position: Int, item: T)

    /**
     * 加载布局获取控件
     *
     * @param parent   父布局
     * @param layoutId 布局ID
     * @return
     */
    protected fun inflateView(parent: ViewGroup, @LayoutRes layoutId: Int): View {
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): V {
        return getViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: V, position: Int) {
        bindData(holder, position, mData[position])
    }

    /**
     * 获取列表项
     *
     * @param position
     * @return
     */
    private fun getItem(position: Int): T? {
        return if (checkPosition(position)) mData[position] else null
    }

    private fun checkPosition(position: Int): Boolean {
        return position >= 0 && position <= mData.size - 1
    }

    val isEmpty: Boolean
        get() = itemCount == 0

    override fun getItemCount(): Int {
        return mData.size
    }

    /**
     * @return 数据源
     */
    val data: List<T>
        get() = mData

    /**
     * 给指定位置添加一项
     *
     * @param pos
     * @param item
     * @return
     */
    fun add(pos: Int, item: T): XDelegateAdapter<*, *> {
        mData.add(pos, item)
        notifyItemInserted(pos)
        return this
    }

    /**
     * 在列表末端增加一项
     *
     * @param item
     * @return
     */
    fun add(item: T): XDelegateAdapter<*, *> {
        mData.add(item)
        notifyItemInserted(mData.size - 1)
        return this
    }

    /**
     * 删除列表中指定索引的数据
     *
     * @param pos
     * @return
     */
    fun delete(pos: Int): XDelegateAdapter<*, *> {
        mData.removeAt(pos)
        notifyItemRemoved(pos)
        return this
    }

    /**
     * 刷新列表中指定位置的数据
     *
     * @param pos
     * @param item
     * @return
     */
    fun refresh(pos: Int, item: T): XDelegateAdapter<*, *> {
        mData[pos] = item
        notifyItemChanged(pos)
        return this
    }

    /**
     * 刷新列表数据
     *
     * @param collection
     * @return
     */
    @SuppressLint("NotifyDataSetChanged")
    open fun refresh(collection: Collection<T>?): XDelegateAdapter<*, *> {
        if (collection != null) {
            mData.clear()
            mData.addAll(collection)
            selectPosition = -1
            notifyDataSetChanged()
        }
        return this
    }

    /**
     * 刷新列表数据
     *
     * @param array
     * @return
     */
    @SuppressLint("NotifyDataSetChanged")
    fun refresh(array: Array<T>?): XDelegateAdapter<*, *> {
        if (!array.isNullOrEmpty()) {
            mData.clear()
            mData.addAll(listOf(*array))
            selectPosition = -1
            notifyDataSetChanged()
        }
        return this
    }

    /**
     * 加载更多
     *
     * @param collection
     * @return
     */
    @SuppressLint("NotifyDataSetChanged")
    fun loadMore(collection: Collection<T>?): XDelegateAdapter<*, *> {
        if (collection != null) {
            mData.addAll(collection)
            notifyDataSetChanged()
        }
        return this
    }

    /**
     * 加载更多
     *
     * @param array
     * @return
     */
    @SuppressLint("NotifyDataSetChanged")
    fun loadMore(array: Array<T>?): XDelegateAdapter<*, *> {
        if (!array.isNullOrEmpty()) {
            mData.addAll(listOf(*array))
            notifyDataSetChanged()
        }
        return this
    }

    /**
     * 添加一个
     *
     * @param item
     * @return
     */
    @SuppressLint("NotifyDataSetChanged")
    fun load(item: T?): XDelegateAdapter<*, *> {
        if (item != null) {
            mData.add(item)
            notifyDataSetChanged()
        }
        return this
    }

    /**
     * 设置当前列表的选中项
     *
     * @param selectPosition
     * @return
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectPosition(selectPosition: Int): XDelegateAdapter<*, *> {
        this.selectPosition = selectPosition
        notifyDataSetChanged()
        return this
    }

    /**
     * 获取当前列表选中项
     *
     * @return 当前列表选中项
     */
    val selectItem: T?
        get() = getItem(selectPosition)

    /**
     * 清除数据
     */
    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        if (!isEmpty) {
            mData.clear()
            selectPosition = -1
            notifyDataSetChanged()
        }
    }
}