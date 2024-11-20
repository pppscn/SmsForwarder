package com.idormy.sms.forwarder.fragment.senders

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersWebhookBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.WebhookSetting
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.WebhookUtils
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

@Page(name = "Webhook")
@Suppress("PrivatePropertyName")
class WebhookFragment : BaseFragment<FragmentSendersWebhookBinding?>(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private val TAG: String = WebhookFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var headerItemMap = HashMap<Int, LinearLayout>(2)

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
    ): FragmentSendersWebhookBinding {
        return FragmentSendersWebhookBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.webhook)
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
                val settingVo = Gson().fromJson(sender.jsonSetting, WebhookSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    binding!!.rgMethod.check(settingVo.getMethodCheckId())
                    binding!!.etWebServer.setText(settingVo.webServer)
                    binding!!.etSecret.setText(settingVo.secret)
                    binding!!.etResponse.setText(settingVo.response)
                    binding!!.etWebParams.setText(settingVo.webParams)
                    //set header
                    for ((key, value) in settingVo.headers) {
                        addHeaderItemLinearLayout(
                            headerItemMap, binding!!.layoutHeaders, key, value
                        )
                    }
                    binding!!.rgProxyType.check(settingVo.getProxyTypeCheckId())
                    binding!!.etProxyHost.setText(settingVo.proxyHost)
                    binding!!.etProxyPort.setText(settingVo.proxyPort)
                    binding!!.sbProxyAuthenticator.isChecked = settingVo.proxyAuthenticator == true
                    binding!!.etProxyUsername.setText(settingVo.proxyUsername)
                    binding!!.etProxyPassword.setText(settingVo.proxyPassword)
                }
            }
        })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.btnAddHeader.setOnClickListener {
            addHeaderItemLinearLayout(headerItemMap, binding!!.layoutHeaders, null, null)
        }
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

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        //注意：因为只有一个监听，暂不需要判断id
        binding!!.layoutProxyAuthenticator.visibility = if (isChecked) View.VISIBLE else View.GONE
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
                            WebhookUtils.sendMsg(settingVo, msgInfo)
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

    private fun checkSetting(): WebhookSetting {
        val webServer = binding!!.etWebServer.text.toString().trim()
        if (!CommonUtils.checkUrl(webServer, false)) {
            throw Exception(getString(R.string.invalid_webserver))
        }

        val method = when (binding!!.rgMethod.checkedRadioButtonId) {
            R.id.rb_method_get -> "GET"
            R.id.rb_method_put -> "PUT"
            R.id.rb_method_patch -> "PATCH"
            else -> "POST"
        }
        val secret = binding!!.etSecret.text.toString().trim()
        val response = binding!!.etResponse.text.toString().trim()
        val webParams = binding!!.etWebParams.text.toString().trim()
        val headers = getHeadersFromHeaderItemMap(headerItemMap)

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

        return WebhookSetting(method, webServer, secret, response, webParams, headers, proxyType, proxyHost, proxyPort, proxyAuthenticator, proxyUsername, proxyPassword)
    }


    //header序号
    private var headerItemId = 0

    /**
     * 动态增删header
     *
     * @param headerItemMap                管理item的map，用于删除指定header
     * @param linearLayoutWebNotifyHeaders 需要挂载item的LinearLayout
     * @param key                          header的key，为空则不设置
     * @param value                        header的value，为空则不设置
     */
    private fun addHeaderItemLinearLayout(
        headerItemMap: MutableMap<Int, LinearLayout>, linearLayoutWebNotifyHeaders: LinearLayout, key: String?, value: String?
    ) {
        val linearLayoutItemAddHeader = View.inflate(requireContext(), R.layout.item_add_header, null) as LinearLayout
        val imageViewRemoveHeader = linearLayoutItemAddHeader.findViewById<ImageView>(R.id.imageViewRemoveHeader)
        if (key != null && value != null) {
            val editTextHeaderKey = linearLayoutItemAddHeader.findViewById<EditText>(R.id.editTextHeaderKey)
            val editTextHeaderValue = linearLayoutItemAddHeader.findViewById<EditText>(R.id.editTextHeaderValue)
            editTextHeaderKey.setText(key)
            editTextHeaderValue.setText(value)
        }
        imageViewRemoveHeader.tag = headerItemId
        imageViewRemoveHeader.setOnClickListener { view2: View ->
            val itemId = view2.tag as Int
            linearLayoutWebNotifyHeaders.removeView(headerItemMap[itemId])
            headerItemMap.remove(itemId)
        }
        linearLayoutWebNotifyHeaders.addView(linearLayoutItemAddHeader)
        headerItemMap[headerItemId] = linearLayoutItemAddHeader
        headerItemId++
    }

    /**
     * 从EditText控件中获取全部headers
     *
     * @param headerItemMap 管理item的map
     * @return 全部headers
     */
    private fun getHeadersFromHeaderItemMap(headerItemMap: Map<Int, LinearLayout>): Map<String, String> {
        val headers: MutableMap<String, String> = HashMap()
        for (headerItem in headerItemMap.values) {
            val editTextHeaderKey = headerItem.findViewById<EditText>(R.id.editTextHeaderKey)
            val editTextHeaderValue = headerItem.findViewById<EditText>(R.id.editTextHeaderValue)
            val key = editTextHeaderKey.text.toString().trim()
            val value = editTextHeaderValue.text.toString().trim()
            headers[key] = value
        }
        return headers
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
