package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.spinner.SenderAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.databinding.FragmentTasksActionNotificationBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.*
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder
import com.xuexiang.xui.widget.picker.widget.listener.OnOptionsSelectListener
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.util.*

@Page(name = "Notification")
@Suppress("PrivatePropertyName", "DEPRECATION")
class NotificationFragment : BaseFragment<FragmentTasksActionNotificationBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = NotificationFragment::class.java.simpleName
    var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    //免打扰(禁用转发)时间段
    private val mTimeOption = DataProvider.timePeriodOption
    private var silentPeriodStart = 0
    private var silentPeriodEnd = 0

    //当前发送通道
    var senderId = 0L
    var senderListSelected: MutableList<Sender> = mutableListOf()
    private var senderItemMap = HashMap<Long, LinearLayout>(2)

    //发送通道列表
    var senderListAll: MutableList<Sender> = mutableListOf()
    private val senderSpinnerList = ArrayList<SenderAdapterItem>()
    private lateinit var senderSpinnerAdapter: SenderSpinnerAdapter<*>

    private var ruleId: Long = 0
    private var ruleType: String = "app"

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionNotificationBinding {
        return FragmentTasksActionNotificationBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setTitle(R.string.task_notification)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 3)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, Rule::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")

            Log.d(TAG, settingVo.senderList.toString())
            settingVo.senderList.forEach {
                senderId = it.id
                senderListSelected.add(it)
            }

            binding!!.rgSenderLogic.check(settingVo.getSenderLogicCheckId())
            if (!TextUtils.isEmpty(settingVo.smsTemplate.trim())) {
                binding!!.sbSmsTemplate.isChecked = true
                binding!!.layoutSmsTemplate.visibility = View.VISIBLE
                binding!!.etSmsTemplate.setText(settingVo.smsTemplate.trim())
            }
            if (!TextUtils.isEmpty(settingVo.regexReplace.trim())) {
                binding!!.sbRegexReplace.isChecked = true
                binding!!.layoutRegexReplace.visibility = View.VISIBLE
                binding!!.etRegexReplace.setText(settingVo.regexReplace.trim())
            }
            silentPeriodStart = settingVo.silentPeriodStart
            silentPeriodEnd = settingVo.silentPeriodEnd
        }

        //初始化发送通道下拉框
        initSenderSpinner()
    }

    override fun initListeners() {
        binding!!.btnSilentPeriod.setOnClickListener(this)
        binding!!.btInsertSender.setOnClickListener(this)
        binding!!.btInsertContent.setOnClickListener(this)
        binding!!.btInsertSenderApp.setOnClickListener(this)
        binding!!.btInsertUid.setOnClickListener(this)
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
            val etSmsTemplate: EditText = binding!!.etSmsTemplate
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

                R.id.bt_insert_uid -> {
                    CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_uid))
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
                    val settingVo = checkSetting()
                    val from = "测试号码"
                    val content = "测试内容"
                    val simInfo = "SIM1_123456789"
                    val msgInfo = MsgInfo(ruleType, from, content, Date(), simInfo)
                    if (!settingVo.checkMsg(msgInfo)) {
                        throw Exception(getString(R.string.unmatched_rule))
                    }

                    Thread {
                        try {
                            SendUtils.sendMsgSender(msgInfo, settingVo)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            LiveEventBus.get(EVENT_TOAST_ERROR, String::class.java).post(e.message.toString())
                        }
                    }.start()
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    var description = getString(R.string.select_sender) + ": "
                    description += settingVo.senderList.joinToString(",") { it.name }
                    if (settingVo.senderList.size > 1) {
                        description += "; " + getString(R.string.sender_logic) + ": " + when (settingVo.senderLogic) {
                            SENDER_LOGIC_UNTIL_FAIL -> getString(R.string.sender_logic_until_fail)
                            SENDER_LOGIC_UNTIL_SUCCESS -> getString(R.string.sender_logic_until_success)
                            else -> getString(R.string.sender_logic_all)
                        }
                    }
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_ACTION, description)
                    intent.putExtra(KEY_BACK_DATA_ACTION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_ACTION_NOTIFICATION, intent)
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
    @SuppressLint("SetTextI18n")
    private fun initSenderSpinner() {
        //免打扰(禁用转发)时间段
        binding!!.tvSilentPeriod.text = mTimeOption[silentPeriodStart] + " ~ " + mTimeOption[silentPeriodEnd]

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
     * 动态增删Sender
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

    //提交前检查表单
    private fun checkSetting(): Rule {
        if (senderListSelected.isEmpty() || senderId == 0L) {
            throw Exception(getString(R.string.new_sender_first))
        }

        val filed = FILED_TRANSPOND_ALL
        val check = CHECK_IS
        val value = ""
        val smsTemplate = binding!!.etSmsTemplate.text.toString().trim()
        val regexReplace = binding!!.etRegexReplace.text.toString().trim()
        val lineNum = checkRegexReplace(regexReplace)
        if (lineNum > 0) {
            throw Exception(String.format(getString(R.string.invalid_regex_replace), lineNum))
        }
        val simSlot = CHECK_SIM_SLOT_ALL
        val status = STATUS_ON

        val senderLogic = when (binding!!.rgSenderLogic.checkedRadioButtonId) {
            R.id.rb_sender_logic_until_fail -> SENDER_LOGIC_UNTIL_FAIL
            R.id.rb_sender_logic_until_success -> SENDER_LOGIC_UNTIL_SUCCESS
            else -> SENDER_LOGIC_ALL
        }

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
            silentPeriodEnd
        )
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

}