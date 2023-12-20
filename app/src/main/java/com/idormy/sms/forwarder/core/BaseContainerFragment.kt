package com.idormy.sms.forwarder.core

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.umeng.analytics.MobclickAgent
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.base.XPageContainerListFragment
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.actionbar.TitleUtils

/**
 * 修改列表样式为主副标题显示
 *
 * @author xuexiang
 * @since 2018/11/22 上午11:26
 */
@Suppress("UNUSED_PARAMETER")
abstract class BaseContainerFragment : XPageContainerListFragment() {
    override fun initPage() {
        initTitle()
        initViews()
        initListeners()
    }

    protected fun initTitle(): TitleBar {
        return TitleUtils.addTitleBarDynamic(
            rootView as ViewGroup,
            pageTitle
        ) { popToBack() }
    }

    override fun initData() {
        mSimpleData = initSimpleData(mSimpleData)
        val data: MutableList<Map<String?, String?>?> = ArrayList()
        for (content in mSimpleData) {
            val item: MutableMap<String?, String?> = HashMap()
            val index = content.indexOf("\n")
            if (index > 0) {
                item[SimpleListAdapter.KEY_TITLE] = content.subSequence(0, index).toString()
                item[SimpleListAdapter.KEY_SUB_TITLE] =
                    content.subSequence(index + 1, content.length).toString()
            } else {
                item[SimpleListAdapter.KEY_TITLE] = content
                item[SimpleListAdapter.KEY_SUB_TITLE] = ""
            }
            data.add(item)
        }
        listView.adapter = SimpleListAdapter(context, data)
        initSimply()
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View, position: Int, id: Long) {
        onItemClick(view, position)
    }

    @SingleClick
    private fun onItemClick(view: View, position: Int) {
        onItemClick(position)
    }

    override fun onDestroyView() {
        listView.onItemClickListener = null
        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        //屏幕旋转时刷新一下title
        super.onConfigurationChanged(newConfig)
        val root = rootView as ViewGroup
        if (root.getChildAt(0) is TitleBar) {
            root.removeViewAt(0)
            initTitle()
        }
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(pageName)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(pageName)
    }
}