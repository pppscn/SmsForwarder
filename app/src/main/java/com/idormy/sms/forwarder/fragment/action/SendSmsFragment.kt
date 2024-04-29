package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksActionSendSmsBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.SmsSetting
import com.idormy.sms.forwarder.server.model.ConfigData
import com.idormy.sms.forwarder.utils.EVENT_KEY_PHONE_NUMBERS
import com.idormy.sms.forwarder.utils.EVENT_KEY_SIM_SLOT
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_ACTION_SENDSMS
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.ActionWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import java.util.Date

@Page(name = "SendSms")
@Suppress("PrivatePropertyName", "DEPRECATION")
class SendSmsFragment : BaseFragment<FragmentTasksActionSendSmsBinding?>(), View.OnClickListener {

    private val TAG: String = SendSmsFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    private var description = ""
    private var simSlot = 1
    private var phoneNumbers = ""
    private var msgContent = ""

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionSendSmsBinding {
        return FragmentTasksActionSendSmsBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_sendsms)
        return titleBar
    }

    /**
     * 初始化控件
     */
    @SuppressLint("SetTextI18n")
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 1)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        //卡槽信息
        val serverConfigStr = HttpServerUtils.serverConfig
        if (!TextUtils.isEmpty(serverConfigStr)) {
            val serverConfig: ConfigData = Gson().fromJson(serverConfigStr, object : TypeToken<ConfigData>() {}.type)
            binding!!.rbSimSlot1.text = "SIM1：" + serverConfig.extraSim1
            binding!!.rbSimSlot2.text = "SIM2：" + serverConfig.extraSim2
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, SmsSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")

            simSlot = settingVo.simSlot
            phoneNumbers = settingVo.phoneNumbers
            msgContent = settingVo.msgContent
        }

        binding!!.rgSimSlot.check(if (simSlot == 1) R.id.rb_sim_slot_1 else R.id.rb_sim_slot_2)
        binding!!.etPhoneNumbers.setText(phoneNumbers)
        binding!!.etMsgContent.setText(msgContent)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        LiveEventBus.get(EVENT_KEY_SIM_SLOT, Int::class.java).observeSticky(this) { value: Int ->
            binding!!.rgSimSlot.check(if (value == 1) R.id.rb_sim_slot_2 else R.id.rb_sim_slot_1)
        }
        LiveEventBus.get(EVENT_KEY_PHONE_NUMBERS, String::class.java).observeSticky(this) { value: String ->
            binding!!.etPhoneNumbers.setText(value)
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()

                    //检查发送短信权限是否获取
                    XXPermissions.with(this)
                        .permission(Permission.SEND_SMS)
                        .request(object : OnPermissionCallback {
                            override fun onGranted(permissions: List<String>, all: Boolean) {
                                mCountDownHelper?.start()
                                try {
                                    val settingVo = checkSetting()
                                    Log.d(TAG, settingVo.toString())
                                    val taskAction = TaskSetting(TASK_ACTION_SENDSMS, getString(R.string.task_sendsms), settingVo.description, Gson().toJson(settingVo), requestCode)
                                    val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                                    val msgInfo = MsgInfo("task", getString(R.string.task_sendsms), settingVo.description, Date(), getString(R.string.task_sendsms))
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

                            override fun onDenied(permissions: List<String>, never: Boolean) {
                                XToastUtils.error(getString(R.string.no_sms_sending_permission), 30000)
                            }
                        })
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
                    setFragmentResult(TASK_ACTION_SENDSMS, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
            Log.e(TAG, "onClick error: ${e.message}")
        }
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): SmsSetting {
        phoneNumbers = binding!!.etPhoneNumbers.text.toString().trim()
        if (!getString(R.string.phone_numbers_with_tag_regex).toRegex().matches(phoneNumbers)) {
            throw Exception(getString(R.string.phone_numbers_with_tag_error))
        }

        msgContent = binding!!.etMsgContent.text.toString().trim()
        if (!getString(R.string.msg_content_regex).toRegex().matches(msgContent)) {
            throw Exception(getString(R.string.msg_content_error))
        }

        simSlot = if (binding!!.rgSimSlot.checkedRadioButtonId == R.id.rb_sim_slot_2) 2 else 1

        description = String.format(getString(R.string.send_sms_to), simSlot, phoneNumbers)

        return SmsSetting(description, simSlot, phoneNumbers, msgContent)
    }
}