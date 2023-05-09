package com.idormy.sms.forwarder.utils.sdkinit

import android.app.Application
import android.content.Context
import com.idormy.sms.forwarder.BuildConfig
import com.idormy.sms.forwarder.utils.SettingUtils.Companion.isAgreePrivacy
//import com.meituan.android.walle.WalleChannelReader
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.xuexiang.xui.XUI

/**
 * UMeng 统计 SDK初始化
 *
 * @author xuexiang
 * @since 2019-06-18 15:49
 */
class UMengInit private constructor() {
    companion object {
        private const val DEFAULT_CHANNEL_ID = "github"
        /**
         * 初始化SDK,合规指南【先进行预初始化，如果用户隐私同意后可以初始化UmengSDK进行信息上报】
         */
        /**
         * 初始化SDK,合规指南【先进行预初始化，如果用户隐私同意后可以初始化UmengSDK进行信息上报】
         */
        @JvmOverloads
        fun init(context: Context = XUI.getContext()) {
            val appContext = context.applicationContext
            if (appContext is Application) {
                initApplication(appContext)
            }
        }

        /**
         * 初始化SDK,合规指南【先进行预初始化，如果用户隐私同意后可以初始化UmengSDK进行信息上报】
         */
        private fun initApplication(application: Application?) {
            // 运营统计数据调试运行时不初始化
            if (com.idormy.sms.forwarder.App.isDebug) {
                return
            }
            UMConfigure.setLogEnabled(false)
            UMConfigure.preInit(application, BuildConfig.APP_ID_UMENG, getChannel()) //getChannel(application)
            // 用户同意了隐私协议
            if (isAgreePrivacy) {
                realInit(application)
            }
        }

        /**
         * 真实的初始化UmengSDK【进行设备信息的统计上报，必须在获得用户隐私同意后方可调用】
         */
        private fun realInit(application: Application?) {
            // 运营统计数据调试运行时不初始化
            if (com.idormy.sms.forwarder.App.isDebug) {
                return
            }
            //初始化组件化基础库, 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，UMConfigure.init调用中appkey和channel参数请置为null）。
            //第二个参数是appkey，最后一个参数是pushSecret
            //这里BuildConfig.APP_ID_UMENG是根据local.properties中定义的APP_ID_UMENG生成的，只是运行看效果的话，可以不初始化该SDK
            UMConfigure.init(
                application,
                BuildConfig.APP_ID_UMENG,
                getChannel(), //getChannel(application)
                UMConfigure.DEVICE_TYPE_PHONE,
                ""
            )
            //统计SDK是否支持采集在子进程中打点的自定义事件，默认不支持
            //支持多进程打点
            UMConfigure.setProcessEvent(true)
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
        }

        /**
         * 获取渠道信息
         */
        private fun getChannel(): String { //context: Context?
            //return WalleChannelReader.getChannel(context!!, DEFAULT_CHANNEL_ID)
            return DEFAULT_CHANNEL_ID
        }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}