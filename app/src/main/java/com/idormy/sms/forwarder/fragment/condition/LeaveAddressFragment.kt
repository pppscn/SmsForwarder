package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionLeaveAddressBinding
import com.idormy.sms.forwarder.entity.condition.LocationSetting
import com.idormy.sms.forwarder.service.LocationService
import com.idormy.sms.forwarder.utils.ACTION_START
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LEAVE_ADDRESS
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog

@Page(name = "LeaveAddress")
@Suppress("PrivatePropertyName", "SameParameterValue")
class LeaveAddressFragment : BaseFragment<FragmentTasksConditionLeaveAddressBinding?>(), View.OnClickListener {

    private val TAG: String = LeaveAddressFragment::class.java.simpleName
    private var titleBar: TitleBar? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_CONDITION)
    var eventData: String? = null

    private var description = ""

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksConditionLeaveAddressBinding {
        return FragmentTasksConditionLeaveAddressBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_leave_address)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        binding!!.rgCalcType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_calc_type_distance) {
                binding!!.layoutCalcTypeDistance.visibility = View.VISIBLE
                binding!!.layoutCalcTypeAddress.visibility = View.GONE
            } else {
                binding!!.layoutCalcTypeDistance.visibility = View.GONE
                binding!!.layoutCalcTypeAddress.visibility = View.VISIBLE
            }
            try {
                checkSetting(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "initViews error:$e")
            }
        }

        var settingVo = LocationSetting(getString(R.string.task_leave_address_tips), "leave")
        if (eventData != null) {
            Log.d(TAG, "initViews eventData:$eventData")
            settingVo = Gson().fromJson(eventData, LocationSetting::class.java)
        }
        Log.d(TAG, "initViews settingVo:$settingVo")
        binding!!.tvDescription.text = settingVo.description
        binding!!.etLongitude.setText(settingVo.longitude.toString())
        binding!!.etLatitude.setText(settingVo.latitude.toString())
        binding!!.etDistance.setText(settingVo.distance.toString())
        binding!!.etAddress.setText(settingVo.address)
        binding!!.rgCalcType.check(settingVo.getCalcTypeCheckId())
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.btnCurrentCoordinates.setOnClickListener(this)
        binding!!.etLongitude.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val inputText = binding!!.etLongitude.text.toString()
                    if (inputText.isEmpty()) {
                        binding!!.etLongitude.setText("0")
                        binding!!.etLongitude.setSelection(binding!!.etLongitude.text.length) // 将光标移至文本末尾
                    } else {
                        checkSetting(true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "etLongitude error:$e")
                }
            }
        }
        binding!!.etLatitude.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val inputText = binding!!.etLatitude.text.toString()
                    if (inputText.isEmpty()) {
                        binding!!.etLatitude.setText("0")
                        binding!!.etLatitude.setSelection(binding!!.etLatitude.text.length) // 将光标移至文本末尾
                    } else {
                        checkSetting(true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "etLatitude error:$e")
                }
            }
        }
        binding!!.etDistance.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val inputText = binding!!.etDistance.text.toString()
                    if (inputText.isEmpty()) {
                        binding!!.etDistance.setText("1")
                        binding!!.etDistance.setSelection(binding!!.etDistance.text.length) // 将光标移至文本末尾
                    } else {
                        checkSetting(true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "etDistance error:$e")
                }
            }
        }
        binding!!.etAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    checkSetting(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "etAddress error:$e")
                }
            }
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_current_coordinates -> {
                    if (!App.LocationClient.isStarted()) {
                        MaterialDialog.Builder(requireContext())
                            .iconRes(R.drawable.auto_task_icon_location)
                            .title(R.string.enable_location)
                            .content(R.string.enable_location_dialog)
                            .cancelable(false)
                            .positiveText(R.string.lab_yes)
                            .negativeText(R.string.lab_no)
                            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                                SettingUtils.enableLocation = true
                                val serviceIntent = Intent(requireContext(), LocationService::class.java)
                                serviceIntent.action = ACTION_START
                                requireContext().startService(serviceIntent)
                            }.show()
                        return
                    }

                    val location = HttpServerUtils.apiLocationCache
                    if (location.latitude == 0.0 || location.longitude == 0.0) {
                        XToastUtils.error(getString(R.string.location_failed), 30000)
                        return
                    }

                    binding!!.etLatitude.setText(location.latitude.toString())
                    binding!!.etLongitude.setText(location.longitude.toString())
                    XToastUtils.success(String.format(getString(R.string.current_address), location.address), 30000)
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_CONDITION_LEAVE_ADDRESS, intent)
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

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(updateView: Boolean = false): LocationSetting {
        val longitude = binding!!.etLongitude.text.toString().toDouble()
        val latitude = binding!!.etLatitude.text.toString().toDouble()
        val distance = binding!!.etDistance.text.toString().toDouble()
        val address = binding!!.etAddress.text.toString()
        val calcType = when (binding!!.rgCalcType.checkedRadioButtonId) {
            R.id.rb_calc_type_distance -> {
                Log.d(TAG, "longitude:$longitude latitude:$latitude distance:$distance")
                if (latitude.isNaN() || longitude.isNaN() || distance.isNaN()) {
                    throw Exception(getString(R.string.calc_type_distance_error))
                }
                description = String.format(getString(R.string.leave_address_distance_description), longitude, latitude, distance)
                "distance"
            }

            else -> {
                if (address.isEmpty()) {
                    throw Exception(getString(R.string.calc_type_address_error))
                }
                description = String.format(getString(R.string.leave_address_keyword_description), address)
                "address"
            }
        }

        val settingVo = LocationSetting(description, "leave", calcType, longitude, latitude, distance, address)

        if (updateView) {
            binding!!.tvDescription.text = description
        }

        return settingVo
    }
}