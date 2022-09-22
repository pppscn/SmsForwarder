package com.idormy.sms.forwarder.activity

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gyf.cactus.ext.cactusUpdateNotification
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.adapter.WidgetItemAdapter
import com.idormy.sms.forwarder.core.BaseActivity
import com.idormy.sms.forwarder.core.webview.AgentWebActivity
import com.idormy.sms.forwarder.database.AppDatabase
import com.idormy.sms.forwarder.databinding.ActivityMainBinding
import com.idormy.sms.forwarder.fragment.*
import com.idormy.sms.forwarder.utils.*
import com.idormy.sms.forwarder.utils.sdkinit.XUpdateInit
import com.idormy.sms.forwarder.widget.GuideTipsDialog.Companion.showTips
import com.idormy.sms.forwarder.widget.GuideTipsDialog.Companion.showTipsForce
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.callback.DownloadProgressCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.base.XPageFragment
import com.xuexiang.xpage.core.PageOption
import com.xuexiang.xpage.model.PageInfo
import com.xuexiang.xui.adapter.FragmentAdapter
import com.xuexiang.xui.adapter.recyclerview.RecyclerViewHolder
import com.xuexiang.xui.utils.DensityUtils
import com.xuexiang.xui.utils.ResUtils
import com.xuexiang.xui.utils.WidgetUtils
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.GravityEnum
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.file.FileUtils
import com.xuexiang.xutil.net.NetworkUtils
import frpclib.Frpclib
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File


@Suppress("DEPRECATION", "PrivatePropertyName")
class MainActivity : BaseActivity<ActivityMainBinding?>(),
    View.OnClickListener,
    BottomNavigationView.OnNavigationItemSelectedListener,
    Toolbar.OnMenuItemClickListener,
    RecyclerViewHolder.OnItemClickListener<PageInfo> {

    private val TAG: String = MainActivity::class.java.simpleName
    private lateinit var mTitles: Array<String>
    private var logsType: String = "sms"
    private var ruleType: String = "sms"

    override fun viewBindingInflate(inflater: LayoutInflater?): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        initData()
        initListeners()

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
    }

    override val isSupportSlideBack: Boolean
        get() = false

    private fun initViews() {
        WidgetUtils.clearActivityBackground(this)
        mTitles = ResUtils.getStringArray(R.array.home_titles)
        binding!!.includeMain.toolbar.title = mTitles[0]
        binding!!.includeMain.toolbar.inflateMenu(R.menu.menu_logs)
        binding!!.includeMain.toolbar.setOnMenuItemClickListener(this)

        //主页内容填充
        val fragments = arrayOf(
            LogsFragment(),
            RulesFragment(),
            SendersFragment(),
            SettingsFragment()
        )
        val adapter = FragmentAdapter(supportFragmentManager, fragments)
        binding!!.includeMain.viewPager.offscreenPageLimit = mTitles.size - 1
        binding!!.includeMain.viewPager.adapter = adapter

        if (!SettingUtils.enableHelpTip) {
            val headerView = binding!!.navView.getHeaderView(0)
            val tvSlogan = headerView.findViewById<TextView>(R.id.tv_slogan)
            tvSlogan.visibility = View.GONE
        }
    }

    private fun initData() {
        //仅当有WIFI网络时自动检查更新/获取提示
        if (NetworkUtils.isWifi() && NetworkUtils.isHaveInternet()) {
            showTips(this)
            XUpdateInit.checkUpdate(this, false)
        }
    }

    fun initListeners() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding!!.drawerLayout,
            binding!!.includeMain.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding!!.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        //侧边栏点击事件
        binding!!.navView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            if (menuItem.isCheckable) {
                binding!!.drawerLayout.closeDrawers()
                return@setNavigationItemSelectedListener handleNavigationItemSelected(menuItem)
            } else {
                when (menuItem.itemId) {
                    R.id.nav_server -> openNewPage(ServerFragment::class.java)
                    R.id.nav_client -> openNewPage(ClientFragment::class.java)
                    R.id.nav_frpc -> {
                        if (!FileUtils.isFileExists(filesDir.absolutePath + "/libs/libgojni.so")) {
                            MaterialDialog.Builder(this)
                                .title(
                                    String.format(
                                        getString(R.string.frpclib_download_title),
                                        FRPC_LIB_VERSION
                                    )
                                )
                                .content(R.string.download_frpc_tips)
                                .positiveText(R.string.lab_yes)
                                .negativeText(R.string.lab_no)
                                .onPositive { _: MaterialDialog?, _: DialogAction? ->
                                    downloadFrpcLib()
                                }
                                .show()
                            return@setNavigationItemSelectedListener false
                        }

                        if (FRPC_LIB_VERSION == Frpclib.getVersion()) {
                            openNewPage(FrpcFragment::class.java)
                        } else {
                            MaterialDialog.Builder(this)
                                .title(R.string.frpclib_version_mismatch)
                                .content(R.string.download_frpc_tips)
                                .positiveText(R.string.lab_yes)
                                .negativeText(R.string.lab_no)
                                .onPositive { _: MaterialDialog?, _: DialogAction? ->
                                    downloadFrpcLib()
                                }
                                .show()
                        }
                    }
                    R.id.nav_app_list -> openNewPage(AppListFragment::class.java)
                    R.id.nav_logcat -> openNewPage(LogcatFragment::class.java)
                    R.id.nav_help -> AgentWebActivity.goWeb(this, getString(R.string.url_help))
                    R.id.nav_about -> openNewPage(AboutFragment::class.java)
                    else -> XToastUtils.toast("Click:" + menuItem.title)
                }
            }
            true
        }

        //主页事件监听
        binding!!.includeMain.viewPager.addOnPageChangeListener(object :
            ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                val item = binding!!.includeMain.bottomNavigation.menu.getItem(position)
                binding!!.includeMain.toolbar.title = item.title
                binding!!.includeMain.toolbar.menu.clear()
                when (item.title) {
                    getString(R.string.menu_rules) -> binding!!.includeMain.toolbar.inflateMenu(
                        R.menu.menu_rules
                    )
                    getString(R.string.menu_senders) -> binding!!.includeMain.toolbar.inflateMenu(
                        R.menu.menu_senders
                    )
                    getString(R.string.menu_settings) -> binding!!.includeMain.toolbar.inflateMenu(
                        R.menu.menu_settings
                    )
                    else -> binding!!.includeMain.toolbar.inflateMenu(R.menu.menu_logs)
                }
                item.isChecked = true
                updateSideNavStatus(item)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        binding!!.includeMain.bottomNavigation.setOnNavigationItemSelectedListener(this)

        //tabBar分类切换
        LiveEventBus.get(EVENT_UPDATE_LOGS_TYPE, String::class.java).observe(this) { type: String ->
            logsType = type
        }
        LiveEventBus.get(EVENT_UPDATE_RULE_TYPE, String::class.java).observe(this) { type: String ->
            ruleType = type
        }

        //更新通知栏文案
        LiveEventBus.get(EVENT_UPDATE_NOTIFY, String::class.java).observe(this) { notify: String ->
            cactusUpdateNotification {
                setContent(notify)
            }
        }
    }

    /**
     * 处理侧边栏点击事件
     *
     * @param menuItem
     * @return
     */
    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        for (index in mTitles.indices) {
            if (mTitles[index] == menuItem.title) {
                binding!!.includeMain.toolbar.title = menuItem.title
                binding!!.includeMain.viewPager.setCurrentItem(index, false)
                return true
            }
        }
        return false
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_notifications -> {
                showTipsForce(this)
            }
            R.id.action_clear_logs -> {
                MaterialDialog.Builder(this)
                    .content(R.string.delete_type_log_tips)
                    .positiveText(R.string.lab_yes)
                    .negativeText(R.string.lab_no)
                    .onPositive { _: MaterialDialog?, _: DialogAction? ->
                        AppDatabase.getInstance(this)
                            .logsDao()
                            .deleteAll(logsType)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : CompletableObserver {
                                override fun onSubscribe(d: Disposable) {}
                                override fun onComplete() {
                                    XToastUtils.success(R.string.delete_type_log_toast)
                                }

                                override fun onError(e: Throwable) {
                                    e.message?.let { XToastUtils.error(it) }
                                }
                            })
                    }
                    .show()
            }
            R.id.action_add_sender -> {
                val dialog = BottomSheetDialog(this)
                val view: View =
                    LayoutInflater.from(this).inflate(R.layout.dialog_sender_bottom_sheet, null)
                val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

                WidgetUtils.initGridRecyclerView(recyclerView, 4, DensityUtils.dp2px(1f))
                val widgetItemAdapter = WidgetItemAdapter(SENDER_FRAGMENT_LIST)
                widgetItemAdapter.setOnItemClickListener(this)
                recyclerView.adapter = widgetItemAdapter

                dialog.setContentView(view)
                dialog.setCancelable(true)
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
                WidgetUtils.transparentBottomSheetDialogBackground(dialog)
            }
            R.id.action_add_rule -> {
                PageOption.to(RulesEditFragment::class.java)
                    .putString(KEY_RULE_TYPE, ruleType)
                    .setNewActivity(true)
                    .open(this)
            }
            /*R.id.action_restore_settings -> {
                XToastUtils.success(logsType)
            }*/
        }
        return false
    }

    @SingleClick
    override fun onClick(v: View) {
    }

    //================Navigation================//
    /**
     * 底部导航栏点击事件
     *
     * @param menuItem
     * @return
     */
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        for (index in mTitles.indices) {
            if (mTitles[index] == menuItem.title) {
                binding!!.includeMain.toolbar.title = menuItem.title
                binding!!.includeMain.viewPager.setCurrentItem(index, false)
                updateSideNavStatus(menuItem)
                return true
            }
        }
        return false
    }

    /**
     * 更新侧边栏菜单选中状态
     *
     * @param menuItem
     */
    private fun updateSideNavStatus(menuItem: MenuItem) {
        val side = binding!!.navView.menu.findItem(menuItem.itemId)
        if (side != null) {
            side.isChecked = true
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

    @SingleClick
    override fun onItemClick(itemView: View, widgetInfo: PageInfo, pos: Int) {
        try {
            @Suppress("UNCHECKED_CAST")
            PageOption.to(Class.forName(widgetInfo.classPath) as Class<XPageFragment>) //跳转的fragment
                .setNewActivity(true)
                .putInt(KEY_SENDER_TYPE, pos) //注意：目前刚好是这个顺序而已
                .open(this)
        } catch (e: Exception) {
            e.printStackTrace()
            XToastUtils.error(e.message.toString())
        }
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

                    val intent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    android.os.Process.killProcess(android.os.Process.myPid()) //杀掉以前进程
                }
            })

    }

}