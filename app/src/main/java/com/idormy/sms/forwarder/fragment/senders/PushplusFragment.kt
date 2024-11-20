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
import com.idormy.sms.forwarder.databinding.FragmentSendersPushplusBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.PushplusSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.PushplusUtils
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

@Page(name = "PushPlus")
@Suppress("PrivatePropertyName")
class PushplusFragment : BaseFragment<FragmentSendersPushplusBinding?>(), View.OnClickListener {

    private val TAG: String = PushplusFragment::class.java.simpleName
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
    ): FragmentSendersPushplusBinding {
        return FragmentSendersPushplusBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.pushplus)
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

        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glTitleTemplate, binding!!.etTitleTemplate)

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
                val settingVo = Gson().fromJson(sender.jsonSetting, PushplusSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    if (TextUtils.isEmpty(settingVo.website) || settingVo.website == getString(R.string.pushplus_plus)) {
                        binding!!.rgWebsite.check(R.id.rb_website_plus)
                    } else {
                        binding!!.rgWebsite.check(R.id.rb_website_hxtrip)
                    }
                    binding!!.etToken.setText(settingVo.token)
                    binding!!.etTopic.setText(settingVo.topic)
                    binding!!.etTemplate.setText(settingVo.template)
                    binding!!.etChannel.setText(settingVo.channel)
                    binding!!.etWebhook.setText(settingVo.webhook)
                    binding!!.etCallbackUrl.setText(settingVo.callbackUrl)
                    binding!!.etValidTime.setText(settingVo.validTime)
                    binding!!.etTitleTemplate.setText(settingVo.titleTemplate)
                }
            }
        })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.rgWebsite.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.rb_website_hxtrip) {
                binding!!.layoutPlusOne.visibility = View.GONE
                binding!!.layoutPlusTwo.visibility = View.GONE
            } else {
                binding!!.layoutPlusOne.visibility = View.VISIBLE
                binding!!.layoutPlusTwo.visibility = View.VISIBLE
            }
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
                            PushplusUtils.sendMsg(settingVo, msgInfo)
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

    private fun checkSetting(): PushplusSetting {
        val website = when (binding!!.rgWebsite.checkedRadioButtonId) {
            R.id.rb_website_hxtrip -> getString(R.string.pushplus_hxtrip)
            else -> getString(R.string.pushplus_plus)
        }

        val token = binding!!.etToken.text.toString().trim()
        if (TextUtils.isEmpty(token)) {
            throw Exception(getString(R.string.invalid_token))
        }

        val topic = binding!!.etTopic.text.toString().trim()
        val template = binding!!.etTemplate.text.toString().trim()
        val channel = binding!!.etChannel.text.toString().trim()
        val webhook = binding!!.etWebhook.text.toString().trim()
        val callbackUrl = binding!!.etCallbackUrl.text.toString().trim()
        val validTime = binding!!.etValidTime.text.toString().trim()
        val title = binding!!.etTitleTemplate.text.toString().trim()

        return PushplusSetting(website, token, topic, template, channel, webhook, callbackUrl, validTime, title)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
