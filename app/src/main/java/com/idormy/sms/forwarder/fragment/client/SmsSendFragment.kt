package com.idormy.sms.forwarder.fragment.client

import android.util.Log
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
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar

@Suppress("PropertyName")
@Page(name = "远程发短信")
class SmsSendFragment : BaseFragment<FragmentClientSmsSendBinding?>(), View.OnClickListener {

    val TAG: String = SmsSendFragment::class.java.simpleName
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
                    msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey.toString())
                }

                val phoneNumbers = binding!!.etPhoneNumbers.text.toString()
                val phoneRegex = getString(R.string.phone_numbers_regex).toRegex()
                if (!phoneRegex.matches(phoneNumbers)) {
                    XToastUtils.error(ResUtils.getString(R.string.phone_numbers_error))
                    return
                }

                val msgContent = binding!!.etMsgContent.text.toString()
                val msgRegex = getString(R.string.msg_content_regex).toRegex()
                if (!msgRegex.matches(msgContent)) {
                    XToastUtils.error(ResUtils.getString(R.string.msg_content_error))
                    return
                }

                val dataMap: MutableMap<String, Any> = mutableMapOf()
                dataMap["sim_slot"] = if (binding!!.rgSimSlot.checkedRadioButtonId == R.id.rb_sim_slot_2) 2 else 1
                dataMap["phone_numbers"] = phoneNumbers
                dataMap["msg_content"] = msgContent
                msgMap["data"] = dataMap

                val requestMsg: String = Gson().toJson(msgMap)
                Log.i(TAG, "requestMsg:$requestMsg")

                mCountDownHelper?.start()
                XHttp.post(requestUrl)
                    .upJson(requestMsg)
                    .keepJson(true)
                    .timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
                    .cacheMode(CacheMode.NO_CACHE)
                    //.retryCount(SettingUtils.requestRetryTimes) //超时重试的次数
                    //.retryDelay(SettingUtils.requestDelayTime) //超时重试的延迟时间
                    //.retryIncreaseDelay(SettingUtils.requestDelayTime) //超时重试叠加延时
                    .timeStamp(true)
                    .execute(object : SimpleCallBack<String>() {

                        override fun onError(e: ApiException) {
                            XToastUtils.error(e.displayMessage)
                            mCountDownHelper?.finish()
                        }

                        override fun onSuccess(response: String) {
                            Log.i(TAG, response)
                            try {
                                val resp: BaseResponse<String> = Gson().fromJson(response, object : TypeToken<BaseResponse<String>>() {}.type)
                                if (resp.code == 200) {
                                    XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                                } else {
                                    XToastUtils.error(ResUtils.getString(R.string.request_failed) + resp.msg)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
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