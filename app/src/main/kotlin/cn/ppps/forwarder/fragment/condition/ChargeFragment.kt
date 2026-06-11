package cn.ppps.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.databinding.FragmentTasksConditionChargeBinding
import cn.ppps.forwarder.entity.condition.ChargeSetting
import cn.ppps.forwarder.utils.KEY_BACK_DATA_CONDITION
import cn.ppps.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import cn.ppps.forwarder.utils.KEY_EVENT_DATA_CONDITION
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.TASK_CONDITION_CHARGE
import cn.ppps.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox

@Page(name = "Charge")
@Suppress("PrivatePropertyName", "SameParameterValue")
class ChargeFragment : BaseFragment<FragmentTasksConditionChargeBinding?>(), View.OnClickListener {

    private val TAG: String = ChargeFragment::class.java.simpleName
    private var titleBar: TitleBar? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_CONDITION)
    var eventData: String? = null

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksConditionChargeBinding {
        return FragmentTasksConditionChargeBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_charge)
        return titleBar
    }

    override fun initViews() {
        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, ChargeSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.tvDescription.text = settingVo.description
            val statusIds = settingVo.getStatusCheckIds()
            binding!!.cbBatteryCharging.isChecked = R.id.cb_battery_charging in statusIds
            binding!!.cbBatteryDischarging.isChecked = R.id.cb_battery_discharging in statusIds
            binding!!.cbBatteryNotCharging.isChecked = R.id.cb_battery_not_charging in statusIds
            binding!!.cbBatteryFull.isChecked = R.id.cb_battery_full in statusIds
            binding!!.cbBatteryUnknown.isChecked = R.id.cb_battery_unknown in statusIds
            val pluggedIds = settingVo.getPluggedCheckIds()
            binding!!.cbPluggedAc.isChecked = R.id.cb_plugged_ac in pluggedIds
            binding!!.cbPluggedUsb.isChecked = R.id.cb_plugged_usb in pluggedIds
            binding!!.cbPluggedWireless.isChecked = R.id.cb_plugged_wireless in pluggedIds
            binding!!.cbPluggedUnlimited.isChecked = R.id.cb_plugged_unlimited in pluggedIds
        } else {
            // 新建任务的默认值（XML 的 android:checked 对 SmoothCheckBox 不一定生效）
            binding!!.cbBatteryCharging.isChecked = true
            binding!!.cbPluggedUnlimited.isChecked = true
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        val listener = SmoothCheckBox.OnCheckedChangeListener { _, _ -> checkSetting(true) }
        listOf(
            binding!!.cbBatteryCharging, binding!!.cbBatteryDischarging,
            binding!!.cbBatteryNotCharging, binding!!.cbBatteryFull, binding!!.cbBatteryUnknown,
            binding!!.cbPluggedAc, binding!!.cbPluggedUsb,
            binding!!.cbPluggedWireless, binding!!.cbPluggedUnlimited
        ).forEach { cb -> cb.setOnCheckedChangeListener(listener) }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_del -> {
                    popToBack()
                    return
                }
                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, settingVo.description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_CONDITION_CHARGE, intent)
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

    private fun checkSetting(updateView: Boolean = false): ChargeSetting {
        val statusCheckIds = mutableListOf<Int>()
        if (binding!!.cbBatteryCharging.isChecked) statusCheckIds.add(R.id.cb_battery_charging)
        if (binding!!.cbBatteryDischarging.isChecked) statusCheckIds.add(R.id.cb_battery_discharging)
        if (binding!!.cbBatteryNotCharging.isChecked) statusCheckIds.add(R.id.cb_battery_not_charging)
        if (binding!!.cbBatteryFull.isChecked) statusCheckIds.add(R.id.cb_battery_full)
        if (binding!!.cbBatteryUnknown.isChecked) statusCheckIds.add(R.id.cb_battery_unknown)

        if (!updateView && statusCheckIds.isEmpty()) {
            throw Exception(getString(R.string.battery_status_required))
        }

        val pluggedCheckIds = mutableListOf<Int>()
        if (binding!!.cbPluggedAc.isChecked) pluggedCheckIds.add(R.id.cb_plugged_ac)
        if (binding!!.cbPluggedUsb.isChecked) pluggedCheckIds.add(R.id.cb_plugged_usb)
        if (binding!!.cbPluggedWireless.isChecked) pluggedCheckIds.add(R.id.cb_plugged_wireless)
        if (binding!!.cbPluggedUnlimited.isChecked) pluggedCheckIds.add(R.id.cb_plugged_unlimited)

        val settingVo = ChargeSetting(statusCheckIds = statusCheckIds, pluggedCheckIds = pluggedCheckIds)
        if (updateView) binding!!.tvDescription.text = settingVo.description
        return settingVo
    }
}
