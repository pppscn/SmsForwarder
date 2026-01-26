package cn.ppps.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import cn.ppps.forwarder.R
import cn.ppps.forwarder.adapter.BluetoothRecyclerAdapter
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.databinding.FragmentTasksConditionBluetoothBinding
import cn.ppps.forwarder.entity.condition.BluetoothSetting
import cn.ppps.forwarder.service.BluetoothScanService
import cn.ppps.forwarder.utils.ACTION_START
import cn.ppps.forwarder.utils.KEY_BACK_DATA_CONDITION
import cn.ppps.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import cn.ppps.forwarder.utils.KEY_EVENT_DATA_CONDITION
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.TASK_CONDITION_BLUETOOTH
import cn.ppps.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog


@Page(name = "Bluetooth")
@Suppress("PrivatePropertyName", "SameParameterValue", "DEPRECATION")
class BluetoothFragment : BaseFragment<FragmentTasksConditionBluetoothBinding?>(), View.OnClickListener {

    private val TAG: String = BluetoothFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothRecyclerAdapter: BluetoothRecyclerAdapter
    private var discoveredDevices: MutableList<BluetoothDevice> = mutableListOf()
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action

            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d(TAG, "Discovered device: ${it.name} - ${it.address}")
                        if (!discoveredDevices.contains(it)) {
                            discoveredDevices.add(it)
                            bluetoothRecyclerAdapter.notifyDataSetChanged()
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Bluetooth scan finished, discoveredDevices: $discoveredDevices")
                }
            }
        }
    }

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_CONDITION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksConditionBluetoothBinding {
        return FragmentTasksConditionBluetoothBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_bluetooth)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnStartDiscovery, 12)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnStartDiscovery.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                requireActivity().unregisterReceiver(bluetoothReceiver)
                binding!!.btnStartDiscovery.text = getString(R.string.start_discovery)
            }
        })

        binding!!.rgBluetoothAction.setOnCheckedChangeListener { _, checkedId ->
            Log.d(TAG, "rgBluetoothState checkedId:$checkedId")
            when (checkedId) {
                R.id.rb_action_state_changed -> {
                    binding!!.layoutBluetoothState.visibility = View.VISIBLE
                    binding!!.layoutDiscoveryFinished.visibility = View.GONE
                    binding!!.layoutDeviceAddress.visibility = View.GONE
                }

                R.id.rb_action_discovery_finished -> {
                    binding!!.layoutBluetoothState.visibility = View.GONE
                    binding!!.layoutDiscoveryFinished.visibility = View.VISIBLE
                    binding!!.layoutDeviceAddress.visibility = View.VISIBLE
                }

                else -> {
                    binding!!.layoutBluetoothState.visibility = View.GONE
                    binding!!.layoutDiscoveryFinished.visibility = View.GONE
                    binding!!.layoutDeviceAddress.visibility = View.VISIBLE
                }
            }
            checkSetting(true)
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, BluetoothSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.tvDescription.text = settingVo.description
            binding!!.rgBluetoothAction.check(settingVo.getActionCheckId())
            binding!!.rgBluetoothState.check(settingVo.getStateCheckId())
            binding!!.rgDiscoveryResult.check(settingVo.getResultCheckId())
            binding!!.etDeviceAddress.setText(settingVo.device)
        } else {
            binding!!.rgBluetoothAction.check(R.id.rb_action_state_changed)
            binding!!.rgBluetoothState.check(R.id.rb_state_on)
            binding!!.rgDiscoveryResult.check(R.id.rb_discovered)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnStartDiscovery.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.rgBluetoothState.setOnCheckedChangeListener { _, _ ->
            checkSetting(true)
        }
        binding!!.rgDiscoveryResult.setOnCheckedChangeListener { _, _ ->
            checkSetting(true)
        }
        binding!!.etDeviceAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkSetting(true)
            }
        })

        binding!!.recyclerDevices.layoutManager = LinearLayoutManager(requireContext())
        bluetoothRecyclerAdapter = BluetoothRecyclerAdapter(discoveredDevices, { position ->
            val device = discoveredDevices[position]
            binding!!.etDeviceAddress.setText(device.address)
        })
        binding!!.recyclerDevices.adapter = bluetoothRecyclerAdapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        @Suppress("SENSELESS_COMPARISON")
        if (bluetoothAdapter == null) {
            XToastUtils.error(getString(R.string.bluetooth_not_supported))
            return
        }

        // 启动蓝牙搜索
        // startBluetoothDiscovery()
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        if (bluetoothReceiver.isOrderedBroadcast) {
            requireActivity().unregisterReceiver(bluetoothReceiver)
        }
        super.onDestroyView()
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {

                R.id.btn_start_discovery -> {
                    if (!SettingUtils.enableBluetooth) {
                        MaterialDialog.Builder(requireContext())
                            .iconRes(R.drawable.auto_task_icon_location)
                            .title(R.string.enable_bluetooth)
                            .content(R.string.enable_bluetooth_dialog)
                            .cancelable(false)
                            .positiveText(R.string.lab_yes)
                            .negativeText(R.string.lab_no)
                            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                                XXPermissions.with(this)
                                    .permission(PermissionLists.getBluetoothScanPermission())
                                    .permission(PermissionLists.getBluetoothConnectPermission())
                                    .permission(PermissionLists.getBluetoothAdvertisePermission())
                                    .permission(PermissionLists.getAccessFineLocationPermission())
                                    .request(object : OnPermissionCallback {
                                        override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                                            val allGranted = deniedList.isEmpty()
                                            if (!allGranted) {
                                                // 判断请求失败的权限是否被用户勾选了不再询问的选项
                                                val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                                                if (doNotAskAgain) {
                                                    XToastUtils.error(getString(R.string.toast_denied_never))
                                                    // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                                    XXPermissions.startPermissionActivity(requireContext(), deniedList)
                                                }
                                                // 处理权限请求失败的逻辑
                                                XToastUtils.error(getString(R.string.toast_denied))
                                                return
                                            }
                                            // 处理权限请求成功的逻辑
                                            startBluetoothDiscovery()
                                            SettingUtils.enableBluetooth = true
                                            val serviceIntent = Intent(requireContext(), BluetoothScanService::class.java)
                                            serviceIntent.action = ACTION_START
                                            requireContext().startService(serviceIntent)
                                        }
                                    })
                            }.show()
                        return
                    }

                    startBluetoothDiscovery()
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, settingVo.description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_CONDITION_BLUETOOTH, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
            Log.e(TAG, "onClick error:$e")
        }
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun startBluetoothDiscovery() {
        try {
            mCountDownHelper?.start()

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            // 注册广播接收器
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            requireActivity().registerReceiver(bluetoothReceiver, filter)

            discoveredDevices.clear()
            bluetoothRecyclerAdapter.notifyDataSetChanged()
            bluetoothAdapter.startDiscovery()
        } catch (e: Exception) {
            mCountDownHelper?.finish()
            XToastUtils.error(e.message.toString(), 30000)
            Log.e(TAG, "startBluetoothDiscovery error:$e")
        }
    }

    //检查设置
    private fun checkSetting(updateView: Boolean = false): BluetoothSetting {
        val actionCheckId = binding!!.rgBluetoothAction.checkedRadioButtonId
        val deviceAddress = binding!!.etDeviceAddress.text.toString().trim()
        if (actionCheckId != R.id.rb_action_state_changed &&
            (deviceAddress.isEmpty() || !BluetoothAdapter.checkBluetoothAddress(deviceAddress))
        ) {
            if (updateView) {
                binding!!.etDeviceAddress.error = getString(R.string.mac_error)
            } else {
                throw Exception(getString(R.string.invalid_bluetooth_mac_address))
            }
        } else {
            binding!!.etDeviceAddress.error = null
        }

        val stateCheckId = binding!!.rgBluetoothState.checkedRadioButtonId
        val resultCheckId = binding!!.rgDiscoveryResult.checkedRadioButtonId
        val settingVo = BluetoothSetting(actionCheckId, stateCheckId, resultCheckId, deviceAddress)
        if (updateView) {
            binding!!.tvDescription.text = settingVo.description
        }

        return settingVo
    }
}