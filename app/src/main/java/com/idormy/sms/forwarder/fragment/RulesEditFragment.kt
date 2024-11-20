package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.App.Companion.CALL_TYPE_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.SenderRecyclerAdapter
import com.idormy.sms.forwarder.adapter.base.ItemMoveCallback
import com.idormy.sms.forwarder.adapter.spinner.AppListAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.AppListSpinnerAdapter
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerAdapter
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerItem
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.RuleViewModel
import com.idormy.sms.forwarder.databinding.FragmentRulesEditBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.CHECK_CONTAIN
import com.idormy.sms.forwarder.utils.CHECK_END_WITH
import com.idormy.sms.forwarder.utils.CHECK_IS
import com.idormy.sms.forwarder.utils.CHECK_NOT_CONTAIN
import com.idormy.sms.forwarder.utils.CHECK_REGEX
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_1
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_2
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_ALL
import com.idormy.sms.forwarder.utils.CHECK_START_WITH
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.DataProvider
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.FILED_CALL_TYPE
import com.idormy.sms.forwarder.utils.FILED_INFORM_CONTENT
import com.idormy.sms.forwarder.utils.FILED_MSG_CONTENT
import com.idormy.sms.forwarder.utils.FILED_MULTI_MATCH
import com.idormy.sms.forwarder.utils.FILED_PACKAGE_NAME
import com.idormy.sms.forwarder.utils.FILED_PHONE_NUM
import com.idormy.sms.forwarder.utils.FILED_TRANSPOND_ALL
import com.idormy.sms.forwarder.utils.FILED_UID
import com.idormy.sms.forwarder.utils.KEY_RULE_CLONE
import com.idormy.sms.forwarder.utils.KEY_RULE_ID
import com.idormy.sms.forwarder.utils.KEY_RULE_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_ALL
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_UNTIL_FAIL
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_UNTIL_SUCCESS
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.STATUS_ON
import com.idormy.sms.forwarder.utils.SendUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder
import com.xuexiang.xui.widget.picker.widget.listener.OnOptionsSelectListener
import com.xuexiang.xui.widget.spinner.materialspinner.MaterialSpinner
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.resource.ResUtils.getColors
import com.xuexiang.xutil.resource.ResUtils.getDrawable
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Calendar
import java.util.Date

@Page(name = "转发规则·编辑器")
@Suppress("PrivatePropertyName")
class RulesEditFragment : BaseFragment<FragmentRulesEditBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = RulesEditFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<RuleViewModel> { BaseViewModelFactory(context) }

    private var callType = 1
    private var callTypeIndex = 0

    //免打扰(禁用转发)时间段
    private val mTimeOption = DataProvider.timePeriodOption
    private var silentPeriodStart = 0
    private var silentPeriodEnd = 0

    //所有发送通道下拉框
    private var senderListAll = mutableListOf<Sender>()
    private val senderSpinnerList = mutableListOf<SenderSpinnerItem>()
    private lateinit var senderSpinnerAdapter: SenderSpinnerAdapter<*>

    //已选发送通道列表
    private var senderId = 0L
    private var senderListSelected = mutableListOf<Sender>()
    private lateinit var senderRecyclerView: RecyclerView
    private lateinit var senderRecyclerAdapter: SenderRecyclerAdapter

    //已安装App信息列表
    private val appListSpinnerList = ArrayList<AppListAdapterItem>()
    private lateinit var appListSpinnerAdapter: AppListSpinnerAdapter<*>
    private val appListObserver = Observer { it: String ->
        Log.d(TAG, "EVENT_LOAD_APP_LIST: $it")
        initAppSpinner()
    }

    @JvmField
    @AutoWired(name = KEY_RULE_ID)
    var ruleId: Long = 0

    @JvmField
    @AutoWired(name = KEY_RULE_TYPE)
    var ruleType: String = "sms"

    @JvmField
    @AutoWired(name = KEY_RULE_CLONE)
    var isClone: Boolean = false

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentRulesEditBinding {
        return FragmentRulesEditBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.menu_rules)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        when (ruleType) {
            "app" -> {
                titleBar?.setTitle(R.string.app_rule)
                binding!!.layoutSimSlot.visibility = View.GONE
                binding!!.rbPhone.visibility = View.GONE
                binding!!.rbCallType.visibility = View.GONE
                binding!!.rbContent.visibility = View.GONE
                binding!!.tvMuRuleTips.setText(R.string.mu_rule_app_tips)
                //初始化APP下拉列表
                initAppSpinner()
                //监听已安装App信息列表加载完成事件
                LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).observeStickyForever(appListObserver)
            }

            "call" -> {
                titleBar?.setTitle(R.string.call_rule)
                binding!!.rbContent.visibility = View.GONE
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbUid.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
                //binding!!.rbMultiMatch.visibility = View.GONE
                binding!!.tvMuRuleTips.setText(R.string.mu_rule_call_tips)

                //通话类型：1.来电挂机 2.去电挂机 3.未接来电 4.来电提醒 5.来电接通 6.去电拨出
                binding!!.spCallType.setItems(CALL_TYPE_MAP.values.toList())
                binding!!.spCallType.setOnItemSelectedListener { _: MaterialSpinner?, _: Int, _: Long, item: Any ->
                    CALL_TYPE_MAP.forEach {
                        if (it.value == item) callType = it.key.toInt()
                    }
                }
                binding!!.spCallType.setOnNothingSelectedListener {
                    callType = 1
                    callTypeIndex = 0
                    binding!!.spCallType.selectedIndex = callTypeIndex
                }
                binding!!.spCallType.selectedIndex = callTypeIndex
            }

            else -> {
                titleBar?.setTitle(R.string.sms_rule)
                binding!!.rbCallType.visibility = View.GONE
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbUid.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
            }
        }

        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glSmsTemplate, binding!!.etSmsTemplate, ruleType)

        if (ruleId <= 0) { //新增
            titleBar?.setSubTitle(getString(R.string.add_rule))
            binding!!.btnDel.setText(R.string.discard)
            initSenderSpinner()
        } else { //编辑 & 克隆
            binding!!.btnDel.setText(R.string.del)
            initForm()
        }

    }

    override fun initListeners() {
        binding!!.btnSilentPeriod.setOnClickListener(this)
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)

        binding!!.sbStatus.setOnCheckedChangeListener(this)
        binding!!.sbSmsTemplate.setOnCheckedChangeListener(this)
        binding!!.sbRegexReplace.setOnCheckedChangeListener(this)

        binding!!.rgFiled.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (ruleType == "app" && appListSpinnerList.isNotEmpty()) {
                binding!!.layoutAppList.visibility = if (checkedId == R.id.rb_inform_content) View.GONE else View.VISIBLE
            }
            when (checkedId) {
                R.id.rb_transpond_all -> {
                    binding!!.rgCheck.check(R.id.rb_is)
                    binding!!.spCallType.visibility = View.GONE
                    binding!!.tvMuRuleTips.visibility = View.GONE
                    binding!!.layoutMatchType.visibility = View.GONE
                    binding!!.layoutMatchValue.visibility = View.GONE
                }

                R.id.rb_multi_match -> {
                    binding!!.rgCheck.check(R.id.rb_is)
                    binding!!.spCallType.visibility = View.GONE
                    binding!!.tvMuRuleTips.visibility = View.VISIBLE
                    binding!!.layoutMatchType.visibility = View.GONE
                    binding!!.layoutMatchValue.visibility = View.VISIBLE
                    binding!!.etValue.visibility = View.VISIBLE
                }

                R.id.rb_call_type -> {
                    binding!!.rgCheck.check(R.id.rb_is)
                    binding!!.tvMuRuleTips.visibility = View.GONE
                    binding!!.layoutMatchType.visibility = View.GONE
                    binding!!.layoutMatchValue.visibility = View.VISIBLE
                    binding!!.etValue.visibility = View.GONE
                    binding!!.spCallType.visibility = View.VISIBLE
                }

                else -> {
                    binding!!.spCallType.visibility = View.GONE
                    binding!!.tvMuRuleTips.visibility = View.GONE
                    binding!!.layoutMatchType.visibility = View.VISIBLE
                    binding!!.layoutMatchValue.visibility = View.VISIBLE
                    binding!!.etValue.visibility = View.VISIBLE
                }
            }
        }

        binding!!.rgCheck.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (group != null && checkedId > 0) {
                binding!!.rgCheck2.clearCheck()
                group.check(checkedId)
            }
        }

        binding!!.rgCheck2.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (group != null && checkedId > 0) {
                binding!!.rgCheck.clearCheck()
                group.check(checkedId)
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.sb_status -> {
                binding!!.layoutSilentPeriod.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            R.id.sb_sms_template -> {
                if (isChecked) {
                    binding!!.layoutSmsTemplate.visibility = View.VISIBLE
                } else {
                    binding!!.layoutSmsTemplate.visibility = View.GONE
                    binding!!.etSmsTemplate.setText("")
                }
            }

            R.id.sb_regex_replace -> {
                if (isChecked) {
                    binding!!.layoutRegexReplace.visibility = View.VISIBLE
                } else {
                    binding!!.layoutRegexReplace.visibility = View.GONE
                    binding!!.etRegexReplace.setText("")
                }
            }

            else -> {}
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_silent_period -> {
                    OptionsPickerBuilder(context, OnOptionsSelectListener { _: View?, options1: Int, options2: Int, _: Int ->
                        silentPeriodStart = options1
                        silentPeriodEnd = options2
                        val txt = mTimeOption[options1] + " ~ " + mTimeOption[options2]
                        binding!!.tvSilentPeriod.text = txt
                        XToastUtils.toast(txt)
                        return@OnOptionsSelectListener false
                    }).setTitleText(getString(R.string.select_time_period)).setSelectOptions(silentPeriodStart, silentPeriodEnd).build<Any>().also {
                        it.setNPicker(mTimeOption, mTimeOption)
                        it.show()
                    }
                }

                R.id.btn_test -> {
                    val ruleNew = checkForm()
                    testRule(ruleNew)
                    return
                }

                R.id.btn_del -> {
                    if (ruleId <= 0 || isClone) {
                        popToBack()
                        return
                    }

                    MaterialDialog.Builder(requireContext()).title(R.string.delete_rule_title).content(R.string.delete_rule_tips).positiveText(R.string.lab_yes).negativeText(R.string.lab_no).onPositive { _: MaterialDialog?, _: DialogAction? ->
                        viewModel.delete(ruleId)
                        XToastUtils.success(R.string.delete_rule_toast)
                        popToBack()
                    }.show()
                    return
                }

                R.id.btn_save -> {
                    val ruleNew = checkForm()
                    if (isClone) ruleNew.id = 0
                    Log.d(TAG, ruleNew.toString())
                    viewModel.insertOrUpdate(ruleNew)
                    XToastUtils.success(R.string.tipSaveSuccess)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString())
            e.printStackTrace()
            Log.e(TAG, e.toString())
        }
    }

    //初始化发送通道下拉框
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun initSenderSpinner() {
        //免打扰(禁用转发)时间段
        binding!!.tvSilentPeriod.text = mTimeOption[silentPeriodStart] + " ~ " + mTimeOption[silentPeriodEnd]

        //初始化发送通道下拉框
        binding!!.spSender.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val item = senderSpinnerAdapter.getItemSource(position) as SenderSpinnerItem
                senderId = item.id!!
                if (senderId > 0L) {
                    senderListSelected.forEach {
                        if (senderId == it.id) {
                            XToastUtils.warning(getString(R.string.sender_contains_tips))
                            return@setOnItemClickListener
                        }
                    }
                    senderListAll.forEach {
                        if (senderId == it.id) {
                            senderListSelected.add(it)
                        }
                    }

                    checkSenderLogicShow()

                    senderRecyclerAdapter.notifyDataSetChanged()

                    if (STATUS_OFF == item.status) {
                        XToastUtils.warning(getString(R.string.sender_disabled_tips))
                    }
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }

        // 初始化已选发送通道列表 RecyclerView 和 Adapter
        senderRecyclerView = binding!!.recyclerSenders
        senderRecyclerAdapter = SenderRecyclerAdapter(senderListSelected, { position ->
            senderListSelected.removeAt(position)
            senderRecyclerAdapter.notifyItemRemoved(position)
            senderRecyclerAdapter.notifyItemRangeChanged(position, senderListSelected.size) // 更新索引
            checkSenderLogicShow()
        })
        senderRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = senderRecyclerAdapter
        }
        val senderMoveCallback = ItemMoveCallback(object : ItemMoveCallback.Listener {
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Log.d(TAG, "onItemMove: $fromPosition $toPosition")
                senderRecyclerAdapter.onItemMove(fromPosition, toPosition)
                senderListSelected = senderRecyclerAdapter.itemList
            }

            override fun onDragFinished() {
                senderListSelected = senderRecyclerAdapter.itemList
                //senderRecyclerAdapter.notifyDataSetChanged()
                Log.d(TAG, "onDragFinished: $senderListSelected")
            }
        })
        val senderTouchHelper = ItemTouchHelper(senderMoveCallback)
        senderTouchHelper.attachToRecyclerView(senderRecyclerView)
        senderRecyclerAdapter.setTouchHelper(senderTouchHelper)

        //获取发送通道列表
        getSenderList()
    }

    //获取发送通道列表
    private fun getSenderList() {
        Core.sender.getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Sender>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "getSenderList error: ${e.message}")
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSuccess(senderList: List<Sender>) {
                if (senderList.isEmpty()) {
                    XToastUtils.error(R.string.add_sender_first)
                    return
                }

                senderSpinnerList.clear()
                senderListAll = senderList as MutableList<Sender>
                for (sender in senderList) {
                    val name = if (sender.name.length > 20) sender.name.substring(0, 19) else sender.name
                    senderSpinnerList.add(SenderSpinnerItem(name, getDrawable(sender.imageId), sender.id, sender.status))
                }
                senderSpinnerAdapter = SenderSpinnerAdapter(senderSpinnerList).setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spSender.setAdapter(senderSpinnerAdapter)
                //senderSpinnerAdapter.notifyDataSetChanged()

                //更新senderListSelected的状态与名称
                senderListSelected.forEach {
                    senderListAll.forEach { sender ->
                        if (it.id == sender.id) {
                            it.name = sender.name
                            it.status = sender.status
                        }
                    }
                }
                senderRecyclerAdapter.notifyDataSetChanged()

            }
        })
    }

    //初始化APP下拉列表
    private fun initAppSpinner() {
        if (ruleType != "app") return

        //未开启异步获取已安装App信息开关时，规则编辑不显示已安装APP下拉框
        if (!SettingUtils.enableLoadUserAppList && !SettingUtils.enableLoadSystemAppList) return

        if (App.UserAppList.isEmpty() && App.SystemAppList.isEmpty()) {
            XToastUtils.info(getString(R.string.loading_app_list))
            val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
            WorkManager.getInstance(XUtil.getContext()).enqueue(request)
            return
        }

        appListSpinnerList.clear()
        if (SettingUtils.enableLoadUserAppList) {
            for (appInfo in App.UserAppList) {
                if (TextUtils.isEmpty(appInfo.packageName)) continue
                appListSpinnerList.add(AppListAdapterItem(appInfo.name, appInfo.icon, appInfo.packageName))
            }
        }
        if (SettingUtils.enableLoadSystemAppList) {
            for (appInfo in App.SystemAppList) {
                if (TextUtils.isEmpty(appInfo.packageName)) continue
                appListSpinnerList.add(AppListAdapterItem(appInfo.name, appInfo.icon, appInfo.packageName))
            }
        }

        //列表为空也不显示下拉框
        if (appListSpinnerList.isEmpty()) return

        appListSpinnerAdapter = AppListSpinnerAdapter(appListSpinnerList).setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
        binding!!.spApp.setAdapter(appListSpinnerAdapter)
        binding!!.spApp.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val appInfo = appListSpinnerAdapter.getItemSource(position) as AppListAdapterItem
                CommonUtils.insertOrReplaceText2Cursor(binding!!.etValue, appInfo.packageName.toString())
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
        binding!!.layoutAppList.visibility = View.VISIBLE

    }

    //初始化表单
    private fun initForm() {
        Core.rule.get(ruleId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<Rule> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, e.toString())
            }

            override fun onSuccess(rule: Rule) {
                Log.d(TAG, rule.senderList.toString())
                rule.senderList.forEach {
                    senderId = it.id
                    senderListSelected.add(it)
                    Log.d(TAG, it.toString())
                }

                if (isClone) {
                    titleBar?.setSubTitle(getString(R.string.clone_rule))
                    binding!!.btnDel.setText(R.string.discard)
                } else {
                    titleBar?.setSubTitle(getString(R.string.edit_rule))
                }
                Log.d(TAG, rule.toString())

                checkSenderLogicShow()
                binding!!.rgSenderLogic.check(rule.getSenderLogicCheckId())
                binding!!.rgSimSlot.check(rule.getSimSlotCheckId())
                binding!!.rgFiled.check(rule.getFiledCheckId())
                val checkId = rule.getCheckCheckId()
                if (checkId == R.id.rb_is || checkId == R.id.rb_contain || checkId == R.id.rb_not_contain) {
                    binding!!.rgCheck.check(checkId)
                } else {
                    binding!!.rgCheck2.check(checkId)
                }
                binding!!.etValue.setText(rule.value)
                if (ruleType == "call" && rule.filed == FILED_CALL_TYPE) {
                    callType = rule.value.toInt()
                    callTypeIndex = callType - 1
                    binding!!.spCallType.selectedIndex = callTypeIndex
                }
                binding!!.sbSmsTemplate.isChecked = !TextUtils.isEmpty(rule.smsTemplate.trim())
                binding!!.etSmsTemplate.setText(rule.smsTemplate.trim())
                binding!!.sbRegexReplace.isChecked = !TextUtils.isEmpty(rule.regexReplace.trim())
                binding!!.etRegexReplace.setText(rule.regexReplace.trim())
                binding!!.sbStatus.isChecked = rule.statusChecked
                silentPeriodStart = rule.silentPeriodStart
                silentPeriodEnd = rule.silentPeriodEnd
                //初始化发送通道下拉框
                initSenderSpinner()

                //绑定免打扰日期
                val silentPeriodDays = rule.silentDayOfWeek.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
                if (silentPeriodDays.isNotEmpty()) {
                    val map = mapOf(
                        Calendar.SUNDAY to binding!!.sun,
                        Calendar.MONDAY to binding!!.mon,
                        Calendar.TUESDAY to binding!!.tue,
                        Calendar.WEDNESDAY to binding!!.wed,
                        Calendar.THURSDAY to binding!!.thu,
                        Calendar.FRIDAY to binding!!.fri,
                        Calendar.SATURDAY to binding!!.sat,
                    )
                    silentPeriodDays.forEach { map[it]?.isChecked = true }
                }
            }
        })
    }

    private fun checkSenderLogicShow() {
        if (senderListSelected.size > 1) {
            binding!!.layoutSenderLogic.visibility = View.VISIBLE
        } else {
            binding!!.layoutSenderLogic.visibility = View.GONE
            binding!!.rgSenderLogic.check(R.id.rb_sender_logic_all)
        }
    }

    //提交前检查表单
    private fun checkForm(): Rule {
        if (senderListSelected.isEmpty() || senderId == 0L) {
            throw Exception(getString(R.string.new_sender_first))
        }
        val filed = when (binding!!.rgFiled.checkedRadioButtonId) {
            R.id.rb_content -> FILED_MSG_CONTENT
            R.id.rb_phone -> FILED_PHONE_NUM
            R.id.rb_call_type -> FILED_CALL_TYPE
            R.id.rb_package_name -> FILED_PACKAGE_NAME
            R.id.rb_uid -> FILED_UID
            R.id.rb_inform_content -> FILED_INFORM_CONTENT
            R.id.rb_multi_match -> FILED_MULTI_MATCH
            else -> FILED_TRANSPOND_ALL
        }
        val check = when (kotlin.math.max(binding!!.rgCheck.checkedRadioButtonId, binding!!.rgCheck2.checkedRadioButtonId)) {
            R.id.rb_contain -> CHECK_CONTAIN
            R.id.rb_not_contain -> CHECK_NOT_CONTAIN
            R.id.rb_start_with -> CHECK_START_WITH
            R.id.rb_end_with -> CHECK_END_WITH
            R.id.rb_regex -> CHECK_REGEX
            else -> CHECK_IS
        }
        var value = binding!!.etValue.text.toString().trim()
        if (FILED_CALL_TYPE == filed) {
            value = callType.toString()
            if (callType !in 1..6) {
                throw Exception(getString(R.string.invalid_call_type))
            }
        } else if (FILED_TRANSPOND_ALL != filed && TextUtils.isEmpty(value)) {
            throw Exception(getString(R.string.invalid_match_value))
        }
        if (FILED_MULTI_MATCH == filed) {
            val lineError = checkMultiMatch(value)
            if (lineError > 0) {
                throw Exception(String.format(getString(R.string.invalid_multi_match), lineError))
            }
        }

        val smsTemplate = binding!!.etSmsTemplate.text.toString().trim()
        val checkResult = CommonUtils.checkTemplateTag(smsTemplate)
        if (checkResult.isNotEmpty()) {
            throw Exception(checkResult)
        }
        val regexReplace = binding!!.etRegexReplace.text.toString().trim()
        val lineNum = checkRegexReplace(regexReplace)
        if (lineNum > 0) {
            throw Exception(String.format(getString(R.string.invalid_regex_replace), lineNum))
        }

        val senderLogic = when (binding!!.rgSenderLogic.checkedRadioButtonId) {
            R.id.rb_sender_logic_until_fail -> SENDER_LOGIC_UNTIL_FAIL
            R.id.rb_sender_logic_until_success -> SENDER_LOGIC_UNTIL_SUCCESS
            else -> SENDER_LOGIC_ALL
        }

        val simSlot = when (binding!!.rgSimSlot.checkedRadioButtonId) {
            R.id.rb_sim_slot_1 -> CHECK_SIM_SLOT_1
            R.id.rb_sim_slot_2 -> CHECK_SIM_SLOT_2
            else -> CHECK_SIM_SLOT_ALL
        }
        val status = if (binding!!.sbStatus.isChecked) STATUS_ON else STATUS_OFF

        val map = mapOf(
            Calendar.SUNDAY to binding!!.sun,
            Calendar.MONDAY to binding!!.mon,
            Calendar.TUESDAY to binding!!.tue,
            Calendar.WEDNESDAY to binding!!.wed,
            Calendar.THURSDAY to binding!!.thu,
            Calendar.FRIDAY to binding!!.fri,
            Calendar.SATURDAY to binding!!.sat,
        )

        val silentDayOfWeek = map.filter { it.value.isChecked }
            .toList().map { it.first }.joinToString(",")

        return Rule(
            ruleId,
            ruleType,
            filed,
            check,
            value,
            senderId,
            smsTemplate,
            regexReplace,
            simSlot,
            status,
            Date(),
            senderListSelected,
            senderLogic,
            silentPeriodStart,
            silentPeriodEnd,
            silentDayOfWeek,
        )
    }

    //检查多重匹配规则是否正确
    private fun checkMultiMatch(ruleStr: String?): Int {
        if (TextUtils.isEmpty(ruleStr)) return 0

        //Log.d(TAG, getString(R.string.regex_multi_match))
        val regex = Regex(pattern = getString(R.string.regex_multi_match))
        var lineNum = 1
        val lineArray = ruleStr?.split("\\n".toRegex())?.toTypedArray()
        for (line in lineArray!!) {
            Log.d(TAG, line)
            if (!line.matches(regex)) return lineNum
            lineNum++
        }

        return 0
    }

    //检查正则替换填写是否正确
    private fun checkRegexReplace(regexReplace: String?): Int {
        if (TextUtils.isEmpty(regexReplace)) return 0

        var lineNum = 1
        val lineArray = regexReplace?.split("\\n".toRegex())?.toTypedArray()
        for (line in lineArray!!) {
            val position = line.indexOf("===")
            if (position < 1) return lineNum

            // 校验正则表达式部分是否合法
            try {
                line.substring(0, position).toRegex()
            } catch (e: Exception) {
                return lineNum
            }

            lineNum++
        }
        return 0
    }

    private fun testRule(rule: Rule) {
        val dialogTest = View.inflate(requireContext(), R.layout.dialog_rule_test, null)
        val tvSimSlot = dialogTest.findViewById<TextView>(R.id.tv_sim_slot)
        val rgSimSlot = dialogTest.findViewById<RadioGroup>(R.id.rg_sim_slot)
        val tvFrom = dialogTest.findViewById<TextView>(R.id.tv_from)
        val etFrom = dialogTest.findViewById<EditText>(R.id.et_from)
        val tvTitle = dialogTest.findViewById<TextView>(R.id.tv_title)
        val etTitle = dialogTest.findViewById<EditText>(R.id.et_title)
        val tvContent = dialogTest.findViewById<TextView>(R.id.tv_content)
        val etContent = dialogTest.findViewById<EditText>(R.id.et_content)
        //通话类型
        val tvCallType = dialogTest.findViewById<TextView>(R.id.tv_call_type)
        val spCallType = dialogTest.findViewById<MaterialSpinner>(R.id.sp_call_type)
        var callTypeTest = callType
        var callTypeIndexTest = callTypeIndex

        if ("app" == ruleType) {
            tvSimSlot.visibility = View.GONE
            rgSimSlot.visibility = View.GONE
            tvTitle.visibility = View.VISIBLE
            etTitle.visibility = View.VISIBLE
            tvFrom.setText(R.string.test_package_name)
            tvContent.setText(R.string.test_inform_content)
            tvCallType.visibility = View.GONE
            spCallType.visibility = View.GONE
        } else if ("call" == ruleType) {
            tvContent.visibility = View.GONE
            etContent.visibility = View.GONE
            tvCallType.visibility = View.VISIBLE
            spCallType.visibility = View.VISIBLE
            spCallType.setItems(CALL_TYPE_MAP.values.toList())
            spCallType.setOnItemSelectedListener { _: MaterialSpinner?, _: Int, _: Long, item: Any ->
                CALL_TYPE_MAP.forEach {
                    if (it.value == item) callTypeTest = it.key.toInt()
                }
            }
            spCallType.setOnNothingSelectedListener {
                callTypeTest = callType
                callTypeIndexTest = callTypeIndex
                spCallType.selectedIndex = callTypeIndexTest
            }
            spCallType.selectedIndex = callTypeIndexTest
        }

        MaterialDialog.Builder(requireContext()).iconRes(android.R.drawable.ic_dialog_email).title(R.string.rule_tester).customView(dialogTest, true).cancelable(false).autoDismiss(false).neutralText(R.string.action_back).neutralColor(getColors(R.color.darkGrey)).onNeutral { dialog: MaterialDialog?, _: DialogAction? ->
            dialog?.dismiss()
        }.positiveText(R.string.action_test).onPositive { _: MaterialDialog?, _: DialogAction? ->
            try {
                val simSlot = when (if (ruleType == "app") -1 else rgSimSlot.checkedRadioButtonId) {
                    R.id.rb_sim_slot_1 -> 0
                    R.id.rb_sim_slot_2 -> 1
                    else -> -1
                }

                val testSim = "SIM" + (simSlot + 1)
                val ruleSim: String = rule.simSlot
                if (ruleSim != "ALL" && ruleSim != testSim) {
                    throw Exception(getString(R.string.card_slot_does_not_match))
                }

                //获取卡槽信息
                val simInfo = when (simSlot) {
                    0 -> "SIM1_" + SettingUtils.extraSim1
                    1 -> "SIM2_" + SettingUtils.extraSim2
                    else -> etTitle.text.toString()
                }
                val subId = when (simSlot) {
                    0 -> SettingUtils.subidSim1
                    1 -> SettingUtils.subidSim2
                    else -> 0
                }

                val msg = StringBuilder()
                if (ruleType == "call") {
                    val phoneNumber = etFrom.text.toString()
                    val contacts = PhoneUtils.getContactByNumber(phoneNumber)
                    val contactName = if (contacts.isNotEmpty()) contacts[0].name else getString(R.string.unknown_number)
                    msg.append(getString(R.string.contact)).append(contactName).append("\n")
                    msg.append(getString(R.string.mandatory_type))
                    msg.append(CALL_TYPE_MAP[callTypeTest.toString()] ?: getString(R.string.unknown_call))
                } else {
                    msg.append(etContent.text.toString())
                }

                val msgInfo = MsgInfo(ruleType, etFrom.text.toString(), msg.toString(), Date(), simInfo, simSlot, subId, callTypeTest)
                if (!rule.checkMsg(msgInfo)) {
                    throw Exception(getString(R.string.unmatched_rule))
                }

                Thread {
                    try {
                        SendUtils.sendMsgSender(msgInfo, rule)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, e.toString())
                        LiveEventBus.get(EVENT_TOAST_ERROR, String::class.java).post(e.message.toString())
                    }
                }.start()

            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }.show()
    }
}
