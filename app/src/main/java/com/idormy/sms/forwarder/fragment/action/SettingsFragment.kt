package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Criteria
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.spinner.AppListAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.AppListSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksActionSettingsBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.TaskSetting
import com.idormy.sms.forwarder.entity.action.SettingsSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_ACTION_SETTINGS
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.workers.ActionWorker
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.XUtil
import java.util.Date

@Page(name = "Settings")
@Suppress("PrivatePropertyName", "DEPRECATION")
class SettingsFragment : BaseFragment<FragmentTasksActionSettingsBinding?>(), View.OnClickListener {

    private val TAG: String = SettingsFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    //已安装App信息列表
    private val appListSpinnerList = ArrayList<AppListAdapterItem>()
    private lateinit var appListSpinnerAdapter: AppListSpinnerAdapter<*>
    private val appListObserver = Observer { it: String ->
        Log.d(TAG, "EVENT_LOAD_APP_LIST: $it")
        initAppSpinner()
    }

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionSettingsBinding {
        return FragmentTasksActionSettingsBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_settings)
        return titleBar
    }

    /**
     * 初始化控件
     */
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

        Log.d(TAG, "initViews eventData:$eventData")
        var settingVo = SettingsSetting(getString(R.string.task_settings_tips))
        if (eventData != null) {
            settingVo = Gson().fromJson(eventData, SettingsSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
        }
        binding!!.sbEnableSms.isChecked = settingVo.enableSms
        binding!!.sbEnablePhone.isChecked = settingVo.enablePhone
        binding!!.scbCallType1.isChecked = settingVo.enableCallType1
        binding!!.scbCallType2.isChecked = settingVo.enableCallType2
        binding!!.scbCallType3.isChecked = settingVo.enableCallType3
        binding!!.scbCallType4.isChecked = settingVo.enableCallType4
        binding!!.scbCallType5.isChecked = settingVo.enableCallType5
        binding!!.scbCallType6.isChecked = settingVo.enableCallType6
        binding!!.sbEnableAppNotify.isChecked = settingVo.enableAppNotify
        binding!!.scbCancelAppNotify.isChecked = settingVo.enableCancelAppNotify
        binding!!.scbNotUserPresent.isChecked = settingVo.enableNotUserPresent
        binding!!.sbEnableLocation.isChecked = settingVo.enableLocation
        binding!!.rgAccuracy.check(
            when (settingVo.locationAccuracy) {
                Criteria.ACCURACY_FINE -> R.id.rb_accuracy_fine
                Criteria.ACCURACY_COARSE -> R.id.rb_accuracy_coarse
                Criteria.NO_REQUIREMENT -> R.id.rb_accuracy_no_requirement
                else -> R.id.rb_accuracy_fine
            }
        )
        binding!!.rgPowerRequirement.check(
            when (settingVo.locationPowerRequirement) {
                Criteria.POWER_HIGH -> R.id.rb_power_requirement_high
                Criteria.POWER_MEDIUM -> R.id.rb_power_requirement_medium
                Criteria.POWER_LOW -> R.id.rb_power_requirement_low
                Criteria.NO_REQUIREMENT -> R.id.rb_power_requirement_no_requirement
                else -> R.id.rb_power_requirement_low
            }
        )
        binding!!.etMinInterval.setText((settingVo.locationMinInterval / 1000).toString())
        binding!!.etMinDistance.setText(settingVo.locationMinDistance.toString())
        binding!!.sbEnableSmsCommand.isChecked = settingVo.enableSmsCommand
        binding!!.etSafePhone.setText(settingVo.smsCommandSafePhone)
        binding!!.sbEnableLoadAppList.isChecked = settingVo.enableLoadAppList
        binding!!.scbLoadUserApp.isChecked = settingVo.enableLoadUserAppList
        binding!!.scbLoadSystemApp.isChecked = settingVo.enableLoadSystemAppList
        binding!!.etAppList.setText(settingVo.cancelExtraAppNotify)
        binding!!.xsbDuplicateMessagesLimits.setDefaultValue(settingVo.duplicateMessagesLimits)
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

        //监听已安装App信息列表加载完成事件
        LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).observeStickyForever(appListObserver)

        binding!!.sbEnableSms.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                XXPermissions.with(this)
                    // 接收 WAP 推送消息
                    .permission(Permission.RECEIVE_WAP_PUSH)
                    // 接收彩信
                    .permission(Permission.RECEIVE_MMS)
                    // 接收短信
                    .permission(Permission.RECEIVE_SMS)
                    // 发送短信
                    //.permission(Permission.SEND_SMS)
                    // 读取短信
                    .permission(Permission.READ_SMS)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                XToastUtils.info(R.string.toast_granted_all)
                            } else {
                                XToastUtils.info(R.string.toast_granted_part)
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.info(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.info(R.string.toast_denied)
                            }
                            binding!!.sbEnableSms.isChecked = false
                        }
                    })
            }
        }

        binding!!.sbEnablePhone.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                XXPermissions.with(this)
                    // 读取电话状态
                    .permission(Permission.READ_PHONE_STATE)
                    // 读取手机号码
                    .permission(Permission.READ_PHONE_NUMBERS)
                    // 读取通话记录
                    .permission(Permission.READ_CALL_LOG)
                    // 读取联系人
                    .permission(Permission.READ_CONTACTS)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                XToastUtils.info(R.string.toast_granted_all)
                            } else {
                                XToastUtils.info(R.string.toast_granted_part)
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.info(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.info(R.string.toast_denied)
                            }
                            binding!!.sbEnablePhone.isChecked = false
                        }
                    })
            }
        }

        binding!!.sbEnableAppNotify.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                XXPermissions.with(this)
                    .permission(Permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                    .request(OnPermissionCallback { _, allGranted ->
                        if (!allGranted) {
                            binding!!.sbEnableAppNotify.isChecked = false
                            XToastUtils.error(R.string.tips_notification_listener)
                            return@OnPermissionCallback
                        }

                        binding!!.sbEnableAppNotify.isChecked = true
                        CommonUtils.toggleNotificationListenerService(requireContext())
                    })
            }
        }

        binding!!.sbEnableLocation.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                XXPermissions.with(this)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .permission(Permission.ACCESS_BACKGROUND_LOCATION)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.error(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(R.string.toast_denied)
                            }
                            binding!!.sbEnableLocation.isChecked = false
                        }
                    })
            }
        }
        //设置位置更新最小时间间隔（单位：毫秒）； 默认间隔：10000毫秒，最小间隔：1000毫秒
        binding!!.etMinInterval.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val inputText = binding!!.etMinInterval.text.toString()
                if (inputText.isEmpty() || inputText == "0") {
                    binding!!.etMinInterval.setText("1")
                    binding!!.etMinInterval.setSelection(binding!!.etMinInterval.text.length) // 将光标移至文本末尾
                }
            }
        }
        //设置位置更新最小距离（单位：米）；默认距离：0米
        binding!!.etMinDistance.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val inputText = binding!!.etMinDistance.text.toString()
                if (inputText.isEmpty()) {
                    binding!!.etMinDistance.setText("0")
                    binding!!.etMinDistance.setSelection(binding!!.etMinDistance.text.length) // 将光标移至文本末尾
                }
            }
        }

        binding!!.sbEnableSmsCommand.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                XXPermissions.with(this)
                    // 系统设置
                    .permission(Permission.WRITE_SETTINGS)
                    // 接收短信
                    .permission(Permission.RECEIVE_SMS)
                    // 发送短信
                    .permission(Permission.SEND_SMS)
                    // 读取短信
                    .permission(Permission.READ_SMS)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                XToastUtils.info(R.string.toast_granted_all)
                            } else {
                                XToastUtils.info(R.string.toast_granted_part)
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.info(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.info(R.string.toast_denied)
                            }
                            binding!!.sbEnableSmsCommand.isChecked = false
                        }
                    })
            }
        }

    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    try {
                        val settingVo = checkSetting()
                        Log.d(TAG, settingVo.toString())
                        val taskAction = TaskSetting(TASK_ACTION_SETTINGS, getString(R.string.task_settings), settingVo.description, Gson().toJson(settingVo), requestCode)
                        val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                        val msgInfo = MsgInfo("task", getString(R.string.task_settings), settingVo.description, Date(), getString(R.string.task_settings))
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
                    intent.putExtra(KEY_BACK_DESCRIPTION_ACTION, settingVo.description)
                    intent.putExtra(KEY_BACK_DATA_ACTION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_ACTION_SETTINGS, intent)
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

    //初始化APP下拉列表
    private fun initAppSpinner() {

        //未开启异步获取已安装App信息开关时，不显示已安装APP下拉框
        if (!SettingUtils.enableLoadAppList) return

        if (App.UserAppList.isEmpty() && App.SystemAppList.isEmpty()) {
            //XToastUtils.info(getString(R.string.loading_app_list))
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
                CommonUtils.insertOrReplaceText2Cursor(binding!!.etAppList, appInfo.packageName.toString() + "\n")
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
        binding!!.layoutSpApp.visibility = View.VISIBLE

    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): SettingsSetting {
        val enableList = mutableListOf<String>()
        val disableList = mutableListOf<String>()

        val enableSms = binding!!.sbEnableSms.isChecked
        if (enableSms) enableList.add(getString(R.string.forward_sms)) else disableList.add(getString(R.string.forward_sms))

        val enablePhone = binding!!.sbEnablePhone.isChecked
        if (enablePhone) enableList.add(getString(R.string.forward_calls)) else disableList.add(getString(R.string.forward_calls))
        val enableCallType1 = binding!!.scbCallType1.isChecked
        val enableCallType2 = binding!!.scbCallType2.isChecked
        val enableCallType3 = binding!!.scbCallType3.isChecked
        val enableCallType4 = binding!!.scbCallType4.isChecked
        val enableCallType5 = binding!!.scbCallType5.isChecked
        val enableCallType6 = binding!!.scbCallType6.isChecked
        if (enablePhone && !enableCallType1 && !enableCallType2 && !enableCallType3 && !enableCallType4 && !enableCallType5 && !enableCallType6) {
            throw Exception(getString(R.string.enable_phone_fw_tips))
        }

        val enableAppNotify = binding!!.sbEnableAppNotify.isChecked
        if (enableAppNotify) enableList.add(getString(R.string.forward_app_notify)) else disableList.add(getString(R.string.forward_app_notify))
        val enableCancelAppNotify = binding!!.scbCancelAppNotify.isChecked
        val enableNotUserPresent = binding!!.scbNotUserPresent.isChecked

        val enableLocation = binding!!.sbEnableLocation.isChecked
        if (enableLocation) enableList.add(getString(R.string.enable_location)) else disableList.add(getString(R.string.enable_location))
        val locationAccuracy = when (binding!!.rgAccuracy.checkedRadioButtonId) {
            R.id.rb_accuracy_fine -> Criteria.ACCURACY_FINE
            R.id.rb_accuracy_coarse -> Criteria.ACCURACY_COARSE
            R.id.rb_accuracy_no_requirement -> Criteria.NO_REQUIREMENT
            else -> Criteria.ACCURACY_FINE
        }
        val locationPowerRequirement = when (binding!!.rgPowerRequirement.checkedRadioButtonId) {
            R.id.rb_power_requirement_high -> Criteria.POWER_HIGH
            R.id.rb_power_requirement_medium -> Criteria.POWER_MEDIUM
            R.id.rb_power_requirement_low -> Criteria.POWER_LOW
            R.id.rb_power_requirement_no_requirement -> Criteria.NO_REQUIREMENT
            else -> Criteria.POWER_LOW
        }
        val locationMinInterval = (binding!!.etMinInterval.text.toString().toLongOrNull() ?: 1) * 1000
        val locationMinDistance = binding!!.etMinDistance.text.toString().toIntOrNull() ?: 0

        val enableSmsCommand = binding!!.sbEnableSmsCommand.isChecked
        if (enableSmsCommand) enableList.add(getString(R.string.sms_command)) else disableList.add(getString(R.string.sms_command))
        val smsCommandSafePhone = binding!!.etSafePhone.text.toString()

        val enableLoadAppList = binding!!.sbEnableLoadAppList.isChecked
        if (enableLoadAppList) enableList.add(getString(R.string.load_app_list)) else disableList.add(getString(R.string.load_app_list))
        val enableLoadUserAppList = binding!!.scbLoadUserApp.isChecked
        val enableLoadSystemAppList = binding!!.scbLoadSystemApp.isChecked

        val cancelExtraAppNotify = binding!!.etAppList.text.toString()
        if (cancelExtraAppNotify.isNotEmpty()) enableList.add(getString(R.string.extra_app)) else disableList.add(getString(R.string.extra_app))

        val duplicateMessagesLimits = binding!!.xsbDuplicateMessagesLimits.selectedNumber
        if (duplicateMessagesLimits > 0) enableList.add(getString(R.string.filtering_duplicate_messages)) else disableList.add(getString(R.string.filtering_duplicate_messages))

        val description = StringBuilder()
        if (enableList.isNotEmpty()) {
            description.append(" ").append(getString(R.string.enable_function)).append(": ").append(enableList.joinToString(","))
        }
        if (disableList.isNotEmpty()) {
            description.append(" ").append(getString(R.string.disable_function)).append(": ").append(disableList.joinToString(","))
        }

        return SettingsSetting(description.toString().trim(), enableSms, enablePhone, enableCallType1, enableCallType2, enableCallType3, enableCallType4, enableCallType5, enableCallType6, enableAppNotify, enableCancelAppNotify, enableNotUserPresent, enableLocation, locationAccuracy, locationPowerRequirement, locationMinInterval, locationMinDistance, enableSmsCommand, smsCommandSafePhone, enableLoadAppList, enableLoadUserAppList, enableLoadSystemAppList, cancelExtraAppNotify, duplicateMessagesLimits)
    }
}