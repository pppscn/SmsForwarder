package com.idormy.sms.forwarder.fragment

import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.spinner.AppListAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.AppListSpinnerAdapter
import com.idormy.sms.forwarder.adapter.spinner.SenderAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.RuleViewModel
import com.idormy.sms.forwarder.databinding.FragmentRulesEditBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.*
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.app.AppUtils
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

@Page(name = "转发规则·编辑器")
@Suppress("PrivatePropertyName", "CAST_NEVER_SUCCEEDS")
class RulesEditFragment : BaseFragment<FragmentRulesEditBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = RulesEditFragment::class.java.simpleName
    var titleBar: TitleBar? = null
    private val viewModel by viewModels<RuleViewModel> { BaseViewModelFactory(context) }

    //当前发送通道
    var senderId = 0L

    //发送通道列表
    private val senderSpinnerList = ArrayList<SenderAdapterItem>()
    private lateinit var senderSpinnerAdapter: SenderSpinnerAdapter<*>

    //已安装App信息列表
    private val appListSpinnerList = ArrayList<AppListAdapterItem>()
    private lateinit var appListSpinnerAdapter: AppListSpinnerAdapter<*>

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
                binding!!.rbContent.visibility = View.GONE
                binding!!.tvMuRuleTips.setText(R.string.mu_rule_app_tips)
                binding!!.btInsertSender.visibility = View.GONE
                binding!!.btInsertContent.visibility = View.GONE
            }
            "call" -> {
                titleBar?.setTitle(R.string.call_rule)
                binding!!.rbContent.visibility = View.GONE
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
                binding!!.rbMultiMatch.visibility = View.GONE
                binding!!.btInsertContent.visibility = View.GONE
                binding!!.btInsertSenderApp.visibility = View.GONE
                binding!!.btInsertContentApp.visibility = View.GONE
            }
            else -> {
                titleBar?.setTitle(R.string.sms_rule)
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
                binding!!.btInsertSenderApp.visibility = View.GONE
                binding!!.btInsertContentApp.visibility = View.GONE
            }
        }

        if (ruleId <= 0) { //新增
            titleBar?.setSubTitle(getString(R.string.add_rule))
            binding!!.btnDel.setText(R.string.discard)
            initSenderSpinner()
        } else { //编辑 & 克隆
            binding!!.btnDel.setText(R.string.del)
            initForm()
        }

        //初始化APP下拉列表
        initAppSpinner()
    }

    override fun initListeners() {
        binding!!.btInsertSender.setOnClickListener(this)
        binding!!.btInsertContent.setOnClickListener(this)
        binding!!.btInsertSenderApp.setOnClickListener(this)
        binding!!.btInsertContentApp.setOnClickListener(this)
        binding!!.btInsertExtra.setOnClickListener(this)
        binding!!.btInsertTime.setOnClickListener(this)
        binding!!.btInsertDeviceName.setOnClickListener(this)
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)

        binding!!.sbSmsTemplate.setOnCheckedChangeListener(this)
        binding!!.sbRegexReplace.setOnCheckedChangeListener(this)

        binding!!.rgFiled.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (ruleType == "app" && appListSpinnerList.isNotEmpty()) {
                binding!!.layoutAppList.visibility = if (checkedId == R.id.rb_inform_content) View.GONE else View.VISIBLE
            }
            when (checkedId) {
                R.id.rb_transpond_all -> {
                    binding!!.rgCheck.check(R.id.rb_is)
                    binding!!.tvMuRuleTips.visibility = View.GONE
                    binding!!.layoutMatchType.visibility = View.GONE
                    binding!!.layoutMatchValue.visibility = View.GONE
                }
                R.id.rb_multi_match -> {
                    binding!!.rgCheck.check(R.id.rb_is)
                    binding!!.tvMuRuleTips.visibility = View.VISIBLE
                    binding!!.layoutMatchType.visibility = View.GONE
                    binding!!.layoutMatchValue.visibility = View.VISIBLE
                }
                else -> {
                    binding!!.tvMuRuleTips.visibility = View.GONE
                    binding!!.layoutMatchType.visibility = View.VISIBLE
                    binding!!.layoutMatchValue.visibility = View.VISIBLE
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
            val etSmsTemplate: EditText = binding!!.etSmsTemplate
            when (v.id) {
                R.id.bt_insert_sender -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_from))
                    return
                }
                R.id.bt_insert_content -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_sms))
                    return
                }
                R.id.bt_insert_sender_app -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_package_name))
                    return
                }
                R.id.bt_insert_content_app -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_msg))
                    return
                }
                R.id.bt_insert_extra -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_card_slot))
                    return
                }
                R.id.bt_insert_time -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_receive_time))
                    return
                }
                R.id.bt_insert_device_name -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_device_name))
                    return
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

                    MaterialDialog.Builder(requireContext())
                        .title(R.string.delete_rule_title)
                        .content(R.string.delete_rule_tips)
                        .positiveText(R.string.lab_yes)
                        .negativeText(R.string.lab_no)
                        .onPositive { _: MaterialDialog?, _: DialogAction? ->
                            viewModel.delete(ruleId)
                            XToastUtils.success(R.string.delete_rule_toast)
                            popToBack()
                        }
                        .show()
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
        }
    }

    //初始化发送通道下拉框
    private fun initSenderSpinner() {
        AppDatabase.getInstance(requireContext())
            .senderDao()
            .getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<List<Sender>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onSuccess(senderList: List<Sender>) {
                    if (senderList.isEmpty()) {
                        XToastUtils.error(R.string.add_sender_first)
                        return
                    }

                    for (sender in senderList) {
                        val name = if (sender.name.length > 20) sender.name.substring(0, 19) else sender.name
                        senderSpinnerList.add(SenderAdapterItem(name, sender.imageId, sender.id, sender.status))
                    }
                    senderSpinnerAdapter = SenderSpinnerAdapter(senderSpinnerList)
                        //.setTextColor(ResUtils.getColor(R.color.green))
                        //.setTextSize(12F)
                        .setIsFilterKey(true)
                        .setFilterColor("#EF5362")
                        .setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                    binding!!.spSender.setAdapter(senderSpinnerAdapter)

                    if (senderId > 0) {
                        for (sender in senderSpinnerList) {
                            if (sender.id == senderId) {
                                binding!!.ivSenderImage.setImageDrawable(sender.icon)
                                binding!!.ivSenderStatus.setImageDrawable(
                                    ResUtils.getDrawable(
                                        when (sender.status) {
                                            STATUS_OFF -> R.drawable.icon_off
                                            else -> R.drawable.icon_on
                                        }
                                    )
                                )
                                binding!!.tvSenderName.text = sender.title
                            }
                        }
                    }
                }
            })
        binding!!.spSender.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                //val sender = senderSpinnerList[position]
                val sender = senderSpinnerAdapter.getItemSource(position) as SenderAdapterItem
                sender.id.also {
                    senderId = it ?: 0L
                }
                binding!!.ivSenderImage.setImageDrawable(sender.icon)
                binding!!.ivSenderStatus.setImageDrawable(
                    ResUtils.getDrawable(
                        when (sender.status) {
                            STATUS_OFF -> R.drawable.icon_off
                            else -> R.drawable.icon_on
                        }
                    )
                )
                binding!!.tvSenderName.text = sender.title
                if (STATUS_OFF == sender.status) {
                    XToastUtils.warning(getString(R.string.sender_disabled_tips))
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
    }

    //初始化APP下拉列表
    private fun initAppSpinner() {
        if (ruleType != "app") return

        //未开启异步获取已安装App信息开关时，规则编辑不显示已安装APP下拉框
        if (!SettingUtils.enableLoadUserAppList && !SettingUtils.enableLoadSystemAppList) return

        val get = GlobalScope.async(Dispatchers.IO) {
            if ((SettingUtils.enableLoadUserAppList && App.UserAppList.isEmpty())
                || (SettingUtils.enableLoadSystemAppList && App.SystemAppList.isEmpty())
            ) {
                App.UserAppList.clear()
                App.SystemAppList.clear()
                val appInfoList = AppUtils.getAppsInfo()
                for (appInfo in appInfoList) {
                    if (appInfo.isSystem) {
                        App.SystemAppList.add(appInfo)
                    } else {
                        App.UserAppList.add(appInfo)
                    }
                }
                App.UserAppList.sortBy { appInfo -> appInfo.name }
                App.SystemAppList.sortBy { appInfo -> appInfo.name }
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            runCatching {
                get.await()
                if (App.UserAppList.isEmpty() && App.SystemAppList.isEmpty()) return@runCatching

                appListSpinnerList.clear()
                if (SettingUtils.enableLoadUserAppList) {
                    for (appInfo in App.UserAppList) {
                        appListSpinnerList.add(AppListAdapterItem(appInfo.name, appInfo.icon, appInfo.packageName))
                    }
                }
                if (SettingUtils.enableLoadSystemAppList) {
                    for (appInfo in App.SystemAppList) {
                        appListSpinnerList.add(AppListAdapterItem(appInfo.name, appInfo.icon, appInfo.packageName))
                    }
                }

                //列表为空也不显示下拉框
                if (appListSpinnerList.isEmpty()) return@runCatching

                appListSpinnerAdapter = AppListSpinnerAdapter(appListSpinnerList)
                    //.setTextColor(ResUtils.getColor(R.color.green))
                    //.setTextSize(12F)
                    .setIsFilterKey(true)
                    .setFilterColor("#EF5362")
                    .setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spApp.setAdapter(appListSpinnerAdapter)
                binding!!.spApp.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
                    try {
                        //val appInfo = appListSpinnerList[position]
                        val appInfo = appListSpinnerAdapter.getItemSource(position) as AppListAdapterItem
                        CommonUtils.insertOrReplaceText2Cursor(binding!!.etValue, appInfo.packageName.toString())
                    } catch (e: Exception) {
                        XToastUtils.error(e.message.toString())
                    }
                }
                binding!!.layoutAppList.visibility = View.VISIBLE
            }.onFailure {
                Log.e("GlobalScope", it.message.toString())
            }
        }
    }

    //初始化表单
    private fun initForm() {
        AppDatabase.getInstance(requireContext())
            .ruleDao()
            .get(ruleId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Rule> {
                override fun onSubscribe(d: Disposable) {}

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onSuccess(rule: Rule) {
                    senderId = rule.senderId

                    if (isClone) {
                        titleBar?.setSubTitle(getString(R.string.clone_rule))
                        binding!!.btnDel.setText(R.string.discard)
                    } else {
                        titleBar?.setSubTitle(getString(R.string.edit_rule))
                    }
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
                    binding!!.sbSmsTemplate.isChecked = !TextUtils.isEmpty(rule.smsTemplate.trim())
                    binding!!.etSmsTemplate.setText(rule.smsTemplate.trim())
                    binding!!.sbRegexReplace.isChecked = !TextUtils.isEmpty(rule.regexReplace.trim())
                    binding!!.etRegexReplace.setText(rule.regexReplace.trim())
                    binding!!.sbStatus.isChecked = rule.statusChecked

                    //初始化发送通道下拉框
                    initSenderSpinner()
                }
            })
    }

    //提交前检查表单
    private fun checkForm(): Rule {
        if (senderId <= 0L) {
            throw Exception(getString(R.string.new_sender_first))
        }
        val filed = when (binding!!.rgFiled.checkedRadioButtonId) {
            R.id.rb_content -> FILED_MSG_CONTENT
            R.id.rb_phone -> FILED_PHONE_NUM
            R.id.rb_package_name -> FILED_PACKAGE_NAME
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
        val value = binding!!.etValue.text.toString().trim()
        if (FILED_TRANSPOND_ALL != filed && TextUtils.isEmpty(value)) {
            throw Exception(getString(R.string.invalid_match_value))
        }
        if (FILED_MULTI_MATCH == filed) {
            val lineError = checkMultiMatch(value)
            if (lineError > 0) {
                throw Exception(String.format(getString(R.string.invalid_multi_match), lineError))
            }
        }

        val smsTemplate = binding!!.etSmsTemplate.text.toString().trim()
        val regexReplace = binding!!.etRegexReplace.text.toString().trim()
        val lineNum = checkRegexReplace(regexReplace)
        if (lineNum > 0) {
            throw Exception(String.format(getString(R.string.invalid_regex_replace), lineNum))
        }

        val simSlot = when (binding!!.rgSimSlot.checkedRadioButtonId) {
            R.id.rb_sim_slot_1 -> CHECK_SIM_SLOT_1
            R.id.rb_sim_slot_2 -> CHECK_SIM_SLOT_2
            else -> CHECK_SIM_SLOT_ALL
        }
        val status = if (binding!!.sbStatus.isChecked) STATUS_ON else STATUS_OFF
        //if (status == STATUS_OFF) {
        //    throw Exception(getString(R.string.invalid_rule_status))
        //}

        return Rule(ruleId, ruleType, filed, check, value, senderId, smsTemplate, regexReplace, simSlot, status)
    }

    //检查多重匹配规则是否正确
    private fun checkMultiMatch(ruleStr: String?): Int {
        if (TextUtils.isEmpty(ruleStr)) return 0

        Log.d(TAG, getString(R.string.regex_multi_match))
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
        val tvContent = dialogTest.findViewById<TextView>(R.id.tv_content)
        val etContent = dialogTest.findViewById<EditText>(R.id.et_content)

        if ("app" == ruleType) {
            tvSimSlot.visibility = View.GONE
            rgSimSlot.visibility = View.GONE
            tvFrom.setText(R.string.test_package_name)
            tvContent.setText(R.string.test_inform_content)
        } else if ("call" == ruleType) {
            tvContent.visibility = View.GONE
            etContent.visibility = View.GONE
        }

        MaterialDialog.Builder(requireContext())
            .iconRes(android.R.drawable.ic_dialog_email)
            .title(R.string.rule_tester)
            .customView(dialogTest, true)
            .cancelable(false)
            .autoDismiss(false)
            .neutralText(R.string.action_back)
            .neutralColor(ResUtils.getColors(R.color.darkGrey))
            .onNeutral { dialog: MaterialDialog?, _: DialogAction? ->
                dialog?.dismiss()
            }
            .positiveText(R.string.action_test)
            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                try {
                    val simSlot = when (rgSimSlot.checkedRadioButtonId) {
                        R.id.rb_sim_slot_1 -> 0
                        R.id.rb_sim_slot_2 -> 1
                        else -> -1
                    }

                    val testSim = "SIM" + (simSlot + 1)
                    val ruleSim: String = rule.simSlot
                    if (ruleSim != "ALL" && ruleSim != testSim) {
                        throw java.lang.Exception(getString(R.string.card_slot_does_not_match))
                    }

                    //获取卡槽信息
                    val simInfo = when (simSlot) {
                        0 -> "SIM1_" + SettingUtils.extraSim1
                        1 -> "SIM2_" + SettingUtils.extraSim2
                        else -> ""
                    }

                    val msgInfo = MsgInfo(ruleType, etFrom.text.toString(), etContent.text.toString(), Date(), simInfo, simSlot)
                    if (!rule.checkMsg(msgInfo)) {
                        throw java.lang.Exception(getString(R.string.unmatched_rule))
                    }

                    AppDatabase.getInstance(requireContext())
                        .senderDao()
                        .get(senderId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Sender> {
                            override fun onSubscribe(d: Disposable) {}

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                            }

                            override fun onSuccess(sender: Sender) {
                                Thread {
                                    try {
                                        SendUtils.sendMsgSender(msgInfo, rule, sender, 0L)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        if (Looper.myLooper() == null) Looper.prepare()
                                        XToastUtils.error(e.message.toString())
                                        Looper.loop()
                                    }
                                }.start()
                            }
                        })

                } catch (e: Exception) {
                    XToastUtils.error(e.message.toString())
                }
            }.show()
    }
}