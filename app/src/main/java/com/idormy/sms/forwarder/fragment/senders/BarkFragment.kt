package com.idormy.sms.forwarder.fragment.senders

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.idormy.sms.forwarder.App.Companion.BARK_ENCRYPTION_ALGORITHM_MAP
import com.idormy.sms.forwarder.App.Companion.BARK_LEVEL_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersBarkBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.BarkSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.BarkUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.utils.CountDownButtonHelper.OnCountDownListener
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.spinner.materialspinner.MaterialSpinner
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Date

@Page(name = "Bark")
@Suppress("PrivatePropertyName")
class BarkFragment : BaseFragment<FragmentSendersBarkBinding?>(), View.OnClickListener {

    private val TAG: String = BarkFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var barkLevel: String = "active" //通知级别
    private var transformation: String = "none" //加密算法

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
    ): FragmentSendersBarkBinding {
        return FragmentSendersBarkBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.bark)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, SettingUtils.requestTimeout)
        mCountDownHelper!!.setOnCountDownListener(object : OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        binding!!.spLevel.setItems(BARK_LEVEL_MAP.values.toList())
        binding!!.spLevel.setOnItemSelectedListener { _: MaterialSpinner?, _: Int, _: Long, item: Any ->
            BARK_LEVEL_MAP.forEach {
                if (it.value == item) barkLevel = it.key
            }
        }
        binding!!.spLevel.setOnNothingSelectedListener {
            binding!!.spLevel.selectedIndex = 0
            barkLevel = "active"
        }
        binding!!.spLevel.selectedIndex = 0

        binding!!.spEncryptionAlgorithm.setItems(BARK_ENCRYPTION_ALGORITHM_MAP.values.toList())
        binding!!.spEncryptionAlgorithm.setOnItemSelectedListener { _: MaterialSpinner?, _: Int, _: Long, item: Any ->
            BARK_ENCRYPTION_ALGORITHM_MAP.forEach {
                if (it.value == item) transformation = it.key
            }
        }
        binding!!.spEncryptionAlgorithm.setOnNothingSelectedListener {
            binding!!.spEncryptionAlgorithm.selectedIndex = 0
            transformation = "none"
        }
        binding!!.spEncryptionAlgorithm.selectedIndex = 0

        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glTitleTemplate, binding!!.etTitleTemplate)
        CommonUtils.createTagButtons(requireContext(), binding!!.glAutoCopyTemplate, binding!!.etAutoCopyTemplate)

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
                val settingVo = Gson().fromJson(sender.jsonSetting, BarkSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    binding!!.etServer.setText(settingVo.server)
                    binding!!.etGroup.setText(settingVo.group)
                    binding!!.etIcon.setText(settingVo.icon)
                    binding!!.sbCall.isChecked = settingVo.call == "1"
                    binding!!.etSound.setText(settingVo.sound)
                    binding!!.etBadge.setText(settingVo.badge)
                    binding!!.etUrl.setText(settingVo.url)
                    BARK_LEVEL_MAP.forEach {
                        if (it.key == settingVo.level) binding!!.spLevel.setSelectedItem(it.value)
                    }
                    binding!!.etTitleTemplate.setText(settingVo.title)
                    BARK_ENCRYPTION_ALGORITHM_MAP.forEach {
                        if (it.value == settingVo.transformation) {
                            transformation = it.value
                            binding!!.spEncryptionAlgorithm.setSelectedItem(it.value)
                        }
                    }
                    binding!!.etEncryptionKey.setText(settingVo.key)
                    binding!!.etEncryptionIv.setText(settingVo.iv)
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
                            BarkUtils.sendMsg(settingVo, msgInfo)
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

    private fun checkSetting(): BarkSetting {
        val server = binding!!.etServer.text.toString().trim()
        if (!CommonUtils.checkUrl(server, false)) {
            throw Exception(getString(R.string.invalid_bark_server))
        }
        val group = binding!!.etGroup.text.toString().trim()
        val icon = binding!!.etIcon.text.toString().trim()
        if (!CommonUtils.checkUrl(icon, true)) {
            throw Exception(getString(R.string.invalid_bark_icon))
        }
        val call = if (binding!!.sbCall.isChecked) "1" else "0"
        val sound = binding!!.etSound.text.toString().trim()
        val badge = binding!!.etBadge.text.toString().trim()
        val url = binding!!.etUrl.text.toString().trim()
        if (!TextUtils.isEmpty(url) && !url.contains("\\w+://".toRegex())) {
            throw Exception(getString(R.string.invalid_bark_url))
        }
        val title = binding!!.etTitleTemplate.text.toString().trim()
        val autoCopy = binding!!.etAutoCopyTemplate.text.toString().trim()
        val key = binding!!.etEncryptionKey.text.toString().trim()
        val iv = binding!!.etEncryptionIv.text.toString().trim()
        if (transformation.startsWith("AES128") && key.length != 16) {
            throw Exception(getString(R.string.bark_encryption_key_error1))
        } else if (transformation.startsWith("AES192") && key.length != 24) {
            throw Exception(getString(R.string.bark_encryption_key_error2))
        } else if (transformation.startsWith("AES256") && key.length != 32) {
            throw Exception(getString(R.string.bark_encryption_key_error3))
        }
        if (transformation.contains("CBC") && iv.length != 16) {
            throw Exception(getString(R.string.bark_encryption_key_error4))
        }

        return BarkSetting(server, group, icon, sound, badge, url, barkLevel, title, transformation, key, iv, call, autoCopy)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
