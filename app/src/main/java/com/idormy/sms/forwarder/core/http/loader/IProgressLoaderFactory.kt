package com.idormy.sms.forwarder.core.http.loader

import android.content.Context
import com.xuexiang.xhttp2.subsciber.impl.IProgressLoader

/**
 * IProgressLoader的创建工厂实现接口
 *
 * @author xuexiang
 * @since 2019-11-18 23:17
 */
interface IProgressLoaderFactory {
    /**
     * 创建进度加载者
     *
     * @param context
     * @return
     */
    fun create(context: Context?): IProgressLoader?

    /**
     * 创建进度加载者
     *
     * @param context
     * @param message 默认提示
     * @return
     */
    fun create(context: Context?, message: String?): IProgressLoader?
}