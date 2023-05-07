package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.XUtil
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.util.*

@Page(name = "转发规则·编辑器")
@Suppress("PrivatePropertyName")
class RulesEditFragment : BaseFragment<FragmentRulesEditBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = RulesEditFragment::class.java.simpleName
    var titleBar: TitleBar? = null
    private val viewModel by viewModels<RuleViewModel> { BaseViewModelFactory(context) }

    //当前发送通道
    var senderId = 0L
    var senderListSelected: MutableList<Sender> = mutableListOf()
    private var senderItemMap = HashMap<Long, LinearLayout>(2)

    //发送通道列表
    var senderListAll: MutableList<Sender> = mutableListOf()
    private val senderSpinnerList = ArrayList<SenderAdapterItem>()
    private lateinit var senderSpinnerAdapter: SenderSpinnerAdapter<*>

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
                binding!!.rbContent.visibility = View.GONE
                binding!!.tvMuRuleTips.setText(R.string.mu_rule_app_tips)
                binding!!.btInsertExtra.visibility = View.GONE
                binding!!.btInsertSender.visibility = View.GONE
                binding!!.btInsertContent.visibility = View.GONE
                //初始化APP下拉列表
                initAppSpinner()
                //监听已安装App信息列表加载完成事件
                LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).observeStickyForever(appListObserver)
            }
            "call" -> {
                titleBar?.setTitle(R.string.call_rule)
                binding!!.rbContent.visibility = View.GONE
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
                binding!!.rbMultiMatch.visibility = View.GONE
                binding!!.btInsertContent.visibility = View.GONE
                binding!!.btInsertSenderApp.visibility = View.GONE
                binding!!.btInsertTitleApp.visibility = View.GONE
                binding!!.btInsertContentApp.visibility = View.GONE
            }
            else -> {
                titleBar?.setTitle(R.string.sms_rule)
                binding!!.rbPackageName.visibility = View.GONE
                binding!!.rbInformContent.visibility = View.GONE
                binding!!.btInsertSenderApp.visibility = View.GONE
                binding!!.btInsertTitleApp.visibility = View.GONE
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

    }

    override fun initListeners() {
        binding!!.btInsertSender.setOnClickListener(this)
        binding!!.btInsertContent.setOnClickListener(this)
        binding!!.btInsertSenderApp.setOnClickListener(this)
        binding!!.btInsertTitleApp.setOnClickListener(this)
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
                R.id.bt_insert_title_app -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_title))
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
        }
    }

    //初始化发送通道下拉框
    private fun initSenderSpinner() {
        AppDatabase.getInstance(requireContext()).senderDao().getAll().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<List<Sender>> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }

            override fun onSuccess(senderList: List<Sender>) {
                if (senderList.isEmpty()) {
                    XToastUtils.error(R.string.add_sender_first)
                    return
                }

                senderListAll = senderList as MutableList<Sender>
                for (sender in senderList) {
                    val name = if (sender.name.length > 20) sender.name.substring(0, 19) else sender.name
                    senderSpinnerList.add(SenderAdapterItem(name, sender.imageId, sender.id, sender.status))
                }
                senderSpinnerAdapter = SenderSpinnerAdapter(senderSpinnerList)
                    //.setTextColor(ResUtils.getColor(R.color.green))
                    //.setTextSize(12F)
                    .setIsFilterKey(true).setFilterColor("#EF5362").setBackgroundSelector(R.drawable.selector_custom_spinner_bg)
                binding!!.spSender.setAdapter(senderSpinnerAdapter)

                if (senderListSelected.isNotEmpty()) {
                    for (sender in senderListSelected) {
                        for (senderItem in senderSpinnerList) {
                            if (sender.id == senderItem.id) {
                                addSenderItemLinearLayout(senderItemMap, binding!!.layoutSenders, senderItem)
                            }
                        }
                    }
                }
            }
        })
        binding!!.spSender.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            try {
                val sender = senderSpinnerAdapter.getItemSource(position) as SenderAdapterItem
                senderId = sender.id!!
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
                            addSenderItemLinearLayout(senderItemMap, binding!!.layoutSenders, sender)
                        }
                    }

                    if (STATUS_OFF == sender.status) {
                        XToastUtils.warning(getString(R.string.sender_disabled_tips))
                    }
                }
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
    }

    /**
     * 动态增删header
     *
     * @param senderItemMap          管理item的map，用于删除指定header
     * @param layoutSenders          需要挂载item的LinearLayout
     * @param sender                 SenderAdapterItem
     */
    @SuppressLint("SetTextI18n")
    private fun addSenderItemLinearLayout(
        senderItemMap: MutableMap<Long, LinearLayout>, layoutSenders: LinearLayout, sender: SenderAdapterItem
    ) {
        val layoutSenderItem = View.inflate(requireContext(), R.layout.item_add_sender, null) as LinearLayout
        val ivRemoveSender = layoutSenderItem.findViewById<ImageView>(R.id.iv_remove_sender)
        val ivSenderImage = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_image)
        val ivSenderStatus = layoutSenderItem.findViewById<ImageView>(R.id.iv_sender_status)
        val tvSenderName = layoutSenderItem.findViewById<TextView>(R.id.tv_sender_name)

        ivSenderImage.setImageDrawable(sender.icon)
        ivSenderStatus.setImageDrawable(ResUtils.getDrawable(if (STATUS_OFF == sender.status) R.drawable.icon_off else R.drawable.icon_on))
        val senderItemId = sender.id as Long
        tvSenderName.text = "ID-$senderItemId：${sender.title}"

        ivRemoveSender.tag = senderItemId
        ivRemoveSender.setOnClickListener { view2: View ->
            val tagId = view2.tag as Long
            layoutSenders.removeView(senderItemMap[tagId])
            senderItemMap.remove(tagId)
            //senderListSelected.removeIf { it.id == tagId }
            for (it in senderListSelected) {
                if (it.id == tagId) {
                    senderListSelected -= it
                    break
                }
            }
            Log.d(TAG, senderListSelected.count().toString())
            Log.d(TAG, senderListSelected.toString())
            if (senderListSelected.isEmpty()) senderId = 0L
            if (senderListSelected.count() > 1) {
                binding!!.layoutSenderLogic.visibility = View.VISIBLE
            } else {
                binding!!.layoutSenderLogic.visibility = View.GONE
                binding!!.rgSenderLogic.check(R.id.rb_sender_logic_all)
            }
        }
        layoutSenders.addView(layoutSenderItem)
        senderItemMap[senderItemId] = layoutSenderItem
        if (senderListSelected.count() > 1) {
            binding!!.layoutSenderLogic.visibility = View.VISIBLE
        } else {
            binding!!.layoutSenderLogic.visibility = View.GONE
            binding!!.rgSenderLogic.check(R.id.rb_sender_logic_all)
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

    //初始化表单
    private fun initForm() {
        AppDatabase.getInstance(requireContext()).ruleDao().get(ruleId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<Rule> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }

            override fun onSuccess(rule: Rule) {
                Log.d(TAG, rule.senderList.toString())
                rule.senderList.forEach {
                    senderId = it.id
                    senderListSelected.add(it)
                }

                if (isClone) {
                    titleBar?.setSubTitle(getString(R.string.clone_rule))
                    binding!!.btnDel.setText(R.string.discard)
                } else {
                    titleBar?.setSubTitle(getString(R.string.edit_rule))
                }
                Log.d(TAG, rule.toString())

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
        if (senderListSelected.isEmpty() || senderId == 0L) {
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
        //if (status == STATUS_OFF) {
        //    throw Exception(getString(R.string.invalid_rule_status))
        //}

        return Rule(ruleId, ruleType, filed, check, value, senderId, smsTemplate, regexReplace, simSlot, status, Date(), senderListSelected, senderLogic)
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
        val tvTitle = dialogTest.findViewById<TextView>(R.id.tv_title)
        val etTitle = dialogTest.findViewById<EditText>(R.id.et_title)
        val tvContent = dialogTest.findViewById<TextView>(R.id.tv_content)
        val etContent = dialogTest.findViewById<EditText>(R.id.et_content)

        if ("app" == ruleType) {
            tvSimSlot.visibility = View.GONE
            rgSimSlot.visibility = View.GONE
            tvTitle.visibility = View.VISIBLE
            etTitle.visibility = View.VISIBLE
            tvFrom.setText(R.string.test_package_name)
            tvContent.setText(R.string.test_inform_content)
        } else if ("call" == ruleType) {
            tvContent.visibility = View.GONE
            etContent.visibility = View.GONE
        }

        MaterialDialog.Builder(requireContext()).iconRes(android.R.drawable.ic_dialog_email).title(R.string.rule_tester).customView(dialogTest, true).cancelable(false).autoDismiss(false).neutralText(R.string.action_back).neutralColor(ResUtils.getColors(R.color.darkGrey)).onNeutral { dialog: MaterialDialog?, _: DialogAction? ->
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
                    throw java.lang.Exception(getString(R.string.card_slot_does_not_match))
                }

                //获取卡槽信息
                val simInfo = when (simSlot) {
                    0 -> "SIM1_" + SettingUtils.extraSim1
                    1 -> "SIM2_" + SettingUtils.extraSim2
                    else -> etTitle.text.toString()
                }

                val msgInfo = MsgInfo(ruleType, etFrom.text.toString(), etContent.text.toString(), Date(), simInfo, simSlot)
                if (!rule.checkMsg(msgInfo)) {
                    throw java.lang.Exception(getString(R.string.unmatched_rule))
                }

                Thread {
                    try {
                        SendUtils.sendMsgSender(msgInfo, rule)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (Looper.myLooper() == null) Looper.prepare()
                        XToastUtils.error(e.message.toString())
                        Looper.loop()
                    }
                }.start()

            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }.show()
    }
}