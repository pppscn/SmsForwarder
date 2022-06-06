package com.idormy.sms.forwarder.fragment.client

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientBatteryQueryBinding
import com.idormy.sms.forwarder.entity.BatteryInfo
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.grouplist.XUIGroupListView

@Suppress("PropertyName")
@Page(name = "远程查电量")
class BatteryQueryFragment : BaseFragment<FragmentClientBatteryQueryBinding?>() {

    val TAG: String = BatteryQueryFragment::class.java.simpleName

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
            msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey.toString())
        }
        val dataMap: MutableMap<String, Any> = mutableMapOf()
        msgMap["data"] = dataMap

        val requestMsg: String = Gson().toJson(msgMap)
        Log.i(TAG, "requestMsg:$requestMsg")

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
                }

                override fun onSuccess(response: String) {
                    Log.i(TAG, response)
                    try {
                        val resp: BaseResponse<BatteryInfo> = Gson().fromJson(response, object : TypeToken<BaseResponse<BatteryInfo>>() {}.type)
                        if (resp.code == 200) {
                            XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                            val batteryInfo = resp.data ?: return

                            val groupListView = binding!!.infoList
                            val section = XUIGroupListView.newSection(context)
                            section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_level), batteryInfo.level))) {}
                            if (batteryInfo.scale != "") section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_scale), batteryInfo.scale))) {}
                            if (batteryInfo.voltage != "") section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_voltage), batteryInfo.voltage))) {}
                            if (batteryInfo.temperature != "") section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_temperature), batteryInfo.temperature))) {}
                            section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_status), batteryInfo.status))) {}
                            section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_health), batteryInfo.health))) {}
                            section.addItemView(groupListView.createItemView(String.format(ResUtils.getString(R.string.battery_plugged), batteryInfo.plugged))) {}
                            section.addTo(groupListView)

                        } else {
                            XToastUtils.error(ResUtils.getString(R.string.request_failed) + resp.msg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
                    }
                }

            })
    }

}