package com.idormy.sms.forwarder.fragment.senders

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersFeishuAppBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.FeishuAppSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.FeishuAppUtils
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

@Page(name = "飞书企业应用")
@Suppress("PrivatePropertyName")
class FeishuAppFragment : BaseFragment<FragmentSendersFeishuAppBinding?>(), View.OnClickListener {

    private val TAG: String = FeishuAppFragment::class.java.simpleName
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
    ): FragmentSendersFeishuAppBinding {
        return FragmentSendersFeishuAppBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.feishu_app)
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

        binding!!.rgMsgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_msg_type_interactive) {
                binding!!.layoutTitleTemplate.visibility = View.VISIBLE
                binding!!.layoutMessageCard.visibility = View.VISIBLE
            } else {
                binding!!.layoutTitleTemplate.visibility = View.GONE
                binding!!.layoutMessageCard.visibility = View.GONE
            }
        }

        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glTitleTemplate, binding!!.etTitleTemplate)
        CommonUtils.createTagButtons(requireContext(), binding!!.glMessageCard, binding!!.etMessageCard)

        //新增
        if (senderId <= 0) {
            titleBar?.setSubTitle(getString(R.string.add_sender))
            binding!!.btnDel.setText(R.string.discard)
            return
        }

        //编辑
        binding!!.btnDel.setText(R.string.del)
        Core.sender.get(senderId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Sender> {
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
                    val settingVo = Gson().fromJson(sender.jsonSetting, FeishuAppSetting::class.java)
                    Log.d(TAG, settingVo.toString())
                    if (settingVo != null) {
                        binding!!.etAppId.setText(settingVo.appId)
                        binding!!.etAppSecret.setText(settingVo.appSecret)
                        binding!!.rgReceiveIdType.check(settingVo.getReceiveIdTypeCheckId())
                        binding!!.etReceiveId.setText(settingVo.receiveId)
                        binding!!.rgMsgType.check(settingVo.getMsgTypeCheckId())
                        binding!!.etTitleTemplate.setText(settingVo.titleTemplate)
                        binding!!.etMessageCard.setText(settingVo.messageCard)
                    }
                }
            })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
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
                            FeishuAppUtils.sendMsg(settingVo, msgInfo)
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

                    MaterialDialog.Builder(requireContext())
                        .title(R.string.delete_sender_title)
                        .content(R.string.delete_sender_tips)
                        .positiveText(R.string.lab_yes)
                        .negativeText(R.string.lab_no)
                        .onPositive { _: MaterialDialog?, _: DialogAction? ->
                            viewModel.delete(senderId)
                            XToastUtils.success(R.string.delete_sender_toast)
                            popToBack()
                        }
                        .show()
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

    private fun checkSetting(): FeishuAppSetting {
        val appId = binding!!.etAppId.text.toString().trim()
        val appSecret = binding!!.etAppSecret.text.toString().trim()
        val receiveId = binding!!.etReceiveId.text.toString().trim()
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appSecret) || TextUtils.isEmpty(receiveId)) {
            throw Exception(getString(R.string.invalid_feishu_app_parameter))
        }

        val receiveIdType = when (binding!!.rgReceiveIdType.checkedRadioButtonId) {
            R.id.rb_receive_id_type_open_id -> "open_id"
            R.id.rb_receive_id_type_user_id -> "user_id"
            R.id.rb_receive_id_type_union_id -> "union_id"
            R.id.rb_receive_id_type_email -> "email"
            R.id.rb_receive_id_type_chat_id -> "chat_id"
            else -> "user_id"
        }
        val msgType = if (binding!!.rgMsgType.checkedRadioButtonId == R.id.rb_msg_type_interactive) "interactive" else "text"
        val title = binding!!.etTitleTemplate.text.toString().trim()
        val messageCard = binding!!.etMessageCard.text.toString().trim()

        try {
            JsonParser.parseString(messageCard)
        } catch (e: Exception) {
            Log.e(TAG, "checkSetting error:$e")
            throw Exception(getString(R.string.invalid_message_card))
        }

        return FeishuAppSetting(appId, appSecret, receiveId, msgType, title, receiveIdType, messageCard)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
