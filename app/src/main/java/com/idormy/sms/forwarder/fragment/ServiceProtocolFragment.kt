package com.idormy.sms.forwarder.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentServiceProtocolBinding
import com.xuexiang.xaop.annotation.MemoryCache
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xutil.resource.ResUtils
import com.xuexiang.xutil.resource.ResourceUtils

/**
 * 服务协议【本地加载】
 *
 * @author xuexiang
 * @since 2021/5/18 1:35 AM
 */
@Suppress("REDUNDANT_MODIFIER_IN_GETTER")
@Page
class ServiceProtocolFragment : BaseFragment<FragmentServiceProtocolBinding?>() {
    @JvmField
    @AutoWired(name = KEY_PROTOCOL_TITLE)
    var title: String? = null

    @JvmField
    @AutoWired(name = KEY_IS_IMMERSIVE)
    var isImmersive = false
    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentServiceProtocolBinding {
        return FragmentServiceProtocolBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        return super.initTitle()!!.setTitle(title).setImmersive(isImmersive)
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        if (title == ResUtils.getString(R.string.title_user_protocol)) {
            binding!!.tvProtocolText.text = accountProtocol
        } else {
            binding!!.tvProtocolText.text = privacyProtocol
        }
    }

    @get:MemoryCache("account_protocol")
    private val accountProtocol: String
        private get() = ResourceUtils.readStringFromAssert(ACCOUNT_PROTOCOL_ASSET_PATH)

    @get:MemoryCache("privacy_protocol")
    private val privacyProtocol: String
        private get() = ResourceUtils.readStringFromAssert(PRIVACY_PROTOCOL_ASSET_PATH)

    companion object {
        const val KEY_PROTOCOL_TITLE = "key_protocol_title"
        const val KEY_IS_IMMERSIVE = "key_is_immersive"

        /**
         * 用户协议asset本地保存路径
         */
        private const val ACCOUNT_PROTOCOL_ASSET_PATH = "protocol/account_protocol.txt"

        /**
         * 隐私政策asset本地保存路径
         */
        private const val PRIVACY_PROTOCOL_ASSET_PATH = "protocol/privacy_protocol.txt"
    }
}