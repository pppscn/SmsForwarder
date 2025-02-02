package com.idormy.sms.forwarder.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.AppListAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentAppListBinding
import com.idormy.sms.forwarder.utils.AppInfo
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.XUI
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.resource.ResUtils.getStringArray

@Suppress("PrivatePropertyName", "DEPRECATION")
@Page(name = "应用列表")
class AppListFragment : BaseFragment<FragmentAppListBinding?>() {

    private val TAG: String = AppListFragment::class.java.simpleName
    private var appListAdapter: AppListAdapter? = null
    private val appListObserver = Observer { it: String ->
        Log.d(TAG, "EVENT_LOAD_APP_LIST: $it")
        appListAdapter?.refresh(getAppsList(false))
    }
    private var currentType: String = "user"

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

        binding!!.tabBar.setTabTitles(getStringArray(R.array.app_type_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            currentType = when (position) {
                1 -> "system"
                else -> "user"
            }
            appListAdapter?.refresh(getAppsList(false))
            binding!!.refreshLayout.finishRefresh()
            binding!!.recyclerView.scrollToPosition(0)
        }
    }

    override fun initListeners() {
        binding!!.refreshLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onLoadMore(refreshLayout: RefreshLayout) {
                refreshLayout.layout.postDelayed({
                    refreshLayout.finishLoadMore()
                }, 1000)
            }

            override fun onRefresh(refreshLayout: RefreshLayout) {
                appListAdapter?.refresh(getAppsList(true))
                refreshLayout.layout.postDelayed({
                    refreshLayout.finishRefresh()
                }, 1000)
            }
        })
        appListAdapter?.setOnItemClickListener { _, item, _ ->
            val cm = requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val mClipData = ClipData.newPlainText("pkgName", item?.packageName)
            cm.setPrimaryClip(mClipData)
            XToastUtils.toast(getString(R.string.package_name_copied) + item?.packageName, 2000)
        }

        //设置刷新加载时禁止所有列表操作
        binding!!.refreshLayout.setDisableContentWhenRefresh(true)
        binding!!.refreshLayout.setDisableContentWhenLoading(true)
        appListAdapter?.refresh(getAppsList(false))
        binding!!.refreshLayout.finishRefresh()
        //监听已安装App信息列表加载完成事件
        LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).observeStickyForever(appListObserver)
    }

    override fun onDestroyView() {
        appListAdapter?.recycle()
        super.onDestroyView()
    }

    private fun getAppsList(refresh: Boolean): MutableList<AppInfo> {
        if (refresh || (currentType == "user" && App.UserAppList.isEmpty()) || (currentType == "system" && App.SystemAppList.isEmpty())) {
            //检查读取应用列表权限是否获取
            XXPermissions.with(this).permission(Permission.GET_INSTALLED_APPS).request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    XToastUtils.info(getString(R.string.loading_app_list))
                    val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                    WorkManager.getInstance(XUI.getContext()).enqueue(request)
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    XToastUtils.error(R.string.tips_get_installed_apps)
                    if (doNotAskAgain) {
                        XXPermissions.startPermissionActivity(XUI.getContext(), permissions)
                    }
                }
            })
        }

        return if (currentType == "system") App.SystemAppList else App.UserAppList
    }

}
