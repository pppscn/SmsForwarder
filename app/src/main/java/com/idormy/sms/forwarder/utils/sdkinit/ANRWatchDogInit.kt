@file:Suppress("MemberVisibilityCanBePrivate")

package com.idormy.sms.forwarder.utils.sdkinit

import com.github.anrwatchdog.ANRError
import com.github.anrwatchdog.ANRWatchDog
import com.github.anrwatchdog.ANRWatchDog.ANRListener
import com.xuexiang.xutil.common.logger.Logger

/**
 * ANR看门狗监听器初始化
 *
 * @author xuexiang
 * @since 2020-02-18 15:08
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ANRWatchDogInit private constructor() {
    companion object {
        private const val TAG = "ANRWatchDog"

        /**
         * ANR看门狗
         */
        var aNRWatchDog: ANRWatchDog? = null
            private set

        /**
         * ANR监听触发的时间
         */
        private const val ANR_DURATION = 4000

        /**
         * ANR静默处理【就是不处理，直接记录一下日志】
         */
        private val SILENT_LISTENER = ANRListener { error: ANRError? -> Logger.eTag(TAG, error) }

        /**
         * ANR自定义处理【可以是记录日志用于上传】
         */
        private val CUSTOM_LISTENER = ANRListener { error: ANRError? ->
            Logger.eTag(TAG, "Detected Application Not Responding!", error)
            throw error!!
        }

        fun init() {
            //这里设置监听的间隔为2秒
            aNRWatchDog = ANRWatchDog(2000)
            aNRWatchDog!!.setANRInterceptor { duration: Long ->
                val ret = ANR_DURATION - duration
                if (ret > 0) {
                    Logger.wTag(
                        TAG,
                        "Intercepted ANR that is too short ($duration ms), postponing for $ret ms."
                    )
                }
                ret
            }.setANRListener(SILENT_LISTENER).start()
        }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}