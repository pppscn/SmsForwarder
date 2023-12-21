package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionChargeBinding
import com.idormy.sms.forwarder.entity.condition.ChargeSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_CHARGE
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar

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

    /**
     * 初始化控件
     */
    override fun initViews() {
        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, ChargeSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.tvDescription.text = settingVo.description
            binding!!.rgStatus.check(settingVo.getStatusCheckId())
            binding!!.rgPlugged.check(settingVo.getPluggedCheckId())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.rgStatus.setOnCheckedChangeListener { _, _ ->
            checkSetting(true)
        }
        binding!!.rgPlugged.setOnCheckedChangeListener { _, _ ->
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

    //检查设置
    private fun checkSetting(updateView: Boolean = false): ChargeSetting {
        val statusCheckId = binding!!.rgStatus.checkedRadioButtonId
        val pluggedCheckId = binding!!.rgPlugged.checkedRadioButtonId
        val settingVo = ChargeSetting(statusCheckId = statusCheckId, pluggedCheckId = pluggedCheckId)

        if (updateView) {
            binding!!.tvDescription.text = settingVo.description
        }

        return settingVo
    }
}