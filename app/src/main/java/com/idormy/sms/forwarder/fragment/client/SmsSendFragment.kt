package com.idormy.sms.forwarder.fragment.client

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientSmsSendBinding
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.server.model.ConfigData
import com.idormy.sms.forwarder.utils.*
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.data.ConvertTools

@Suppress("PrivatePropertyName")
@Page(name = "远程发短信")
class SmsSendFragment : BaseFragment<FragmentClientSmsSendBinding?>(), View.OnClickListener {

    private val TAG: String = SmsSendFragment::class.java.simpleName
    private var mCountDownHelper: CountDownButtonHelper? = null

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientSmsSendBinding {
        return FragmentClientSmsSendBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        return super.initTitle()!!.setImmersive(false).setTitle(R.string.api_sms_send)
    }

    /**
     * 初始化控件
     */
    @SuppressLint("SetTextI18n")
    override fun initViews() {
        //发送按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnSubmit, SettingUtils.requestTimeout)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnSubmit.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnSubmit.text = getString(R.string.send)
            }
        })

        //卡槽信息
        val serverConfigStr = HttpServerUtils.serverConfig
        if (!TextUtils.isEmpty(serverConfigStr)) {
            val serverConfig: ConfigData = Gson().fromJson(serverConfigStr, object : TypeToken<ConfigData>() {}.type)
            binding!!.rbSimSlot1.text = "SIM1：" + serverConfig.extraSim1
            binding!!.rbSimSlot2.text = "SIM2：" + serverConfig.extraSim2
        }
    }

    override fun initListeners() {
        binding!!.btnSubmit.setOnClickListener(this)
        LiveEventBus.get(EVENT_KEY_SIM_SLOT, Int::class.java).observeSticky(this) { value: Int ->
            binding!!.rgSimSlot.check(if (value == 1) R.id.rb_sim_slot_2 else R.id.rb_sim_slot_1)
        }
        LiveEventBus.get(EVENT_KEY_PHONE_NUMBERS, String::class.java).observeSticky(this) { value: String ->
            binding!!.etPhoneNumbers.setText(value)
        }
    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_submit -> {
                val requestUrl: String = HttpServerUtils.serverAddress + "/sms/send"
                Log.i(TAG, "requestUrl:$requestUrl")

                val msgMap: MutableMap<String, Any> = mutableMapOf()
                val timestamp = System.currentTimeMillis()
                msgMap["timestamp"] = timestamp
                val clientSignKey = HttpServerUtils.clientSignKey
                if (!TextUtils.isEmpty(clientSignKey)) {
                    msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey)
                }

                val phoneNumbers = binding!!.etPhoneNumbers.text.toString()
                val phoneRegex = getString(R.string.phone_numbers_regex).toRegex()
                if (!phoneRegex.matches(phoneNumbers)) {
                    XToastUtils.error(getString(R.string.phone_numbers_error))
                    return
                }

                val msgContent = binding!!.etMsgContent.text.toString()
                val msgRegex = getString(R.string.msg_content_regex).toRegex()
                if (!msgRegex.matches(msgContent)) {
                    XToastUtils.error(getString(R.string.msg_content_error))
                    return
                }

                val dataMap: MutableMap<String, Any> = mutableMapOf()
                dataMap["sim_slot"] = if (binding!!.rgSimSlot.checkedRadioButtonId == R.id.rb_sim_slot_2) 2 else 1
                dataMap["phone_numbers"] = phoneNumbers
                dataMap["msg_content"] = msgContent
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