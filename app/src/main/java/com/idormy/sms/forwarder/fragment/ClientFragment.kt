package com.idormy.sms.forwarder.fragment

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.WidgetItemAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientBinding
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.server.model.ConfigData
import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.RSACrypt
import com.idormy.sms.forwarder.utils.SM4Crypt
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.enums.CoreAnim
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.data.ConvertTools
import com.xuexiang.xutil.resource.ResUtils.getColors

@Suppress("PrivatePropertyName", "DEPRECATION")
@Page(name = "主动控制·客户端")
class ClientFragment : BaseFragment<FragmentClientBinding?>(), View.OnClickListener, RecyclerViewHolder.OnItemClickListener<PageInfo> {

    private val TAG: String = ClientFragment::class.java.simpleName
    private var appContext: App? = null
    private var serverConfig: ConfigData? = null
    private var serverHistory: MutableMap<String, String> = mutableMapOf()
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var CLIENT_FRAGMENT_LIST = listOf(
        PageInfo(
            getString(R.string.api_clone), "com.idormy.sms.forwarder.fragment.client.CloneFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_clone
        ),
        PageInfo(
            getString(R.string.api_sms_query), "com.idormy.sms.forwarder.fragment.client.SmsQueryFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_sms_query
        ),
        PageInfo(
            getString(R.string.api_sms_send), "com.idormy.sms.forwarder.fragment.client.SmsSendFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_sms_send
        ),
        PageInfo(
            getString(R.string.api_call_query), "com.idormy.sms.forwarder.fragment.client.CallQueryFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_call_query
        ),
        PageInfo(
            getString(R.string.api_contact_query), "com.idormy.sms.forwarder.fragment.client.ContactQueryFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_contact_query
        ),
        PageInfo(
            getString(R.string.api_contact_add), "com.idormy.sms.forwarder.fragment.client.ContactAddFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_contact_add
        ),
        PageInfo(
            getString(R.string.api_wol), "com.idormy.sms.forwarder.fragment.client.WolSendFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_wol
        ),
        PageInfo(
            getString(R.string.api_location), "com.idormy.sms.forwarder.fragment.client.LocationFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_location
        ),
        PageInfo(
            getString(R.string.api_battery_query), "com.idormy.sms.forwarder.fragment.client.BatteryQueryFragment", "{\"\":\"\"}", CoreAnim.slide, R.drawable.icon_api_battery_query
        ),
    )

    override fun initViews() {
        appContext = requireActivity().application as App

        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnServerTest, 3)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnServerTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnServerTest.text = getString(R.string.server_test)
            }
        })

        WidgetUtils.initGridRecyclerView(binding!!.recyclerView, 3, DensityUtils.dp2px(1f))
        val widgetItemAdapter = WidgetItemAdapter(CLIENT_FRAGMENT_LIST)
        widgetItemAdapter.setOnItemClickListener(this)
        binding!!.recyclerView.adapter = widgetItemAdapter

        //取出历史记录
        val history = HttpServerUtils.serverHistory
        if (!TextUtils.isEmpty(history)) {
            serverHistory = Gson().fromJson(history, object : TypeToken<MutableMap<String, String>>() {}.type)
        }

        if (CommonUtils.checkUrl(HttpServerUtils.serverAddress)) queryConfig(false)
    }

    override fun initTitle(): TitleBar? {
        val titleBar = super.initTitle()!!.setImmersive(false)
        //纯客户端模式
        if (SettingUtils.enablePureClientMode) {
            titleBar.setTitle(R.string.app_name).setSubTitle(getString(R.string.menu_client)).disableLeftView()
            titleBar.addAction(object : TitleBar.ImageAction(R.drawable.ic_logout) {
                @SingleClick
                override fun performAction(view: View) {
                    XToastUtils.success(getString(R.string.exit_pure_client_mode))
                    SettingUtils.enablePureClientMode = false
                    try {
                        Thread.sleep(500) //延迟500毫秒，避免退出时enablePureClientMode还没保存
                        XUtil.exitApp()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        Log.e(TAG, "InterruptedException: ${e.message}")
                    }
                }
            })
        } else {
            titleBar.setTitle(R.string.menu_client)
        }
        return titleBar
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientBinding {
        return FragmentClientBinding.inflate(inflater, container, false)
    }

    override fun initListeners() {
        binding!!.etServerAddress.setText(HttpServerUtils.serverAddress)
        binding!!.etServerAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverAddress = binding!!.etServerAddress.text.toString().trim().trimEnd('/')
            }
        })

        //安全措施
        var safetyMeasuresId = R.id.rb_safety_measures_none
        when (HttpServerUtils.clientSafetyMeasures) {
            1 -> {
                safetyMeasuresId = R.id.rb_safety_measures_sign
                binding!!.tvSignKey.text = getString(R.string.sign_key)
            }

            2 -> {
                safetyMeasuresId = R.id.rb_safety_measures_rsa
                binding!!.tvSignKey.text = getString(R.string.public_key)
            }

            3 -> {
                safetyMeasuresId = R.id.rb_safety_measures_sm4
                binding!!.tvSignKey.text = getString(R.string.sm4_key)
            }

            else -> {
                binding!!.layoutSignKey.visibility = View.GONE
            }
        }
        binding!!.rgSafetyMeasures.check(safetyMeasuresId)
        binding!!.rgSafetyMeasures.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            var safetyMeasures = 0
            binding!!.layoutSignKey.visibility = View.VISIBLE
            when (checkedId) {
                R.id.rb_safety_measures_sign -> {
                    safetyMeasures = 1
                    binding!!.tvSignKey.text = getString(R.string.sign_key)
                }

                R.id.rb_safety_measures_rsa -> {
                    safetyMeasures = 2
                    binding!!.tvSignKey.text = getString(R.string.public_key)
                }

                R.id.rb_safety_measures_sm4 -> {
                    safetyMeasures = 3
                    binding!!.tvSignKey.text = getString(R.string.sm4_key)
                }

                else -> {
                    binding!!.layoutSignKey.visibility = View.GONE
                }
            }
            HttpServerUtils.clientSafetyMeasures = safetyMeasures
        }

        binding!!.etSignKey.setText(HttpServerUtils.clientSignKey)
        binding!!.etSignKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.clientSignKey = binding!!.etSignKey.text.toString().trim()
            }
        })

        binding!!.btnWechatMiniprogram.setOnClickListener(this)
        binding!!.btnServerHistory.setOnClickListener(this)
        binding!!.btnServerTest.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_wechat_miniprogram -> {
                if (HttpServerUtils.safetyMeasures != 3) {
                    XToastUtils.error("微信小程序只支持SM4加密传输！请前往主动控制·服务端修改安全措施！")
                    return
                }
                CommonUtils.previewPicture(this, getString(R.string.url_wechat_miniprogram), null)
            }

            R.id.btn_server_history -> {
                if (serverHistory.isEmpty()) {
                    XToastUtils.warning(getString(R.string.no_server_history))
                    return
                }
                Log.d(TAG, "serverHistory = $serverHistory")

                MaterialDialog.Builder(requireContext()).title(R.string.server_history).items(serverHistory.keys).itemsCallbackSingleChoice(0) { _: MaterialDialog?, _: View?, _: Int, text: CharSequence ->
                    //XToastUtils.info("$which: $text")
                    val matches = Regex("【(.*)】(.*)", RegexOption.IGNORE_CASE).findAll(text).toList().flatMap(MatchResult::groupValues)
                    Log.i(TAG, "matches = $matches")
                    if (matches.isNotEmpty()) {
                        binding!!.etServerAddress.setText(matches[2])
                    } else {
                        binding!!.etServerAddress.setText(text)
                    }
                    val signKey = serverHistory[text].toString()
                    if (!TextUtils.isEmpty(signKey)) {
                        val keyMatches = Regex("(.*)##(.*)", RegexOption.IGNORE_CASE).findAll(signKey).toList().flatMap(MatchResult::groupValues)
                        Log.i(TAG, "keyMatches = $keyMatches")
                        if (keyMatches.isNotEmpty()) {
                            binding!!.etSignKey.setText(keyMatches[1])
                            var safetyMeasuresId = R.id.rb_safety_measures_none
                            when (keyMatches[2]) {
                                "1" -> {
                                    safetyMeasuresId = R.id.rb_safety_measures_sign
                                    binding!!.tvSignKey.text = getString(R.string.sign_key)
                                }

                                "2" -> {
                                    safetyMeasuresId = R.id.rb_safety_measures_rsa
                                    binding!!.tvSignKey.text = getString(R.string.public_key)
                                }

                                "3" -> {
                                    safetyMeasuresId = R.id.rb_safety_measures_sm4
                                    binding!!.tvSignKey.text = getString(R.string.sm4_key)
                                }

                                else -> {
                                    binding!!.tvSignKey.visibility = View.GONE
                                    binding!!.etSignKey.visibility = View.GONE
                                }
                            }
                            binding!!.rgSafetyMeasures.check(safetyMeasuresId)
                        } else {
                            binding!!.etSignKey.setText(serverHistory[text])
                        }
                    }
                    true // allow selection
                }.positiveText(R.string.select).negativeText(R.string.cancel).neutralText(R.string.clear_history).neutralColor(getColors(R.color.red)).onNeutral { _: MaterialDialog?, _: DialogAction? ->
                    serverHistory.clear()
                    HttpServerUtils.serverHistory = ""
                }.show()
            }

            R.id.btn_server_test -> {
                if (!CommonUtils.checkUrl(HttpServerUtils.serverAddress)) {
                    XToastUtils.error(getString(R.string.invalid_service_address))
                    return
                }
                queryConfig(true)
            }

            else -> {}
        }
    }

    override fun onItemClick(itemView: View, item: PageInfo, position: Int) {
        try {
            if (item.name != getString(R.string.api_clone) && !CommonUtils.checkUrl(HttpServerUtils.serverAddress)) {
                XToastUtils.error(getString(R.string.invalid_service_address))
                serverConfig = null
                return
            }
            if (serverConfig == null && item.name != getString(R.string.api_clone)) {
                XToastUtils.error(getString(R.string.click_test_button_first))
                return
            }
            if (serverConfig != null && ((item.name == getString(R.string.api_sms_send) && !serverConfig!!.enableApiSmsSend) || (item.name == getString(R.string.api_sms_query) && !serverConfig!!.enableApiSmsQuery) || (item.name == getString(R.string.api_call_query) && !serverConfig!!.enableApiCallQuery) || (item.name == getString(R.string.api_contact_query) && !serverConfig!!.enableApiContactQuery) || (item.name == getString(R.string.api_contact_add) && !serverConfig!!.enableApiContactAdd) || (item.name == getString(R.string.api_battery_query) && !serverConfig!!.enableApiBatteryQuery) || (item.name == getString(R.string.api_wol) && !serverConfig!!.enableApiWol) || (item.name == getString(R.string.api_location) && !serverConfig!!.enableApiLocation))) {
                XToastUtils.error(getString(R.string.disabled_on_the_server))
                return
            }
            @Suppress("UNCHECKED_CAST") PageOption.to(Class.forName(item.classPath) as Class<XPageFragment>).setNewActivity(true).open(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onItemClick error: ${e.message}")
            XToastUtils.error(e.message.toString())
        }
    }

    private fun queryConfig(needToast: Boolean) {
        val requestUrl: String = HttpServerUtils.serverAddress + "/config/query"
        Log.i(TAG, "requestUrl:$requestUrl")

        val msgMap: MutableMap<String, Any> = mutableMapOf()
        val timestamp = System.currentTimeMillis()
        msgMap["timestamp"] = timestamp

        val clientSignKey = HttpServerUtils.clientSignKey
        if (HttpServerUtils.clientSafetyMeasures != 0 && TextUtils.isEmpty(clientSignKey)) {
            if (needToast) XToastUtils.error("请输入签名密钥/RSA公钥/SM4密钥")
            return
        }

        if (HttpServerUtils.clientSafetyMeasures == 1 && !TextUtils.isEmpty(clientSignKey)) {
            msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey)
        }
        val dataMap: MutableMap<String, Any> = mutableMapOf()
        msgMap["data"] = dataMap

        var requestMsg: String = Gson().toJson(msgMap)
        Log.i(TAG, "requestMsg:$requestMsg")

        val postRequest = XHttp.post(requestUrl).keepJson(true).timeStamp(true)

        when (HttpServerUtils.clientSafetyMeasures) {
            2 -> {
                try {
                    val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                    requestMsg = Base64.encode(requestMsg.toByteArray())
                    requestMsg = RSACrypt.encryptByPublicKey(requestMsg, publicKey)
                    Log.i(TAG, "requestMsg: $requestMsg")
                } catch (e: Exception) {
                    if (needToast) XToastUtils.error(getString(R.string.request_failed) + e.message)
                    e.printStackTrace()
                    Log.e(TAG, "RSACrypt error: ${e.message}")
                    return
                }
                postRequest.upString(requestMsg)
            }

            3 -> {
                try {
                    val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.clientSignKey)
                    //requestMsg = Base64.encode(requestMsg.toByteArray())
                    val encryptCBC = SM4Crypt.encrypt(requestMsg.toByteArray(), sm4Key)
                    requestMsg = ConvertTools.bytes2HexString(encryptCBC)
                    Log.i(TAG, "requestMsg: $requestMsg")
                } catch (e: Exception) {
                    if (needToast) XToastUtils.error(getString(R.string.request_failed) + e.message)
                    e.printStackTrace()
                    Log.e(TAG, "SM4Crypt error: ${e.message}")
                    return
                }
                postRequest.upString(requestMsg)
            }

            else -> {
                postRequest.upJson(requestMsg)
            }
        }

        if (needToast) mCountDownHelper?.start()
        postRequest.execute(object : SimpleCallBack<String>() {
            override fun onError(e: ApiException) {
                XToastUtils.error(e.displayMessage)
                if (needToast) mCountDownHelper?.finish()
            }

            override fun onSuccess(response: String) {
                Log.i(TAG, response)
                try {
                    var json = response
                    if (HttpServerUtils.clientSafetyMeasures == 2) {
                        val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                        json = RSACrypt.decryptByPublicKey(json, publicKey)
                        json = String(Base64.decode(json))
                    } else if (HttpServerUtils.clientSafetyMeasures == 3) {
                        val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.clientSignKey)
                        val encryptCBC = ConvertTools.hexStringToByteArray(json)
                        val decryptCBC = SM4Crypt.decrypt(encryptCBC, sm4Key)
                        json = String(decryptCBC)
                    }
                    val resp: BaseResponse<ConfigData> = Gson().fromJson(json, object : TypeToken<BaseResponse<ConfigData>>() {}.type)
                    if (resp.code == 200) {
                        serverConfig = resp.data!!
                        if (needToast) XToastUtils.success(getString(R.string.request_succeeded))
                        //删除3.0.8之前保存的记录
                        serverHistory.remove(HttpServerUtils.serverAddress)
                        //添加到历史记录
                        val key = "【${serverConfig?.extraDeviceMark}】${HttpServerUtils.serverAddress}"
                        if (TextUtils.isEmpty(HttpServerUtils.clientSignKey)) {
                            serverHistory[key] = "SMSFORWARDER##" + HttpServerUtils.clientSafetyMeasures.toString()
                        } else {
                            serverHistory[key] = HttpServerUtils.clientSignKey + "##" + HttpServerUtils.clientSafetyMeasures.toString()
                        }
                        HttpServerUtils.serverHistory = Gson().toJson(serverHistory)
                        HttpServerUtils.serverConfig = Gson().toJson(serverConfig)
                    } else {
                        if (needToast) XToastUtils.error(getString(R.string.request_failed) + resp.msg)
                    }
                    if (needToast) mCountDownHelper?.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "onSuccess error: ${e.message}")
                    if (needToast) {
                        XToastUtils.error(getString(R.string.request_failed) + response)
                        mCountDownHelper?.finish()
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}