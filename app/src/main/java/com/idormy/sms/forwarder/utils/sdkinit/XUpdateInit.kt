package com.idormy.sms.forwarder.utils.sdkinit

import android.app.Application
import android.content.Context
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.BuildConfig
import com.idormy.sms.forwarder.utils.KEY_PREVIEW_URL
import com.idormy.sms.forwarder.utils.KEY_UPDATE_URL
import com.idormy.sms.forwarder.utils.update.CustomUpdateDownloader
import com.idormy.sms.forwarder.utils.update.CustomUpdateFailureListener
import com.idormy.sms.forwarder.utils.update.XHttpUpdateHttpServiceImpl
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.utils.UpdateUtils
import com.xuexiang.xutil.common.StringUtils

/**
 * XUpdate 版本更新 SDK 初始化
 *
 * 详细使用参见：https://github.com/xuexiangjys/XUpdate/wiki
 *
 * @author xuexiang
 * @since 2019-06-18 15:51
 */
@Suppress("SameParameterValue")
class XUpdateInit private constructor() {
    companion object {
        /**
         * 应用版本更新的检查地址
         */
        fun init(application: Application) {
            XUpdate.get()
                .debug(App.isDebug)
                //默认设置只在wifi下检查版本更新
                .isWifiOnly(false)
                //默认设置使用get请求检查版本
                .isGet(true)
                //默认设置非自动模式，可根据具体使用配置
                //.isAutoMode(false)
                //设置默认公共请求参数
                .param("versionCode", UpdateUtils.getVersionCode(application))
                .param("appKey", application.packageName)
                //设置预览计划请求参数
                .param("versionName", UpdateUtils.getVersionName(application))
                .param("buildTime", BuildConfig.BUILD_TIME)
                .param("gitCommitId", BuildConfig.GIT_COMMIT_ID)
                //这个必须设置！实现网络请求功能。
                .setIUpdateHttpService(XHttpUpdateHttpServiceImpl())
                .setIUpdateDownLoader(CustomUpdateDownloader())
                //这个必须初始化
                .init(application)
        }

        /**
         * 进行版本更新检查
         */
        fun checkUpdate(context: Context, needErrorTip: Boolean, joinPreviewProgram: Boolean) {
            if (joinPreviewProgram) {
                checkUpdate(context, KEY_PREVIEW_URL, needErrorTip)
            } else {
                checkUpdate(context, KEY_UPDATE_URL, needErrorTip)
            }
        }

        /**
         * 进行版本更新检查
         *
         * @param context      上下文
         * @param url          版本更新检查的地址
         * @param needErrorTip 是否需要错误的提示
         */
        private fun checkUpdate(context: Context, url: String, needErrorTip: Boolean) {
            if (StringUtils.isEmpty(url)) {
                return
            }
            XUpdate.newBuild(context).updateUrl(url).update()
            XUpdate.get().debug(App.isDebug).setOnUpdateFailureListener(CustomUpdateFailureListener(needErrorTip))
        }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}