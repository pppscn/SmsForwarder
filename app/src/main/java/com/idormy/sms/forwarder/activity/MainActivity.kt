package com.idormy.sms.forwarder.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.tabs.TabLayout
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.menu.DrawerAdapter
import com.idormy.sms.forwarder.adapter.menu.DrawerItem
import com.idormy.sms.forwarder.adapter.menu.SimpleItem
import com.idormy.sms.forwarder.adapter.menu.SpaceItem
import com.idormy.sms.forwarder.core.BaseActivity
import com.idormy.sms.forwarder.core.webview.AgentWebActivity
import com.idormy.sms.forwarder.databinding.ActivityMainBinding
import com.idormy.sms.forwarder.fragment.AboutFragment
import com.idormy.sms.forwarder.fragment.AppListFragment
import com.idormy.sms.forwarder.fragment.ClientFragment
import com.idormy.sms.forwarder.fragment.FrpcFragment
import com.idormy.sms.forwarder.fragment.LogsFragment
import com.idormy.sms.forwarder.fragment.RulesFragment
import com.idormy.sms.forwarder.fragment.SendersFragment
import com.idormy.sms.forwarder.fragment.ServerFragment
import com.idormy.sms.forwarder.fragment.SettingsFragment
import com.idormy.sms.forwarder.fragment.TasksFragment
import com.idormy.sms.forwarder.service.ForegroundService
import com.idormy.sms.forwarder.utils.ACTION_START
import com.idormy.sms.forwarder.utils.CommonUtils.Companion.restartApplication
import com.idormy.sms.forwarder.utils.EVENT_LOAD_APP_LIST
import com.idormy.sms.forwarder.utils.FRPC_LIB_DOWNLOAD_URL
import com.idormy.sms.forwarder.utils.FRPC_LIB_VERSION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sdkinit.XUpdateInit
import com.idormy.sms.forwarder.widget.GuideTipsDialog.Companion.showTips
import com.idormy.sms.forwarder.workers.LoadAppListWorker
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.DownloadProgressCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xui.XUI.getContext
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.utils.ThemeUtils
import com.xuexiang.xui.utils.ViewUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.GravityEnum
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.file.FileUtils
import com.xuexiang.xutil.net.NetworkUtils
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import com.yarolegovich.slidingrootnav.callback.DragStateListener
import java.io.File

@Suppress("PrivatePropertyName", "unused", "DEPRECATION")
class MainActivity : BaseActivity<ActivityMainBinding?>(), DrawerAdapter.OnItemSelectedListener {

    private val TAG: String = MainActivity::class.java.simpleName
    private val POS_LOG = 0
    private val POS_RULE = 1
    private val POS_SENDER = 2
    private val POS_SETTING = 3
    private val POS_TASK = 5 //4为空行
    private val POS_SERVER = 6
    private val POS_CLIENT = 7
    private val POS_FRPC = 8
    private val POS_APPS = 9
    private val POS_HELP = 11 //10为空行
    private val POS_ABOUT = 12
    private var needToAppListFragment = false

    private lateinit var mTabLayout: TabLayout
    private lateinit var mSlidingRootNav: SlidingRootNav
    private lateinit var mLLMenu: LinearLayout
    private lateinit var mMenuTitles: Array<String>
    private lateinit var mMenuIcons: Array<Drawable>
    private lateinit var mAdapter: DrawerAdapter

    override fun viewBindingInflate(inflater: LayoutInflater?): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initData()
        initViews()
        initSlidingMenu(savedInstanceState)

        //不在最近任务列表中显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SettingUtils.enableExcludeFromRecents) {
            val am = App.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.let {
                val tasks = it.appTasks
                if (!tasks.isNullOrEmpty()) {
                    tasks[0].setExcludeFromRecents(true)
                }
            }
        }

        //检查通知权限是否获取
        XXPermissions.with(this).permission(Permission.NOTIFICATION_SERVICE).permission(Permission.POST_NOTIFICATIONS).request(OnPermissionCallback { _, allGranted ->
            if (!allGranted) {
                XToastUtils.error(R.string.tips_notification)
                return@OnPermissionCallback
            }

            //启动前台服务
            if (!ForegroundService.isRunning) {
                val serviceIntent = Intent(this, ForegroundService::class.java)
                serviceIntent.action = ACTION_START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        })

        //监听已安装App信息列表加载完成事件
        LiveEventBus.get(EVENT_LOAD_APP_LIST, String::class.java).observe(this) {
            if (needToAppListFragment) {
                openNewPage(AppListFragment::class.java)
            }
        }
    }

    override val isSupportSlideBack: Boolean
        get() = false

    private fun initViews() {
        WidgetUtils.clearActivityBackground(this)
        initTab()
    }

    private fun initTab() {
        mTabLayout = binding!!.tabs
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_logs), R.drawable.selector_icon_tabbar_logs)
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_rules), R.drawable.selector_icon_tabbar_rules)
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_senders), R.drawable.selector_icon_tabbar_senders)
        WidgetUtils.addTabWithoutRipple(mTabLayout, getString(R.string.menu_settings), R.drawable.selector_icon_tabbar_settings)
        WidgetUtils.setTabLayoutTextFont(mTabLayout)
        switchPage(LogsFragment::class.java)
        mTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                needToAppListFragment = false
                mAdapter.setSelected(tab.position)
                when (tab.position) {
                    POS_LOG -> switchPage(LogsFragment::class.java)
                    POS_RULE -> switchPage(RulesFragment::class.java)
                    POS_SENDER -> switchPage(SendersFragment::class.java)
                    POS_SETTING -> switchPage(SettingsFragment::class.java)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun initData() {
        mMenuTitles = ResUtils.getStringArray(this, R.array.menu_titles)
        mMenuIcons = ResUtils.getDrawableArray(this, R.array.menu_icons)

        //仅当开启自动检查且有网络时自动检查更新/获取提示
        if (SettingUtils.autoCheckUpdate && NetworkUtils.isHaveInternet()) {
            showTips(this)
            XUpdateInit.checkUpdate(this, false, SettingUtils.joinPreviewProgram)
        }
    }

    //按返回键不退出回到桌面
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }

    fun openMenu() {
        mSlidingRootNav.openMenu()
    }

    fun closeMenu() {
        mSlidingRootNav.closeMenu()
    }

    fun isMenuOpen(): Boolean {
        return mSlidingRootNav.isMenuOpened
    }

    private fun initSlidingMenu(savedInstanceState: Bundle?) {
        mSlidingRootNav = SlidingRootNavBuilder(this).withGravity(if (ResUtils.isRtl(this)) SlideGravity.RIGHT else SlideGravity.LEFT).withMenuOpened(false).withContentClickableWhenMenuOpened(false).withSavedState(savedInstanceState).withMenuLayout(R.layout.menu_left_drawer).inject()
        mLLMenu = mSlidingRootNav.layout.findViewById(R.id.ll_menu)
        ViewUtils.setVisibility(mLLMenu, false)
        mAdapter = DrawerAdapter(
            mutableListOf(
                createItemFor(POS_LOG).setChecked(true),
                createItemFor(POS_RULE),
                createItemFor(POS_SENDER),
                createItemFor(POS_SETTING),
                SpaceItem(15),
                createItemFor(POS_TASK),
                createItemFor(POS_SERVER),
                createItemFor(POS_CLIENT),
                createItemFor(POS_FRPC),
                createItemFor(POS_APPS),
                SpaceItem(15),
                createItemFor(POS_HELP),
                createItemFor(POS_ABOUT),
            )
        )
        mAdapter.setListener(this)
        val list: RecyclerView = findViewById(R.id.list)
        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = mAdapter
        mAdapter.setSelected(POS_LOG)
        mSlidingRootNav.isMenuLocked = false
        mSlidingRootNav.layout.addDragStateListener(object : DragStateListener {
            override fun onDragStart() {
                ViewUtils.setVisibility(mLLMenu, true)
            }

            override fun onDragEnd(isMenuOpened: Boolean) {
                ViewUtils.setVisibility(mLLMenu, isMenuOpened)
            }
        })
    }

    override fun onItemSelected(position: Int) {
        needToAppListFragment = false
        when (position) {
            POS_LOG, POS_RULE, POS_SENDER, POS_SETTING -> {
                val tab = mTabLayout.getTabAt(position)
                tab?.select()
                mSlidingRootNav.closeMenu()
            }

            POS_TASK -> openNewPage(TasksFragment::class.java)
            POS_SERVER -> openNewPage(ServerFragment::class.java)
            POS_CLIENT -> openNewPage(ClientFragment::class.java)
            POS_FRPC -> {
                if (App.FrpclibInited) {
                    openNewPage(FrpcFragment::class.java)
                    return
                }

                val title = if (!FileUtils.isFileExists(filesDir.absolutePath + "/libs/libgojni.so")) {
                    String.format(getString(R.string.frpclib_download_title), FRPC_LIB_VERSION)
                } else {
                    getString(R.string.frpclib_version_mismatch)
                }

                MaterialDialog.Builder(this)
                    .title(title)
                    .content(R.string.download_frpc_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        downloadFrpcLib()
                    }
                    .show()
            }

            POS_APPS -> {
                //检查读取应用列表权限是否获取
                XXPermissions.with(this).permission(Permission.GET_INSTALLED_APPS).request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        if (App.UserAppList.isEmpty() && App.SystemAppList.isEmpty()) {
                            XToastUtils.info(getString(R.string.loading_app_list))
                            val request = OneTimeWorkRequestBuilder<LoadAppListWorker>().build()
                            WorkManager.getInstance(getContext()).enqueue(request)
                            needToAppListFragment = true
                            return
                        }
                        openNewPage(AppListFragment::class.java)
                    }

                    override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                        XToastUtils.error(R.string.tips_get_installed_apps)
                        if (doNotAskAgain) {
                            XXPermissions.startPermissionActivity(getContext(), permissions)
                        }
                    }
                })
            }

            POS_HELP -> AgentWebActivity.goWeb(this, getString(R.string.url_help))
            POS_ABOUT -> openNewPage(AboutFragment::class.java)
        }
    }

    private fun createItemFor(position: Int): DrawerItem<*> {
        return SimpleItem(mMenuIcons[position], mMenuTitles[position])
            .withIconTint(ThemeUtils.resolveColor(this, R.attr.xui_config_color_content_text))
            .withTextTint(ThemeUtils.resolveColor(this, R.attr.xui_config_color_content_text))
            .withSelectedIconTint(ThemeUtils.getMainThemeColor(this))
            .withSelectedTextTint(ThemeUtils.getMainThemeColor(this))
    }

    //动态加载FrpcLib
    private fun downloadFrpcLib() {
        val cpuAbi = when (Build.CPU_ABI) {
            "x86" -> "x86"
            "x86_64" -> "x86_64"
            "arm64-v8a" -> "arm64-v8a"
            else -> "armeabi-v7a"
        }

        val libPath = filesDir.absolutePath + "/libs"
        val soFile = File(libPath)
        if (!soFile.exists()) soFile.mkdirs()
        val downloadUrl = String.format(FRPC_LIB_DOWNLOAD_URL, FRPC_LIB_VERSION, cpuAbi)
        val mContext = this
        val dialog: MaterialDialog = MaterialDialog.Builder(mContext)
            .title(String.format(getString(R.string.frpclib_download_title), FRPC_LIB_VERSION))
            .content(getString(R.string.frpclib_download_content))
            .contentGravity(GravityEnum.CENTER)
            .progress(false, 0, true)
            .progressNumberFormat("%2dMB/%1dMB")
            .build()

        XHttp.downLoad(downloadUrl)
            .ignoreHttpsCert()
            .savePath(cacheDir.absolutePath)
            .execute(object : DownloadProgressCallBack<String?>() {
                override fun onStart() {
                    dialog.show()
                }

                override fun onError(e: ApiException) {
                    dialog.dismiss()
                    XToastUtils.error(e.message.toString())
                }

                override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                    Log.d(TAG, "onProgress: bytesRead=$bytesRead, contentLength=$contentLength")
                    dialog.maxProgress = (contentLength / 1048576L).toInt()
                    dialog.setProgress((bytesRead / 1048576L).toInt())
                }

                override fun onComplete(srcPath: String) {
                    dialog.dismiss()
                    Log.d(TAG, "srcPath = $srcPath")

                    val srcFile = File(srcPath)
                    val destFile = File("$libPath/libgojni.so")
                    FileUtils.moveFile(srcFile, destFile, null)

                    MaterialDialog.Builder(this@MainActivity)
                        .iconRes(R.drawable.ic_menu_frpc)
                        .title(R.string.menu_frpc)
                        .content(R.string.download_frpc_tips2)
                        .cancelable(false)
                        .positiveText(R.string.confirm)
                        .onPositive { _: MaterialDialog?, _: DialogAction? ->
                            restartApplication()
                        }
                        .show()
                }
            })

    }

}
