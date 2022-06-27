package com.idormy.sms.forwarder.fragment.senders

import android.annotation.SuppressLint
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersWeworkAgentBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.WeworkAgentSetting
import com.idormy.sms.forwarder.utils.*
import com.idormy.sms.forwarder.utils.sender.WeworkAgentUtils
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
import java.util.*

@Page(name = "企业微信应用")
@Suppress("PrivatePropertyName")
class WeworkAgentFragment : BaseFragment<FragmentSendersWeworkAgentBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = WeworkAgentFragment::class.java.simpleName
    var titleBar: TitleBar? = null
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
        AppDatabase.getInstance(requireContext())
            .senderDao()
            .get(senderId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Sender> {
                override fun onSubscribe(d: Disposable) {}

                override fun onError(e: Throwable) {
                    e.printStackTrace()
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
                    }
                }
            })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.sbAtAll.setOnCheckedChangeListener(this)
    }

    @SuppressLint("SetTextI18n")
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        //注意：因为只有一个监听，暂不需要判断id
        if (isChecked) {
            binding!!.etToUser.setText("@all")
            binding!!.layoutToUser.visibility = View.GONE
        } else {
            binding!!.etToUser.setText("")
            binding!!.layoutToUser.visibility = View.VISIBLE
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
                            val msgInfo = MsgInfo("sms", getString(R.string.test_phone_num), getString(R.string.test_sender_sms), Date(), getString(R.string.test_sim_info))
                            WeworkAgentUtils.sendMsg(settingVo, msgInfo)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (Looper.myLooper() == null) Looper.prepare()
                            XToastUtils.error(e.message.toString())
                            Looper.loop()
                        }
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

        return WeworkAgentSetting(corpID, agentID, secret, atAll, toUser)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}