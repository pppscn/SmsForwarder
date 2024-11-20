package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.SenderRecyclerAdapter
import com.idormy.sms.forwarder.adapter.base.ItemMoveCallback
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerAdapter
import com.idormy.sms.forwarder.adapter.spinner.SenderSpinnerItem
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Rule
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.databinding.FragmentTasksActionNotificationBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.utils.CHECK_IS
import com.idormy.sms.forwarder.utils.CHECK_SIM_SLOT_ALL
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.DataProvider
import com.idormy.sms.forwarder.utils.FILED_TRANSPOND_ALL
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_ALL
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_UNTIL_FAIL
import com.idormy.sms.forwarder.utils.SENDER_LOGIC_UNTIL_SUCCESS
import com.idormy.sms.forwarder.utils.STATUS_OFF
import com.idormy.sms.forwarder.utils.STATUS_ON
import com.idormy.sms.forwarder.utils.TASK_ACTION_NOTIFICATION
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.ActionWorker
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder
import com.xuexiang.xui.widget.picker.widget.listener.OnOptionsSelectListener
import com.xuexiang.xutil.resource.ResUtils.getDrawable
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Date

@Page(name = "Notification")
@Suppress("PrivatePropertyName", "DEPRECATION")
class NotificationFragment : BaseFragment<FragmentTasksActionNotificationBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = NotificationFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

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
    private lateinit var sendersRecyclerView: RecyclerView
    private lateinit var senderRecyclerAdapter: SenderRecyclerAdapter

    private var ruleId: Long = 0
    private var ruleType: String = "app"

    private var description: String = ""

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
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 2)
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

            checkSenderLogicShow()
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

        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glSmsTemplate, binding!!.etSmsTemplate, ruleType)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

    override fun initListeners() {
        binding!!.btnSilentPeriod.setOnClickListener(this)
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
                    mCountDownHelper?.start()
                    try {
                        val settingVo = checkSetting()
                        Log.d(TAG, settingVo.toString())
                        val taskAction = TaskSetting(TASK_ACTION_NOTIFICATION, getString(R.string.task_notification), description, Gson().toJson(settingVo), requestCode)
                        val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                        val msgInfo = MsgInfo("task", getString(R.string.task_notification), description, Date(), getString(R.string.task_notification))
                        val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, 0).putString(TaskWorker.TASK_ACTIONS, taskActionsJson).putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo)).build()
                        val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                        WorkManager.getInstance().enqueue(actionRequest)
                    } catch (e: Exception) {
                        mCountDownHelper?.finish()
                        e.printStackTrace()
                        Log.e(TAG, "onClick error: ${e.message}")
                        XToastUtils.error(e.message.toString(), 30000)
                    }
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
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
            Log.e(TAG, "onClick error: ${e.message}")
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
        sendersRecyclerView = binding!!.recyclerSenders
        senderRecyclerAdapter = SenderRecyclerAdapter(senderListSelected, { position ->
            senderListSelected.removeAt(position)
            senderRecyclerAdapter.notifyItemRemoved(position)
            senderRecyclerAdapter.notifyItemRangeChanged(position, senderListSelected.size) // 更新索引
            checkSenderLogicShow()
        })
        sendersRecyclerView.apply {
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
        senderTouchHelper.attachToRecyclerView(sendersRecyclerView)
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

    private fun checkSenderLogicShow() {
        if (senderListSelected.size > 1) {
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

        val settingVo = Rule(
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

        description = getString(R.string.task_notification) + ": "
        description += settingVo.senderList.joinToString(",") { it.name }
        if (settingVo.senderList.size > 1) {
            description += "; " + getString(R.string.sender_logic) + ": " + when (settingVo.senderLogic) {
                SENDER_LOGIC_UNTIL_FAIL -> getString(R.string.sender_logic_until_fail)
                SENDER_LOGIC_UNTIL_SUCCESS -> getString(R.string.sender_logic_until_success)
                else -> getString(R.string.sender_logic_all)
            }
        }

        return settingVo
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
