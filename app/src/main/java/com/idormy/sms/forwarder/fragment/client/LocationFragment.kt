package com.idormy.sms.forwarder.fragment.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientLocationBinding
import com.idormy.sms.forwarder.entity.LocationInfo
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
import com.xuexiang.xui.widget.grouplist.XUIGroupListView
import com.xuexiang.xutil.data.ConvertTools

@Suppress("PrivatePropertyName")
@Page(name = "远程找手机")
class LocationFragment : BaseFragment<FragmentClientLocationBinding?>(), View.OnClickListener {

    private val TAG: String = LocationFragment::class.java.simpleName
    private var mCountDownHelper: CountDownButtonHelper? = null

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientLocationBinding {
        return FragmentClientLocationBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        return super.initTitle()!!.setImmersive(false).setTitle(R.string.api_location)
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //发送按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnRefresh, SettingUtils.requestTimeout)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnRefresh.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnRefresh.text = getString(R.string.refresh)
            }
        })

        getLocation()
    }

    override fun initListeners() {
        binding!!.btnRefresh.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_refresh -> {
                getLocation()
            }

            else -> {}
        }
    }

    private fun getLocation() {
        val requestUrl: String = HttpServerUtils.serverAddress + "/location/query"
        Log.i(TAG, "requestUrl:$requestUrl")

        val msgMap: MutableMap<String, Any> = mutableMapOf()
        val timestamp = System.currentTimeMillis()
        msgMap["timestamp"] = timestamp
        val clientSignKey = HttpServerUtils.clientSignKey
        if (!TextUtils.isEmpty(clientSignKey)) {
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
                    val resp: BaseResponse<LocationInfo> = Gson().fromJson(json, object : TypeToken<BaseResponse<LocationInfo>>() {}.type)
                    if (resp.code == 200) {
                        XToastUtils.success(getString(R.string.request_succeeded))
                        mCountDownHelper?.finish()

                        val locationInfo = resp.data ?: return

                        val groupListView = binding!!.infoList
                        groupListView.removeAllViews()
                        val section = XUIGroupListView.newSection(context)
                        section.addItemView(groupListView.createItemView(String.format(getString(R.string.location_longitude), locationInfo.longitude))) {}
                        section.addItemView(groupListView.createItemView(String.format(getString(R.string.location_latitude), locationInfo.latitude))) {}
                        if (locationInfo.address != "") section.addItemView(groupListView.createItemView(String.format(getString(R.string.location_address), locationInfo.address))) {}
                        if (locationInfo.time != "") section.addItemView(groupListView.createItemView(String.format(getString(R.string.location_time), locationInfo.time))) {}
                        if (locationInfo.provider != "") section.addItemView(groupListView.createItemView(String.format(getString(R.string.location_provider), locationInfo.provider))) {}
                        section.addTo(groupListView)
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

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}