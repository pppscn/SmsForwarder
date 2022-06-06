package com.idormy.sms.forwarder.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.viewbinding.ViewBinding
import com.xuexiang.xpage.base.XPageActivity
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.CoreSwitchBean
import com.xuexiang.xrouter.facade.service.SerializationService
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.slideback.SlideBack
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * 基础容器Activity
 *
 * @author XUE
 * @since 2019/3/22 11:21
 */
@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
open class BaseActivity<Binding : ViewBinding?> : XPageActivity() {
    /**
     * 获取Binding
     *
     * @return Binding
     */
    /**
     * ViewBinding
     */
    var binding: Binding? = null
        protected set

    override fun attachBaseContext(newBase: Context) {
        //注入字体
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun getCustomRootView(): View? {
        binding = viewBindingInflate(layoutInflater)
        return if (binding != null) binding!!.root else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initStatusBarStyle()
        super.onCreate(savedInstanceState)
        registerSlideBack()
    }

    /**
     * 构建ViewBinding
     *
     * @param inflater  inflater
     * @return ViewBinding
     */
    protected open fun viewBindingInflate(inflater: LayoutInflater?): Binding? {
        return null
    }

    /**
     * 初始化状态栏的样式
     */
    protected open fun initStatusBarStyle() {}

    /**
     * 打开fragment
     *
     * @param clazz          页面类
     * @param addToBackStack 是否添加到栈中
     * @return 打开的fragment对象
     */
    fun <T : XPageFragment?> openPage(clazz: Class<T>?, addToBackStack: Boolean): T {
        val page = CoreSwitchBean(clazz)
            .setAddToBackStack(addToBackStack)
        return openPage(page) as T
    }

    /**
     * 打开fragment
     *
     * @return 打开的fragment对象
     */
    fun <T : XPageFragment?> openNewPage(clazz: Class<T>?): T {
        val page = CoreSwitchBean(clazz)
            .setNewActivity(true)
        return openPage(page) as T
    }

    /**
     * 切换fragment
     *
     * @param clazz 页面类
     * @return 打开的fragment对象
     */
    fun <T : XPageFragment?> switchPage(clazz: Class<T>?): T {
        return openPage(clazz, false)
    }

    /**
     * 序列化对象
     *
     * @param object
     * @return
     */
    fun serializeObject(`object`: Any?): String {
        return XRouter.getInstance().navigation(SerializationService::class.java)
            .object2Json(`object`)
    }

    override fun onRelease() {
        unregisterSlideBack()
        super.onRelease()
    }

    /**
     * 注册侧滑回调
     */
    protected fun registerSlideBack() {
        if (isSupportSlideBack) {
            SlideBack.with(this)
                .haveScroll(true)
                .edgeMode(if (ResUtils.isRtl()) SlideBack.EDGE_RIGHT else SlideBack.EDGE_LEFT)
                .callBack { popPage() }
                .register()
        }
    }

    /**
     * 注销侧滑回调
     */
    protected fun unregisterSlideBack() {
        if (isSupportSlideBack) {
            SlideBack.unregister(this)
        }
    }

    /**
     * @return 是否支持侧滑返回
     */
    protected open val isSupportSlideBack: Boolean
        get() {
            val page: CoreSwitchBean? = intent.getParcelableExtra(CoreSwitchBean.KEY_SWITCH_BEAN)
            return page == null || page.bundle == null || page.bundle.getBoolean(
                KEY_SUPPORT_SLIDE_BACK,
                true
            )
        }

    companion object {
        /**
         * 是否支持侧滑返回
         */
        const val KEY_SUPPORT_SLIDE_BACK = "key_support_slide_back"
    }
}