package com.idormy.sms.forwarder.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.adapter.RulePagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.RuleViewModel
import com.idormy.sms.forwarder.databinding.FragmentRulesBinding
import com.idormy.sms.forwarder.utils.KEY_RULE_CLONE
import com.idormy.sms.forwarder.utils.KEY_RULE_ID
import com.idormy.sms.forwarder.utils.KEY_RULE_TYPE
import com.idormy.sms.forwarder.utils.XToastUtils
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Page(name = "转发规则")
class RulesFragment : BaseFragment<FragmentRulesBinding?>(), RulePagingAdapter.OnItemClickListener {

    //private val TAG: String = RulesFragment::class.java.simpleName
    private val that = this
    private var adapter = RulePagingAdapter(this)
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<RuleViewModel> { BaseViewModelFactory(context) }
    private var currentType: String = "sms"

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentRulesBinding {
        return FragmentRulesBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setLeftImageResource(R.drawable.ic_action_menu)
        titleBar!!.setTitle(R.string.menu_rules)
        titleBar!!.setLeftClickListener { getContainer()?.openMenu() }
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_add) {
            @SingleClick
            override fun performAction(view: View) {
                PageOption.to(RulesEditFragment::class.java)
                    .putString(KEY_RULE_TYPE, currentType)
                    .setNewActivity(true)
                    .open(that)
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

        binding!!.tabBar.setTabTitles(getStringArray(R.array.type_param_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            currentType = when (position) {
                1 -> "call"
                2 -> "app"
                else -> "sms"
            }
            viewModel.setType(currentType)
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
                    viewModel.setType(currentType).allRules.collectLatest { adapter.submitData(it) }
                }
                refreshLayout.finishRefresh()
            }, 200)
        }

        binding!!.refreshLayout.autoRefresh()
    }

    override fun onItemClicked(view: View?, item: Rule) {
        when (view?.id) {
            R.id.iv_copy -> {
                PageOption.to(RulesEditFragment::class.java)
                    .setNewActivity(true)
                    .putLong(KEY_RULE_ID, item.id)
                    .putString(KEY_RULE_TYPE, item.type)
                    .putBoolean(KEY_RULE_CLONE, true)
                    .open(this)
            }

            R.id.iv_edit -> {
                PageOption.to(RulesEditFragment::class.java)
                    .setNewActivity(true)
                    .putLong(KEY_RULE_ID, item.id)
                    .putString(KEY_RULE_TYPE, item.type)
                    .open(this)
            }

            R.id.iv_delete -> {
                MaterialDialog.Builder(requireContext())
                    .title(R.string.delete_rule_title)
                    .content(R.string.delete_rule_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        viewModel.delete(item.id)
                        XToastUtils.success(R.string.delete_rule_toast)
                    }
                    .show()
            }

            else -> {}
        }
    }

    override fun onItemRemove(view: View?, id: Int) {}
}