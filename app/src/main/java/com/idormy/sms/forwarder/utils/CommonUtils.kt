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
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.webview.AgentWebActivity
import com.idormy.sms.forwarder.core.webview.AgentWebFragment
import com.idormy.sms.forwarder.entity.ImageInfo
import com.idormy.sms.forwarder.fragment.MarkdownFragment
import com.idormy.sms.forwarder.fragment.ServiceProtocolFragment
import com.idormy.sms.forwarder.service.NotificationService
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xui.utils.ColorUtils
import com.xuexiang.xui.widget.dialog.DialogLoader
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog.SingleButtonCallback
import com.xuexiang.xui.widget.imageview.preview.PreviewBuilder
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.common.StringUtils
import com.xuexiang.xutil.resource.ResUtils.getString
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.regex.Pattern

/**
 * 常用工具类
 */
@Suppress("RegExpRedundantEscape", "unused", "RegExpUnnecessaryNonCapturingGroup")
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
            val dialog = MaterialDialog.Builder(context).title(R.string.title_reminder).autoDismiss(false).cancelable(false).positiveText(R.string.lab_agree).onPositive { dialog1: MaterialDialog, which: DialogAction? ->
                if (submitListener != null) {
                    submitListener.onClick(dialog1, which!!)
                } else {
                    dialog1.dismiss()
                }
            }.negativeText(R.string.lab_disagree).onNegative { dialog, _ ->
                dialog.dismiss()
                DialogLoader.getInstance().showConfirmDialog(
                    context, getString(R.string.title_reminder), String.format(
                        getString(R.string.content_privacy_explain_again), getString(R.string.app_name)
                    ), getString(R.string.lab_look_again), { dialog, _ ->
                        dialog.dismiss()
                        showPrivacyDialog(context, submitListener)
                    }, getString(R.string.lab_still_disagree)
                ) { dialog, _ ->
                    dialog.dismiss()
                    DialogLoader.getInstance().showConfirmDialog(
                        context, getString(R.string.content_think_about_it_again), getString(R.string.lab_look_again), { dialog, _ ->
                            dialog.dismiss()
                            showPrivacyDialog(context, submitListener)
                        }, getString(R.string.lab_exit_app)
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
            return SpannableStringBuilder().append("    ").append(getString(R.string.privacy_content_1)).append(" ").append(getString(R.string.app_name)).append("!\n").append("    ").append(getString(R.string.privacy_content_2)).append("    ").append(getString(R.string.privacy_content_3)).append(getPrivacyLink(context, PRIVACY_URL)).append(getString(R.string.privacy_content_4)).append("    ").append(getString(R.string.privacy_content_5)).append(getPrivacyLink(context, PRIVACY_URL)).append(getString(R.string.privacy_content_6)).append("    ").append(getString(R.string.privacy_content_7))
        }

        /**
         * @param context 隐私政策的链接
         * @return
         */
        @Suppress("SameParameterValue")
        private fun getPrivacyLink(context: Context, privacyUrl: String): SpannableString {
            val privacyName = String.format(
                getString(R.string.lab_privacy_name), getString(R.string.app_name)
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
            PageOption.to(ServiceProtocolFragment::class.java).putString(
                ServiceProtocolFragment.KEY_PROTOCOL_TITLE, if (isPrivacy) getString(R.string.title_privacy_protocol) else getString(
                    R.string.title_user_protocol
                )
            ).putBoolean(ServiceProtocolFragment.KEY_IS_IMMERSIVE, isImmersive).open(fragment!!)
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
            if (TextUtils.isEmpty(str)) return

            //避免出错：java.lang.IndexOutOfBoundsException: setSpan (36 ... 36) ends beyond length 20
            if (str.length > 20) {
                editText.text.append(str)
                return
            }

            editText.isFocusable = true
            editText.requestFocus()
            val nSection: Int = editText.selectionStart
            editText.text.insert(nSection, str)
            editText.setSelection(nSection + str.length)
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
            PreviewBuilder.from(fragment).setImgs(ImageInfo.newInstance(url, bounds)).setCurrentIndex(0).setSingleFling(true).setProgressColor(R.color.xui_config_color_main_theme).setType(PreviewBuilder.IndicatorType.Number).start()
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
            PageOption.to(MarkdownFragment::class.java).putString(MarkdownFragment.KEY_MD_TITLE, title).putString(MarkdownFragment.KEY_MD_URL, url).putBoolean(MarkdownFragment.KEY_IS_IMMERSIVE, isImmersive).open(fragment!!)
        }

        //检查自定义模板中的标签是否合法
        fun checkTemplateTag(template: String): String {
            val tagRegex = "\\{\\{[^#]+###([^=]+)===(.*?)\\}\\}".toRegex()
            tagRegex.findAll(template).forEach {
                try {
                    it.groupValues[1].toRegex()
                    //TODO:怎么测试反向引用是否正确？
                    /*val replacement = it.groupValues[2]
                    if (replacement.isNotEmpty()) {
                        "pppscn/SmsForwarder".replace(regex, replacement)
                    }*/
                } catch (e: Exception) {
                    return String.format(getString(R.string.invalid_tag), it.value, e.message)
                }
            }
            return ""
        }

        //是否合法的url
        fun checkUrl(url: String?, emptyResult: Boolean = false): Boolean {
            if (url.isNullOrEmpty()) return emptyResult

            val regex = Regex("^https?://\\S+\$")
            return regex.matches(url)
        }

        //是否合法的URL Scheme
        fun checkUrlScheme(url: String?, emptyResult: Boolean = false): Boolean {
            if (url.isNullOrEmpty()) return emptyResult

            val regex = Regex("^[a-zA-Z\\d]+://\\S+\$")
            return regex.matches(url)
        }

        //是否合法的IP地址
        fun checkIP(ip: String): String {
            if (TextUtils.isEmpty(ip)) return "Neither"

            if (ip.contains(".")) {
                val chunkIPv4 = "([\\d]|[1-9][\\d]|1[\\d][\\d]|2[0-4][\\d]|25[0-5])"
                val pattenIPv4 = Pattern.compile("^($chunkIPv4\\.){3}$chunkIPv4$")
                return if (pattenIPv4.matcher(ip).matches()) "IPv4" else "Neither"
            } else if (ip.contains(":")) {
                val chunkIPv6 = "([\\da-fA-F]{1,4})"
                val pattenIPv6 = Pattern.compile("^($chunkIPv6\\:){7}$chunkIPv6$")
                return if (pattenIPv6.matcher(ip).matches()) "IPv6" else "Neither"
            }
            return "Neither"
        }

        //是否合法的域名
        fun checkDomain(domain: String): Boolean {
            val pattenDomain = Pattern.compile("^(?=^.{3,255}$)(?:(?:(?:[a-zA-Z\\d]|[a-zA-Z\\d][a-zA-Z\\d\\-]*[a-zA-Z\\d])\\.){1,126}(?:[A-Za-z\\d]|[A-Za-z\\d][A-Za-z\\d\\-]*[A-Za-z\\d]))$")
            return pattenDomain.matcher(domain).matches()
        }

        //是否合法的端口号
        fun checkPort(port: String): Boolean {
            if (TextUtils.isEmpty(port)) return false
            val pattenPort = Pattern.compile("^((6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])|[0-5]?\\d{0,4})$")
            return pattenPort.matcher(port).matches()
        }

        fun checkEmail(email: String): Boolean {
            val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$")
            return emailRegex.matches(email)
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
                ComponentName(context.applicationContext, NotificationService::class.java), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ComponentName(context.applicationContext, NotificationService::class.java), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        }

        //获取本机IP地址
        fun getIPAddresses(): List<String> {
            val ipAddresses = mutableListOf<String>()

            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()

                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    val addresses = networkInterface.inetAddresses

                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()

                        if (address is Inet4Address || address is Inet6Address) {
                            address.hostAddress?.let { ipAddresses.add(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CommonUtils", "getIPAddresses: ${e.message}")
            }

            return ipAddresses
        }

        fun removeInterfaceFromIP(ipAddress: String): String {
            val index = ipAddress.indexOf("%")
            return if (index != -1) {
                ipAddress.substring(0, index)
            } else {
                ipAddress
            }
        }

        fun restartApplication() {
            val context = App.context
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            XUtil.exitApp()
        }

        /*fun switchLanguage(oldLocale: Locale, newLocale: Locale) {
            val oldLang = if (TAG_LANG.contains(oldLocale.toString())) oldLocale.toString() else "en"
            val newLang = if (TAG_LANG.contains(newLocale.toString())) newLocale.toString() else "en"
            Log.i(App.TAG, "switchLanguage: oldLang=$oldLang, newLang=$newLang")

            //替换自定义模板标签
            var smsTemplate = SettingUtils.smsTemplate
            //替换Rule.sms_template中的标签
            var ruleColumn = "sms_template"
            //替换Sender.json_setting中的标签
            var senderColumn = "json_setting"

            for (i in TAG_LIST.indices) {
                val oldTag = TAG_LIST[i][oldLang].toString()
                val newTag = TAG_LIST[i][newLang].toString()
                if (oldTag == newTag) continue

                smsTemplate = smsTemplate.replace(oldTag, newTag)
                ruleColumn = "REPLACE($ruleColumn, '$oldTag', '$newTag')"
                senderColumn = "REPLACE($senderColumn, '$oldTag', '$newTag')"
            }

            SettingUtils.smsTemplate = smsTemplate

            val updateRuleSql = "UPDATE Rule SET sms_template = $ruleColumn WHERE sms_template != ''"
            Log.d(App.TAG, "updateRuleSql: $updateRuleSql")
            Core.rule.replaceTags(updateRuleSql)

            val updateSenderSql = "UPDATE Sender SET json_setting = $senderColumn WHERE type NOT IN (4, 5, 6, 7, 8, 14)"
            Log.d(App.TAG, "updateSenderSql: $updateSenderSql")
            Core.sender.replaceTags(updateSenderSql)
        }*/
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}