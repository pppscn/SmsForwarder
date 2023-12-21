package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.os.BatteryManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionBatteryBinding
import com.idormy.sms.forwarder.entity.condition.BatterySetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BATTERY
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar

@Page(name = "Battery")
@Suppress("PrivatePropertyName", "SameParameterValue")
class BatteryFragment : BaseFragment<FragmentTasksConditionBatteryBinding?>(), View.OnClickListener {

    private val TAG: String = BatteryFragment::class.java.simpleName
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
    ): FragmentTasksConditionBatteryBinding {
        return FragmentTasksConditionBatteryBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_battery)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        binding!!.rgStatus.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_battery_discharging) {
                binding!!.xsbLevelMin.visibility = View.VISIBLE
                binding!!.xsbLevelMax.visibility = View.GONE
            } else {
                binding!!.xsbLevelMin.visibility = View.GONE
                binding!!.xsbLevelMax.visibility = View.VISIBLE
            }
            checkSetting(true)
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, BatterySetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.tvDescription.text = settingVo.description
            binding!!.xsbLevelMin.setDefaultValue(settingVo.levelMin)
            binding!!.xsbLevelMax.setDefaultValue(settingVo.levelMax)
            binding!!.sbKeepReminding.isChecked = settingVo.keepReminding
            binding!!.rgStatus.check(settingVo.getStatusCheckId())
        } else {
            binding!!.xsbLevelMin.setDefaultValue(10)
            binding!!.xsbLevelMax.setDefaultValue(90)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.xsbLevelMin.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.xsbLevelMax.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.sbKeepReminding.setOnCheckedChangeListener { _, _ ->
            checkSetting(true)
        }
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
                    intent.putExtra(KEY_BACK_DESCRIPTION_CONDITION, description)
                    intent.putExtra(KEY_BACK_DATA_CONDITION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_CONDITION_BATTERY, intent)
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
    private fun checkSetting(updateView: Boolean = false): BatterySetting {
        val levelMin = binding!!.xsbLevelMin.selectedNumber
        val levelMax = binding!!.xsbLevelMax.selectedNumber
        val keepReminding = binding!!.sbKeepReminding.isChecked
        val status: Int = when (binding!!.rgStatus.checkedRadioButtonId) {
            R.id.rb_battery_discharging -> {
                description = if (keepReminding) {
                    String.format(getString(R.string.battery_discharged_below), levelMin.toString())
                } else {
                    String.format(getString(R.string.battery_discharged_to), levelMin.toString())
                }
                BatteryManager.BATTERY_STATUS_DISCHARGING
            }

            else -> {
                description = if (keepReminding) {
                    String.format(getString(R.string.battery_charged_above), levelMax.toString())
                } else {
                    String.format(getString(R.string.battery_charged_to), levelMax.toString())
                }
                BatteryManager.BATTERY_STATUS_CHARGING
            }
        }

        if (updateView) {
            binding!!.tvDescription.text = description
        }

        return BatterySetting(description, status, levelMin, levelMax, keepReminding)
    }
}