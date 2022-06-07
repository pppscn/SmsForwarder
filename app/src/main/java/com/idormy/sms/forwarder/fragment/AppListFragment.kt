package com.idormy.sms.forwarder.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.AppListAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentAppListBinding
import com.idormy.sms.forwarder.utils.XToastUtils
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.app.AppUtils

@Page(name = "应用列表")
class AppListFragment : BaseFragment<FragmentAppListBinding?>() {

    var appListAdapter: AppListAdapter? = null

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentAppListBinding {
        return FragmentAppListBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.menu_apps)
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_refresh) {
            @SingleClick
            override fun performAction(view: View) {
                binding!!.refreshLayout.autoRefresh()
            }
        })
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        WidgetUtils.initRecyclerView(binding!!.recyclerView, DensityUtils.dp2px(5f), ThemeUtils.resolveColor(context, R.attr.xui_config_color_background))
        binding!!.recyclerView.adapter = AppListAdapter(true).also { appListAdapter = it }
    }

    override fun initListeners() {
        binding!!.refreshLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onLoadMore(refreshLayout: RefreshLayout) {
                refreshLayout.layout.postDelayed({
                    refreshLayout.finishLoadMore()
                }, 1000)
            }

            override fun onRefresh(refreshLayout: RefreshLayout) {
                refreshLayout.layout.postDelayed({
                    appListAdapter?.refresh(AppUtils.getAppsInfo())
                    refreshLayout.finishRefresh()
                }, 3000)
            }
        })
        appListAdapter?.setOnItemClickListener { _, item, _ ->
            val cm = requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val mClipData = ClipData.newPlainText("pkgName", item?.packageName)
            cm.setPrimaryClip(mClipData)
            XToastUtils.toast(ResUtils.getString(R.string.package_name_copied) + item?.packageName, 2000)
        }

        //设置刷新加载时禁止所有列表操作
        binding!!.refreshLayout.setDisableContentWhenRefresh(true)
        binding!!.refreshLayout.setDisableContentWhenLoading(true)
        //binding!!.refreshLayout.autoRefresh()
        if (App.AppInfoList.isEmpty()) {
            appListAdapter?.refresh(AppUtils.getAppsInfo())
        } else {
            appListAdapter?.refresh(App.AppInfoList)
        }
        binding!!.refreshLayout.finishRefresh()
    }

    override fun onDestroyView() {
        appListAdapter?.recycle()
        super.onDestroyView()
    }
}