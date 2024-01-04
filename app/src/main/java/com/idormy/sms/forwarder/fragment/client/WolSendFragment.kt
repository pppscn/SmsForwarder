package com.idormy.sms.forwarder.fragment.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientWolSendBinding
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.utils.Base64
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
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.data.ConvertTools
import com.xuexiang.xutil.resource.ResUtils.getColors

@Suppress("PrivatePropertyName")
@Page(name = "远程WOL")
class WolSendFragment : BaseFragment<FragmentClientWolSendBinding?>(), View.OnClickListener {

    private val TAG: String = WolSendFragment::class.java.simpleName
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var wolHistory: MutableMap<String, String> = mutableMapOf()

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientWolSendBinding {
        return FragmentClientWolSendBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        return super.initTitle()!!.setImmersive(false).setTitle(R.string.api_wol)
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //发送按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnSubmit, SettingUtils.requestTimeout)
        mCountDownHelper!!.setOnCountDownListener(object :
            CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnSubmit.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnSubmit.text = getString(R.string.send)
            }
        })

        //取出历史记录
        val history = HttpServerUtils.wolHistory
        if (!TextUtils.isEmpty(history)) {
            wolHistory =
                Gson().fromJson(history, object : TypeToken<MutableMap<String, String>>() {}.type)
        }
    }

    override fun initListeners() {
        binding!!.btnServerHistory.setOnClickListener(this)
        binding!!.btnSubmit.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_server_history -> {
                if (wolHistory.isEmpty()) {
                    XToastUtils.warning(getString(R.string.no_server_history))
                    return
                }
                Log.d(TAG, "wolHistory = $wolHistory")

                MaterialDialog.Builder(requireContext())
                    .title(R.string.server_history)
                    .items(wolHistory.keys)
                    .itemsCallbackSingleChoice(0) { _: MaterialDialog?, _: View?, _: Int, text: CharSequence ->
                        //XToastUtils.info("$which: $text")
                        binding!!.etMac.setText(text)
                        binding!!.etIp.setText(wolHistory[text])
                        true // allow selection
                    }
                    .positiveText(R.string.select)
                    .negativeText(R.string.cancel)
                    .neutralText(R.string.clear_history)
                    .neutralColor(getColors(R.color.red))
                    .onNeutral { _: MaterialDialog?, _: DialogAction? ->
                        wolHistory.clear()
                        HttpServerUtils.wolHistory = ""
                    }
                    .show()
            }

            R.id.btn_submit -> {
                val requestUrl: String = HttpServerUtils.serverAddress + "/wol/send"
                Log.i(TAG, "requestUrl:$requestUrl")

                val msgMap: MutableMap<String, Any> = mutableMapOf()
                val timestamp = System.currentTimeMillis()
                msgMap["timestamp"] = timestamp
                val clientSignKey = HttpServerUtils.clientSignKey
                if (!TextUtils.isEmpty(clientSignKey)) {
                    msgMap["sign"] =
                        HttpServerUtils.calcSign(timestamp.toString(), clientSignKey)
                }

                val mac = binding!!.etMac.text.toString()
                val macRegex = getString(R.string.mac_regex).toRegex()
                if (!macRegex.matches(mac)) {
                    XToastUtils.error(getString(R.string.mac_error))
                    return
                }

                val ip = binding!!.etIp.text.toString()
                val ipRegex = getString(R.string.ip_regex).toRegex()
                if (!TextUtils.isEmpty(ip) && !ipRegex.matches(ip)) {
                    XToastUtils.error(getString(R.string.ip_error))
                    return
                }

                val port = binding!!.etPort.text.toString()
                val portRegex = getString(R.string.wol_port_regex).toRegex()
                if (!TextUtils.isEmpty(port) && !portRegex.matches(port)) {
                    XToastUtils.error(getString(R.string.wol_port_error))
                    return
                }

                val dataMap: MutableMap<String, Any> = mutableMapOf()
                dataMap["ip"] = ip
                dataMap["mac"] = mac
                dataMap["port"] = port
                msgMap["data"] = dataMap

                var requestMsg: String = Gson().toJson(msgMap)
                Log.i(TAG, "requestMsg:$requestMsg")

                val postRequest = XHttp.post(requestUrl).keepJson(true).timeStamp(true)

                when (HttpServerUtils.clientSafetyMeasures) {
                    2 -> {
                        val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                        try {
                            requestMsg = Base64.encode(requestMsg.toByteArray())
                            requestMsg = RSACrypt.encryptByPublicKey(requestMsg, publicKey)
                            Log.i(TAG, "requestMsg: $requestMsg")
                        } catch (e: Exception) {
                            XToastUtils.error(getString(R.string.request_failed) + e.message)
                            e.printStackTrace()
                            Log.e(TAG, e.toString())
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
                            XToastUtils.error(getString(R.string.request_failed) + e.message)
                            e.printStackTrace()
                            Log.e(TAG, e.toString())
                            return
                        }
                        postRequest.upString(requestMsg)
                    }

                    else -> {
                        postRequest.upJson(requestMsg)
                    }
                }

                mCountDownHelper?.start()
                postRequest.execute(object : SimpleCallBack<String>() {
                    override fun onError(e: ApiException) {
                        XToastUtils.error(e.displayMessage)
                        mCountDownHelper?.finish()
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
                            val resp: BaseResponse<String> = Gson().fromJson(json, object : TypeToken<BaseResponse<String>>() {}.type)
                            if (resp.code == 200) {
                                XToastUtils.success(getString(R.string.request_succeeded))
                                //添加到历史记录
                                wolHistory[mac] = ip
                                HttpServerUtils.wolHistory = Gson().toJson(wolHistory)
                            } else {
                                XToastUtils.error(getString(R.string.request_failed) + resp.msg)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, e.toString())
                            XToastUtils.error(getString(R.string.request_failed) + response)
                        }
                        mCountDownHelper?.finish()
                    }
                })
            }

            else -> {}
        }
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}