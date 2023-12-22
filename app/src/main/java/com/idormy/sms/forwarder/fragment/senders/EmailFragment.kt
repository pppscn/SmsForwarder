package com.idormy.sms.forwarder.fragment.senders

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersEmailBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.EmailSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.EmailUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.spinner.materialspinner.MaterialSpinner
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Date

@Page(name = "Email")
@Suppress("PrivatePropertyName")
class EmailFragment : BaseFragment<FragmentSendersEmailBinding?>(), View.OnClickListener {

    private val TAG: String = EmailFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var mailType: String = getString(R.string.other_mail_type) //邮箱类型

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
    ): FragmentSendersEmailBinding {
        return FragmentSendersEmailBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.email)
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

        val mailTypeArray = getStringArray(R.array.MailType)
        Log.d(TAG, mailTypeArray.toString())
        binding!!.spMailType.setOnItemSelectedListener { _: MaterialSpinner?, position: Int, _: Long, item: Any ->
            mailType = item.toString()
            //XToastUtils.warning(mailType)
            binding!!.layoutServiceSetting.visibility = if (position == mailTypeArray.size - 1) View.VISIBLE else View.GONE
        }
        binding!!.spMailType.setOnNothingSelectedListener {
            mailType = mailTypeArray[mailTypeArray.size - 1]
            binding!!.spMailType.selectedIndex = mailTypeArray.size - 1
            binding!!.layoutServiceSetting.visibility = View.VISIBLE
        }
        binding!!.spMailType.selectedIndex = mailTypeArray.size - 1
        binding!!.layoutServiceSetting.visibility = View.VISIBLE

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
                val settingVo = Gson().fromJson(sender.jsonSetting, EmailSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    if (!TextUtils.isEmpty(settingVo.mailType)) {
                        mailType = settingVo.mailType.toString()
                        binding!!.spMailType.setSelectedItem(mailType)
                        if (mailType != getString(R.string.other_mail_type)) {
                            binding!!.layoutServiceSetting.visibility = View.GONE
                        }
                    }
                    binding!!.etFromEmail.setText(settingVo.fromEmail)
                    binding!!.etPwd.setText(settingVo.pwd)
                    binding!!.etNickname.setText(settingVo.nickname)
                    binding!!.etHost.setText(settingVo.host)
                    binding!!.etPort.setText(settingVo.port)
                    binding!!.sbSsl.isChecked = settingVo.ssl == true
                    binding!!.sbStartTls.isChecked = settingVo.startTls == true
                    binding!!.etToEmail.setText(settingVo.toEmail)
                    binding!!.etTitleTemplate.setText(settingVo.title)
                }
            }
        })
    }

    override fun initListeners() {
        binding!!.btInsertSenderToNickname.setOnClickListener(this)
        binding!!.btInsertExtraToNickname.setOnClickListener(this)
        binding!!.btInsertTimeToNickname.setOnClickListener(this)
        binding!!.btInsertDeviceNameToNickname.setOnClickListener(this)
        binding!!.btInsertSender.setOnClickListener(this)
        binding!!.btInsertExtra.setOnClickListener(this)
        binding!!.btInsertTime.setOnClickListener(this)
        binding!!.btInsertDeviceName.setOnClickListener(this)
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        LiveEventBus.get(KEY_SENDER_TEST, String::class.java).observe(this) { mCountDownHelper?.finish() }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            val etNickname: EditText = binding!!.etNickname
            val etTitleTemplate: EditText = binding!!.etTitleTemplate
            when (v.id) {
                R.id.bt_insert_sender_to_nickname -> {
                    CommonUtils.insertOrReplaceText2Cursor(etNickname, getString(R.string.tag_from))
                    return
                }

                R.id.bt_insert_extra_to_nickname -> {
                    CommonUtils.insertOrReplaceText2Cursor(etNickname, getString(R.string.tag_card_slot))
                    return
                }

                R.id.bt_insert_time_to_nickname -> {
                    CommonUtils.insertOrReplaceText2Cursor(etNickname, getString(R.string.tag_receive_time))
                    return
                }

                R.id.bt_insert_device_name_to_nickname -> {
                    CommonUtils.insertOrReplaceText2Cursor(etNickname, getString(R.string.tag_device_name))
                    return
                }

                R.id.bt_insert_sender -> {
                    CommonUtils.insertOrReplaceText2Cursor(etTitleTemplate, getString(R.string.tag_from))
                    return
                }

                R.id.bt_insert_extra -> {
                    CommonUtils.insertOrReplaceText2Cursor(etTitleTemplate, getString(R.string.tag_card_slot))
                    return
                }

                R.id.bt_insert_time -> {
                    CommonUtils.insertOrReplaceText2Cursor(etTitleTemplate, getString(R.string.tag_receive_time))
                    return
                }

                R.id.bt_insert_device_name -> {
                    CommonUtils.insertOrReplaceText2Cursor(etTitleTemplate, getString(R.string.tag_device_name))
                    return
                }

                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    Thread {
                        try {
                            val settingVo = checkSetting()
                            Log.d(TAG, settingVo.toString())
                            val name = binding!!.etName.text.toString().trim().takeIf { it.isNotEmpty() } ?: getString(R.string.test_sender_name)
                            val msgInfo = MsgInfo("sms", getString(R.string.test_phone_num), String.format(getString(R.string.test_sender_sms), name), Date(), getString(R.string.test_sim_info))
                            EmailUtils.sendMsg(settingVo, msgInfo)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, "onClick error:$e")
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
            Log.e(TAG, "onClick error:$e")
        }
    }

    private fun checkSetting(): EmailSetting {
        val fromEmail = binding!!.etFromEmail.text.toString().trim()
        val pwd = binding!!.etPwd.text.toString().trim()
        val nickname = binding!!.etNickname.text.toString().trim()
        val host = binding!!.etHost.text.toString().trim()
        val port = binding!!.etPort.text.toString().trim()
        val ssl = binding!!.sbSsl.isChecked
        val startTls = binding!!.sbStartTls.isChecked
        val toEmail = binding!!.etToEmail.text.toString().trim()
        val title = binding!!.etTitleTemplate.text.toString().trim()
        if (TextUtils.isEmpty(fromEmail) || TextUtils.isEmpty(pwd) || TextUtils.isEmpty(toEmail)) {
            throw Exception(getString(R.string.invalid_email))
        }
        if (mailType == getString(R.string.other_mail_type) && (TextUtils.isEmpty(host) || TextUtils.isEmpty(port))) {
            throw Exception(getString(R.string.invalid_email_server))
        }

        return EmailSetting(mailType, fromEmail, pwd, nickname, host, port, ssl, startTls, toEmail, title)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}