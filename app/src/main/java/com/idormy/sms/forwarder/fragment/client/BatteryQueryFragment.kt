package com.idormy.sms.forwarder.fragment.client

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientBatteryQueryBinding
import com.idormy.sms.forwarder.entity.BatteryInfo
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.RSACrypt
import com.idormy.sms.forwarder.utils.SM4Crypt
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.grouplist.XUIGroupListView
import com.xuexiang.xutil.data.ConvertTools

@Suppress("PrivatePropertyName")
@Page(name = "远程查电量")
class BatteryQueryFragment : BaseFragment<FragmentClientBatteryQueryBinding?>() {

    private val TAG: String = BatteryQueryFragment::class.java.simpleName

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientBatteryQueryBinding {
        return FragmentClientBatteryQueryBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.api_battery_query)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {

        val requestUrl: String = HttpServerUtils.serverAddress + "/battery/query"
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

        postRequest.execute(object : SimpleCallBack<String>() {
            override fun onError(e: ApiException) {
                XToastUtils.error(e.displayMessage)
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
                    val resp: BaseResponse<BatteryInfo> = Gson().fromJson(json, object : TypeToken<BaseResponse<BatteryInfo>>() {}.type)
                    if (resp.code == 200) {
                        XToastUtils.success(getString(R.string.request_succeeded))
                        val batteryInfo = resp.data ?: return

                        val groupListView = binding!!.infoList
                        val section = XUIGroupListView.newSection(context)
                        section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_level), batteryInfo.level))) {}
                        if (batteryInfo.scale != "") section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_scale), batteryInfo.scale))) {}
                        if (batteryInfo.voltage != "") section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_voltage), batteryInfo.voltage))) {}
                        if (batteryInfo.temperature != "") section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_temperature), batteryInfo.temperature))) {}
                        section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_status), batteryInfo.status))) {}
                        section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_health), batteryInfo.health))) {}
                        section.addItemView(groupListView.createItemView(String.format(getString(R.string.battery_plugged), batteryInfo.plugged))) {}
                        section.addTo(groupListView)

                    } else {
                        XToastUtils.error(getString(R.string.request_failed) + resp.msg)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    XToastUtils.error(getString(R.string.request_failed) + response)
                }
            }
        })

    }

}