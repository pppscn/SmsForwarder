package com.idormy.sms.forwarder.fragment.client

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientWolSendBinding
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
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
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog

@Suppress("PropertyName")
@Page(name = "远程WOL")
class WolSendFragment : BaseFragment<FragmentClientWolSendBinding?>(), View.OnClickListener {

    val TAG: String = WolSendFragment::class.java.simpleName
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
                    .neutralColor(ResUtils.getColors(R.color.red))
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
                        HttpServerUtils.calcSign(timestamp.toString(), clientSignKey.toString())
                }

                val mac = binding!!.etMac.text.toString()
                val macRegex = getString(R.string.mac_regex).toRegex()
                if (!macRegex.matches(mac)) {
                    XToastUtils.error(ResUtils.getString(R.string.mac_error))
                    return
                }

                val ip = binding!!.etIp.text.toString()
                val ipRegex = getString(R.string.ip_regex).toRegex()
                if (!TextUtils.isEmpty(ip) && !ipRegex.matches(ip)) {
                    XToastUtils.error(ResUtils.getString(R.string.ip_error))
                    return
                }

                val port = binding!!.etPort.text.toString()
                val portRegex = getString(R.string.wol_port_regex).toRegex()
                if (!TextUtils.isEmpty(port) && !portRegex.matches(port)) {
                    XToastUtils.error(ResUtils.getString(R.string.wol_port_error))
                    return
                }

                val dataMap: MutableMap<String, Any> = mutableMapOf()
                dataMap["ip"] = ip
                dataMap["mac"] = mac
                dataMap["port"] = port
                msgMap["data"] = dataMap

                val requestMsg: String = Gson().toJson(msgMap)
                Log.i(TAG, "requestMsg:$requestMsg")

                mCountDownHelper?.start()
                XHttp.post(requestUrl)
                    .upJson(requestMsg)
                    .keepJson(true)
                    .timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
                    .cacheMode(CacheMode.NO_CACHE)
                    .timeStamp(true)
                    .execute(object : SimpleCallBack<String>() {

                        override fun onError(e: ApiException) {
                            XToastUtils.error(e.displayMessage)
                            mCountDownHelper?.finish()
                        }

                        override fun onSuccess(response: String) {
                            Log.i(TAG, response)
                            try {
                                val resp: BaseResponse<String> = Gson().fromJson(
                                    response,
                                    object : TypeToken<BaseResponse<String>>() {}.type
                                )
                                if (resp.code == 200) {
                                    XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                                    //添加到历史记录
                                    wolHistory[mac] = ip
                                    HttpServerUtils.wolHistory = Gson().toJson(wolHistory)
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