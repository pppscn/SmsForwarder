package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.adapter.SenderPagingAdapter
import com.idormy.sms.forwarder.adapter.WidgetItemAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersBinding
import com.idormy.sms.forwarder.fragment.senders.BarkFragment
import com.idormy.sms.forwarder.fragment.senders.DingtalkGroupRobotFragment
import com.idormy.sms.forwarder.fragment.senders.DingtalkInnerRobotFragment
import com.idormy.sms.forwarder.fragment.senders.EmailFragment
import com.idormy.sms.forwarder.fragment.senders.FeishuAppFragment
import com.idormy.sms.forwarder.fragment.senders.FeishuFragment
import com.idormy.sms.forwarder.fragment.senders.GotifyFragment
import com.idormy.sms.forwarder.fragment.senders.PushplusFragment
import com.idormy.sms.forwarder.fragment.senders.ServerchanFragment
import com.idormy.sms.forwarder.fragment.senders.SmsFragment
import com.idormy.sms.forwarder.fragment.senders.SocketFragment
import com.idormy.sms.forwarder.fragment.senders.TelegramFragment
import com.idormy.sms.forwarder.fragment.senders.UrlSchemeFragment
import com.idormy.sms.forwarder.fragment.senders.WebhookFragment
import com.idormy.sms.forwarder.fragment.senders.WeworkAgentFragment
import com.idormy.sms.forwarder.fragment.senders.WeworkRobotFragment
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TYPE_BARK
import com.idormy.sms.forwarder.utils.TYPE_DINGTALK_GROUP_ROBOT
import com.idormy.sms.forwarder.utils.TYPE_DINGTALK_INNER_ROBOT
import com.idormy.sms.forwarder.utils.TYPE_EMAIL
import com.idormy.sms.forwarder.utils.TYPE_FEISHU
import com.idormy.sms.forwarder.utils.TYPE_FEISHU_APP
import com.idormy.sms.forwarder.utils.TYPE_GOTIFY
import com.idormy.sms.forwarder.utils.TYPE_PUSHPLUS
import com.idormy.sms.forwarder.utils.TYPE_SERVERCHAN
import com.idormy.sms.forwarder.utils.TYPE_SMS
import com.idormy.sms.forwarder.utils.TYPE_SOCKET
import com.idormy.sms.forwarder.utils.TYPE_TELEGRAM
import com.idormy.sms.forwarder.utils.TYPE_URL_SCHEME
import com.idormy.sms.forwarder.utils.TYPE_WEBHOOK
import com.idormy.sms.forwarder.utils.TYPE_WEWORK_AGENT
import com.idormy.sms.forwarder.utils.TYPE_WEWORK_ROBOT
import com.idormy.sms.forwarder.utils.XToastUtils
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.enums.CoreAnim
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.alpha.XUIAlphaTextView
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("PrivatePropertyName", "DEPRECATION")
@Page(name = "发送通道")
class SendersFragment : BaseFragment<FragmentSendersBinding?>(),
    SenderPagingAdapter.OnItemClickListener,
    RecyclerViewHolder.OnItemClickListener<PageInfo> {

    private val TAG: String = SendersFragment::class.java.simpleName
    private val that = this
    private var titleBar: TitleBar? = null
    private var adapter = SenderPagingAdapter(this)
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private val dialog: BottomSheetDialog by lazy { BottomSheetDialog(requireContext()) }
    private var currentStatus: Int = 1
    private var SENDER_FRAGMENT_LIST = listOf(
        PageInfo(
            getString(R.string.dingtalk_robot),
            "com.idormy.sms.forwarder.fragment.senders.DingtalkGroupRobotFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_dingtalk
        ),
        PageInfo(
            getString(R.string.email),
            "com.idormy.sms.forwarder.fragment.senders.EmailFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_email
        ),
        PageInfo(
            getString(R.string.bark),
            "com.idormy.sms.forwarder.fragment.senders.BarkFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_bark
        ),
        PageInfo(
            getString(R.string.webhook),
            "com.idormy.sms.forwarder.fragment.senders.WebhookFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_webhook
        ),
        PageInfo(
            getString(R.string.wework_robot),
            "com.idormy.sms.forwarder.fragment.senders.WeworkRobotFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_wework_robot
        ),
        PageInfo(
            getString(R.string.wework_agent),
            "com.idormy.sms.forwarder.fragment.senders.WeworkAgentFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_wework_agent
        ),
        PageInfo(
            getString(R.string.server_chan),
            "com.idormy.sms.forwarder.fragment.senders.ServerchanFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_serverchan
        ),
        PageInfo(
            getString(R.string.telegram),
            "com.idormy.sms.forwarder.fragment.senders.TelegramFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_telegram
        ),
        PageInfo(
            getString(R.string.sms_menu),
            "com.idormy.sms.forwarder.fragment.senders.SmsFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_sms
        ),
        PageInfo(
            getString(R.string.feishu),
            "com.idormy.sms.forwarder.fragment.senders.FeishuFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_feishu
        ),
        PageInfo(
            getString(R.string.pushplus),
            "com.idormy.sms.forwarder.fragment.senders.PushplusFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_pushplus
        ),
        PageInfo(
            getString(R.string.gotify),
            "com.idormy.sms.forwarder.fragment.senders.GotifyFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_gotify
        ),
        PageInfo(
            getString(R.string.dingtalk_inner_robot),
            "com.idormy.sms.forwarder.fragment.senders.DingtalkInnerRobotFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_dingtalk_inner
        ),
        PageInfo(
            getString(R.string.feishu_app),
            "com.idormy.sms.forwarder.fragment.senders.FeishuAppFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_feishu_app
        ),
        PageInfo(
            getString(R.string.url_scheme),
            "com.idormy.sms.forwarder.fragment.senders.UrlSchemeFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_url_scheme
        ),
        PageInfo(
            getString(R.string.socket),
            "com.idormy.sms.forwarder.fragment.senders.SocketFragment",
            "{\"\":\"\"}",
            CoreAnim.slide,
            R.drawable.icon_socket
        ),
    )

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSendersBinding {
        return FragmentSendersBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setLeftImageResource(R.drawable.ic_action_menu)
        titleBar!!.setTitle(R.string.menu_senders)
        titleBar!!.setLeftClickListener { getContainer()?.openMenu() }
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_add) {
            @SuppressLint("InflateParams")
            @SingleClick
            override fun performAction(view: View) {
                val bottomSheet: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sender_bottom_sheet, null)
                val recyclerView: RecyclerView = bottomSheet.findViewById(R.id.recyclerView)

                WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                val widgetItemAdapter = WidgetItemAdapter(SENDER_FRAGMENT_LIST)
                widgetItemAdapter.setOnItemClickListener(that)
                recyclerView.adapter = widgetItemAdapter

                val bottomSheetCloseButton: XUIAlphaTextView = bottomSheet.findViewById(R.id.bottom_sheet_close_button)
                bottomSheetCloseButton.setOnClickListener { dialog.dismiss() }

                dialog.setContentView(bottomSheet)
                dialog.setCancelable(true)
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
                WidgetUtils.transparentBottomSheetDialogBackground(dialog)
            }
        })
        return titleBar
    }

    private fun getContainer(): MainActivity? {
        return activity as MainActivity?
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

        binding!!.tabBar.setTabTitles(getStringArray(R.array.status_param_option))
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
                PageOption.to(getFragment(item.type))
                    .setNewActivity(true)
                    .putLong(KEY_SENDER_ID, item.id)
                    .putInt(KEY_SENDER_TYPE, item.type)
                    .putBoolean(KEY_SENDER_CLONE, true)
                    .open(this)
            }

            R.id.iv_edit -> {
                PageOption.to(getFragment(item.type))
                    .setNewActivity(true)
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

    @SingleClick
    override fun onItemClick(itemView: View, widgetInfo: PageInfo, pos: Int) {
        try {
            @Suppress("UNCHECKED_CAST")
            PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>) //跳转的fragment
                .setNewActivity(true)
                .putInt(KEY_SENDER_TYPE, pos) //注意：目前刚好是这个顺序而已
                .open(this)
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onItemClick error: ${e.message}")
            XToastUtils.error(e.message.toString())
        }
    }

    private fun getFragment(type: Int): Class<out XPageFragment> {
        return when (type) {
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
            TYPE_FEISHU_APP -> FeishuAppFragment::class.java
            TYPE_URL_SCHEME -> UrlSchemeFragment::class.java
            TYPE_SOCKET -> SocketFragment::class.java
            else -> DingtalkGroupRobotFragment::class.java
        }
    }

}