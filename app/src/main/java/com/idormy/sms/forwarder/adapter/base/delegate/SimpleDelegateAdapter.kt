package com.idormy.sms.forwarder.adapter.base.delegate

import com.alibaba.android.vlayout.LayoutHelper

/**
 * 简易DelegateAdapter适配器
 *
 * @author xuexiang
 * @since 2020/3/20 12:55 AM
 */
abstract class SimpleDelegateAdapter<T> : BaseDelegateAdapter<T> {
    private var mLayoutId: Int
    private var mLayoutHelper: LayoutHelper

    constructor(layoutId: Int, layoutHelper: LayoutHelper) : super() {
        mLayoutId = layoutId
        mLayoutHelper = layoutHelper
    }

    constructor(layoutId: Int, layoutHelper: LayoutHelper, list: Collection<T>?) : super(list) {
        mLayoutId = layoutId
        mLayoutHelper = layoutHelper
    }

    constructor(layoutId: Int, layoutHelper: LayoutHelper, data: Array<T>?) : super(data) {
        mLayoutId = layoutId
        mLayoutHelper = layoutHelper
    }

    override fun getItemLayoutId(viewType: Int): Int {
        return mLayoutId
    }

    override fun onCreateLayoutHelper(): LayoutHelper {
        return mLayoutHelper
    }
}