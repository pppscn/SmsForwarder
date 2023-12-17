package com.idormy.sms.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentTasksActionFrpcBinding
import com.idormy.sms.forwarder.entity.action.FrpcSetting
import com.idormy.sms.forwarder.utils.KEY_BACK_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import com.idormy.sms.forwarder.utils.KEY_EVENT_DATA_ACTION
import com.idormy.sms.forwarder.utils.KEY_TEST_ACTION
import com.idormy.sms.forwarder.utils.TASK_ACTION_FRPC
import com.idormy.sms.forwarder.utils.XToastUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar

@Page(name = "Frpc")
@Suppress("PrivatePropertyName")
class FrpcFragment : BaseFragment<FragmentTasksActionFrpcBinding?>(), View.OnClickListener {

    private val TAG: String = FrpcFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    private var description = "测试描述"

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionFrpcBinding {
        return FragmentTasksActionFrpcBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_frpc)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 3)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        binding!!.rgFrpcState.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_start_server -> {
                    binding!!.layoutStartServer.visibility = View.VISIBLE
                    binding!!.layoutStopServer.visibility = View.GONE
                }

                R.id.rb_stop_server -> {
                    binding!!.layoutStartServer.visibility = View.GONE
                    binding!!.layoutStopServer.visibility = View.VISIBLE
                }
            }
        }

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, FrpcSetting::class.java)
            binding!!.etStartUid.setText(settingVo.uids)
            binding!!.etStopUid.setText(settingVo.uids)
            binding!!.rgFrpcState.check(if (settingVo.action == "start") R.id.rb_start_server else R.id.rb_stop_server)
            Log.d(TAG, "initViews settingVo:$settingVo")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        LiveEventBus.get(KEY_TEST_ACTION, String::class.java).observe(this) {
            mCountDownHelper?.finish()

            if (it == "success") {
                XToastUtils.success("测试通过", 30000)
            } else {
                XToastUtils.error(it, 30000)
            }
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    Thread {
                        try {
                            val settingVo = checkSetting()
                            Log.d(TAG, settingVo.toString())
                            LiveEventBus.get(KEY_TEST_ACTION, String::class.java).post("success")
                        } catch (e: Exception) {
                            LiveEventBus.get(KEY_TEST_ACTION, String::class.java).post(e.message.toString())
                            e.printStackTrace()
                        }
                    }.start()
                    return
                }

                R.id.btn_del -> {
                    popToBack()
                    return
                }

                R.id.btn_save -> {
                    val settingVo = checkSetting()
                    val intent = Intent()
                    intent.putExtra(KEY_BACK_DESCRIPTION_ACTION, description)
                    intent.putExtra(KEY_BACK_DATA_ACTION, Gson().toJson(settingVo))
                    setFragmentResult(TASK_ACTION_FRPC, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
        }
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): FrpcSetting {
        val startUid = binding!!.etStartUid.text.toString().trim()
        val stopUid = binding!!.etStopUid.text.toString().trim()
        val action: String
        val uids: String
        if (binding!!.rgFrpcState.checkedRadioButtonId == R.id.rb_start_server) {
            description = if (startUid == "") "启动全部自启动的Frpc" else "启动UID为${startUid}的Frpc"
            action = "start"
            uids = startUid
        } else {
            description = if (stopUid == "") "停止全部自启动的Frpc" else "停止UID为${stopUid}的Frpc"
            action = "stop"
            uids = stopUid
        }
        return FrpcSetting(description, action, uids)
    }
}