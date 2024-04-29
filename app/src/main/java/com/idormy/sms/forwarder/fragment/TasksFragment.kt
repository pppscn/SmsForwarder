package com.idormy.sms.forwarder.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.TaskPagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.TaskViewModel
import com.idormy.sms.forwarder.databinding.FragmentTasksBinding
import com.idormy.sms.forwarder.utils.KEY_TASK_CLONE
import com.idormy.sms.forwarder.utils.KEY_TASK_ID
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.XToastUtils
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("PrivatePropertyName")
@Page(name = "自动任务")
class TasksFragment : BaseFragment<FragmentTasksBinding?>(), TaskPagingAdapter.OnItemClickListener {

    private val TAG: String = TasksFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var adapter = TaskPagingAdapter(this)
    private val viewModel by viewModels<TaskViewModel> { BaseViewModelFactory(context) }
    private var currentType: String = "mine"

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksBinding {
        return FragmentTasksBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.menu_tasks)
        titleBar!!.setActionTextColor(ThemeUtils.resolveColor(context, R.attr.colorAccent))
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_add) {
            @SingleClick
            override fun performAction(view: View) {
                openNewPage(TasksEditFragment::class.java)
            }
        })
        return titleBar
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

        binding!!.tabBar.setTabTitles(getStringArray(R.array.task_type_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            currentType = when (position) {
                1 -> "fixed"
                else -> "mine"
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
                    viewModel.setType(currentType).allTasks.collectLatest { adapter.submitData(it) }
                }
                refreshLayout.finishRefresh()
            }, 200)
        }

        binding!!.refreshLayout.autoRefresh()
    }

    override fun onItemClicked(view: View?, item: Task) {
        when (view?.id) {
            R.id.sb_enable -> {
                item.status = if (item.status == 0) 1 else 0
                Log.d(TAG, "sb_enable: ${item.id}, ${item.status}")
                viewModel.updateStatus(item.id, item.status)
            }

            R.id.iv_copy -> {
                PageOption.to(TasksEditFragment::class.java)
                    .setNewActivity(true).putLong(KEY_TASK_ID, item.id)
                    //.putString(KEY_TASK_TYPE, item.type.toString())
                    .putBoolean(KEY_TASK_CLONE, true)
                    .open(this)
            }

            R.id.iv_edit -> {
                PageOption.to(TasksEditFragment::class.java)
                    .setNewActivity(true).putLong(KEY_TASK_ID, item.id)
                    //.putString(KEY_TASK_TYPE, item.type.toString())
                    .open(this)
            }

            R.id.iv_delete -> {
                MaterialDialog.Builder(requireContext()).title(R.string.delete_task_title).content(R.string.delete_task_tips).positiveText(R.string.lab_yes).negativeText(R.string.lab_no).onPositive { _: MaterialDialog?, _: DialogAction? ->
                    viewModel.delete(item.id)
                    XToastUtils.success(R.string.delete_task_toast)
                }.show()
            }

            else -> {}
        }
    }

    override fun onItemRemove(view: View?, id: Int) {}

}