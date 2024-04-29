package com.idormy.sms.forwarder.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.BuildConfig
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.webview.AgentWebActivity
import com.idormy.sms.forwarder.databinding.FragmentAboutBinding
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.CacheUtils
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.gotoProtocol
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.previewMarkdown
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.previewPicture
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.restartApplication
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

        if (App.FrpclibInited) {
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

        binding!!.sbJoinPreviewProgram.isChecked = SettingUtils.joinPreviewProgram
        binding!!.sbJoinPreviewProgram.setOnCheckedChangeListener { _, isChecked ->
            SettingUtils.joinPreviewProgram = isChecked
            if (isChecked) {
                XToastUtils.success(getString(R.string.join_preview_program_tips))
            }
        }
    }

    override fun initListeners() {
        binding!!.btnUpdate.setOnClickListener {
            XUpdateInit.checkUpdate(requireContext(), true, SettingUtils.joinPreviewProgram)
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
                        restartApplication()
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

        binding!!.menuJoinPreviewProgram.setOnSuperTextViewClickListener(this)
        binding!!.menuVersion.setOnSuperTextViewClickListener(this)
        binding!!.menuWechatMiniprogram.setOnSuperTextViewClickListener(this)
        binding!!.menuDonation.setOnSuperTextViewClickListener(this)
        binding!!.menuUserProtocol.setOnSuperTextViewClickListener(this)
        binding!!.menuPrivacyProtocol.setOnSuperTextViewClickListener(this)
    }

    @SingleClick
    override fun onClick(v: SuperTextView) {
        when (v.id) {
            R.id.menu_join_preview_program -> {
                XToastUtils.info(getString(R.string.join_preview_program_tips))
            }

            R.id.menu_version -> {
                XToastUtils.info(
                    String.format(
                        getString(R.string.about_app_version_tips),
                        AppUtils.getAppVersionName(),
                        AppUtils.getAppVersionCode(),
                        BuildConfig.BUILD_TIME,
                        BuildConfig.GIT_COMMIT_ID
                    )
                )
            }

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

            R.id.menu_user_protocol -> {
                gotoProtocol(this, isPrivacy = false, isImmersive = false)
            }

            R.id.menu_privacy_protocol -> {
                gotoProtocol(this, isPrivacy = true, isImmersive = false)
            }
        }
    }
}