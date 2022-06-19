package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentSettingsBinding
import com.idormy.sms.forwarder.entity.SimInfo
import com.idormy.sms.forwarder.receiver.BootReceiver
import com.idormy.sms.forwarder.utils.*
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox
import com.xuexiang.xui.widget.button.switchbutton.SwitchButton
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.picker.XRangeSlider
import com.xuexiang.xui.widget.picker.XRangeSlider.OnRangeSliderListener
import com.xuexiang.xui.widget.picker.XSeekBar
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder
import com.xuexiang.xui.widget.picker.widget.builder.TimePickerBuilder
import com.xuexiang.xui.widget.picker.widget.listener.OnOptionsSelectListener
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.XUtil.getPackageManager
import com.xuexiang.xutil.app.AppUtils.getAppPackageName
import com.xuexiang.xutil.data.DateUtils
import java.util.*


@Suppress("PropertyName", "SpellCheckingInspection")
@Page(name = "通用设置")
class SettingsFragment : BaseFragment<FragmentSettingsBinding?>(), View.OnClickListener {

    val TAG: String = SettingsFragment::class.java.simpleName
    private val mTimeOption = DataProvider.timePeriodOption

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    /**
     * @return 返回为 null意为不需要导航栏
     */
    override fun initTitle(): TitleBar? {
        return null
    }

    @SuppressLint("NewApi")
    override fun initViews() {

        //转发短信广播
        switchEnableSms(binding!!.sbEnableSms)
        //转发通话记录
        switchEnablePhone(binding!!.sbEnablePhone, binding!!.scbCallType1, binding!!.scbCallType2, binding!!.scbCallType3)
        //转发应用通知
        switchEnableAppNotify(binding!!.sbEnableAppNotify, binding!!.scbCancelAppNotify, binding!!.scbNotUserPresent)
        //启动时异步获取已安装App信息
        switchEnableLoadAppList(binding!!.sbEnableLoadAppList, binding!!.scbLoadUserApp, binding!!.scbLoadSystemApp)
        //过滤多久内重复消息
        binding!!.xsbDuplicateMessagesLimits.setDefaultValue(SettingUtils.duplicateMessagesLimits)
        binding!!.xsbDuplicateMessagesLimits.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            SettingUtils.duplicateMessagesLimits = newValue
        }
        //免打扰(禁用转发)时间段
        binding!!.tvSilentPeriod.text = mTimeOption[SettingUtils.silentPeriodStart] + " ~ " + mTimeOption[SettingUtils.silentPeriodEnd]

        //监听电池状态变化
        switchBatteryReceiver(binding!!.sbBatteryReceiver)
        //电量预警
        editBatteryLevelAlarm(binding!!.xrsBatteryLevelAlarm, binding!!.scbBatteryLevelAlarmOnce)
        //定时推送电池状态
        switchBatteryCron(binding!!.sbBatteryCron)
        //设置推送电池状态时机
        editBatteryCronTiming(binding!!.etBatteryCronStartTime, binding!!.etBatteryCronInterval)

        //开机启动
        checkWithReboot(binding!!.sbWithReboot, binding!!.tvAutoStartup)
        //忽略电池优化设置
        batterySetting(binding!!.layoutBatterySetting, binding!!.sbBatterySetting)
        //不在最近任务列表中显示
        switchExcludeFromRecents(binding!!.layoutExcludeFromRecents, binding!!.sbExcludeFromRecents)

        //Cactus增强保活措施
        switchEnableCactus(binding!!.sbEnableCactus, binding!!.scbPlaySilenceMusic, binding!!.scbOnePixelActivity)

        //接口请求失败重试时间间隔
        editRetryDelayTime(binding!!.etRetryTimes, binding!!.etDelayTime, binding!!.etTimeout)

        //设备备注
        editAddExtraDeviceMark(binding!!.etExtraDeviceMark)
        //SIM1备注
        editAddExtraSim1(binding!!.etExtraSim1)
        //SIM2备注
        editAddExtraSim2(binding!!.etExtraSim2)
        //通知内容
        editNotifyContent(binding!!.etNotifyContent)

        //启用自定义模版
        switchSmsTemplate(binding!!.sbSmsTemplate)
        //自定义模板
        editSmsTemplate(binding!!.etSmsTemplate)

        //帮助提示
        switchHelpTip(binding!!.sbHelpTip)

        //纯客户端模式
        switchDirectlyToClient(binding!!.sbDirectlyToClient)
    }

    override fun initListeners() {
        binding!!.btnSilentPeriod.setOnClickListener(this)
        binding!!.btnExtraDeviceMark.setOnClickListener(this)
        binding!!.btnExtraSim1.setOnClickListener(this)
        binding!!.btnExtraSim2.setOnClickListener(this)
        binding!!.btInsertSender.setOnClickListener(this)
        binding!!.btInsertContent.setOnClickListener(this)
        binding!!.btInsertExtra.setOnClickListener(this)
        binding!!.btInsertTime.setOnClickListener(this)
        binding!!.btInsertDeviceName.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    @SingleClick
    override fun onClick(v: View) {
        val etSmsTemplate: EditText = binding!!.etSmsTemplate
        when (v.id) {
            R.id.btn_silent_period -> {
                OptionsPickerBuilder(context, OnOptionsSelectListener { _: View?, options1: Int, options2: Int, _: Int ->
                    SettingUtils.silentPeriodStart = options1
                    SettingUtils.silentPeriodEnd = options2
                    val txt = mTimeOption[options1] + " ~ " + mTimeOption[options2]
                    binding!!.tvSilentPeriod.text = txt
                    XToastUtils.toast(txt)
                    return@OnOptionsSelectListener false
                }).setTitleText(getString(R.string.select_time_period))
                    .setSelectOptions(SettingUtils.silentPeriodStart, SettingUtils.silentPeriodEnd)
                    .build<Any>().also {
                        it.setNPicker(mTimeOption, mTimeOption)
                        it.show()
                    }
            }
            R.id.btn_extra_device_mark -> {
                binding!!.etExtraDeviceMark.setText(PhoneUtils.getDeviceName())
                return
            }
            R.id.btn_extra_sim1 -> {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
                if (App.SimInfoList.isEmpty()) {
                    XToastUtils.error(R.string.tip_can_not_get_sim_infos)
                    return
                }
                Log.d(TAG, App.SimInfoList.toString())
                if (!App.SimInfoList.containsKey(0)) {
                    XToastUtils.error(String.format(getString(R.string.tip_can_not_get_sim_info), 1))
                    return
                }
                val simInfo: SimInfo? = App.SimInfoList[0]
                binding!!.etExtraSim1.setText(simInfo?.mCarrierName.toString() + "_" + simInfo?.mNumber.toString())
                return
            }
            R.id.btn_extra_sim2 -> {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
                if (App.SimInfoList.isEmpty()) {
                    XToastUtils.error(R.string.tip_can_not_get_sim_infos)
                    return
                }
                Log.d(TAG, App.SimInfoList.toString())
                if (!App.SimInfoList.containsKey(1)) {
                    XToastUtils.error(String.format(getString(R.string.tip_can_not_get_sim_info), 2))
                    return
                }
                val simInfo: SimInfo? = App.SimInfoList[1]
                binding!!.etExtraSim2.setText(simInfo?.mCarrierName.toString() + "_" + simInfo?.mNumber.toString())
                return
            }
            R.id.bt_insert_sender -> {
                CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_from))
                return
            }
            R.id.bt_insert_content -> {
                CommonUtils.insertOrReplaceText2Cursor(etSmsTemplate, getString(R.string.tag_sms))
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
            else -> {}
        }
    }

    //转发短信
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchEnableSms(sbEnableSms: SwitchButton) {
        sbEnableSms.isChecked = SettingUtils.enableSms
        sbEnableSms.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableSms = isChecked
            if (isChecked) {
                //检查权限是否获取
                XXPermissions.with(this)
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
                            SettingUtils.enableSms = false
                            sbEnableSms.isChecked = false
                        }
                    })
            }
        }
    }

    //转发通话
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchEnablePhone(sbEnablePhone: SwitchButton, scbCallType1: SmoothCheckBox, scbCallType2: SmoothCheckBox, scbCallType3: SmoothCheckBox) {
        sbEnablePhone.isChecked = SettingUtils.enablePhone
        scbCallType1.isChecked = SettingUtils.enableCallType1
        scbCallType2.isChecked = SettingUtils.enableCallType2
        scbCallType3.isChecked = SettingUtils.enableCallType3
        sbEnablePhone.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
                return@setOnCheckedChangeListener
            }
            SettingUtils.enablePhone = isChecked
            if (isChecked) {
                //检查权限是否获取
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
                            SettingUtils.enablePhone = false
                            sbEnablePhone.isChecked = false
                        }
                    })
            }
        }
        scbCallType1.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType1 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType2.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType2 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType3.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType3 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
    }

    //转发应用通知
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchEnableAppNotify(sbEnableAppNotify: SwitchButton, scbCancelAppNotify: SmoothCheckBox, scbNotUserPresent: SmoothCheckBox) {
        val layoutOptionalAction: LinearLayout = binding!!.layoutOptionalAction
        val isEnable: Boolean = SettingUtils.enableAppNotify
        sbEnableAppNotify.isChecked = isEnable
        layoutOptionalAction.visibility = if (isEnable) View.VISIBLE else View.GONE

        sbEnableAppNotify.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            layoutOptionalAction.visibility = if (isChecked) View.VISIBLE else View.GONE
            SettingUtils.enableAppNotify = isChecked
            if (isChecked) {
                //检查权限是否获取
                XXPermissions.with(this)
                    // 通知栏监听权限
                    .permission(Permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                    // 通知栏权限
                    .permission(Permission.NOTIFICATION_SERVICE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            SettingUtils.enableAppNotify = true
                            sbEnableAppNotify.isChecked = true
                            CommonUtils.toggleNotificationListenerService(requireContext())
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            SettingUtils.enableAppNotify = false
                            sbEnableAppNotify.isChecked = false
                            XToastUtils.error(R.string.tips_notification_listener)
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            MaterialDialog.Builder(context!!)
                                .content(R.string.toast_denied_never)
                                .positiveText(R.string.lab_yes)
                                .negativeText(R.string.lab_no)
                                .onPositive { _: MaterialDialog?, _: DialogAction? ->
                                    XXPermissions.startPermissionActivity(requireContext(), permissions)
                                }
                                .show()
                        }
                    })
            }
        }
        scbCancelAppNotify.isChecked = SettingUtils.enableCancelAppNotify
        scbCancelAppNotify.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCancelAppNotify = isChecked
        }
        scbNotUserPresent.isChecked = SettingUtils.enableNotUserPresent
        scbNotUserPresent.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableNotUserPresent = isChecked
        }
    }

    //启动时异步获取已安装App信息 (binding!!.sbEnableLoadAppList, binding!!.scbLoadUserApp, binding!!.scbLoadSystemApp)
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchEnableLoadAppList(sbEnableLoadAppList: SwitchButton, scbLoadUserApp: SmoothCheckBox, scbLoadSystemApp: SmoothCheckBox) {
        val isEnable: Boolean = SettingUtils.enableLoadAppList
        sbEnableLoadAppList.isChecked = isEnable

        sbEnableLoadAppList.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked && !SettingUtils.enableLoadUserAppList && !SettingUtils.enableLoadSystemAppList) {
                sbEnableLoadAppList.isChecked = false
                SettingUtils.enableLoadAppList = false
                XToastUtils.error(getString(R.string.load_app_list_toast))
                return@setOnCheckedChangeListener
            }
            SettingUtils.enableLoadAppList = isChecked
        }
        scbLoadUserApp.isChecked = SettingUtils.enableLoadUserAppList
        scbLoadUserApp.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableLoadUserAppList = isChecked
            if (SettingUtils.enableLoadAppList && !SettingUtils.enableLoadUserAppList && !SettingUtils.enableLoadSystemAppList) {
                sbEnableLoadAppList.isChecked = false
                SettingUtils.enableLoadAppList = false
                XToastUtils.error(getString(R.string.load_app_list_toast))
            }
        }
        scbLoadSystemApp.isChecked = SettingUtils.enableLoadSystemAppList
        scbLoadSystemApp.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableLoadSystemAppList = isChecked
            if (SettingUtils.enableLoadAppList && !SettingUtils.enableLoadUserAppList && !SettingUtils.enableLoadSystemAppList) {
                sbEnableLoadAppList.isChecked = false
                SettingUtils.enableLoadAppList = false
                XToastUtils.error(getString(R.string.load_app_list_toast))
            }
        }
    }

    //监听电池状态变化
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchBatteryReceiver(sbBatteryReceiver: SwitchButton) {
        sbBatteryReceiver.isChecked = SettingUtils.enableBatteryReceiver
        sbBatteryReceiver.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableBatteryReceiver = isChecked
        }
    }

    //设置低电量报警
    private fun editBatteryLevelAlarm(xrsBatteryLevelAlarm: XRangeSlider, scbBatteryLevelAlarmOnce: SmoothCheckBox) {
        xrsBatteryLevelAlarm.setStartingMinMax(SettingUtils.batteryLevelMin, SettingUtils.batteryLevelMax)
        xrsBatteryLevelAlarm.setOnRangeSliderListener(object : OnRangeSliderListener {
            override fun onMaxChanged(slider: XRangeSlider, maxValue: Int) {
                //SettingUtils.batteryLevelMin = slider.selectedMin
                SettingUtils.batteryLevelMax = slider.selectedMax
            }

            override fun onMinChanged(slider: XRangeSlider, minValue: Int) {
                SettingUtils.batteryLevelMin = slider.selectedMin
                //SettingUtils.batteryLevelMax = slider.selectedMax
            }
        })

        scbBatteryLevelAlarmOnce.isChecked = SettingUtils.batteryLevelOnce
        scbBatteryLevelAlarmOnce.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.batteryLevelOnce = isChecked
            if (isChecked && 0 == SettingUtils.batteryLevelMin && 0 == SettingUtils.batteryLevelMax) {
                XToastUtils.warning(R.string.tips_battery_level_alarm_once)
            }
        }
    }

    //定时推送电池状态
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchBatteryCron(sbBatteryCron: SwitchButton) {
        sbBatteryCron.isChecked = SettingUtils.enableBatteryCron
        binding!!.layoutBatteryCron.visibility = if (SettingUtils.enableBatteryCron) View.VISIBLE else View.GONE
        sbBatteryCron.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            binding!!.layoutBatteryCron.visibility = if (isChecked) View.VISIBLE else View.GONE
            SettingUtils.enableBatteryCron = isChecked
            //TODO:BatteryReportCronTask
            //BatteryReportCronTask.getSingleton().updateTimer()
        }
    }

    //设置推送电池状态时机
    private fun editBatteryCronTiming(etBatteryCronStartTime: EditText, etBatteryCronInterval: EditText) {
        etBatteryCronStartTime.setText(SettingUtils.batteryCronStartTime)
        etBatteryCronStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = DateUtils.getNowDate()
            val mTimePicker = TimePickerBuilder(context) { date: Date?, _: View? ->
                etBatteryCronStartTime.setText(DateUtils.date2String(date, DateUtils.HHmm.get()))
                //TODO:BatteryReportCronTask
                //BatteryReportCronTask.getSingleton().updateTimer()
            }
                //.setTimeSelectChangeListener { date: Date? -> etBatteryCronStartTime.setText(DateUtils.date2String(date, DateUtils.HHmm.get())) }
                .setType(false, false, false, true, true, false)
                .setTitleText(getString(R.string.time_picker))
                .setSubmitText(getString(R.string.ok))
                .setCancelText(getString(R.string.cancel))
                .setDate(calendar)
                .build()
            mTimePicker.show()
        }

        etBatteryCronInterval.setText(SettingUtils.batteryCronInterval.toString())
        etBatteryCronInterval.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val interval = etBatteryCronInterval.text.toString().trim()
                if (interval.isNotEmpty() && interval.toInt() > 0) {
                    SettingUtils.batteryCronInterval = interval.toInt()
                    //TODO:BatteryReportCronTask
                    //BatteryReportCronTask.getSingleton().updateTimer()
                } else {
                    SettingUtils.batteryCronInterval = 60
                }
            }
        })
    }

    //开机启动
    private fun checkWithReboot(@SuppressLint("UseSwitchCompatOrMaterialCode") sbWithReboot: SwitchButton, tvAutoStartup: TextView) {
        tvAutoStartup.text = getAutoStartTips()

        //获取组件
        val cm = ComponentName(getAppPackageName(), BootReceiver::class.java.name)
        val pm: PackageManager = getPackageManager()
        val state = pm.getComponentEnabledSetting(cm)
        sbWithReboot.isChecked = (state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
        sbWithReboot.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            try {
                val newState = if (isChecked) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                pm.setComponentEnabledSetting(cm, newState, PackageManager.DONT_KILL_APP)
                if (isChecked) startToAutoStartSetting(requireContext())
            } catch (e: Exception) {
                XToastUtils.error(e.message.toString())
            }
        }
    }

    //电池优化设置
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun batterySetting(layoutBatterySetting: LinearLayout, sbBatterySetting: SwitchButton) {
        //安卓6.0以下没有忽略电池优化
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            layoutBatterySetting.visibility = View.GONE
            return
        }

        try {
            val isIgnoreBatteryOptimization: Boolean = KeepAliveUtils.isIgnoreBatteryOptimization(requireActivity())
            sbBatterySetting.isChecked = isIgnoreBatteryOptimization
            sbBatterySetting.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked && !isIgnoreBatteryOptimization) {
                    KeepAliveUtils.ignoreBatteryOptimization(requireActivity())
                } else if (isChecked) {
                    XToastUtils.info(R.string.isIgnored)
                    sbBatterySetting.isChecked = isIgnoreBatteryOptimization
                } else {
                    XToastUtils.info(R.string.isIgnored2)
                    sbBatterySetting.isChecked = isIgnoreBatteryOptimization
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    //不在最近任务列表中显示
    @SuppressLint("ObsoleteSdkInt,UseSwitchCompatOrMaterialCode")
    fun switchExcludeFromRecents(layoutExcludeFromRecents: LinearLayout, sbExcludeFromRecents: SwitchButton) {
        //安卓6.0以下没有不在最近任务列表中显示
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            layoutExcludeFromRecents.visibility = View.GONE
            return
        }
        sbExcludeFromRecents.isChecked = SettingUtils.enableExcludeFromRecents
        sbExcludeFromRecents.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableExcludeFromRecents = isChecked
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val am = App.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.let {
                    val tasks = it.appTasks
                    if (!tasks.isNullOrEmpty()) {
                        tasks[0].setExcludeFromRecents(true)
                    }
                }
            }
        }
    }

    //转发应用通知
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchEnableCactus(sbEnableCactus: SwitchButton, scbPlaySilenceMusic: SmoothCheckBox, scbOnePixelActivity: SmoothCheckBox) {
        val layoutCactusOptional: LinearLayout = binding!!.layoutCactusOptional
        val isEnable: Boolean = SettingUtils.enableCactus
        sbEnableCactus.isChecked = isEnable
        layoutCactusOptional.visibility = if (isEnable) View.VISIBLE else View.GONE

        sbEnableCactus.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            layoutCactusOptional.visibility = if (isChecked) View.VISIBLE else View.GONE
            SettingUtils.enableCactus = isChecked
            XToastUtils.warning(getString(R.string.need_to_restart))
        }

        scbPlaySilenceMusic.isChecked = SettingUtils.enablePlaySilenceMusic
        scbPlaySilenceMusic.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enablePlaySilenceMusic = isChecked
            XToastUtils.warning(getString(R.string.need_to_restart))
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            binding!!.layoutOnePixelActivity.visibility = View.VISIBLE
        }
        scbOnePixelActivity.isChecked = SettingUtils.enableOnePixelActivity
        scbOnePixelActivity.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableOnePixelActivity = isChecked
            XToastUtils.warning(getString(R.string.need_to_restart))
        }
    }

    //接口请求失败重试时间间隔
    private fun editRetryDelayTime(etRetryTimes: EditText, etDelayTime: EditText, etTimeout: EditText) {
        etRetryTimes.setText(java.lang.String.valueOf(SettingUtils.requestRetryTimes))
        etRetryTimes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val retryTimes = etRetryTimes.text.toString().trim()
                if (retryTimes.isNotEmpty()) {
                    SettingUtils.requestRetryTimes = retryTimes.toInt()
                } else {
                    etRetryTimes.setText("0")
                    SettingUtils.requestRetryTimes = 0
                }
            }
        })
        etDelayTime.setText(java.lang.String.valueOf(SettingUtils.requestDelayTime))
        etDelayTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val delayTime = etDelayTime.text.toString().trim()
                if (delayTime.isNotEmpty()) {
                    SettingUtils.requestDelayTime = delayTime.toInt()
                    if (SettingUtils.requestDelayTime < 1) {
                        etDelayTime.setText("1")
                        XToastUtils.error(R.string.invalid_delay_time)
                    }
                } else {
                    XToastUtils.warning(R.string.invalid_delay_time)
                    etDelayTime.setText("1")
                    SettingUtils.requestDelayTime = 1
                }
            }
        })
        etTimeout.setText(java.lang.String.valueOf(SettingUtils.requestTimeout))
        etTimeout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val timeout = etTimeout.text.toString().trim()
                if (timeout.isNotEmpty()) {
                    SettingUtils.requestTimeout = timeout.toInt()
                    if (SettingUtils.requestTimeout < 1) {
                        etTimeout.setText("1")
                        XToastUtils.error(R.string.invalid_timeout)
                    }
                } else {
                    XToastUtils.warning(R.string.invalid_timeout)
                    etTimeout.setText("1")
                    SettingUtils.requestTimeout = 1
                }
            }
        })
    }

    //设置设备名称
    private fun editAddExtraDeviceMark(etExtraDeviceMark: EditText) {
        etExtraDeviceMark.setText(SettingUtils.extraDeviceMark)
        etExtraDeviceMark.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.extraDeviceMark = etExtraDeviceMark.text.toString().trim()
            }
        })
    }

    //设置SIM1备注
    private fun editAddExtraSim1(etExtraSim1: EditText) {
        etExtraSim1.setText(SettingUtils.extraSim1)
        etExtraSim1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.extraSim1 = etExtraSim1.text.toString().trim()
            }
        })
    }

    //设置SIM2备注
    private fun editAddExtraSim2(etExtraSim2: EditText) {
        etExtraSim2.setText(SettingUtils.extraSim2)
        etExtraSim2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.extraSim2 = etExtraSim2.text.toString().trim()
            }
        })
    }

    //设置通知内容
    private fun editNotifyContent(etNotifyContent: EditText) {
        etNotifyContent.setText(SettingUtils.notifyContent)
        etNotifyContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.notifyContent = etNotifyContent.text.toString().trim()
                LiveEventBus.get(EVENT_UPDATE_NOTIFY, String::class.java).post(SettingUtils.notifyContent.toString())
            }
        })
    }

    //设置转发时启用自定义模版
    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    fun switchSmsTemplate(sb_sms_template: SwitchButton) {
        val isOn: Boolean = SettingUtils.enableSmsTemplate
        sb_sms_template.isChecked = isOn
        val layoutSmsTemplate: LinearLayout = binding!!.layoutSmsTemplate
        layoutSmsTemplate.visibility = if (isOn) View.VISIBLE else View.GONE
        val etSmsTemplate: EditText = binding!!.etSmsTemplate
        sb_sms_template.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            layoutSmsTemplate.visibility = if (isChecked) View.VISIBLE else View.GONE
            SettingUtils.enableSmsTemplate = isChecked
            if (!isChecked) {
                etSmsTemplate.setText(
                    """
                    ${getString(R.string.tag_from)}
                    ${getString(R.string.tag_sms)}
                    ${getString(R.string.tag_card_slot)}
                    ${getString(R.string.tag_receive_time)}
                    ${getString(R.string.tag_device_name)}
                    """.trimIndent()
                )
            }
        }
    }

    //设置转发信息模版
    private fun editSmsTemplate(textSmsTemplate: EditText) {
        textSmsTemplate.setText(SettingUtils.smsTemplate)
        textSmsTemplate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.smsTemplate = textSmsTemplate.text.toString().trim()
            }
        })
    }

    //页面帮助提示
    private fun switchHelpTip(@SuppressLint("UseSwitchCompatOrMaterialCode") switchHelpTip: SwitchButton) {
        switchHelpTip.isChecked = SettingUtils.enableHelpTip
        switchHelpTip.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableHelpTip = isChecked
        }
    }

    //纯客户端模式
    private fun switchDirectlyToClient(@SuppressLint("UseSwitchCompatOrMaterialCode") switchDirectlyToClient: SwitchButton) {
        switchDirectlyToClient.isChecked = SettingUtils.enablePureClientMode
        switchDirectlyToClient.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enablePureClientMode = isChecked
            if (isChecked) {
                MaterialDialog.Builder(requireContext())
                    .content(getString(R.string.enabling_pure_client_mode))
                    .positiveText(R.string.lab_yes)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        XUtil.exitApp()
                    }
                    .negativeText(R.string.lab_no)
                    .show()
            }
        }
    }

    //获取当前手机品牌
    private fun getAutoStartTips(): String {
        return when (Build.BRAND.lowercase(Locale.ROOT)) {
            "huawei" -> getString(R.string.auto_start_huawei)
            "honor" -> getString(R.string.auto_start_honor)
            "xiaomi" -> getString(R.string.auto_start_xiaomi)
            "oppo" -> getString(R.string.auto_start_oppo)
            "vivo" -> getString(R.string.auto_start_vivo)
            "meizu" -> getString(R.string.auto_start_meizu)
            "samsung" -> getString(R.string.auto_start_samsung)
            "letv" -> getString(R.string.auto_start_letv)
            "smartisan" -> getString(R.string.auto_start_smartisan)
            else -> getString(R.string.auto_start_unknown)
        }
    }

    //Intent跳转到[自启动]页面全网最全适配机型解决方案
    val hashMap = object : HashMap<String?, List<String?>?>() {
        init {
            put(
                "Xiaomi", listOf(
                    "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",  //MIUI10_9.8.1(9.0)
                    "com.miui.securitycenter"
                )
            )
            put(
                "samsung", listOf(
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.ram.AutoRunActivity",
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.appmanagement.AppManagementActivity",
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm_cn/.ui.ram.RamActivity",
                    "com.samsung.android.sm_cn/.app.dashboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.ram.AutoRunActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.appmanagement.AppManagementActivity",
                    "com.samsung.android.sm/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.sm/.ui.ram.RamActivity",
                    "com.samsung.android.sm/.app.dashboard.SmartManagerDashBoardActivity",
                    "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm"
                )
            )
            put(
                "HUAWEI", listOf(
                    "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",  //EMUI9.1.0(方舟,9.0)
                    "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity",
                    "com.huawei.systemmanager/.optimize.process.ProtectActivity",
                    "com.huawei.systemmanager/.optimize.bootstart.BootStartActivity",
                    "com.huawei.systemmanager" //最后一行可以写包名, 这样如果签名的类路径在某些新版本的ROM中没找到 就直接跳转到对应的安全中心/手机管家 首页.
                )
            )
            put(
                "vivo", listOf(
                    "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager",
                    "com.iqoo.secure/.safeguard.PurviewTabActivity",
                    "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",  //"com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity", //这是白名单, 不是自启动
                    "com.iqoo.secure",
                    "com.vivo.permissionmanager"
                )
            )
            put(
                "Meizu", listOf(
                    "com.meizu.safe/.permission.SmartBGActivity",  //Flyme7.3.0(7.1.2)
                    "com.meizu.safe/.permission.PermissionMainActivity",  //网上的
                    "com.meizu.safe"
                )
            )
            put(
                "OPPO", listOf(
                    "com.coloros.safecenter/.startupapp.StartupAppListActivity",
                    "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
                    "com.oppo.safe/.permission.startup.StartupAppListActivity",
                    "com.coloros.oppoguardelf/com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
                    "com.coloros.safecenter/com.coloros.privacypermissionsentry.PermissionTopActivity",
                    "com.coloros.safecenter",
                    "com.oppo.safe",
                    "com.coloros.oppoguardelf"
                )
            )
            put(
                "oneplus", listOf(
                    "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity",
                    "com.oneplus.security"
                )
            )
            put(
                "letv", listOf(
                    "com.letv.android.letvsafe/.AutobootManageActivity",
                    "com.letv.android.letvsafe/.BackgroundAppManageActivity",  //应用保护
                    "com.letv.android.letvsafe"
                )
            )
            put(
                "zte", listOf(
                    "com.zte.heartyservice/.autorun.AppAutoRunManager",
                    "com.zte.heartyservice"
                )
            )

            //金立
            put(
                "F", listOf(
                    "com.gionee.softmanager/.MainActivity",
                    "com.gionee.softmanager"
                )
            )

            //以下为未确定(厂商名也不确定)
            put(
                "smartisanos", listOf(
                    "com.smartisanos.security/.invokeHistory.InvokeHistoryActivity",
                    "com.smartisanos.security"
                )
            )

            //360
            put(
                "360", listOf(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity",
                    "com.yulong.android.coolsafe"
                )
            )

            //360
            put(
                "ulong", listOf(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity",
                    "com.yulong.android.coolsafe"
                )
            )

            //酷派
            put(
                "coolpad" /*厂商名称不确定是否正确*/, listOf(
                    "com.yulong.android.security/com.yulong.android.seccenter.tabbarmain",
                    "com.yulong.android.security"
                )
            )

            //联想
            put(
                "lenovo" /*厂商名称不确定是否正确*/, listOf(
                    "com.lenovo.security/.purebackground.PureBackgroundActivity",
                    "com.lenovo.security"
                )
            )
            put(
                "htc" /*厂商名称不确定是否正确*/, listOf(
                    "com.htc.pitroad/.landingpage.activity.LandingPageActivity",
                    "com.htc.pitroad"
                )
            )

            //华硕
            put(
                "asus" /*厂商名称不确定是否正确*/, listOf(
                    "com.asus.mobilemanager/.MainActivity",
                    "com.asus.mobilemanager"
                )
            )
        }
    }

    //跳转自启动页面
    private fun startToAutoStartSetting(context: Context) {
        Log.e("Util", "******************The current phone model is:" + Build.MANUFACTURER)
        val entries: MutableSet<MutableMap.MutableEntry<String?, List<String?>?>> = hashMap.entries
        var has = false
        for ((manufacturer, actCompatList) in entries) {
            if (Build.MANUFACTURER.equals(manufacturer, ignoreCase = true)) {
                if (actCompatList != null) {
                    for (act in actCompatList) {
                        try {
                            var intent: Intent?
                            if (act?.contains("/") == true) {
                                intent = Intent()
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                val componentName = ComponentName.unflattenFromString(act)
                                intent.component = componentName
                            } else {
                                //找不到? 网上的做法都是跳转到设置... 这基本上是没意义的 基本上自启动这个功能是第三方厂商自己写的安全管家类app
                                //所以我是直接跳转到对应的安全管家/安全中心
                                intent = act?.let { context.packageManager.getLaunchIntentForPackage(it) }
                            }
                            context.startActivity(intent)
                            has = true
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        if (!has) {
            XToastUtils.info(R.string.tips_compatible_solution)
            try {
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

}