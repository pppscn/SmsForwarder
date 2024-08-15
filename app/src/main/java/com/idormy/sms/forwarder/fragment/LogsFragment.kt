package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.alibaba.android.vlayout.VirtualLayoutManager
import com.idormy.sms.forwarder.App.Companion.FORWARD_STATUS_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.adapter.MsgPagingAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.LogsDetail
import com.idormy.sms.forwarder.database.entity.MsgAndLogs
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.MsgViewModel
import com.idormy.sms.forwarder.databinding.FragmentLogsBinding
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.picker.widget.TimePickerView
import com.xuexiang.xui.widget.picker.widget.builder.TimePickerBuilder
import com.xuexiang.xui.widget.picker.widget.configure.TimePickerType
import com.xuexiang.xutil.data.DateUtils
import com.xuexiang.xutil.resource.ResUtils.getColors
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Suppress("PrivatePropertyName")
@Page(name = "转发日志")
class LogsFragment : BaseFragment<FragmentLogsBinding?>(), MsgPagingAdapter.OnItemClickListener {

    private val TAG: String = LogsFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var adapter = MsgPagingAdapter(this)
    private val viewModel by viewModels<MsgViewModel> { BaseViewModelFactory(context) }
    private var currentType: String = "sms"

    //日志筛选
    private var currentFilter: MutableMap<String, Any> = mutableMapOf()
    private var logsFilterPopup: MaterialDialog? = null
    private var timePicker: TimePickerView? = null

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentLogsBinding {
        return FragmentLogsBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setLeftImageResource(R.drawable.ic_action_menu)
        titleBar!!.setTitle(R.string.menu_logs)
        titleBar!!.setLeftClickListener { getContainer()?.openMenu() }
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_delete) {
            @SingleClick
            override fun performAction(view: View) {
                MaterialDialog.Builder(requireContext())
                    .content(if (currentFilter.isEmpty()) R.string.delete_type_log_tips else R.string.delete_filter_log_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        try {
                            Log.d(TAG, "deleteAll, currentType:$currentType, currentFilter:$currentFilter")
                            viewModel.setType(currentType).setFilter(currentFilter).deleteAll()
                            reloadData()
                            XToastUtils.success(if (currentFilter.isEmpty()) R.string.delete_type_log_toast else R.string.delete_filter_log_toast)
                        } catch (e: Exception) {
                            e.message?.let { XToastUtils.error(it) }
                        }
                    }
                    .show()
            }
        })
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_filter) {
            @SingleClick
            override fun performAction(view: View) {
                initLogsFilterDialog()
                logsFilterPopup?.show()
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
        binding!!.recyclerView.isFocusableInTouchMode = false

        binding!!.tabBar.setTabTitles(getStringArray(R.array.type_param_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            currentType = when (position) {
                1 -> "call"
                2 -> "app"
                else -> "sms"
            }
            initLogsFilterDialog(true)
            reloadData()
        }
    }

    override fun initListeners() {
        binding!!.recyclerView.adapter = adapter

        //下拉刷新
        binding!!.refreshLayout.setOnRefreshListener { refreshLayout: RefreshLayout ->
            //adapter.refresh()
            lifecycleScope.launch {
                viewModel.setType(currentType).setFilter(currentFilter).allMsg.collectLatest { adapter.submitData(it) }
            }
            refreshLayout.finishRefresh()
        }

        binding!!.refreshLayout.autoRefresh()
    }

    override fun onItemClicked(view: View?, item: MsgAndLogs) {
        Log.d(TAG, "item: $item")

        val detailStr = StringBuilder()
        detailStr.append(getString(R.string.from)).append(item.msg.from).append("\n\n")
        if (!TextUtils.isEmpty(item.msg.simInfo)) {
            if (item.msg.type == "app") {
                detailStr.append(getString(R.string.title)).append(item.msg.simInfo).append("\n\n")
                detailStr.append(getString(R.string.msg)).append(item.msg.content).append("\n\n")
            } else {
                detailStr.append(getString(R.string.msg)).append(item.msg.content).append("\n\n")
                detailStr.append(getString(R.string.slot)).append(item.msg.simInfo).append("\n\n")
            }
        }
        @SuppressLint("SimpleDateFormat") val utcFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        detailStr.append(getString(R.string.time)).append(DateUtils.date2String(item.msg.time, utcFormatter))

        MaterialDialog.Builder(requireContext())
            .iconRes(item.msg.simImageId)
            .title(R.string.details)
            .content(detailStr.toString())
            .cancelable(true)
            .positiveText(R.string.del)
            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                viewModel.delete(item.msg.id)
                XToastUtils.success(R.string.delete_log_toast)
            }
            .neutralText(R.string.rematch)
            .neutralColor(getColors(R.color.red))
            .onNeutral { _: MaterialDialog?, _: DialogAction? ->
                XToastUtils.toast(R.string.rematch_toast)
                SendUtils.rematchSendMsg(item)
            }
            .show()
    }

    override fun onLogsClicked(view: View?, item: LogsDetail) {
        Log.d(TAG, "item: $item")
        val ruleStr = StringBuilder()
        ruleStr.append(Rule.getRuleMatch(item.type, item.ruleFiled, item.ruleCheck, item.ruleValue, item.ruleSimSlot)).append(item.senderName)
        val detailStr = StringBuilder()
        detailStr.append(getString(R.string.rule)).append(ruleStr.toString()).append("\n\n")
        @SuppressLint("SimpleDateFormat") val utcFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        detailStr.append(getString(R.string.time)).append(DateUtils.date2String(item.time, utcFormatter)).append("\n\n")
        detailStr.append(getString(R.string.result)).append(FORWARD_STATUS_MAP[item.forwardStatus]).append("\n--------------------\n").append(item.forwardResponse)

        MaterialDialog.Builder(requireContext())
            .title(R.string.details)
            .content(detailStr.toString())
            .cancelable(true)
            .negativeText(R.string.resend)
            .onNegative { _: MaterialDialog?, _: DialogAction? ->
                XToastUtils.toast(R.string.resend_toast)
                SendUtils.retrySendMsg(item.id)
            }
            .show()
    }

    override fun onItemRemove(view: View?, id: Int) {}

    private fun reloadData() {
        viewModel.setType(currentType).setFilter(currentFilter)
        adapter.refresh()
        binding!!.recyclerView.scrollToPosition(0)
    }

    @Suppress("SameParameterValue")
    private fun initLogsFilterDialog(needInit: Boolean = false) {
        if (logsFilterPopup == null || needInit) {
            currentFilter = mutableMapOf()

            val logsFilterDialog = View.inflate(requireContext(), R.layout.dialog_logs_filter, null)
            val layoutTitle = logsFilterDialog.findViewById<LinearLayout>(R.id.layout_title)
            val layoutSimSlot = logsFilterDialog.findViewById<LinearLayout>(R.id.layout_sim_slot)
            val layoutCallType = logsFilterDialog.findViewById<LinearLayout>(R.id.layout_call_type)
            when (currentType) {
                "app" -> {
                    layoutTitle.visibility = View.VISIBLE
                    layoutSimSlot.visibility = View.GONE
                    layoutCallType.visibility = View.GONE
                }

                "call" -> {
                    layoutTitle.visibility = View.GONE
                    layoutSimSlot.visibility = View.VISIBLE
                    layoutCallType.visibility = View.VISIBLE
                }

                else -> {
                    layoutTitle.visibility = View.GONE
                    layoutSimSlot.visibility = View.VISIBLE
                    layoutCallType.visibility = View.GONE
                }
            }

            val scbCallType1 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type1)
            val scbCallType2 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type2)
            val scbCallType3 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type3)
            val scbCallType4 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type4)
            val scbCallType5 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type5)
            val scbCallType6 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_call_type6)
            val etFrom = logsFilterDialog.findViewById<EditText>(R.id.et_from)
            val etContent = logsFilterDialog.findViewById<EditText>(R.id.et_content)
            val etTitle = logsFilterDialog.findViewById<EditText>(R.id.et_title)
            val rgSimSlot = logsFilterDialog.findViewById<RadioGroup>(R.id.rg_sim_slot)
            val etStartTime = logsFilterDialog.findViewById<EditText>(R.id.et_start_time)
            val scbForwardStatus0 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_forward_status_0)
            val scbForwardStatus1 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_forward_status_1)
            val scbForwardStatus2 = logsFilterDialog.findViewById<SmoothCheckBox>(R.id.scb_forward_status_2)
            etStartTime.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showTimePicker(etStartTime.text.toString().trim(), getString(R.string.start_time), etStartTime)
                } else {
                    timePicker?.dismiss()
                }
            }
            val etEndTime = logsFilterDialog.findViewById<EditText>(R.id.et_end_time)
            etEndTime.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showTimePicker(etEndTime.text.toString().trim(), getString(R.string.end_time), etEndTime)
                } else {
                    timePicker?.dismiss()
                }
            }

            logsFilterPopup = MaterialDialog.Builder(requireContext())
                .iconRes(android.R.drawable.ic_menu_search)
                .title(R.string.menu_logs)
                .customView(logsFilterDialog, true)
                .cancelable(false)
                .autoDismiss(false)
                .neutralText(R.string.reset)
                .neutralColor(getColors(R.color.darkGrey))
                .onNeutral { dialog: MaterialDialog?, _: DialogAction? ->
                    dialog?.dismiss()
                    currentFilter = mutableMapOf()
                    logsFilterPopup = null
                    reloadData()
                }.positiveText(R.string.search).onPositive { dialog: MaterialDialog?, _: DialogAction? ->
                    currentFilter = mutableMapOf()
                    currentFilter["from"] = etFrom.text.toString().trim()
                    currentFilter["content"] = etContent.text.toString().trim()
                    currentFilter["title"] = etTitle.text.toString().trim()
                    currentFilter["start_time"] = etStartTime.text.toString().trim()
                    currentFilter["end_time"] = etEndTime.text.toString().trim()
                    currentFilter["sim_slot"] = if (currentType == "app") -1 else when (rgSimSlot.checkedRadioButtonId) {
                        R.id.rb_sim_slot_1 -> 0
                        R.id.rb_sim_slot_2 -> 1
                        else -> -1
                    }
                    if (currentType == "call") {
                        currentFilter["call_type"] = mutableListOf<Int>().apply {
                            if (scbCallType1.isChecked) add(1)
                            if (scbCallType2.isChecked) add(2)
                            if (scbCallType3.isChecked) add(3)
                            if (scbCallType4.isChecked) add(4)
                            if (scbCallType5.isChecked) add(5)
                            if (scbCallType6.isChecked) add(6)
                        }
                    }
                    currentFilter["forward_status"] = mutableListOf<Int>().apply {
                        if (scbForwardStatus0.isChecked) add(0)
                        if (scbForwardStatus1.isChecked) add(1)
                        if (scbForwardStatus2.isChecked) add(2)
                    }
                    reloadData()
                    dialog?.dismiss()
                }.build()
        }
    }

    private fun showTimePicker(time: String, title: String, et: EditText) {
        et.inputType = InputType.TYPE_NULL
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = try {
            if (time.isEmpty()) Date() else DateUtils.string2Date(time, DateUtils.yyyyMMddHHmmss.get())
        } catch (e: Exception) {
            Date()
        }
        timePicker = TimePickerBuilder(context) { date, _ ->
            ToastUtils.toast(DateUtils.date2String(date, DateUtils.yyyyMMddHHmmss.get()))
            et.setText(DateUtils.date2String(date, DateUtils.yyyyMMddHHmmss.get()))
        }
            .setTimeSelectChangeListener { date ->
                Log.i("pvTime", "onTimeSelectChanged")
                et.setText(DateUtils.date2String(date, DateUtils.yyyyMMddHHmmss.get()))
            }
            .setType(TimePickerType.ALL)
            .setTitleText(title)
            .isDialog(true)
            .setOutSideCancelable(false)
            .setDate(calendar)
            .build()
        timePicker?.show(false)
    }
}