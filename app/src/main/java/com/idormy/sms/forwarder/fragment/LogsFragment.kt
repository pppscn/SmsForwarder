package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.LogsPagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.LogsAndRuleAndSender
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.LogsViewModel
import com.idormy.sms.forwarder.databinding.FragmentLogsBinding
import com.idormy.sms.forwarder.utils.EVENT_UPDATE_LOGS_TYPE
import com.idormy.sms.forwarder.utils.FORWARD_STATUS_MAP
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.data.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Suppress("PropertyName")
@Page(name = "转发日志")
class LogsFragment : BaseFragment<FragmentLogsBinding?>(), LogsPagingAdapter.OnItemClickListener {

    val TAG: String = LogsFragment::class.java.simpleName
    private var adapter = LogsPagingAdapter(this)
    private val viewModel by viewModels<LogsViewModel> { BaseViewModelFactory(context) }
    private var currentType: String = "sms"

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentLogsBinding {
        return FragmentLogsBinding.inflate(inflater, container, false)
    }

    /**
     * @return 返回为 null意为不需要导航栏
     */
    override fun initTitle(): TitleBar? {
        return null
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        val virtualLayoutManager = VirtualLayoutManager(requireContext())
        binding!!.recyclerView.layoutManager = virtualLayoutManager
        val viewPool = RecycledViewPool()
        binding!!.recyclerView.setRecycledViewPool(viewPool)
        viewPool.setMaxRecycledViews(0, 10)
        binding!!.recyclerView.isFocusableInTouchMode = false

        binding!!.tabBar.setTabTitles(ResUtils.getStringArray(R.array.type_param_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            currentType = when (position) {
                1 -> "call"
                2 -> "app"
                else -> "sms"
            }
            viewModel.setType(currentType)
            LiveEventBus.get(EVENT_UPDATE_LOGS_TYPE, String::class.java).post(currentType)
            adapter.refresh()
            binding!!.recyclerView.scrollToPosition(0)
        }
    }

    override fun initListeners() {
        binding!!.recyclerView.adapter = adapter

        //下拉刷新
        binding!!.refreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout ->
            //adapter.refresh()
            lifecycleScope.launch {
                viewModel.setType(currentType).allLogs.collectLatest { adapter.submitData(it) }
            }
            refreshLayout.finishRefresh()
        }

        binding!!.refreshLayout.autoRefresh()
    }

    override fun onItemClicked(view: View?, item: LogsAndRuleAndSender) {
        val ruleStr = StringBuilder()
        ruleStr.append(Rule.getRuleMatch(item.relation.rule.filed, item.relation.rule.check, item.relation.rule.value, item.relation.rule.simSlot)).append(item.relation.sender.name)
        val detailStr = StringBuilder()
        detailStr.append(ResUtils.getString(R.string.from)).append(item.logs.from).append("\n\n")
        detailStr.append(ResUtils.getString(R.string.msg)).append(item.logs.content).append("\n\n")
        if (!TextUtils.isEmpty(item.logs.simInfo)) detailStr.append(ResUtils.getString(R.string.slot)).append(item.logs.simInfo).append("\n\n")
        detailStr.append(ResUtils.getString(R.string.rule)).append(ruleStr.toString()).append("\n\n")
        @SuppressLint("SimpleDateFormat") val utcFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        detailStr.append(ResUtils.getString(R.string.time)).append(DateUtils.date2String(item.logs.time, utcFormatter)).append("\n\n")
        detailStr.append(ResUtils.getString(R.string.result)).append(FORWARD_STATUS_MAP[item.logs.forwardStatus]).append("\n--------------------\n").append(item.logs.forwardResponse)

        MaterialDialog.Builder(requireContext())
            .iconRes(item.logs.simImageId)
            .title(R.string.details)
            .content(detailStr.toString())
            .cancelable(true)
            .positiveText(R.string.del)
            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                viewModel.delete(item.logs.id)
                XToastUtils.success(R.string.delete_log_toast)
            }
            .negativeText(R.string.resend)
            .onNegative { _: MaterialDialog?, _: DialogAction? ->
                XToastUtils.toast(R.string.resend_toast)
                SendUtils.resendMsg(item, false)
            }
            .neutralText(R.string.rematch)
            .neutralColor(ResUtils.getColors(R.color.red))
            .onNeutral { _: MaterialDialog?, _: DialogAction? ->
                XToastUtils.toast(R.string.rematch_toast)
                SendUtils.resendMsg(item, true)
            }
            .show()
    }

    override fun onItemRemove(view: View?, id: Int) {}

}