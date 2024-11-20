package com.idormy.sms.forwarder.fragment.senders

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersWeworkAgentBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.WeworkAgentSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.WeworkAgentUtils
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
import java.net.Proxy
import java.util.Date

@Page(name = "企业微信应用")
@Suppress("PrivatePropertyName")
class WeworkAgentFragment : BaseFragment<FragmentSendersWeworkAgentBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = WeworkAgentFragment::class.java.simpleName
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
    ): FragmentSendersWeworkAgentBinding {
        return FragmentSendersWeworkAgentBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.wework_agent)
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
                val settingVo = Gson().fromJson(sender.jsonSetting, WeworkAgentSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    binding!!.etCorpID.setText(settingVo.corpID)
                    binding!!.etAgentID.setText(settingVo.agentID)
                    binding!!.etSecret.setText(settingVo.secret)
                    binding!!.sbAtAll.isChecked = settingVo.atAll == true
                    binding!!.etToUser.setText(settingVo.toUser)
                    binding!!.etToParty.setText(settingVo.toParty)
                    binding!!.etToTag.setText(settingVo.toTag)
                    binding!!.layoutToUser.visibility = if (settingVo.atAll) View.GONE else View.VISIBLE
                    binding!!.layoutToParty.visibility = if (settingVo.atAll) View.GONE else View.VISIBLE
                    binding!!.layoutToTag.visibility = if (settingVo.atAll) View.GONE else View.VISIBLE
                    binding!!.rgProxyType.check(settingVo.getProxyTypeCheckId())
                    binding!!.etProxyHost.setText(settingVo.proxyHost)
                    binding!!.etProxyPort.setText(settingVo.proxyPort)
                    binding!!.sbProxyAuthenticator.isChecked = settingVo.proxyAuthenticator == true
                    binding!!.etProxyUsername.setText(settingVo.proxyUsername)
                    binding!!.etProxyPassword.setText(settingVo.proxyPassword)
                    binding!!.etCustomizeAPI.setText(settingVo.customizeAPI)
                }
            }
        })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.sbAtAll.setOnCheckedChangeListener(this)
        binding!!.sbProxyAuthenticator.setOnCheckedChangeListener(this)
        binding!!.rgProxyType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.rb_proxyHttp || checkedId == R.id.rb_proxySocks) {
                binding!!.layoutProxyHost.visibility = View.VISIBLE
                binding!!.layoutProxyPort.visibility = View.VISIBLE
                binding!!.layoutProxyAuthenticator.visibility = if (binding!!.sbProxyAuthenticator.isChecked) View.VISIBLE else View.GONE
            } else {
                binding!!.layoutProxyHost.visibility = View.GONE
                binding!!.layoutProxyPort.visibility = View.GONE
                binding!!.layoutProxyAuthenticator.visibility = View.GONE
            }
        }
        LiveEventBus.get(KEY_SENDER_TEST, String::class.java).observe(this) { mCountDownHelper?.finish() }
    }

    @SuppressLint("SetTextI18n")
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.sb_at_all -> {
                if (isChecked) {
                    binding!!.etToUser.setText("@all")
                    binding!!.etToParty.setText("")
                    binding!!.etToTag.setText("")
                    binding!!.layoutToUser.visibility = View.GONE
                    binding!!.layoutToParty.visibility = View.GONE
                    binding!!.layoutToTag.visibility = View.GONE
                } else {
                    binding!!.etToUser.setText("")
                    binding!!.etToParty.setText("")
                    binding!!.etToTag.setText("")
                    binding!!.layoutToUser.visibility = View.VISIBLE
                    binding!!.layoutToParty.visibility = View.VISIBLE
                    binding!!.layoutToTag.visibility = View.VISIBLE
                }
            }

            R.id.sb_proxyAuthenticator -> {
                binding!!.layoutProxyAuthenticator.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            else -> {}
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
                            val name = binding!!.etName.text.toString().trim().takeIf { it.isNotEmpty() } ?: getString(R.string.test_sender_name)
                            val msgInfo = MsgInfo("sms", getString(R.string.test_phone_num), String.format(getString(R.string.test_sender_sms), name), Date(), getString(R.string.test_sim_info))
                            WeworkAgentUtils.sendMsg(settingVo, msgInfo)
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

    private fun checkSetting(): WeworkAgentSetting {
        val corpID = binding!!.etCorpID.text.toString().trim()
        val agentID = binding!!.etAgentID.text.toString().trim()
        val secret = binding!!.etSecret.text.toString().trim()
        if (TextUtils.isEmpty(corpID) || TextUtils.isEmpty(agentID) || TextUtils.isEmpty(secret)) {
            throw Exception(getString(R.string.invalid_wework_agent))
        }

        val atAll = binding!!.sbAtAll.isChecked
        val toUser = binding!!.etToUser.text.toString().trim()
        val toParty = binding!!.etToParty.text.toString().trim()
        val toTag = binding!!.etToTag.text.toString().trim()
        if (!atAll && TextUtils.isEmpty(toUser) && TextUtils.isEmpty(toParty) && TextUtils.isEmpty(toTag)) {
            throw Exception(getString(R.string.invalid_at_mobiles))
        }

        val proxyType: Proxy.Type = when (binding!!.rgProxyType.checkedRadioButtonId) {
            R.id.rb_proxyHttp -> Proxy.Type.HTTP
            R.id.rb_proxySocks -> Proxy.Type.SOCKS
            else -> Proxy.Type.DIRECT
        }
        val proxyHost = binding!!.etProxyHost.text.toString().trim()
        val proxyPort = binding!!.etProxyPort.text.toString().trim()

        if (proxyType != Proxy.Type.DIRECT && (TextUtils.isEmpty(proxyHost) || TextUtils.isEmpty(proxyPort))) {
            throw Exception(getString(R.string.invalid_host_or_port))
        }

        val proxyAuthenticator = binding!!.sbProxyAuthenticator.isChecked
        val proxyUsername = binding!!.etProxyUsername.text.toString().trim()
        val proxyPassword = binding!!.etProxyPassword.text.toString().trim()
        if (proxyAuthenticator && TextUtils.isEmpty(proxyUsername) && TextUtils.isEmpty(proxyPassword)) {
            throw Exception(getString(R.string.invalid_username_or_password))
        }

        val customizeAPI = binding!!.etCustomizeAPI.text.toString().trim()
        if (!CommonUtils.checkUrl(customizeAPI, true)) {
            throw Exception(getString(R.string.invalid_customize_api))
        }

        return WeworkAgentSetting(corpID, agentID, secret, atAll, toUser, toParty, toTag, proxyType, proxyHost, proxyPort, proxyAuthenticator, proxyUsername, proxyPassword, customizeAPI)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
