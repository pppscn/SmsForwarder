package com.idormy.sms.forwarder.utils

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.webview.AgentWebActivity
import com.idormy.sms.forwarder.core.webview.AgentWebFragment
import com.idormy.sms.forwarder.entity.ImageInfo
import com.idormy.sms.forwarder.fragment.MarkdownFragment
import com.idormy.sms.forwarder.fragment.ServiceProtocolFragment
import com.idormy.sms.forwarder.service.NotifyService
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.utils.ColorUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.widget.dialog.DialogLoader
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog.SingleButtonCallback
import com.xuexiang.xui.widget.imageview.preview.PreviewBuilder
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.common.StringUtils
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

/**
 * 常用工具类
 */
@Suppress("RegExpRedundantEscape", "unused")
class CommonUtils private constructor() {
    companion object {
        /**
         * 这里填写你的应用隐私政策网页地址
         */
        private const val PRIVACY_URL = "https://gitee.com/pp/SmsForwarder/raw/main/PRIVACY"

        /**
         * 显示隐私政策的提示
         *
         * @param context
         * @param submitListener 同意的监听
         * @return
         */
        @Suppress("SameParameterValue", "NAME_SHADOWING")
        @JvmStatic
        fun showPrivacyDialog(context: Context, submitListener: SingleButtonCallback?): Dialog {
            val dialog =
                MaterialDialog.Builder(context).title(R.string.title_reminder).autoDismiss(false)
                    .cancelable(false)
                    .positiveText(R.string.lab_agree)
                    .onPositive { dialog1: MaterialDialog, which: DialogAction? ->
                        if (submitListener != null) {
                            submitListener.onClick(dialog1, which!!)
                        } else {
                            dialog1.dismiss()
                        }
                    }
                    .negativeText(R.string.lab_disagree).onNegative { dialog, _ ->
                        dialog.dismiss()
                        DialogLoader.getInstance().showConfirmDialog(
                            context,
                            ResUtils.getString(R.string.title_reminder),
                            String.format(
                                ResUtils.getString(R.string.content_privacy_explain_again),
                                ResUtils.getString(R.string.app_name)
                            ),
                            ResUtils.getString(R.string.lab_look_again),
                            { dialog, _ ->
                                dialog.dismiss()
                                showPrivacyDialog(context, submitListener)
                            },
                            ResUtils.getString(R.string.lab_still_disagree)
                        ) { dialog, _ ->
                            dialog.dismiss()
                            DialogLoader.getInstance().showConfirmDialog(
                                context,
                                ResUtils.getString(R.string.content_think_about_it_again),
                                ResUtils.getString(R.string.lab_look_again),
                                { dialog, _ ->
                                    dialog.dismiss()
                                    showPrivacyDialog(context, submitListener)
                                },
                                ResUtils.getString(R.string.lab_exit_app)
                            ) { dialog, _ ->
                                dialog.dismiss()
                                XUtil.exitApp()
                            }
                        }
                    }.build()
            dialog.setContent(getPrivacyContent(context))
            //开始响应点击事件
            dialog.contentView!!.movementMethod = LinkMovementMethod.getInstance()
            dialog.show()
            return dialog
        }

        /**
         * @return 隐私政策说明
         */
        private fun getPrivacyContent(context: Context): SpannableStringBuilder {
            return SpannableStringBuilder()
                .append("    ").append(ResUtils.getString(R.string.privacy_content_1)).append(" ").append(ResUtils.getString(R.string.app_name)).append("!\n")
                .append("    ").append(ResUtils.getString(R.string.privacy_content_2))
                .append("    ").append(ResUtils.getString(R.string.privacy_content_3))
                .append(getPrivacyLink(context, PRIVACY_URL))
                .append(ResUtils.getString(R.string.privacy_content_4))
                .append("    ").append(ResUtils.getString(R.string.privacy_content_5))
                .append(getPrivacyLink(context, PRIVACY_URL))
                .append(ResUtils.getString(R.string.privacy_content_6))
        }

        /**
         * @param context 隐私政策的链接
         * @return
         */
        @Suppress("SameParameterValue")
        private fun getPrivacyLink(context: Context, privacyUrl: String): SpannableString {
            val privacyName = String.format(
                ResUtils.getString(R.string.lab_privacy_name),
                ResUtils.getString(R.string.app_name)
            )
            val spannableString = SpannableString(privacyName)
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    goWeb(context, privacyUrl)
                }
            }, 0, privacyName.length, Spanned.SPAN_MARK_MARK)
            return spannableString
        }

        /**
         * 请求浏览器
         *
         * @param url
         */
        @JvmStatic
        fun goWeb(context: Context, url: String?) {
            val intent = Intent(context, AgentWebActivity::class.java)
            intent.putExtra(AgentWebFragment.KEY_URL, url)
            context.startActivity(intent)
        }

        /**
         * 打开用户协议和隐私协议
         *
         * @param fragment
         * @param isPrivacy   是否是隐私协议
         * @param isImmersive 是否沉浸式
         */
        @JvmStatic
        fun gotoProtocol(fragment: XPageFragment?, isPrivacy: Boolean, isImmersive: Boolean) {
            PageOption.to(ServiceProtocolFragment::class.java)
                .putString(
                    ServiceProtocolFragment.KEY_PROTOCOL_TITLE,
                    if (isPrivacy) ResUtils.getString(R.string.title_privacy_protocol) else ResUtils.getString(
                        R.string.title_user_protocol
                    )
                )
                .putBoolean(ServiceProtocolFragment.KEY_IS_IMMERSIVE, isImmersive)
                .open(fragment!!)
        }

        /**
         * 是否是深色的颜色
         *
         * @param color
         * @return
         */
        @JvmStatic
        fun isColorDark(@ColorInt color: Int): Boolean {
            return ColorUtils.isColorDark(color, 0.382)
        }

        //焦点位置插入文本
        fun insertOrReplaceText2Cursor(editText: EditText, str: String) {
            editText.isFocusable = true
            editText.requestFocus()
            val start = max(editText.selectionStart, 0)
            val end = max(editText.selectionEnd, 0)
            editText.text.replace(min(start, end), max(start, end), str, 0, str.length)
        }

        //==========图片预览===========//
        /**
         * 大图预览
         *
         * @param fragment
         * @param url      图片资源
         * @param view     小图加载控件
         */
        fun previewPicture(fragment: Fragment?, url: String, view: View?) {
            if (fragment == null || StringUtils.isEmpty(url)) {
                return
            }
            val bounds = Rect()
            view?.getGlobalVisibleRect(bounds)
            PreviewBuilder.from(fragment)
                .setImgs(ImageInfo.newInstance(url, bounds))
                .setCurrentIndex(0)
                .setSingleFling(true)
                .setProgressColor(R.color.xui_config_color_main_theme)
                .setType(PreviewBuilder.IndicatorType.Number)
                .start()
        }

        /**
         * 打开Markdown链接并渲染
         *
         * @param fragment
         * @param url   Markdown链接
         * @param isImmersive 是否沉浸式
         */
        @JvmStatic
        fun previewMarkdown(fragment: XPageFragment?, title: String, url: String, isImmersive: Boolean) {
            PageOption.to(MarkdownFragment::class.java)
                .putString(MarkdownFragment.KEY_MD_TITLE, title)
                .putString(MarkdownFragment.KEY_MD_URL, url)
                .putBoolean(MarkdownFragment.KEY_IS_IMMERSIVE, isImmersive)
                .open(fragment!!)
        }

        //是否合法的url
        fun checkUrl(urls: String?): Boolean {
            return checkUrl(urls, false)
        }

        //是否合法的url
        fun checkUrl(urls: String?, emptyResult: Boolean): Boolean {
            if (TextUtils.isEmpty(urls)) return emptyResult
            val regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
            val pat = Pattern.compile(regex)
            val mat = pat.matcher(urls?.trim() ?: "")
            return mat.matches()
        }

        //是否启用通知监听服务
        fun isNotificationListenerServiceEnabled(context: Context): Boolean {
            val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
            return packageNames.contains(context.packageName)
        }

        //开关通知监听服务
        fun toggleNotificationListenerService(context: Context) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                ComponentName(context.applicationContext, NotifyService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ComponentName(context.applicationContext, NotifyService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        }

    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}