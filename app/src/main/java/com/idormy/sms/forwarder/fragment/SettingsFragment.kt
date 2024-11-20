package com.idormy.sms.forwarder.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hjq.language.LocaleContract
import com.hjq.language.MultiLanguages
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.adapter.spinner.AppListAdapterItem
import com.idormy.sms.forwarder.adapter.spinner.AppListSpinnerAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentSettingsBinding
import com.idormy.sms.forwarder.entity.SimInfo
import com.idormy.sms.forwarder.fragment.client.CloneFragment
import com.idormy.sms.forwarder.receiver.BootCompletedReceiver
import com.idormy.sms.forwarder.service.BluetoothScanService
import com.idormy.sms.forwarder.service.ForegroundService
import com.idormy.sms.forwarder.service.LocationService
import com.idormy.sms.forwarder.utils.ACTION_RESTART
import com.idormy.sms.forwarder.utils.ACTION_START
import com.idormy.sms.forwarder.utils.ACTION_STOP
import com.idormy.sms.forwarder.utils.ACTION_UPDATE_NOTIFICATION
import com.idormy.sms.forwarder.utils.AppUtils.getAppPackageName
import com.idormy.sms.forwarder.utils.BluetoothUtils
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.DataProvider
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.idormy.sms.forwarder.utils.EXTRA_UPDATE_NOTIFICATION
import com.idormy.sms.forwarder.utils.KEY_DEFAULT_SELECTION
import com.idormy.sms.forwarder.utils.KeepAliveUtils
import com.idormy.sms.forwarder.utils.LocationUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.PhoneUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.widget.GuideTipsDialog
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox
import com.xuexiang.xui.widget.button.switchbutton.SwitchButton
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.picker.XSeekBar
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder
import com.xuexiang.xui.widget.picker.widget.listener.OnOptionsSelectListener
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.XUtil.getPackageManager
import com.xuexiang.xutil.file.FileUtils
import java.util.Locale

@Suppress("SpellCheckingInspection", "PrivatePropertyName")
@Page(name = "通用设置")
class SettingsFragment : BaseFragment<FragmentSettingsBinding?>(), View.OnClickListener {

    private val TAG: String = SettingsFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val mTimeOption = DataProvider.timePeriodOption
    private var initViewsFinished = false

    //已安装App信息列表
    private val appListSpinnerList = ArrayList<AppListAdapterItem>()
    private lateinit var appListSpinnerAdapter: AppListSpinnerAdapter<*>
    private val appListObserver = Observer { it: String ->
        Log.d(TAG, "EVENT_LOAD_APP_LIST: $it")
        initAppSpinner()
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false)
        titleBar!!.setLeftImageResource(R.drawable.ic_action_menu)
        titleBar!!.setTitle(R.string.menu_settings)
        titleBar!!.setLeftClickListener { getContainer()?.openMenu() }
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_menu_notifications_white) {
            @SingleClick
            override fun performAction(view: View) {
                GuideTipsDialog.showTipsForce(requireContext())
            }
        })
        titleBar!!.addAction(object : TitleBar.ImageAction(R.drawable.ic_restore) {
            @SingleClick
            override fun performAction(view: View) {
                PageOption.to(CloneFragment::class.java)
                    .putInt(KEY_DEFAULT_SELECTION, 1) //默认离线模式
                    .setNewActivity(true)
                    .open(this@SettingsFragment)
            }
        })
        return titleBar
    }

    private fun getContainer(): MainActivity? {
        return activity as MainActivity?
    }

    @SuppressLint("NewApi", "SetTextI18n")
    override fun initViews() {

        //转发短信广播
        switchEnableSms(binding!!.sbEnableSms)
        //转发通话记录
        switchEnablePhone(binding!!.sbEnablePhone, binding!!.scbCallType1, binding!!.scbCallType2, binding!!.scbCallType3, binding!!.scbCallType4, binding!!.scbCallType5, binding!!.scbCallType6)
        //转发应用通知
        switchEnableAppNotify(binding!!.sbEnableAppNotify, binding!!.scbCancelAppNotify, binding!!.scbNotUserPresent)

        //发现蓝牙设备服务
        switchEnableBluetooth(binding!!.sbEnableBluetooth, binding!!.layoutBluetoothSetting, binding!!.xsbScanInterval, binding!!.scbIgnoreAnonymous)
        //GPS定位功能
        switchEnableLocation(binding!!.sbEnableLocation, binding!!.layoutLocationSetting, binding!!.rgAccuracy, binding!!.rgPowerRequirement, binding!!.xsbMinInterval, binding!!.xsbMinDistance)
        //短信指令
        switchEnableSmsCommand(binding!!.sbEnableSmsCommand, binding!!.etSafePhone)
        //启动时异步获取已安装App信息
        switchEnableLoadAppList(binding!!.sbEnableLoadAppList, binding!!.scbLoadUserApp, binding!!.scbLoadSystemApp)
        //设置自动消除额外APP通知
        editExtraAppList(binding!!.etAppList)
        //自动过滤多久内重复消息
        binding!!.xsbDuplicateMessagesLimits.setDefaultValue(SettingUtils.duplicateMessagesLimits)
        binding!!.xsbDuplicateMessagesLimits.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            SettingUtils.duplicateMessagesLimits = newValue
        }
        //免打扰(禁用转发)时间段
        binding!!.tvSilentPeriod.text = mTimeOption[SettingUtils.silentPeriodStart] + " ~ " + mTimeOption[SettingUtils.silentPeriodEnd]
        binding!!.scbSilentPeriodLogs.isChecked = SettingUtils.enableSilentPeriodLogs
        binding!!.scbSilentPeriodLogs.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableSilentPeriodLogs = isChecked
        }

        //开机启动
        checkWithReboot(binding!!.sbWithReboot, binding!!.tvAutoStartup)
        //忽略电池优化设置
        batterySetting(binding!!.layoutBatterySetting, binding!!.sbBatterySetting)
        //不在最近任务列表中显示
        switchExcludeFromRecents(binding!!.layoutExcludeFromRecents, binding!!.sbExcludeFromRecents)
        //Cactus增强保活措施
        switchEnableCactus(binding!!.sbEnableCactus, binding!!.scbPlaySilenceMusic, binding!!.scbOnePixelActivity)
        //接口请求失败重试时间间隔
        editRetryDelayTime(binding!!.xsbRetryTimes, binding!!.xsbDelayTime, binding!!.xsbTimeout)

        //设备备注
        editAddExtraDeviceMark(binding!!.etExtraDeviceMark)
        //SIM1主键
        editAddSubidSim1(binding!!.etSubidSim1)
        //SIM1备注
        editAddExtraSim1(binding!!.etExtraSim1)

        // sim 槽只有一个的时候不显示 SIM2 设置
        if (PhoneUtils.getSimSlotCount() != 1) {
            //SIM2主键
            editAddSubidSim2(binding!!.etSubidSim2)
            //SIM2备注
            editAddExtraSim2(binding!!.etExtraSim2)
        } else {
            binding!!.layoutSim2.visibility = View.GONE
        }
        //通知内容
        editNotifyContent(binding!!.etNotifyContent)
        //启用自定义模版
        switchSmsTemplate(binding!!.sbSmsTemplate)
        //自定义模板
        editSmsTemplate(binding!!.etSmsTemplate)
        //纯客户端模式
        switchDirectlyToClient(binding!!.sbDirectlyToClient)
        //纯自动任务模式
        switchDirectlyToTask(binding!!.sbDirectlyToTask)
        //调试模式
        switchDebugMode(binding!!.sbDebugMode)
        //多语言设置
        switchLanguage(binding!!.rgMainLanguages)

        initViewsFinished = true
    }

    override fun onResume() {
        super.onResume()
        //初始化APP下拉列表
        initAppSpinner()
    }

    override fun initListeners() {
        binding!!.btnSilentPeriod.setOnClickListener(this)
        binding!!.btnExtraDeviceMark.setOnClickListener(this)
        binding!!.btnExtraSim1.setOnClickListener(this)
        binding!!.btnExtraSim2.setOnClickListener(this)
        binding!!.btnExportLog.setOnClickListener(this)

        //监听已安装App信息列表加载完成事件
        LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).observeStickyForever(appListObserver)
    }

    @SuppressLint("SetTextI18n")
    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_silent_period -> {
                OptionsPickerBuilder(context, OnOptionsSelectListener { _: View?, options1: Int, options2: Int, _: Int ->
                    SettingUtils.silentPeriodStart = options1
                    SettingUtils.silentPeriodEnd = options2
                    val txt = mTimeOption[options1] + " ~ " + mTimeOption[options2]
                    binding!!.tvSilentPeriod.text = txt
                    XToastUtils.toast(txt)
                    return@OnOptionsSelectListener false
                }).setTitleText(getString(R.string.select_time_period)).setSelectOptions(SettingUtils.silentPeriodStart, SettingUtils.silentPeriodEnd).build<Any>().also {
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
                    XXPermissions.startPermissionActivity(
                        requireContext(), "android.permission.READ_PHONE_STATE"
                    )
                    return
                }
                Log.d(TAG, App.SimInfoList.toString())
                if (!App.SimInfoList.containsKey(0)) {
                    XToastUtils.error(
                        String.format(
                            getString(R.string.tip_can_not_get_sim_info), 1
                        )
                    )
                    return
                }
                val simInfo: SimInfo? = App.SimInfoList[0]
                binding!!.etSubidSim1.setText(simInfo?.mSubscriptionId.toString())
                binding!!.etExtraSim1.setText(simInfo?.mCarrierName.toString() + "_" + simInfo?.mNumber.toString())
                return
            }

            R.id.btn_extra_sim2 -> {
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
                if (App.SimInfoList.isEmpty()) {
                    XToastUtils.error(R.string.tip_can_not_get_sim_infos)
                    XXPermissions.startPermissionActivity(
                        requireContext(), "android.permission.READ_PHONE_STATE"
                    )
                    return
                }
                Log.d(TAG, App.SimInfoList.toString())
                if (!App.SimInfoList.containsKey(1)) {
                    XToastUtils.error(
                        String.format(
                            getString(R.string.tip_can_not_get_sim_info), 2
                        )
                    )
                    return
                }
                val simInfo: SimInfo? = App.SimInfoList[1]
                binding!!.etSubidSim2.setText(simInfo?.mSubscriptionId.toString())
                binding!!.etExtraSim2.setText(simInfo?.mCarrierName.toString() + "_" + simInfo?.mNumber.toString())
                return
            }

            R.id.btn_export_log -> {
                XXPermissions.with(this)
                    // 申请储存权限
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        @SuppressLint("SetTextI18n")
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            try {
                                val srcDirPath = App.context.cacheDir.absolutePath + "/logs"
                                val destDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/SmsForwarder"
                                if (FileUtils.copyDir(srcDirPath, destDirPath, null)) {
                                    XToastUtils.success(getString(R.string.log_export_success) + destDirPath)
                                } else {
                                    XToastUtils.error(getString(R.string.log_export_failed))
                                }
                            } catch (e: Exception) {
                                XToastUtils.error(getString(R.string.log_export_failed) + e.message)
                                e.printStackTrace()
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.error(R.string.toast_denied_never)
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(R.string.toast_denied)
                            }
                        }
                    })
                return
            }

            else -> {}
        }
    }

    //转发短信
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun switchEnableSms(sbEnableSms: SwitchButton) {
        sbEnableSms.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableSms = isChecked
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
                            Log.d(TAG, "onGranted: permissions=$permissions, all=$all")
                            if (!all) {
                                XToastUtils.warning(getString(R.string.forward_sms) + ": " + getString(R.string.toast_granted_part))
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            Log.e(TAG, "onDenied: permissions=$permissions, never=$never")
                            if (never) {
                                XToastUtils.error(getString(R.string.forward_sms) + ": " + getString(R.string.toast_denied_never))
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(getString(R.string.forward_sms) + ": " + getString(R.string.toast_denied))
                            }
                            SettingUtils.enableSms = false
                            sbEnableSms.isChecked = false
                        }
                    })
            }
        }
        sbEnableSms.isChecked = SettingUtils.enableSms
    }

    //转发通话
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun switchEnablePhone(sbEnablePhone: SwitchButton, scbCallType1: SmoothCheckBox, scbCallType2: SmoothCheckBox, scbCallType3: SmoothCheckBox, scbCallType4: SmoothCheckBox, scbCallType5: SmoothCheckBox, scbCallType6: SmoothCheckBox) {
        scbCallType1.isChecked = SettingUtils.enableCallType1
        scbCallType2.isChecked = SettingUtils.enableCallType2
        scbCallType3.isChecked = SettingUtils.enableCallType3
        scbCallType4.isChecked = SettingUtils.enableCallType4
        scbCallType5.isChecked = SettingUtils.enableCallType5
        scbCallType6.isChecked = SettingUtils.enableCallType6
        sbEnablePhone.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
                return@setOnCheckedChangeListener
            }
            SettingUtils.enablePhone = isChecked
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
                            Log.d(TAG, "onGranted: permissions=$permissions, all=$all")
                            if (!all) {
                                XToastUtils.warning(getString(R.string.forward_calls) + ": " + getString(R.string.toast_granted_part))
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            Log.e(TAG, "onDenied: permissions=$permissions, never=$never")
                            if (never) {
                                XToastUtils.error(getString(R.string.forward_calls) + ": " + getString(R.string.toast_denied_never))
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(getString(R.string.forward_calls) + ": " + getString(R.string.toast_denied))
                            }
                            SettingUtils.enablePhone = false
                            sbEnablePhone.isChecked = false
                        }
                    })
            }
        }
        sbEnablePhone.isChecked = SettingUtils.enablePhone
        scbCallType1.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType1 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType2.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType2 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType3.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType3 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType4.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType4 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType5.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType5 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
        scbCallType6.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCallType6 = isChecked
            if (!isChecked && !SettingUtils.enableCallType1 && !SettingUtils.enableCallType2 && !SettingUtils.enableCallType3 && !SettingUtils.enableCallType4 && !SettingUtils.enableCallType5 && !SettingUtils.enableCallType6) {
                XToastUtils.info(R.string.enable_phone_fw_tips)
                SettingUtils.enablePhone = false
                sbEnablePhone.isChecked = false
            }
        }
    }

    //转发应用通知
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun switchEnableAppNotify(sbEnableAppNotify: SwitchButton, scbCancelAppNotify: SmoothCheckBox, scbNotUserPresent: SmoothCheckBox) {
        sbEnableAppNotify.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            binding!!.layoutOptionalAction.visibility = if (isChecked) View.VISIBLE else View.GONE
            SettingUtils.enableAppNotify = isChecked
            if (isChecked) {
                XXPermissions.with(this)
                    .permission(Permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                    .request(OnPermissionCallback { permissions, allGranted ->
                        if (!allGranted) {
                            Log.e(TAG, "onGranted: permissions=$permissions, allGranted=false")
                            SettingUtils.enableAppNotify = false
                            sbEnableAppNotify.isChecked = false
                            XToastUtils.error(R.string.tips_notification_listener)
                            return@OnPermissionCallback
                        }

                        SettingUtils.enableAppNotify = true
                        sbEnableAppNotify.isChecked = true
                        CommonUtils.toggleNotificationListenerService(requireContext())
                    })
            }
        }
        val isEnable = SettingUtils.enableAppNotify
        sbEnableAppNotify.isChecked = isEnable
        binding!!.layoutOptionalAction.visibility = if (isEnable) View.VISIBLE else View.GONE

        scbCancelAppNotify.isChecked = SettingUtils.enableCancelAppNotify
        scbCancelAppNotify.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableCancelAppNotify = isChecked
        }
        scbNotUserPresent.isChecked = SettingUtils.enableNotUserPresent
        scbNotUserPresent.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableNotUserPresent = isChecked
        }
    }

    //发现蓝牙设备服务
    private fun switchEnableBluetooth(@SuppressLint("UseSwitchCompatOrMaterialCode") sbEnableBluetooth: SwitchButton, layoutBluetoothSetting: LinearLayout, xsbScanInterval: XSeekBar, scbIgnoreAnonymous: SmoothCheckBox) {
        sbEnableBluetooth.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableBluetooth = isChecked
            layoutBluetoothSetting.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                XXPermissions.with(this)
                    .permission(Permission.BLUETOOTH_SCAN)
                    .permission(Permission.BLUETOOTH_CONNECT)
                    .permission(Permission.BLUETOOTH_ADVERTISE)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            Log.d(TAG, "onGranted: permissions=$permissions, all=$all")
                            if (!all) {
                                XToastUtils.warning(getString(R.string.enable_bluetooth) + ": " + getString(R.string.toast_granted_part))
                            }
                            restartBluetoothService(ACTION_START)
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            Log.e(TAG, "onDenied: permissions=$permissions, never=$never")
                            if (never) {
                                XToastUtils.error(getString(R.string.enable_bluetooth) + ": " + getString(R.string.toast_denied_never))
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(getString(R.string.enable_bluetooth) + ": " + getString(R.string.toast_denied))
                            }
                            SettingUtils.enableBluetooth = false
                            sbEnableBluetooth.isChecked = false
                            restartBluetoothService(ACTION_STOP)
                        }
                    })
            } else {
                restartBluetoothService(ACTION_STOP)
            }
        }
        val isEnable = SettingUtils.enableBluetooth
        sbEnableBluetooth.isChecked = isEnable
        layoutBluetoothSetting.visibility = if (isEnable) View.VISIBLE else View.GONE

        //扫描蓝牙设备间隔
        xsbScanInterval.setDefaultValue((SettingUtils.bluetoothScanInterval / 1000).toInt())
        xsbScanInterval.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            if (newValue * 1000L != SettingUtils.bluetoothScanInterval) {
                SettingUtils.bluetoothScanInterval = newValue * 1000L
                restartBluetoothService()
            }
        }

        //是否忽略匿名设备
        scbIgnoreAnonymous.isChecked = SettingUtils.bluetoothIgnoreAnonymous
        scbIgnoreAnonymous.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.bluetoothIgnoreAnonymous = isChecked
            restartBluetoothService()
        }

    }

    //重启蓝牙扫描服务
    private fun restartBluetoothService(action: String = ACTION_RESTART) {
        if (!initViewsFinished) return
        Log.d(TAG, "restartBluetoothService, action: $action")
        val serviceIntent = Intent(requireContext(), BluetoothScanService::class.java)
        //如果蓝牙功能已启用，但是系统蓝牙功能不可用，则关闭蓝牙功能
        if (SettingUtils.enableBluetooth && (!BluetoothUtils.isBluetoothEnabled() || !BluetoothUtils.hasBluetoothCapability(App.context))) {
            XToastUtils.error(getString(R.string.toast_bluetooth_not_enabled))
            SettingUtils.enableBluetooth = false
            binding!!.sbEnableBluetooth.isChecked = false
            binding!!.layoutBluetoothSetting.visibility = View.GONE
            serviceIntent.action = ACTION_STOP
        } else {
            serviceIntent.action = action
        }
        requireContext().startService(serviceIntent)
    }

    //GPS定位服务
    private fun switchEnableLocation(@SuppressLint("UseSwitchCompatOrMaterialCode") sbEnableLocation: SwitchButton, layoutLocationSetting: LinearLayout, rgAccuracy: RadioGroup, rgPowerRequirement: RadioGroup, xsbMinInterval: XSeekBar, xsbMinDistance: XSeekBar) {
        sbEnableLocation.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableLocation = isChecked
            layoutLocationSetting.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                XXPermissions.with(this)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .permission(Permission.ACCESS_BACKGROUND_LOCATION)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            Log.d(TAG, "onGranted: permissions=$permissions, all=$all")
                            if (!all) {
                                XToastUtils.warning(getString(R.string.enable_location) + ": " + getString(R.string.toast_granted_part))
                            }
                            restartLocationService(ACTION_START)
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            Log.e(TAG, "onDenied: permissions=$permissions, never=$never")
                            if (never) {
                                XToastUtils.error(getString(R.string.enable_location) + ": " + getString(R.string.toast_denied_never))
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(getString(R.string.enable_location) + ": " + getString(R.string.toast_denied))
                            }
                            SettingUtils.enableLocation = false
                            sbEnableLocation.isChecked = false
                            restartLocationService(ACTION_STOP)
                        }
                    })
            } else {
                restartLocationService(ACTION_STOP)
            }
        }
        val isEnable = SettingUtils.enableLocation
        sbEnableLocation.isChecked = isEnable
        layoutLocationSetting.visibility = if (isEnable) View.VISIBLE else View.GONE

        //设置位置精度：高精度（默认）
        rgAccuracy.check(
            when (SettingUtils.locationAccuracy) {
                Criteria.ACCURACY_FINE -> R.id.rb_accuracy_fine
                Criteria.ACCURACY_COARSE -> R.id.rb_accuracy_coarse
                Criteria.NO_REQUIREMENT -> R.id.rb_accuracy_no_requirement
                else -> R.id.rb_accuracy_fine
            }
        )
        rgAccuracy.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            SettingUtils.locationAccuracy = when (checkedId) {
                R.id.rb_accuracy_fine -> Criteria.ACCURACY_FINE
                R.id.rb_accuracy_coarse -> Criteria.ACCURACY_COARSE
                R.id.rb_accuracy_no_requirement -> Criteria.NO_REQUIREMENT
                else -> Criteria.ACCURACY_FINE
            }
            restartLocationService()
        }

        //设置电量消耗：低电耗（默认）
        rgPowerRequirement.check(
            when (SettingUtils.locationPowerRequirement) {
                Criteria.POWER_HIGH -> R.id.rb_power_requirement_high
                Criteria.POWER_MEDIUM -> R.id.rb_power_requirement_medium
                Criteria.POWER_LOW -> R.id.rb_power_requirement_low
                Criteria.NO_REQUIREMENT -> R.id.rb_power_requirement_no_requirement
                else -> R.id.rb_power_requirement_low
            }
        )
        rgPowerRequirement.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            SettingUtils.locationPowerRequirement = when (checkedId) {
                R.id.rb_power_requirement_high -> Criteria.POWER_HIGH
                R.id.rb_power_requirement_medium -> Criteria.POWER_MEDIUM
                R.id.rb_power_requirement_low -> Criteria.POWER_LOW
                R.id.rb_power_requirement_no_requirement -> Criteria.NO_REQUIREMENT
                else -> Criteria.POWER_LOW
            }
            restartLocationService()
        }

        //设置位置更新最小时间间隔（单位：毫秒）； 默认间隔：10000毫秒，最小间隔：1000毫秒
        xsbMinInterval.setDefaultValue((SettingUtils.locationMinInterval / 1000).toInt())
        xsbMinInterval.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            if (newValue * 1000L != SettingUtils.locationMinInterval) {
                SettingUtils.locationMinInterval = newValue * 1000L
                restartLocationService()
            }
        }

        //设置位置更新最小距离（单位：米）；默认距离：0米
        xsbMinDistance.setDefaultValue(SettingUtils.locationMinDistance)
        xsbMinDistance.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            if (newValue != SettingUtils.locationMinDistance) {
                SettingUtils.locationMinDistance = newValue
                restartLocationService()
            }
        }
    }

    //重启定位服务
    private fun restartLocationService(action: String = ACTION_RESTART) {
        if (!initViewsFinished) return
        Log.d(TAG, "restartLocationService, action: $action")
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        //如果定位功能已启用，但是系统定位功能不可用，则关闭定位功能
        if (SettingUtils.enableLocation && (!LocationUtils.isLocationEnabled(App.context) || !LocationUtils.hasLocationCapability(App.context))) {
            XToastUtils.error(getString(R.string.toast_location_not_enabled))
            SettingUtils.enableLocation = false
            binding!!.sbEnableLocation.isChecked = false
            binding!!.layoutLocationSetting.visibility = View.GONE
            serviceIntent.action = ACTION_STOP
        } else {
            serviceIntent.action = action
        }
        requireContext().startService(serviceIntent)
    }

    //接受短信指令
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun switchEnableSmsCommand(sbEnableSmsCommand: SwitchButton, etSafePhone: EditText) {
        sbEnableSmsCommand.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableSmsCommand = isChecked
            etSafePhone.visibility = if (isChecked) View.VISIBLE else View.GONE
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
                            if (!all) {
                                XToastUtils.warning(getString(R.string.sms_command) + ": " + getString(R.string.toast_denied_never))
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                XToastUtils.error(getString(R.string.sms_command) + ": " + getString(R.string.toast_denied_never))
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(requireContext(), permissions)
                            } else {
                                XToastUtils.error(getString(R.string.sms_command) + ": " + getString(R.string.toast_denied))
                            }
                            SettingUtils.enableSmsCommand = false
                            sbEnableSmsCommand.isChecked = false
                        }
                    })
            }
        }
        val isEnable = SettingUtils.enableSmsCommand
        sbEnableSmsCommand.isChecked = isEnable
        etSafePhone.visibility = if (isEnable) View.VISIBLE else View.GONE

        etSafePhone.setText(SettingUtils.smsCommandSafePhone)
        etSafePhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.smsCommandSafePhone = etSafePhone.text.toString().trim().removeSuffix("\n")
            }
        })
    }

    //设置自动消除额外APP通知
    private fun editExtraAppList(textAppList: EditText) {
        textAppList.setText(SettingUtils.cancelExtraAppNotify)
        textAppList.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.cancelExtraAppNotify = textAppList.text.toString().trim().removeSuffix("\n")
            }
        })
    }

    //启动时异步获取已安装App信息
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun switchEnableLoadAppList(sbEnableLoadAppList: SwitchButton, scbLoadUserApp: SmoothCheckBox, scbLoadSystemApp: SmoothCheckBox) {
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
            if (isChecked) {
                XToastUtils.info(getString(R.string.loading_app_list))
                val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                WorkManager.getInstance(XUtil.getContext()).enqueue(request)
            }
        }
        scbLoadUserApp.isChecked = SettingUtils.enableLoadUserAppList
        scbLoadUserApp.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            SettingUtils.enableLoadUserAppList = isChecked
            if (SettingUtils.enableLoadAppList && !SettingUtils.enableLoadUserAppList && !SettingUtils.enableLoadSystemAppList) {
                sbEnableLoadAppList.isChecked = false
                SettingUtils.enableLoadAppList = false
                XToastUtils.error(getString(R.string.load_app_list_toast))
            }
            if (isChecked && SettingUtils.enableLoadAppList && App.UserAppList.isEmpty()) {
                XToastUtils.info(getString(R.string.loading_app_list))
                val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                WorkManager.getInstance(XUtil.getContext()).enqueue(request)
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
            if (isChecked && SettingUtils.enableLoadAppList && App.SystemAppList.isEmpty()) {
                XToastUtils.info(getString(R.string.loading_app_list))
                val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                WorkManager.getInstance(XUtil.getContext()).enqueue(request)
            }
        }
    }

    //开机启动
    private fun checkWithReboot(@SuppressLint("UseSwitchCompatOrMaterialCode") sbWithReboot: SwitchButton, tvAutoStartup: TextView) {
        tvAutoStartup.text = getAutoStartTips()

        //获取组件
        val cm = ComponentName(getAppPackageName(), BootCompletedReceiver::class.java.name)
        val pm: PackageManager = getPackageManager()
        val state = pm.getComponentEnabledSetting(cm)
        sbWithReboot.isChecked = !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
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
    private fun batterySetting(layoutBatterySetting: LinearLayout, sbBatterySetting: SwitchButton) {
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
                    sbBatterySetting.isChecked = true
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
    private fun switchExcludeFromRecents(layoutExcludeFromRecents: LinearLayout, sbExcludeFromRecents: SwitchButton) {
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

    //Cactus增强保活措施
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun switchEnableCactus(sbEnableCactus: SwitchButton, scbPlaySilenceMusic: SmoothCheckBox, scbOnePixelActivity: SmoothCheckBox) {
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
    private fun editRetryDelayTime(xsbRetryTimes: XSeekBar, xsbDelayTime: XSeekBar, xsbTimeout: XSeekBar) {
        xsbRetryTimes.setDefaultValue(SettingUtils.requestRetryTimes)
        xsbRetryTimes.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            SettingUtils.requestRetryTimes = newValue
            binding!!.layoutDelayTime.visibility = if (newValue > 0) View.VISIBLE else View.GONE
        }
        xsbDelayTime.setDefaultValue(SettingUtils.requestDelayTime)
        xsbDelayTime.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            SettingUtils.requestDelayTime = newValue
        }
        xsbTimeout.setDefaultValue(SettingUtils.requestTimeout)
        xsbTimeout.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            SettingUtils.requestTimeout = newValue
        }
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

    //设置SIM1主键
    private fun editAddSubidSim1(etSubidSim1: EditText) {
        etSubidSim1.setText("${SettingUtils.subidSim1}")
        etSubidSim1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val v = etSubidSim1.text.toString()
                SettingUtils.subidSim1 = if (!TextUtils.isEmpty(v)) {
                    v.toInt()
                } else {
                    1
                }
            }
        })
    }

    //设置SIM2主键
    private fun editAddSubidSim2(etSubidSim2: EditText) {
        etSubidSim2.setText("${SettingUtils.subidSim2}")
        etSubidSim2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val v = etSubidSim2.text.toString()
                SettingUtils.subidSim2 = if (!TextUtils.isEmpty(v)) {
                    v.toInt()
                } else {
                    2
                }
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
                val notifyContent = etNotifyContent.text.toString().trim()
                SettingUtils.notifyContent = notifyContent
                val updateIntent = Intent(context, ForegroundService::class.java)
                updateIntent.action = ACTION_UPDATE_NOTIFICATION
                updateIntent.putExtra(EXTRA_UPDATE_NOTIFICATION, notifyContent)
                context?.let { ContextCompat.startForegroundService(it, updateIntent) }
            }
        })
    }

    //设置转发时启用自定义模版
    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    private fun switchSmsTemplate(sbSmsTemplate: SwitchButton) {
        val isOn: Boolean = SettingUtils.enableSmsTemplate
        sbSmsTemplate.isChecked = isOn
        val layoutSmsTemplate: LinearLayout = binding!!.layoutSmsTemplate
        layoutSmsTemplate.visibility = if (isOn) View.VISIBLE else View.GONE
        val etSmsTemplate: EditText = binding!!.etSmsTemplate
        sbSmsTemplate.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            layoutSmsTemplate.visibility = if (isChecked) View.VISIBLE else View.GONE
            SettingUtils.enableSmsTemplate = isChecked
            if (!isChecked) {
                etSmsTemplate.setText(
                    """
                    ${getString(R.string.tag_from)}
                    ${getString(R.string.tag_sms)}
                    ${getString(R.string.tag_card_slot)}
                    SubId：${getString(R.string.tag_card_subid)}
                    ${getString(R.string.tag_receive_time)}
                    ${getString(R.string.tag_device_name)}
                    """.trimIndent()
                )
            }
        }
    }

    //设置转发信息模版
    private fun editSmsTemplate(textSmsTemplate: EditText) {
        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glSmsTemplate, textSmsTemplate, "all")
        textSmsTemplate.setText(SettingUtils.smsTemplate)
        textSmsTemplate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                SettingUtils.smsTemplate = textSmsTemplate.text.toString().trim()
            }
        })
    }

    //纯客户端模式
    private fun switchDirectlyToClient(@SuppressLint("UseSwitchCompatOrMaterialCode") switchDirectlyToClient: SwitchButton) {
        switchDirectlyToClient.isChecked = SettingUtils.enablePureClientMode
        switchDirectlyToClient.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enablePureClientMode = isChecked
            if (isChecked) {
                MaterialDialog.Builder(requireContext()).content(getString(R.string.enabling_pure_client_mode)).positiveText(R.string.lab_yes).onPositive { _: MaterialDialog?, _: DialogAction? ->
                    XUtil.exitApp()
                }.negativeText(R.string.lab_no).show()
            }
        }
    }

    //纯自动任务模式
    private fun switchDirectlyToTask(@SuppressLint("UseSwitchCompatOrMaterialCode") switchDirectlyToTask: SwitchButton) {
        switchDirectlyToTask.isChecked = SettingUtils.enablePureTaskMode
        switchDirectlyToTask.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enablePureTaskMode = isChecked
            if (isChecked) {
                MaterialDialog.Builder(requireContext()).content(getString(R.string.enabling_pure_client_mode)).positiveText(R.string.lab_yes).onPositive { _: MaterialDialog?, _: DialogAction? ->
                    XUtil.exitApp()
                }.negativeText(R.string.lab_no).show()
            }
        }
    }

    //调试模式
    private fun switchDebugMode(@SuppressLint("UseSwitchCompatOrMaterialCode") switchDebugMode: SwitchButton) {
        switchDebugMode.isChecked = SettingUtils.enableDebugMode
        switchDebugMode.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            SettingUtils.enableDebugMode = isChecked
            App.isDebug = isChecked
        }
    }

    //多语言设置
    private fun switchLanguage(rgMainLanguages: RadioGroup) {
        val context = App.context
        rgMainLanguages.check(
            if (MultiLanguages.isSystemLanguage(context)) {
                R.id.rb_main_language_auto
            } else {
                when (MultiLanguages.getAppLanguage(context)) {
                    LocaleContract.getSimplifiedChineseLocale() -> R.id.rb_main_language_cn
                    LocaleContract.getTraditionalChineseLocale() -> R.id.rb_main_language_tw
                    LocaleContract.getEnglishLocale() -> R.id.rb_main_language_en
                    else -> R.id.rb_main_language_auto
                }
            }
        )

        rgMainLanguages.setOnCheckedChangeListener { _, checkedId ->
            val oldLang = MultiLanguages.getAppLanguage(context)
            var newLang = MultiLanguages.getSystemLanguage(context)
            //SettingUtils.isFlowSystemLanguage = false
            when (checkedId) {
                R.id.rb_main_language_auto -> {
                    // 只为了触发onAppLocaleChange
                    MultiLanguages.setAppLanguage(context, newLang)
                    // SettingUtils.isFlowSystemLanguage = true
                    // 跟随系统
                    MultiLanguages.clearAppLanguage(context)
                }

                R.id.rb_main_language_cn -> {
                    // 简体中文
                    newLang = LocaleContract.getSimplifiedChineseLocale()
                    MultiLanguages.setAppLanguage(context, newLang)
                }

                R.id.rb_main_language_tw -> {
                    // 繁体中文
                    newLang = LocaleContract.getTraditionalChineseLocale()
                    MultiLanguages.setAppLanguage(context, newLang)
                }

                R.id.rb_main_language_en -> {
                    // 英语
                    newLang = LocaleContract.getEnglishLocale()
                    MultiLanguages.setAppLanguage(context, newLang)
                }
            }

            // 重启应用
            Log.d(TAG, "oldLang: $oldLang, newLang: $newLang")
            if (oldLang.toString() != newLang.toString()) {
                //CommonUtils.switchLanguage(oldLang, newLang)
                XToastUtils.toast(R.string.multi_languages_toast)
                //切换语种后重启APP
                Thread.sleep(200)
                val intent = Intent(App.context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    //获取当前手机品牌
    private fun getAutoStartTips(): String {
        return when (Build.BRAND.lowercase(Locale.ROOT)) {
            "huawei" -> getString(R.string.auto_start_huawei)
            "honor" -> getString(R.string.auto_start_honor)
            "xiaomi" -> getString(R.string.auto_start_xiaomi)
            "redmi" -> getString(R.string.auto_start_redmi)
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
    private val hashMap = object : HashMap<String?, List<String?>?>() {
        init {
            put(
                "Xiaomi", listOf(
                    "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",  //MIUI10_9.8.1(9.0)
                    "com.miui.securitycenter"
                )
            )
            put(
                "samsung", listOf(
                    "com.samsung.android.sm_cn/com.samsung.android.sm.ui.ram.AutoRunActivity", "com.samsung.android.sm_cn/com.samsung.android.sm.ui.appmanagement.AppManagementActivity", "com.samsung.android.sm_cn/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity", "com.samsung.android.sm_cn/.ui.ram.RamActivity", "com.samsung.android.sm_cn/.app.dashboard.SmartManagerDashBoardActivity", "com.samsung.android.sm/com.samsung.android.sm.ui.ram.AutoRunActivity", "com.samsung.android.sm/com.samsung.android.sm.ui.appmanagement.AppManagementActivity", "com.samsung.android.sm/com.samsung.android.sm.ui.cstyleboard.SmartManagerDashBoardActivity", "com.samsung.android.sm/.ui.ram.RamActivity", "com.samsung.android.sm/.app.dashboard.SmartManagerDashBoardActivity", "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity", "com.samsung.android.sm_cn", "com.samsung.android.sm"
                )
            )
            put(
                "HUAWEI", listOf(
                    "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",  //EMUI9.1.0(方舟,9.0)
                    "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity", "com.huawei.systemmanager/.optimize.process.ProtectActivity", "com.huawei.systemmanager/.optimize.bootstart.BootStartActivity", "com.huawei.systemmanager" //最后一行可以写包名, 这样如果签名的类路径在某些新版本的ROM中没找到 就直接跳转到对应的安全中心/手机管家 首页.
                )
            )
            put(
                "vivo", listOf(
                    "com.iqoo.secure/.ui.phoneoptimize.BgStartUpManager", "com.iqoo.secure/.safeguard.PurviewTabActivity", "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",  //"com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity", //这是白名单, 不是自启动
                    "com.iqoo.secure", "com.vivo.permissionmanager"
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
                    "com.coloros.safecenter/.startupapp.StartupAppListActivity", "com.coloros.safecenter/.permission.startup.StartupAppListActivity", "com.oppo.safe/.permission.startup.StartupAppListActivity", "com.coloros.oppoguardelf/com.coloros.powermanager.fuelgaue.PowerUsageModelActivity", "com.coloros.safecenter/com.coloros.privacypermissionsentry.PermissionTopActivity", "com.coloros.safecenter", "com.oppo.safe", "com.coloros.oppoguardelf"
                )
            )
            put(
                "oneplus", listOf(
                    "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity", "com.oneplus.security"
                )
            )
            put(
                "letv", listOf(
                    "com.letv.android.letvsafe/.AutobootManageActivity", "com.letv.android.letvsafe/.BackgroundAppManageActivity",  //应用保护
                    "com.letv.android.letvsafe"
                )
            )
            put(
                "zte", listOf(
                    "com.zte.heartyservice/.autorun.AppAutoRunManager", "com.zte.heartyservice"
                )
            )

            //金立
            put(
                "F", listOf(
                    "com.gionee.softmanager/.MainActivity", "com.gionee.softmanager"
                )
            )

            //以下为未确定(厂商名也不确定)
            put(
                "smartisanos", listOf(
                    "com.smartisanos.security/.invokeHistory.InvokeHistoryActivity", "com.smartisanos.security"
                )
            )

            //360
            put(
                "360", listOf(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity", "com.yulong.android.coolsafe"
                )
            )

            //360
            put(
                "ulong", listOf(
                    "com.yulong.android.coolsafe/.ui.activity.autorun.AutoRunListActivity", "com.yulong.android.coolsafe"
                )
            )

            //酷派
            put(
                "coolpad" /*厂商名称不确定是否正确*/, listOf(
                    "com.yulong.android.security/com.yulong.android.seccenter.tabbarmain", "com.yulong.android.security"
                )
            )

            //联想
            put(
                "lenovo" /*厂商名称不确定是否正确*/, listOf(
                    "com.lenovo.security/.purebackground.PureBackgroundActivity", "com.lenovo.security"
                )
            )
            put(
                "htc" /*厂商名称不确定是否正确*/, listOf(
                    "com.htc.pitroad/.landingpage.activity.LandingPageActivity", "com.htc.pitroad"
                )
            )

            //华硕
            put(
                "asus" /*厂商名称不确定是否正确*/, listOf(
                    "com.asus.mobilemanager/.MainActivity", "com.asus.mobilemanager"
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
                            Log.e("Util", "******************e:" + e.message)
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
                Log.e("Util", "******************e:" + e.message)
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
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

}
