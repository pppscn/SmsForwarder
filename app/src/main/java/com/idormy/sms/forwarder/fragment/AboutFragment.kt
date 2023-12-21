package com.idormy.sms.forwarder.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.webview.AgentWebActivity
import com.idormy.sms.forwarder.databinding.FragmentAboutBinding
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.CacheUtils
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.gotoProtocol
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.previewMarkdown
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.previewPicture
import com.idormy.sms.forwarder.utils.HistoryUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sdkinit.XUpdateInit
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.textview.supertextview.SuperTextView
import com.xuexiang.xutil.file.FileUtils
import frpclib.Frpclib
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Page(name = "关于软件")
class AboutFragment : BaseFragment<FragmentAboutBinding?>(), SuperTextView.OnSuperTextViewClickListener {

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentAboutBinding {
        return FragmentAboutBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.menu_about)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        binding!!.menuVersion.setLeftString(String.format(resources.getString(R.string.about_app_version), AppUtils.getAppVersionName()))
        binding!!.menuCache.setLeftString(String.format(resources.getString(R.string.about_cache_size), CacheUtils.getTotalCacheSize(requireContext())))

        if (FileUtils.isFileExists(context?.filesDir?.absolutePath + "/libs/libgojni.so")) {
            binding!!.menuFrpc.setLeftString(String.format(resources.getString(R.string.about_frpc_version), Frpclib.getVersion()))
            binding!!.menuFrpc.visibility = View.VISIBLE
        }

        val dateFormat = SimpleDateFormat("yyyy", Locale.CHINA)
        val currentYear = dateFormat.format(Date())
        binding!!.copyright.text = java.lang.String.format(resources.getString(R.string.about_copyright), currentYear)

        binding!!.scbAutoCheckUpdate.isChecked = SettingUtils.autoCheckUpdate
        binding!!.scbAutoCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
            SettingUtils.autoCheckUpdate = isChecked
        }
    }

    override fun initListeners() {
        binding!!.btnUpdate.setOnClickListener {
            XUpdateInit.checkUpdate(requireContext(), true)
        }
        binding!!.btnCache.setOnClickListener {
            HistoryUtils.clearPreference()
            CacheUtils.clearAllCache(requireContext())
            XToastUtils.success(R.string.about_cache_purged)
            binding!!.menuCache.setLeftString(String.format(resources.getString(R.string.about_cache_size), CacheUtils.getTotalCacheSize(requireContext())))
        }
        binding!!.btnFrpc.setOnClickListener {
            try {
                val soFile = File(context?.filesDir?.absolutePath + "/libs/libgojni.so")
                if (soFile.exists()) soFile.delete()
                MaterialDialog.Builder(requireContext())
                    .iconRes(R.drawable.ic_menu_frpc)
                    .title(R.string.menu_frpc)
                    .content(R.string.about_frpc_deleted)
                    .cancelable(false)
                    .positiveText(R.string.confirm)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        val intent = Intent(App.context, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AboutFragment", "btnFrpc.setOnClickListener error: ${e.message}")
                XToastUtils.error(e.message.toString())
            }
        }
        binding!!.btnGithub.setOnClickListener {
            AgentWebActivity.goWeb(context, getString(R.string.url_project_github))
        }
        binding!!.btnGitee.setOnClickListener {
            AgentWebActivity.goWeb(context, getString(R.string.url_project_gitee))
        }

        binding!!.menuWechatMiniprogram.setOnSuperTextViewClickListener(this)
        binding!!.menuDonation.setOnSuperTextViewClickListener(this)
        binding!!.menuTelegramGroup.setOnSuperTextViewClickListener(this)
        binding!!.menuDingtalkGroup.setOnSuperTextViewClickListener(this)
        binding!!.menuQqChannel.setOnSuperTextViewClickListener(this)
        binding!!.menuUserProtocol.setOnSuperTextViewClickListener(this)
        binding!!.menuPrivacyProtocol.setOnSuperTextViewClickListener(this)
    }

    @SingleClick
    override fun onClick(v: SuperTextView) {
        when (v.id) {
            R.id.menu_donation -> {
                previewMarkdown(this, getString(R.string.about_item_donation_link), getString(R.string.url_donation_link), false)
            }

            R.id.menu_wechat_miniprogram -> {
                if (HttpServerUtils.safetyMeasures != 3) {
                    XToastUtils.error("微信小程序只支持SM4加密传输！请前往主动控制·服务端修改安全措施！")
                    //return
                }
                previewPicture(this, getString(R.string.url_wechat_miniprogram), null)
            }

            R.id.menu_telegram_group -> {
                previewPicture(this, getString(R.string.url_telegram_group), null)
            }

            R.id.menu_dingtalk_group -> {
                previewPicture(this, getString(R.string.url_dingtalk_group), null)
            }

            R.id.menu_qq_channel -> {
                AgentWebActivity.goWeb(context, getString(R.string.url_qq_channel))
            }

            R.id.menu_user_protocol -> {
                gotoProtocol(this, isPrivacy = false, isImmersive = false)
            }

            R.id.menu_privacy_protocol -> {
                gotoProtocol(this, isPrivacy = true, isImmersive = false)
            }
        }
    }
}