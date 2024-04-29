package com.idormy.sms.forwarder.fragment.condition

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksConditionLockScreenBinding
import com.idormy.sms.forwarder.entity.condition.LockScreenSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_CONDITION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_CONDITION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_CONDITION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.TASK_CONDITION_LOCK_SCREEN
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar

@Page(name = "LockScreen")
@Suppress("PrivatePropertyName", "SameParameterValue")
class LockScreenFragment : BaseFragment<FragmentTasksConditionLockScreenBinding?>(), View.OnClickListener {

    private val TAG: String = LockScreenFragment::class.java.simpleName
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
    ): FragmentTasksConditionLockScreenBinding {
        return FragmentTasksConditionLockScreenBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_lock_screen)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        binding!!.rgAction.setOnCheckedChangeListener { _, checkedId ->
            binding!!.xsbTimeAfterScreenOff.visibility = View.GONE
            binding!!.xsbTimeAfterScreenLocked.visibility = View.GONE
            binding!!.xsbTimeAfterScreenOn.visibility = View.GONE
            binding!!.xsbTimeAfterScreenUnlocked.visibility = View.GONE
            when (checkedId) {
                R.id.rb_action_screen_on -> binding!!.xsbTimeAfterScreenOn.visibility = View.VISIBLE
                R.id.rb_action_screen_unlocked -> binding!!.xsbTimeAfterScreenUnlocked.visibility = View.VISIBLE
                R.id.rb_action_screen_locked -> binding!!.xsbTimeAfterScreenLocked.visibility = View.VISIBLE
                else -> binding!!.xsbTimeAfterScreenOff.visibility = View.VISIBLE
            }
            checkSetting(true)
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, LockScreenSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")
            binding!!.tvDescription.text = settingVo.description
            binding!!.xsbTimeAfterScreenOff.setDefaultValue(settingVo.timeAfterScreenOff)
            binding!!.xsbTimeAfterScreenOn.setDefaultValue(settingVo.timeAfterScreenOn)
            binding!!.xsbTimeAfterScreenLocked.setDefaultValue(settingVo.timeAfterScreenLocked)
            binding!!.xsbTimeAfterScreenUnlocked.setDefaultValue(settingVo.timeAfterScreenUnlocked)
            binding!!.rgAction.check(settingVo.getActionCheckId())
            binding!!.sbCheckAgain.isChecked = settingVo.checkAgain
        } else {
            binding!!.xsbTimeAfterScreenOff.setDefaultValue(0)
            binding!!.xsbTimeAfterScreenOn.setDefaultValue(0)
            binding!!.xsbTimeAfterScreenLocked.setDefaultValue(0)
            binding!!.xsbTimeAfterScreenUnlocked.setDefaultValue(0)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.xsbTimeAfterScreenOff.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.xsbTimeAfterScreenOn.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.xsbTimeAfterScreenLocked.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.xsbTimeAfterScreenUnlocked.setOnSeekBarListener { _, _ ->
            checkSetting(true)
        }
        binding!!.sbCheckAgain.setOnCheckedChangeListener { _, _ ->
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
                    setFragmentResult(TASK_CONDITION_LOCK_SCREEN, intent)
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
    private fun checkSetting(updateView: Boolean = false): LockScreenSetting {
        val actionCheckId = binding!!.rgAction.checkedRadioButtonId
        val timeAfterScreenOff = binding!!.xsbTimeAfterScreenOff.selectedNumber
        val timeAfterScreenOn = binding!!.xsbTimeAfterScreenOn.selectedNumber
        val timeAferScreenLocked = binding!!.xsbTimeAfterScreenLocked.selectedNumber
        val timeAfterScreenUnlocked = binding!!.xsbTimeAfterScreenUnlocked.selectedNumber
        val checkAgain = binding!!.sbCheckAgain.isChecked
        val settingVo = LockScreenSetting(actionCheckId, timeAfterScreenOff, timeAfterScreenOn, timeAferScreenLocked, timeAfterScreenUnlocked, checkAgain)

        if (updateView) {
            binding!!.tvDescription.text = settingVo.description
        }

        return settingVo
    }
}