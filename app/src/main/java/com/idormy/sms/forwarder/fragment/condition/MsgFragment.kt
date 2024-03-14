package com.idormy.sms.forwarder.fragment.condition

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.App.Companion.CALL_TYPE_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.spinner.AppListAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.AppListSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionMsgBinding
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
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.idormy.sms.forwarder.utils.FILED_CALL_TYPE
import com.idormy.sms.forwarder.utils.FILED_INFORM_CONTENT
import com.idormy.sms.forwarder.utils.FILED_MSG_CONTENT
import com.idormy.sms.forwarder.utils.FILED_MULTI_MATCH
import com.idormy.sms.forwarder.utils.FILED_PACKAGE_NAME
import com.idormy.sms.forwarder.utils.FILED_PHONE_NUM
import com.idormy.sms.forwarder.utils.FILED_TRANSPOND_ALL
import com.idormy.sms.forwarder.utils.FILED_UID
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_PARAMS_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.STATUS_ON
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_CONDITION_APP
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CALL
import com.idormy.sms.forwarder.utils.TASK_CONDITION_SMS
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
import com.xuexiang.xui.widget.spinner.materialspinner.MaterialSpinner
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.resource.ResUtils.getColors
import java.util.Date

@Page(name = "Msg")
@Suppress("PrivatePropertyName")
class MsgFragment : BaseFragment<FragmentTasksConditionMsgBinding?>(), View.OnClickListener {

    private val TAG: String = MsgFragment::class.java.simpleName
    private var titleBar: TitleBar? = null

    private var callType = 1
    private var callTypeIndex = 0
    private var resultCode: Int = TASK_CONDITION_SMS

    //已安装App信息列表
    private val appListSpinnerList = ArrayList<AppListAdapterItem>()
    private lateinit var appListSpinnerAdapter: AppListSpinnerAdapter<*>
    private val appListObserver = Observer { it: String ->
        Log.d(TAG, "EVENT_LOAD_APP_LIST: $it")
        initAppSpinner()
    }

    @JvmField
    @AutoWired(name = KEY_EVENT_PARAMS_CONDITION)
    var ruleType: String = "sms"

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_CONDITION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksConditionMsgBinding {
        return FragmentTasksConditionMsgBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        when (ruleType) {
            "app" -> {
                resultCode = TASK_CONDITION_APP
                titleBar?.setTitle(R.string.task_app)
                binding!!.ivTaskApp.visibility = View.VISIBLE
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
                resultCode = TASK_CONDITION_CALL
                titleBar?.setTitle(R.string.task_call)
                binding!!.ivTaskCall.visibility = View.VISIBLE
                binding!!.rbContent.visibility = View.GONE
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbUid.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
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
                titleBar?.setTitle(R.string.task_sms)
                binding!!.ivTaskSms.visibility = View.VISIBLE
                binding!!.rbCallType.visibility = View.GONE
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbUid.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
            }
        }
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)

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

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val rule = Gson().fromJson(eventData, Rule::class.java)
            Log.d(TAG, rule.toString())

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
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    val ruleNew = checkForm()
                    testRule(ruleNew)
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkForm()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, settingVo.description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(resultCode, intent)
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

    //提交前检查表单
    private fun checkForm(): Rule {
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

        val simSlot = when (binding!!.rgSimSlot.checkedRadioButtonId) {
            R.id.rb_sim_slot_1 -> CHECK_SIM_SLOT_1
            R.id.rb_sim_slot_2 -> CHECK_SIM_SLOT_2
            else -> CHECK_SIM_SLOT_ALL
        }

        return Rule(0, ruleType, filed, check, value, 0, "", "", simSlot, STATUS_ON, Date(), listOf())
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
                    msg.append(CALL_TYPE_MAP[callType.toString()] ?: getString(R.string.unknown_call))
                } else {
                    msg.append(etContent.text.toString())
                }

                val msgInfo = MsgInfo(ruleType, etFrom.text.toString(), msg.toString(), Date(), simInfo, simSlot, subId, callTypeTest)
                if (!rule.checkMsg(msgInfo)) {
                    throw Exception(getString(R.string.unmatched_rule))
                }

                XToastUtils.success(getString(R.string.matched_rule))

            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }.show()
    }
}