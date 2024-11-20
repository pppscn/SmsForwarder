package com.idormy.sms.forwarder.fragment.senders

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersSocketBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.SocketSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.SocketUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Date

@Page(name = "Socket")
@Suppress("PrivatePropertyName")
class SocketFragment : BaseFragment<FragmentSendersSocketBinding?>(), View.OnClickListener {

    private val TAG: String = SocketFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private var mCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_SENDER_ID)
    var senderId: Long = 0

    @JvmField
    @AutoWired(name = KEY_SENDER_TYPE)
    var senderType: Int = 0

    @JvmField
    @AutoWired(name = KEY_SENDER_CLONE)
    var isClone: Boolean = false

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSendersSocketBinding {
        return FragmentSendersSocketBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.socket)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, SettingUtils.requestTimeout)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        //新增
        if (senderId <= 0) {
            titleBar?.setSubTitle(getString(R.string.add_sender))
            binding!!.btnDel.setText(R.string.discard)
            return
        }

        //编辑
        binding!!.btnDel.setText(R.string.del)
        Core.sender.get(senderId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<Sender> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "onError:$e")
            }

            override fun onSuccess(sender: Sender) {
                if (isClone) {
                    titleBar?.setSubTitle(getString(R.string.clone_sender) + ": " + sender.name)
                    binding!!.btnDel.setText(R.string.discard)
                } else {
                    titleBar?.setSubTitle(getString(R.string.edit_sender) + ": " + sender.name)
                }
                binding!!.etName.setText(sender.name)
                binding!!.sbEnable.isChecked = sender.status == 1
                val settingVo = Gson().fromJson(sender.jsonSetting, SocketSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    val checkedId = settingVo.getMethodCheckId()
                    binding!!.rgMethod.check(checkedId)
                    binding!!.etAddress.setText(settingVo.address)
                    binding!!.etPort.setText(settingVo.port.toString())
                    binding!!.etMsgTemplate.setText(settingVo.msgTemplate)
                    binding!!.etSecret.setText(settingVo.secret)
                    binding!!.etResponse.setText(settingVo.response)
                    binding!!.etUsername.setText(settingVo.username)
                    binding!!.etPassword.setText(settingVo.password)
                    binding!!.etInCharset.setSelectedItem(settingVo.inCharset)
                    binding!!.etOutCharset.setSelectedItem(settingVo.outCharset)
                    binding!!.etInMessageTopic.setText(settingVo.inMessageTopic)
                    binding!!.etOutMessageTopic.setText(settingVo.outMessageTopic)
                    binding!!.rgUriType.check(settingVo.getUriTypeCheckId())
                    binding!!.etPath.setText(settingVo.path)
                    binding!!.etClientId.setText(settingVo.clientId)
                    binding!!.layoutMqtt.visibility = if (checkedId == R.id.rb_method_mqtt) View.VISIBLE else View.GONE
                }
            }
        })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.rgMethod.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            binding!!.layoutMqtt.visibility = if (checkedId == R.id.rb_method_mqtt) View.VISIBLE else View.GONE
        }
        LiveEventBus.get(KEY_SENDER_TEST, String::class.java).observe(this) { mCountDownHelper?.finish() }
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
                            val name = binding!!.etName.text.toString().trim().takeIf { it.isNotEmpty() } ?: getString(R.string.test_sender_name)
                            val msgInfo = MsgInfo("sms", getString(R.string.test_phone_num), String.format(getString(R.string.test_sender_sms), name), Date(), getString(R.string.test_sim_info))
                            SocketUtils.sendMsg(settingVo, msgInfo)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, "onClick: $e")
                            LiveEventBus.get(EVENT_TOAST_ERROR, String::class.java).post(e.message.toString())
                        }
                        LiveEventBus.get(KEY_SENDER_TEST, String::class.java).post("finish")
                    }.start()
                    return
                }

                R.id.btn_del -> {
                    if (senderId <= 0 || isClone) {
                        popToBack()
                        return
                    }

                    MaterialDialog.Builder(requireContext()).title(R.string.delete_sender_title).content(R.string.delete_sender_tips).positiveText(R.string.lab_yes).negativeText(R.string.lab_no).onPositive { _: MaterialDialog?, _: DialogAction? ->
                        viewModel.delete(senderId)
                        XToastUtils.success(R.string.delete_sender_toast)
                        popToBack()
                    }.show()
                    return
                }

                R.id.btn_save -> {
                    val name = binding!!.etName.text.toString().trim()
                    if (TextUtils.isEmpty(name)) {
                        throw Exception(getString(R.string.invalid_name))
                    }

                    val status = if (binding!!.sbEnable.isChecked) 1 else 0
                    val settingVo = checkSetting()
                    if (isClone) senderId = 0
                    val senderNew = Sender(senderId, senderType, name, Gson().toJson(settingVo), status)
                    Log.d(TAG, senderNew.toString())

                    viewModel.insertOrUpdate(senderNew)
                    XToastUtils.success(R.string.tipSaveSuccess)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString())
            e.printStackTrace()
            Log.e(TAG, "onClick: $e")
        }
    }

    private fun checkSetting(): SocketSetting {
        val address = binding!!.etAddress.text.toString().trim()
        if (CommonUtils.checkIP(address) == "Neither" && !CommonUtils.checkDomain(address)) {
            throw Exception(getString(R.string.invalid_ip))
        }

        val port = binding!!.etPort.text.toString().trim()
        if (!CommonUtils.checkPort(port)) {
            throw Exception(getString(R.string.invalid_port))
        }

        val method = when (binding!!.rgMethod.checkedRadioButtonId) {
            R.id.rb_method_tcp -> "TCP"
            R.id.rb_method_udp -> "UDP"
            else -> "MQTT"
        }

        val msgTemplate = binding!!.etMsgTemplate.text.toString().trim()
        val secret = binding!!.etSecret.text.toString().trim()
        val response = binding!!.etResponse.text.toString().trim()
        val username = binding!!.etUsername.text.toString().trim()
        val password = binding!!.etPassword.text.toString().trim()
        val inCharset = binding!!.etInCharset.text.toString().trim()
        val outCharset = binding!!.etOutCharset.text.toString().trim()
        val inMessageTopic = binding!!.etInMessageTopic.text.toString().trim()
        val outMessageTopic = binding!!.etOutMessageTopic.text.toString().trim()
        val uriType = when (binding!!.rgUriType.checkedRadioButtonId) {
            R.id.rb_uriType_ssl -> "ssl"
            else -> "tcp"
        }
        val path = binding!!.etPath.text.toString().trim()
        val clientId = binding!!.etClientId.text.toString().trim()

        if (method == "MQTT" && (TextUtils.isEmpty(inMessageTopic) || TextUtils.isEmpty(outMessageTopic))) {
            throw Exception(getString(R.string.invalid_mqtt_message_topic))
        }

        return SocketSetting(method, address, port.toInt(), msgTemplate, secret, response, username, password, inCharset, outCharset, inMessageTopic, outMessageTopic, uriType, path, clientId)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
