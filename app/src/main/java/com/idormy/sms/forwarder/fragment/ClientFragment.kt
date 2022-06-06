package com.idormy.sms.forwarder.fragment

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.WidgetItemAdapter
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientBinding
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.server.model.ConfigData
import com.idormy.sms.forwarder.utils.CLIENT_FRAGMENT_LIST
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.net.NetworkUtils

@Suppress("PrivatePropertyName", "PropertyName")
@Page(name = "主动控制·客户端")
class ClientFragment : BaseFragment<FragmentClientBinding?>(),
    View.OnClickListener,
    RecyclerViewHolder.OnItemClickListener<PageInfo> {

    val TAG: String = ClientFragment::class.java.simpleName
    private var appContext: App? = null
    private var serverConfig: ConfigData? = null

    override fun initViews() {
        appContext = requireActivity().application as App
        //val context = requireContext()
        //Set Server-Logging for Server
        //val serverLogging = ServerLogging(context.filesDir.absolutePath)
        //appContext!!.httpServer.serverLogging = serverLogging

        WidgetUtils.initGridRecyclerView(binding!!.recyclerView, 3, DensityUtils.dp2px(1f))
        val widgetItemAdapter = WidgetItemAdapter(CLIENT_FRAGMENT_LIST)
        widgetItemAdapter.setOnItemClickListener(this)
        binding!!.recyclerView.adapter = widgetItemAdapter

        if (NetworkUtils.isUrlValid(HttpServerUtils.serverAddress)) queryConfig(false)
    }

    override fun initTitle(): TitleBar? {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.menu_client)
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

        binding!!.etSignKey.setText(HttpServerUtils.clientSignKey)
        binding!!.etSignKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.clientSignKey = binding!!.etSignKey.text.toString().trim()
            }
        })

        binding!!.btnServerTest.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_server_test -> {
                if (!NetworkUtils.isUrlValid(HttpServerUtils.serverAddress)) {
                    XToastUtils.error("请输入有效的服务地址")
                    return
                }
                queryConfig(true)
            }
            else -> {}
        }
    }

    override fun onItemClick(itemView: View, item: PageInfo, position: Int) {
        try {
            if (!NetworkUtils.isUrlValid(HttpServerUtils.serverAddress)) {
                XToastUtils.error("请输入有效的服务地址")
                serverConfig = null
                return
            }
            if (serverConfig == null && item.name != ResUtils.getString(R.string.api_clone)) {
                XToastUtils.error("请先点击【测试接口】按钮")
                return
            }
            if (serverConfig != null && (
                        (item.name == ResUtils.getString(R.string.api_sms_send) && !serverConfig!!.enableApiSmsSend)
                                || (item.name == ResUtils.getString(R.string.api_sms_query) && !serverConfig!!.enableApiSmsQuery)
                                || (item.name == ResUtils.getString(R.string.api_call_query) && !serverConfig!!.enableApiCallQuery)
                                || (item.name == ResUtils.getString(R.string.api_contact_query) && !serverConfig!!.enableApiContactQuery)
                                || (item.name == ResUtils.getString(R.string.api_battery_query) && !serverConfig!!.enableApiBatteryQuery)
                        )
            ) {
                XToastUtils.error("服务端未开启此功能")
                return
            }
            @Suppress("UNCHECKED_CAST")
            PageOption.to(Class.forName(item.classPath) as Class<XPageFragment>) //跳转的fragment
                .setNewActivity(true)
                .open(this)
        } catch (e: Exception) {
            e.printStackTrace()
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
                        val resp: BaseResponse<ConfigData> = Gson().fromJson(response, object : TypeToken<BaseResponse<ConfigData>>() {}.type)
                        if (resp.code == 200) {
                            serverConfig = resp.data!!
                            if (needToast) XToastUtils.success(ResUtils.getString(R.string.request_succeeded))
                        } else {
                            if (needToast) XToastUtils.error(ResUtils.getString(R.string.request_failed) + resp.msg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (needToast) XToastUtils.error(ResUtils.getString(R.string.request_failed) + response)
                    }
                }

            })
    }

}