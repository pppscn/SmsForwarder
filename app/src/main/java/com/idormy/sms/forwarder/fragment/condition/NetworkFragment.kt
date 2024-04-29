package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionNetworkBinding
import com.idormy.sms.forwarder.entity.condition.NetworkSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_NETWORK
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar

@Page(name = "Network")
@Suppress("PrivatePropertyName", "SameParameterValue")
class NetworkFragment : BaseFragment<FragmentTasksConditionNetworkBinding?>(), View.OnClickListener {

    private val TAG: String = NetworkFragment::class.java.simpleName
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
    ): FragmentTasksConditionNetworkBinding {
        return FragmentTasksConditionNetworkBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_network)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {

        binding!!.rgNetworkState.setOnCheckedChangeListener { _, checkedId ->
            Log.d(TAG, "rgNetworkState checkedId:$checkedId")
            binding!!.layoutDataSimSlot.visibility = if (checkedId == R.id.rb_net_mobile && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) View.VISIBLE else View.GONE
            binding!!.layoutWifiSsid.visibility = if (checkedId == R.id.rb_net_wifi) View.VISIBLE else View.GONE
            checkSetting(true)
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, NetworkSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.tvDescription.text = settingVo.description
            binding!!.rgDataSimSlot.check(settingVo.getDataSimSlotCheckId())
            binding!!.etWifiSsid.setText(settingVo.wifiSsid)
            binding!!.rgNetworkState.check(settingVo.getNetworkStateCheckId())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.rgDataSimSlot.setOnCheckedChangeListener { _, _ ->
            checkSetting(true)
        }
        binding!!.etWifiSsid.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkSetting(true)
            }
        })
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
                    setFragmentResult(TASK_CONDITION_NETWORK, intent)
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
    private fun checkSetting(updateView: Boolean = false): NetworkSetting {
        val networkStateCheckId = binding!!.rgNetworkState.checkedRadioButtonId
        val dataSimSlotCheckId = binding!!.rgDataSimSlot.checkedRadioButtonId
        val wifiSsid = binding!!.etWifiSsid.text.toString().trim()
        val settingVo = NetworkSetting(networkStateCheckId, dataSimSlotCheckId, wifiSsid)

        if (updateView) {
            binding!!.tvDescription.text = settingVo.description
        }

        return settingVo
    }
}