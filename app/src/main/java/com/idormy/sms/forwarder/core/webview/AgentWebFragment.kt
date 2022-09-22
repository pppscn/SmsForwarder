package com.idormy.sms.forwarder.core.webview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.XToastUtils
import com.just.agentweb.action.PermissionInterceptor
import com.just.agentweb.core.AgentWeb
import com.just.agentweb.core.client.MiddlewareWebChromeBase
import com.just.agentweb.core.client.MiddlewareWebClientBase
import com.just.agentweb.core.client.WebListenerManager
import com.just.agentweb.core.web.AbsAgentWebSettings
import com.just.agentweb.core.web.AgentWebConfig
import com.just.agentweb.core.web.IAgentWebSettings
import com.just.agentweb.download.AgentWebDownloader.Extra
import com.just.agentweb.download.DefaultDownloadImpl
import com.just.agentweb.download.DownloadListenerAdapter
import com.just.agentweb.download.DownloadingService
import com.just.agentweb.utils.LogUtils
import com.just.agentweb.widget.IWebLayout
import com.xuexiang.xutil.net.JsonUtil

/**
 * 通用WebView页面
 *
 * @author xuexiang
 * @since 2019/1/4 下午11:13
 */
@Suppress(
    "unused",
    "MemberVisibilityCanBePrivate",
    "ProtectedInFinal",
    "NAME_SHADOWING",
    "UNUSED_PARAMETER",
    "OVERRIDE_DEPRECATION"
)
class AgentWebFragment : Fragment(), FragmentKeyDown {
    private var mBackImageView: ImageView? = null
    private var mLineView: View? = null
    private var mFinishImageView: ImageView? = null
    private var mTitleTextView: TextView? = null
    private var mAgentWeb: AgentWeb? = null
    private var mMoreImageView: ImageView? = null
    private var mPopupMenu: PopupMenu? = null
    private var mDownloadingService: DownloadingService? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_agentweb, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAgentWeb = AgentWeb.with(this) //传入AgentWeb的父控件。
            .setAgentWebParent(
                (view as LinearLayout),
                -1,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            ) //设置进度条颜色与高度，-1为默认值，高度为2，单位为dp。
            .useDefaultIndicator(-1, 3) //设置 IAgentWebSettings。
            .setAgentWebWebSettings(settings) //WebViewClient ， 与 WebView 使用一致 ，但是请勿获取WebView调用setWebViewClient(xx)方法了,会覆盖AgentWeb DefaultWebClient,同时相应的中间件也会失效。
            .setWebViewClient(mWebViewClient) //WebChromeClient
            .setWebChromeClient(mWebChromeClient) //设置WebChromeClient中间件，支持多个WebChromeClient，AgentWeb 3.0.0 加入。
            .useMiddlewareWebChrome(middlewareWebChrome) //设置WebViewClient中间件，支持多个WebViewClient， AgentWeb 3.0.0 加入。
            .useMiddlewareWebClient(middlewareWebClient) //权限拦截 2.0.0 加入。
            .setPermissionInterceptor(mPermissionInterceptor) //严格模式 Android 4.2.2 以下会放弃注入对象 ，使用AgentWebView没影响。
            .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK) //自定义UI  AgentWeb3.0.0 加入。
            .setAgentWebUIController(UIController(requireActivity())) //参数1是错误显示的布局，参数2点击刷新控件ID -1表示点击整个布局都刷新， AgentWeb 3.0.0 加入。
            .setMainFrameErrorView(R.layout.agentweb_error_page, -1)
            .setWebLayout(webLayout)
            .interceptUnkownUrl() //创建AgentWeb。
            .createAgentWeb()
            .ready() //设置 WebSettings。
            //WebView载入该url地址的页面并显示。
            .go(url)
        if (com.idormy.sms.forwarder.App.isDebug) {
            AgentWebConfig.debug()
        }

        // 得到 AgentWeb 最底层的控件
        addBackgroundChild(mAgentWeb!!.webCreator.webParentLayout)
        initView(view)

        // AgentWeb 没有把WebView的功能全面覆盖 ，所以某些设置 AgentWeb 没有提供，请从WebView方面入手设置。
        mAgentWeb!!.webCreator.webView.overScrollMode = WebView.OVER_SCROLL_NEVER
    }

    protected val webLayout: IWebLayout<*, *>
        get() = WebLayout(activity)

    protected fun initView(view: View) {
        mBackImageView = view.findViewById(R.id.iv_back)
        mLineView = view.findViewById(R.id.view_line)
        mFinishImageView = view.findViewById(R.id.iv_finish)
        mTitleTextView = view.findViewById(R.id.toolbar_title)
        mBackImageView?.setOnClickListener(mOnClickListener)
        mFinishImageView?.setOnClickListener(mOnClickListener)
        mMoreImageView = view.findViewById(R.id.iv_more)
        mMoreImageView?.setOnClickListener(mOnClickListener)
        pageNavigator(View.GONE)
    }

    protected fun addBackgroundChild(frameLayout: FrameLayout) {
        val textView = TextView(frameLayout.context)
        textView.text = getString(R.string.provided_by_agentweb)
        textView.textSize = 16f
        textView.setTextColor(Color.parseColor("#727779"))
        frameLayout.setBackgroundColor(Color.parseColor("#272b2d"))
        val params = FrameLayout.LayoutParams(-2, -2)
        params.gravity = Gravity.CENTER_HORIZONTAL
        val scale = frameLayout.context.resources.displayMetrics.density
        params.topMargin = (15 * scale + 0.5f).toInt()
        frameLayout.addView(textView, 0, params)
    }

    private fun pageNavigator(tag: Int) {
        mBackImageView!!.visibility = tag
        mLineView!!.visibility = tag
    }

    private val mOnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.iv_back ->                     // true表示AgentWeb处理了该事件
                if (!mAgentWeb!!.back()) {
                    this.requireActivity().finish()
                }
            R.id.iv_finish -> this.requireActivity().finish()
            R.id.iv_more -> showPoPup(v)
            else -> {}
        }
    }
    //========================================//
    /**
     * 权限申请拦截器
     */
    protected var mPermissionInterceptor = PermissionInterceptor { url, permissions, action ->

        /**
         * PermissionInterceptor 能达到 url1 允许授权， url2 拒绝授权的效果。
         * @param url
         * @param permissions
         * @param action
         * @return true 该Url对应页面请求权限进行拦截 ，false 表示不拦截。
         */
        /**
         * PermissionInterceptor 能达到 url1 允许授权， url2 拒绝授权的效果。
         * @param url
         * @param permissions
         * @param action
         * @return true 该Url对应页面请求权限进行拦截 ，false 表示不拦截。
         */
        Log.i(
            TAG,
            "mUrl:" + url + "  permission:" + JsonUtil.toJson(permissions) + " action:" + action
        )
        false
    }
    //=====================下载============================//
    /**
     * 更新于 AgentWeb 4.0.0，下载监听
     */
    protected var mDownloadListenerAdapter: DownloadListenerAdapter =
        object : DownloadListenerAdapter() {
            /**
             *
             * @param url                下载链接
             * @param userAgent          UserAgent
             * @param contentDisposition ContentDisposition
             * @param mimetype           资源的媒体类型
             * @param contentLength      文件长度
             * @param extra              下载配置 ， 用户可以通过 Extra 修改下载icon ， 关闭进度条 ， 是否强制下载。
             * @return true 表示用户处理了该下载事件 ， false 交给 AgentWeb 下载
             */
            override fun onStart(
                url: String,
                userAgent: String,
                contentDisposition: String,
                mimetype: String,
                contentLength: Long,
                extra: Extra,
            ): Boolean {
                LogUtils.i(TAG, "onStart:$url")
                // 是否开启断点续传
                extra.setOpenBreakPointDownload(true) //下载通知的icon
                    .setIcon(R.drawable.ic_file_download_black_24dp) // 连接的超时时间
                    .setConnectTimeOut(6000) // 以8KB位单位，默认60s ，如果60s内无法从网络流中读满8KB数据，则抛出异常
                    .setBlockMaxTime(10 * 60 * 1000) // 下载的超时时间
                    .setDownloadTimeOut(Long.MAX_VALUE) // 串行下载更节省资源哦
                    .setParallelDownload(false) // false 关闭进度通知
                    .setEnableIndicator(true) // 自定义请求头
                    .addHeader("Cookie", "xx") // 下载完成自动打开
                    .setAutoOpen(true).isForceDownload = true
                return false
            }

            /**
             *
             * 不需要暂停或者停止下载该方法可以不必实现
             * @param url
             * @param downloadingService  用户可以通过 DownloadingService#shutdownNow 终止下载
             */
            override fun onBindService(url: String, downloadingService: DownloadingService) {
                super.onBindService(url, downloadingService)
                mDownloadingService = downloadingService
                LogUtils.i(TAG, "onBindService:$url  DownloadingService:$downloadingService")
            }

            /**
             * 回调onUnbindService方法，让用户释放掉 DownloadingService。
             * @param url
             * @param downloadingService
             */
            override fun onUnbindService(url: String, downloadingService: DownloadingService) {
                super.onUnbindService(url, downloadingService)
                mDownloadingService = null
                LogUtils.i(TAG, "onUnbindService:$url")
            }

            /**
             *
             * @param url  下载链接
             * @param loaded  已经下载的长度
             * @param length    文件的总大小
             * @param usedTime   耗时 ，单位ms
             * 注意该方法回调在子线程 ，线程名 AsyncTask #XX 或者 AgentWeb # XX
             */
            override fun onProgress(url: String, loaded: Long, length: Long, usedTime: Long) {
                val mProgress = (loaded / java.lang.Float.valueOf(length.toFloat()) * 100).toInt()
                LogUtils.i(TAG, "onProgress:$mProgress")
                super.onProgress(url, loaded, length, usedTime)
            }

            /**
             *
             * @param path 文件的绝对路径
             * @param url  下载地址
             * @param throwable    如果异常，返回给用户异常
             * @return true 表示用户处理了下载完成后续的事件 ，false 默认交给AgentWeb 处理
             */
            override fun onResult(path: String, url: String, throwable: Throwable): Boolean {
                //下载成功
                //if (null == throwable) {
                //do you work
                //} else { //下载失败
                //}
                // true  不会发出下载完成的通知 , 或者打开文件
                return false
            }
        }
    /**
     * AgentWeb 4.0.0 内部删除了 DownloadListener 监听 ，以及相关API ，将 Download 部分完全抽离出来独立一个库，
     * 如果你需要使用 AgentWeb Download 部分 ， 请依赖上 compile 'com.just.agentweb:download:4.0.0 ，
     * 如果你需要监听下载结果，请自定义 AgentWebSetting ， New 出 DefaultDownloadImpl，传入DownloadListenerAdapter
     * 实现进度或者结果监听，例如下面这个例子，如果你不需要监听进度，或者下载结果，下面 setDownloader 的例子可以忽略。
     * @return WebListenerManager
     */
    /**
     * @return IAgentWebSettings
     */
    val settings: IAgentWebSettings<*>
        get() = object : AbsAgentWebSettings() {
            private val mAgentWeb: AgentWeb? = null
            override fun bindAgentWebSupport(agentWeb: AgentWeb) {
                this.mAgentWeb = agentWeb
            }

            /**
             * AgentWeb 4.0.0 内部删除了 DownloadListener 监听 ，以及相关API ，将 Download 部分完全抽离出来独立一个库，
             * 如果你需要使用 AgentWeb Download 部分 ， 请依赖上 compile 'com.just.agentweb:download:4.0.0 ，
             * 如果你需要监听下载结果，请自定义 AgentWebSetting ， New 出 DefaultDownloadImpl，传入DownloadListenerAdapter
             * 实现进度或者结果监听，例如下面这个例子，如果你不需要监听进度，或者下载结果，下面 setDownloader 的例子可以忽略。
             * @return WebListenerManager
             */
            override fun setDownloader(
                webView: WebView,
                downloadListener: DownloadListener?,
            ): WebListenerManager {
                return super.setDownloader(
                    webView,
                    DefaultDownloadImpl
                        .create(
                            requireActivity(),
                            webView,
                            mDownloadListenerAdapter,
                            mDownloadListenerAdapter,
                            this.mAgentWeb.permissionInterceptor
                        )
                )
            }
        }
    //===================WebChromeClient 和 WebViewClient===========================//
    /**
     * 页面空白，请检查scheme是否加上， scheme://host:port/path?query&query 。
     *
     * @return mUrl
     */
    val url: String
        get() {
            var target = ""
            val bundle = arguments
            if (bundle != null) {
                target = bundle.getString(KEY_URL).toString()
            }
            if (TextUtils.isEmpty(target)) {
                target = "https://github.com/xuexiangjys"
            }
            return target
        }
    protected var mWebChromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            Log.i(TAG, "onProgressChanged:$newProgress  view:$view")
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            var title = title
            super.onReceivedTitle(view, title)
            if (mTitleTextView != null && !TextUtils.isEmpty(title)) {
                if (title.length > 10) {
                    title = title.substring(0, 10) + "..."
                }
                mTitleTextView!!.text = title
            }
        }
    }

    @Suppress("DEPRECATION")
    protected var mWebViewClient: WebViewClient = object : WebViewClient() {
        private val timer = HashMap<String, Long?>()
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            super.onReceivedError(view, request, error)
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return shouldOverrideUrlLoading(view, request.url.toString() + "")
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            //intent:// scheme的处理 如果返回false ， 则交给 DefaultWebClient 处理 ， 默认会打开该Activity  ， 如果Activity不存在则跳到应用市场上去.  true 表示拦截
            //例如优酷视频播放 ，intent://play?...package=com.youku.phone;end;
            //优酷想唤起自己应用播放该视频 ， 下面拦截地址返回 true  则会在应用内 H5 播放 ，禁止优酷唤起播放该视频， 如果返回 false ， DefaultWebClient  会根据intent 协议处理 该地址 ， 首先匹配该应用存不存在 ，如果存在 ， 唤起该应用播放 ， 如果不存在 ， 则跳到应用市场下载该应用 .
            return url.startsWith("intent://") && url.contains("com.youku.phone")
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Log.i(TAG, "mUrl:$url onPageStarted  target:$url")
            timer[url] = System.currentTimeMillis()
            //if (url == url) {
            //    pageNavigator(View.GONE)
            //} else {
            pageNavigator(View.VISIBLE)
            //}
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            if (timer[url] != null) {
                val overTime = System.currentTimeMillis()
                val startTime = timer[url]
                Log.i(TAG, "  page mUrl:" + url + "  used time:" + (overTime - startTime!!))
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String,
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
        }
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }*/
    //========================菜单功能================================//
    /**
     * 打开浏览器
     *
     * @param targetUrl 外部浏览器打开的地址
     */
    private fun openBrowser(targetUrl: String) {
        if (TextUtils.isEmpty(targetUrl) || targetUrl.startsWith("file://")) {
            XToastUtils.toast(targetUrl + getString(R.string.cannot_open_with_browser))
            return
        }
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        val uri = Uri.parse(targetUrl)
        intent.data = uri
        startActivity(intent)
    }

    /**
     * 显示更多菜单
     *
     * @param view 菜单依附在该View下面
     */
    private fun showPoPup(view: View) {
        if (mPopupMenu == null) {
            mPopupMenu = PopupMenu(requireContext(), view)
            mPopupMenu!!.inflate(R.menu.menu_toolbar_web)
            mPopupMenu!!.setOnMenuItemClickListener(mOnMenuItemClickListener)
        }
        mPopupMenu!!.show()
    }

    /**
     * 菜单事件
     */
    private val mOnMenuItemClickListener = PopupMenu.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.refresh -> {
                if (mAgentWeb != null) {
                    mAgentWeb!!.urlLoader.reload() // 刷新
                }
                true
            }
            R.id.copy -> {
                if (mAgentWeb != null) {
                    mAgentWeb!!.webCreator.webView.url?.let { toCopy(context, it) }
                }
                true
            }
            R.id.default_browser -> {
                if (mAgentWeb != null) {
                    mAgentWeb!!.webCreator.webView.url?.let { openBrowser(it) }
                }
                true
            }
            R.id.share -> {
                if (mAgentWeb != null) {
                    mAgentWeb!!.webCreator.webView.url?.let { shareWebUrl(it) }
                }
                true
            }
            else -> false
        }
    }

    /**
     * 分享网页链接
     *
     * @param url 网页链接
     */
    private fun shareWebUrl(url: String) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        shareIntent.type = "text/plain"
        //设置分享列表的标题，并且每次都显示分享列表
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)))
    }

    /**
     * 复制字符串
     *
     * @param context
     * @param text
     */
    private fun toCopy(context: Context?, text: String) {
        val manager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(null, text))
    }

    //===================生命周期管理===========================//
    override fun onResume() {
        mAgentWeb!!.webLifeCycle.onResume() //恢复
        super.onResume()
    }

    override fun onPause() {
        mAgentWeb!!.webLifeCycle.onPause() //暂停应用内所有WebView ， 调用mWebView.resumeTimers();/mAgentWeb.getWebLifeCycle().onResume(); 恢复。
        super.onPause()
    }

    override fun onFragmentKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return mAgentWeb!!.handleKeyEvent(keyCode, event)
    }

    override fun onDestroyView() {
        mAgentWeb!!.webLifeCycle.onDestroy()
        super.onDestroyView()
    }
    //===================中间键===========================//// 拦截 url，不执行 DefaultWebClient#shouldOverrideUrlLoading
    // 执行 DefaultWebClient#shouldOverrideUrlLoading
    // do you work
    /**
     * MiddlewareWebClientBase 是 AgentWeb 3.0.0 提供一个强大的功能，
     * 如果用户需要使用 AgentWeb 提供的功能， 不想重写 WebClientView方
     * 法覆盖AgentWeb提供的功能，那么 MiddlewareWebClientBase 是一个
     * 不错的选择 。
     *
     * @return
     */
    @Suppress("DEPRECATION")
    protected val middlewareWebClient: MiddlewareWebClientBase
        get() = object : MiddlewareWebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // 拦截 url，不执行 DefaultWebClient#shouldOverrideUrlLoading
                if (url.startsWith("agentweb")) {
                    Log.i(TAG, "agentweb scheme ~")
                    return true
                }
                // 执行 DefaultWebClient#shouldOverrideUrlLoading
                return super.shouldOverrideUrlLoading(view, url)
                // do you work
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    protected val middlewareWebChrome: MiddlewareWebChromeBase
        get() = object : MiddlewareChromeClient() {}

    companion object {
        const val KEY_URL = "com.xuexiang.xuidemo.base.webview.key_url"
        val TAG: String = AgentWebFragment::class.java.simpleName
        fun getInstance(url: String?): AgentWebFragment {
            val bundle = Bundle()
            bundle.putString(KEY_URL, url)
            return getInstance(bundle)
        }

        fun getInstance(bundle: Bundle?): AgentWebFragment {
            val fragment = AgentWebFragment()
            if (bundle != null) {
                fragment.arguments = bundle
            }
            return fragment
        }
    }
}