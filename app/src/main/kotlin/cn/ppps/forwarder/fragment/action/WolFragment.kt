package cn.ppps.forwarder.fragment.action

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.databinding.FragmentTasksActionWolBinding
import cn.ppps.forwarder.entity.MsgInfo
import cn.ppps.forwarder.entity.TaskSetting
import cn.ppps.forwarder.entity.action.WolSetting
import cn.ppps.forwarder.utils.KEY_BACK_DATA_ACTION
import cn.ppps.forwarder.utils.KEY_BACK_DESCRIPTION_ACTION
import cn.ppps.forwarder.utils.KEY_EVENT_DATA_ACTION
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.TASK_ACTION_WOL
import cn.ppps.forwarder.utils.TaskWorker
import cn.ppps.forwarder.utils.XToastUtils
import cn.ppps.forwarder.workers.ActionWorker
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import java.util.Date

@Page(name = "Wol")
@Suppress("PrivatePropertyName", "DEPRECATION")
class WolFragment : BaseFragment<FragmentTasksActionWolBinding?>(), View.OnClickListener {

    private val TAG: String = WolFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_EVENT_DATA_ACTION)
    var eventData: String? = null

    private var description = ""
    private var mac = ""
    private var ip = ""
    private var port = ""
    private var wakeMethod = 0 // 0: 通过本地服务API, 1: 直接发送幻数据包

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentTasksActionWolBinding {
        return FragmentTasksActionWolBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.task_wol)
        return titleBar
    }

    /**
     * 初始化控件
     */
    @SuppressLint("SetTextI18n")
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, 1)
        mCountDownHelper!!.setOnCountDownListener(object :
            CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        Log.d(TAG, "initViews eventData:$eventData")
        if (eventData != null) {
            val settingVo = Gson().fromJson(eventData, WolSetting::class.java)
            Log.d(TAG, "initViews settingVo:$settingVo")

            mac = settingVo.mac
            ip = settingVo.ip
            port = settingVo.port
            wakeMethod = settingVo.wakeMethod
        }

        binding!!.etMac.setText(mac)
        binding!!.etIp.setText(ip)
        binding!!.etPort.setText(port)
        // 设置唤醒方式
        if (wakeMethod == 1) {
            binding!!.rbDirect.isChecked = true
        } else {
            binding!!.rbApi.isChecked = true
        }
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

    @SuppressLint("SetTextI18n")
    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {
                R.id.btn_test -> {
                    mCountDownHelper?.start()

                    try {
                        val settingVo = checkSetting()
                        Log.d(TAG, settingVo.toString())
                        val taskAction = TaskSetting(
                            TASK_ACTION_WOL,
                            getString(R.string.task_wol),
                            settingVo.description,
                            Gson().toJson(settingVo),
                            requestCode
                        )
                        val taskActionsJson = Gson().toJson(arrayListOf(taskAction))
                        val msgInfo = MsgInfo(
                            "task",
                            getString(R.string.task_wol),
                            settingVo.description,
                            Date(),
                            getString(R.string.task_wol)
                        )
                        val actionData = Data.Builder().putLong(TaskWorker.TASK_ID, 0)
                            .putString(TaskWorker.TASK_ACTIONS, taskActionsJson)
                            .putString(TaskWorker.MSG_INFO, Gson().toJson(msgInfo))
                            .build()
                        val actionRequest = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(actionData).build()
                        WorkManager.getInstance().enqueue(actionRequest)
                    } catch (e: Exception) {
                        mCountDownHelper?.finish()
                        e.printStackTrace()
                        Log.e(TAG, "onClick error: ${e.message}")
                        XToastUtils.error(e.message.toString(), 30000)
                    }
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
                    setFragmentResult(TASK_ACTION_WOL, intent)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString(), 30000)
            e.printStackTrace()
            Log.e(TAG, "onClick error: ${e.message}")
        }
    }

    //检查设置
    @SuppressLint("SetTextI18n")
    private fun checkSetting(): WolSetting {
        mac = binding!!.etMac.text.toString().trim()
        val macRegex = getString(R.string.mac_regex).toRegex()
        if (!macRegex.matches(mac)) {
            throw Exception(getString(R.string.mac_error))
        }

        ip = binding!!.etIp.text.toString().trim()
        val ipRegex = getString(R.string.ip_regex).toRegex()
        if (!ip.isNullOrEmpty() && !ipRegex.matches(ip)) {
            throw Exception(getString(R.string.ip_error))
        }

        port = binding!!.etPort.text.toString().trim()
        val portRegex = getString(R.string.wol_port_regex).toRegex()
        if (!port.isNullOrEmpty() && !portRegex.matches(port)) {
            throw Exception(getString(R.string.wol_port_error))
        }

        // 获取唤醒方式
        wakeMethod = if (binding!!.rbDirect.isChecked) 1 else 0

        description = String.format(getString(R.string.wol_description), mac)

        return WolSetting(description, mac, ip, port, wakeMethod)
    }
}
