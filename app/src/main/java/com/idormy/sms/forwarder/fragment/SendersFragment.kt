package com.idormy.sms.forwarder.fragment

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.SenderPagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersBinding
import com.idormy.sms.forwarder.fragment.senders.*
import com.idormy.sms.forwarder.utils.*
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("PropertyName")
@Page(name = "发送通道")
class SendersFragment : BaseFragment<FragmentSendersBinding?>(), SenderPagingAdapter.OnItemClickListener {

    val TAG: String = SendersFragment::class.java.simpleName
    private var adapter = SenderPagingAdapter(this)
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private var currentStatus: Int = 1
    //private val statusValueArray = ResUtils.getIntArray(R.array.status_param_value)

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSendersBinding {
        return FragmentSendersBinding.inflate(inflater, container, false)
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

        binding!!.tabBar.setTabTitles(ResUtils.getStringArray(R.array.status_param_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            //currentStatus = statusValueArray[position]
            currentStatus = 1 - position //注意：这里刚好相反，可以取巧
            viewModel.setStatus(currentStatus)
            adapter.refresh()
            binding!!.recyclerView.scrollToPosition(0)
        }
    }

    override fun initListeners() {
        binding!!.recyclerView.adapter = adapter

        //下拉刷新
        binding!!.refreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout ->
            refreshLayout.layout.postDelayed({
                //adapter!!.refresh()
                lifecycleScope.launch {
                    viewModel.setStatus(currentStatus).allSenders.collectLatest { adapter.submitData(it) }
                }
                refreshLayout.finishRefresh()
            }, 200)
        }

        binding!!.refreshLayout.autoRefresh()
    }

    override fun onItemClicked(view: View?, item: Sender) {
        Log.e(TAG, item.toString())
        when (view?.id) {
            R.id.iv_copy -> {
                PageOption.to(
                    when (item.type) {
                        TYPE_DINGTALK_GROUP_ROBOT -> DingtalkGroupRobotFragment::class.java
                        TYPE_EMAIL -> EmailFragment::class.java
                        TYPE_BARK -> BarkFragment::class.java
                        TYPE_WEBHOOK -> WebhookFragment::class.java
                        TYPE_WEWORK_ROBOT -> WeworkRobotFragment::class.java
                        TYPE_WEWORK_AGENT -> WeworkAgentFragment::class.java
                        TYPE_SERVERCHAN -> ServerchanFragment::class.java
                        TYPE_TELEGRAM -> TelegramFragment::class.java
                        TYPE_SMS -> SmsFragment::class.java
                        TYPE_FEISHU -> FeishuFragment::class.java
                        TYPE_PUSHPLUS -> PushplusFragment::class.java
                        TYPE_GOTIFY -> GotifyFragment::class.java
                        TYPE_DINGTALK_INNER_ROBOT -> DingtalkInnerRobotFragment::class.java
                        else -> DingtalkGroupRobotFragment::class.java
                    }
                ).setNewActivity(true)
                    .putLong(KEY_SENDER_ID, item.id)
                    .putInt(KEY_SENDER_TYPE, item.type)
                    .putBoolean(KEY_SENDER_CLONE, true)
                    .open(this)
            }
            R.id.iv_edit -> {
                PageOption.to(
                    when (item.type) {
                        TYPE_DINGTALK_GROUP_ROBOT -> DingtalkGroupRobotFragment::class.java
                        TYPE_EMAIL -> EmailFragment::class.java
                        TYPE_BARK -> BarkFragment::class.java
                        TYPE_WEBHOOK -> WebhookFragment::class.java
                        TYPE_WEWORK_ROBOT -> WeworkRobotFragment::class.java
                        TYPE_WEWORK_AGENT -> WeworkAgentFragment::class.java
                        TYPE_SERVERCHAN -> ServerchanFragment::class.java
                        TYPE_TELEGRAM -> TelegramFragment::class.java
                        TYPE_SMS -> SmsFragment::class.java
                        TYPE_FEISHU -> FeishuFragment::class.java
                        TYPE_PUSHPLUS -> PushplusFragment::class.java
                        TYPE_GOTIFY -> GotifyFragment::class.java
                        TYPE_DINGTALK_INNER_ROBOT -> DingtalkInnerRobotFragment::class.java
                        else -> DingtalkGroupRobotFragment::class.java
                    }
                ).setNewActivity(true)
                    .putLong(KEY_SENDER_ID, item.id)
                    .putInt(KEY_SENDER_TYPE, item.type)
                    .open(this)
            }
            R.id.iv_delete -> {
                MaterialDialog.Builder(requireContext())
                    .title(R.string.delete_sender_title)
                    .content(R.string.delete_sender_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        viewModel.delete(item.id)
                        XToastUtils.success(R.string.delete_sender_toast)
                    }
                    .show()
            }
            else -> {}
        }
    }

    override fun onItemRemove(view: View?, id: Int) {}

}