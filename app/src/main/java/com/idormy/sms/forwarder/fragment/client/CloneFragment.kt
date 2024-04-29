package com.idormy.sms.forwarder.fragment.client

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.activity.MainActivity
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentClientCloneBinding
import com.idormy.sms.forwarder.entity.CloneInfo
import com.idormy.sms.forwarder.server.model.BaseResponse
import com.idormy.sms.forwarder.utils.AppUtils
import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.KEY_DEFAULT_SELECTION
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.RSACrypt
import com.idormy.sms.forwarder.utils.SM4Crypt
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xhttp2.XHttp
import com.xuexiang.xhttp2.cache.model.CacheMode
import com.xuexiang.xhttp2.callback.SimpleCallBack
import com.xuexiang.xhttp2.exception.ApiException
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xrouter.utils.TextUtils
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xutil.data.ConvertTools
import com.xuexiang.xutil.file.FileIOUtils
import com.xuexiang.xutil.file.FileUtils
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import java.io.File
import java.util.Date

@Suppress("PrivatePropertyName")
@Page(name = "一键换新机")
class CloneFragment : BaseFragment<FragmentClientCloneBinding?>(), View.OnClickListener {

    private val TAG: String = CloneFragment::class.java.simpleName
    private var backupPath: String? = null
    private val backupFile = "SmsForwarder.json"
    private var pushCountDownHelper: CountDownButtonHelper? = null
    private var pullCountDownHelper: CountDownButtonHelper? = null
    private var exportCountDownHelper: CountDownButtonHelper? = null
    private var importCountDownHelper: CountDownButtonHelper? = null

    @JvmField
    @AutoWired(name = KEY_DEFAULT_SELECTION)
    var defaultSelection: Int = 0

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentClientCloneBinding {
        return FragmentClientCloneBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.api_clone)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        // 申请储存权限
        XXPermissions.with(this)
            //.permission(*Permission.Group.STORAGE)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE).request(object : OnPermissionCallback {
                @SuppressLint("SetTextI18n")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    backupPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                    binding!!.tvBackupPath.text = backupPath + File.separator + backupFile
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        XToastUtils.error(R.string.toast_denied_never)
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        XToastUtils.error(R.string.toast_denied)
                    }
                    binding!!.tvBackupPath.text = getString(R.string.storage_permission_tips)
                }
            })

        binding!!.tabBar.setTabTitles(getStringArray(R.array.clone_type_option))
        binding!!.tabBar.setOnTabClickListener { _, position ->
            //XToastUtils.toast("点击了$title--$position")
            if (position == 1) {
                binding!!.layoutNetwork.visibility = View.GONE
                binding!!.layoutOffline.visibility = View.VISIBLE
            } else {
                binding!!.layoutNetwork.visibility = View.VISIBLE
                binding!!.layoutOffline.visibility = View.GONE
            }
        }
        //通用设置界面跳转时只使用离线模式
        if (defaultSelection == 1) {
            binding!!.tabBar.visibility = View.GONE
            binding!!.layoutNetwork.visibility = View.GONE
            binding!!.layoutOffline.visibility = View.VISIBLE
        }

        //按钮增加倒计时，避免重复点击
        pushCountDownHelper = CountDownButtonHelper(binding!!.btnPush, SettingUtils.requestTimeout)
        pushCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnPush.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnPush.text = getString(R.string.push)
            }
        })
        pullCountDownHelper = CountDownButtonHelper(binding!!.btnPull, SettingUtils.requestTimeout)
        pullCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnPull.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnPull.text = getString(R.string.pull)
            }
        })
        exportCountDownHelper = CountDownButtonHelper(binding!!.btnExport, 3)
        exportCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnExport.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnExport.text = getString(R.string.export)
            }
        })
        importCountDownHelper = CountDownButtonHelper(binding!!.btnImport, 3)
        importCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnImport.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnImport.text = getString(R.string.imports)
            }
        })
    }

    override fun initListeners() {
        binding!!.btnPush.setOnClickListener(this)
        binding!!.btnPull.setOnClickListener(this)
        binding!!.btnExport.setOnClickListener(this)
        binding!!.btnImport.setOnClickListener(this)
    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            //推送配置
            R.id.btn_push -> pushData()
            //拉取配置
            R.id.btn_pull -> pullData()
            //导出配置
            R.id.btn_export -> {
                try {
                    exportCountDownHelper?.start()
                    val file = File(backupPath + File.separator + backupFile)
                    //判断文件是否存在，存在则在创建之前删除
                    FileUtils.createFileByDeleteOldFile(file)
                    val cloneInfo = HttpServerUtils.exportSettings()
                    val jsonStr = Gson().toJson(cloneInfo)
                    Log.d(TAG, "jsonStr = $jsonStr")
                    if (FileIOUtils.writeFileFromString(file, jsonStr)) {
                        XToastUtils.success(getString(R.string.export_succeeded))
                    } else {
                        binding!!.tvExport.text = getString(R.string.export_failed)
                        XToastUtils.error(getString(R.string.export_failed))
                    }
                } catch (e: Exception) {
                    XToastUtils.error(String.format(getString(R.string.export_failed_tips), e.message))
                }
            }
            //导入配置
            R.id.btn_import -> {
                try {
                    importCountDownHelper?.start()
                    val file = File(backupPath + File.separator + backupFile)
                    //判断文件是否存在
                    if (!FileUtils.isFileExists(file)) {
                        XToastUtils.error(getString(R.string.import_failed_file_not_exist))
                        return
                    }

                    val jsonStr = FileIOUtils.readFile2String(file)
                    Log.d(TAG, "jsonStr = $jsonStr")
                    if (TextUtils.isEmpty(jsonStr)) {
                        XToastUtils.error(getString(R.string.import_failed))
                        return
                    }

                    //替换Date字段为当前时间
                    val builder = GsonBuilder()
                    builder.registerTypeAdapter(Date::class.java, JsonDeserializer<Any?> { _, _, _ -> Date() })
                    val gson = builder.create()
                    val cloneInfo = gson.fromJson(jsonStr, CloneInfo::class.java)
                    Log.d(TAG, "cloneInfo = $cloneInfo")

                    //判断版本是否一致
                    HttpServerUtils.compareVersion(cloneInfo)

                    if (HttpServerUtils.restoreSettings(cloneInfo)) {
                        MaterialDialog.Builder(requireContext())
                            .iconRes(R.drawable.icon_api_clone)
                            .title(R.string.clone)
                            .content(R.string.import_succeeded)
                            .cancelable(false)
                            .positiveText(R.string.confirm)
                            .onPositive { _: MaterialDialog?, _: DialogAction? ->
                                val intent = Intent(App.context, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                            .show()
                    } else {
                        XToastUtils.error(getString(R.string.import_failed))
                    }
                } catch (e: Exception) {
                    XToastUtils.error(String.format(getString(R.string.import_failed_tips), e.message))
                }
            }
        }
    }

    //推送配置
    private fun pushData() {
        if (!CommonUtils.checkUrl(HttpServerUtils.serverAddress)) {
            XToastUtils.error(getString(R.string.invalid_service_address))
            return
        }

        pushCountDownHelper?.start()

        val requestUrl: String = HttpServerUtils.serverAddress + "/clone/push"
        Log.i(TAG, "requestUrl:$requestUrl")

        val msgMap: MutableMap<String, Any> = mutableMapOf()
        val timestamp = System.currentTimeMillis()
        msgMap["timestamp"] = timestamp
        val clientSignKey = HttpServerUtils.clientSignKey
        if (!TextUtils.isEmpty(clientSignKey)) {
            msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey)
        }
        msgMap["data"] = HttpServerUtils.exportSettings()

        var requestMsg: String = Gson().toJson(msgMap)
        Log.i(TAG, "requestMsg:$requestMsg")

        val postRequest = XHttp.post(requestUrl).keepJson(true).timeOut((SettingUtils.requestTimeout * 1000).toLong()) //超时时间10s
            .cacheMode(CacheMode.NO_CACHE).timeStamp(true)

        when (HttpServerUtils.clientSafetyMeasures) {
            2 -> {
                val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                try {
                    requestMsg = Base64.encode(requestMsg.toByteArray())
                    requestMsg = RSACrypt.encryptByPublicKey(requestMsg, publicKey)
                    Log.i(TAG, "requestMsg: $requestMsg")
                } catch (e: Exception) {
                    XToastUtils.error(getString(R.string.request_failed) + e.message)
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    return
                }
                postRequest.upString(requestMsg)
            }

            3 -> {
                try {
                    val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.clientSignKey)
                    //requestMsg = Base64.encode(requestMsg.toByteArray())
                    val encryptCBC = SM4Crypt.encrypt(requestMsg.toByteArray(), sm4Key)
                    requestMsg = ConvertTools.bytes2HexString(encryptCBC)
                    Log.i(TAG, "requestMsg: $requestMsg")
                } catch (e: Exception) {
                    XToastUtils.error(getString(R.string.request_failed) + e.message)
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    return
                }
                postRequest.upString(requestMsg)
            }

            else -> {
                postRequest.upJson(requestMsg)
            }
        }

        postRequest.execute(object : SimpleCallBack<String>() {
            override fun onError(e: ApiException) {
                XToastUtils.error(e.displayMessage)
                pushCountDownHelper?.finish()
            }

            override fun onSuccess(response: String) {
                Log.i(TAG, response)
                try {
                    var json = response
                    if (HttpServerUtils.clientSafetyMeasures == 2) {
                        val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                        json = RSACrypt.decryptByPublicKey(json, publicKey)
                        json = String(Base64.decode(json))
                    } else if (HttpServerUtils.clientSafetyMeasures == 3) {
                        val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.clientSignKey)
                        val encryptCBC = ConvertTools.hexStringToByteArray(json)
                        val decryptCBC = SM4Crypt.decrypt(encryptCBC, sm4Key)
                        json = String(decryptCBC)
                    }
                    val resp: BaseResponse<String> = Gson().fromJson(json, object : TypeToken<BaseResponse<String>>() {}.type)
                    if (resp.code == 200) {
                        XToastUtils.success(getString(R.string.request_succeeded))
                    } else {
                        XToastUtils.error(getString(R.string.request_failed) + resp.msg)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    XToastUtils.error(getString(R.string.request_failed) + response)
                }
                pushCountDownHelper?.finish()
            }
        })

    }

    //拉取配置
    private fun pullData() {
        if (!CommonUtils.checkUrl(HttpServerUtils.serverAddress)) {
            XToastUtils.error(getString(R.string.invalid_service_address))
            return
        }

        exportCountDownHelper?.start()

        val requestUrl: String = HttpServerUtils.serverAddress + "/clone/pull"
        Log.i(TAG, "requestUrl:$requestUrl")

        val msgMap: MutableMap<String, Any> = mutableMapOf()
        val timestamp = System.currentTimeMillis()
        msgMap["timestamp"] = timestamp
        val clientSignKey = HttpServerUtils.clientSignKey
        if (!TextUtils.isEmpty(clientSignKey)) {
            msgMap["sign"] = HttpServerUtils.calcSign(timestamp.toString(), clientSignKey)
        }

        val dataMap: MutableMap<String, Any> = mutableMapOf()
        dataMap["version_code"] = AppUtils.getAppVersionCode()
        msgMap["data"] = dataMap

        var requestMsg: String = Gson().toJson(msgMap)
        Log.i(TAG, "requestMsg:$requestMsg")

        val postRequest = XHttp.post(requestUrl).keepJson(true).timeStamp(true)

        when (HttpServerUtils.clientSafetyMeasures) {
            2 -> {
                val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                try {
                    requestMsg = Base64.encode(requestMsg.toByteArray())
                    requestMsg = RSACrypt.encryptByPublicKey(requestMsg, publicKey)
                    Log.i(TAG, "requestMsg: $requestMsg")
                } catch (e: Exception) {
                    XToastUtils.error(getString(R.string.request_failed) + e.message)
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    return
                }
                postRequest.upString(requestMsg)
            }

            3 -> {
                try {
                    val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.clientSignKey)
                    //requestMsg = Base64.encode(requestMsg.toByteArray())
                    val encryptCBC = SM4Crypt.encrypt(requestMsg.toByteArray(), sm4Key)
                    requestMsg = ConvertTools.bytes2HexString(encryptCBC)
                    Log.i(TAG, "requestMsg: $requestMsg")
                } catch (e: Exception) {
                    XToastUtils.error(getString(R.string.request_failed) + e.message)
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    return
                }
                postRequest.upString(requestMsg)
            }

            else -> {
                postRequest.upJson(requestMsg)
            }
        }

        postRequest.execute(object : SimpleCallBack<String>() {
            override fun onError(e: ApiException) {
                XToastUtils.error(e.displayMessage)
                exportCountDownHelper?.finish()
            }

            override fun onSuccess(response: String) {
                Log.i(TAG, response)
                try {
                    var json = response
                    if (HttpServerUtils.clientSafetyMeasures == 2) {
                        val publicKey = RSACrypt.getPublicKey(HttpServerUtils.clientSignKey)
                        json = RSACrypt.decryptByPublicKey(json, publicKey)
                        json = String(Base64.decode(json))
                    } else if (HttpServerUtils.clientSafetyMeasures == 3) {
                        val sm4Key = ConvertTools.hexStringToByteArray(HttpServerUtils.clientSignKey)
                        val encryptCBC = ConvertTools.hexStringToByteArray(json)
                        val decryptCBC = SM4Crypt.decrypt(encryptCBC, sm4Key)
                        json = String(decryptCBC)
                    }

                    //替换Date字段为当前时间
                    val builder = GsonBuilder()
                    builder.registerTypeAdapter(Date::class.java, JsonDeserializer<Any?> { _, _, _ -> Date() })
                    val gson = builder.create()
                    val resp: BaseResponse<CloneInfo> = gson.fromJson(json, object : TypeToken<BaseResponse<CloneInfo>>() {}.type)
                    if (resp.code == 200) {
                        val cloneInfo = resp.data
                        Log.d(TAG, "cloneInfo = $cloneInfo")

                        if (cloneInfo == null) {
                            XToastUtils.error(getString(R.string.request_failed))
                            return
                        }

                        //判断版本是否一致
                        HttpServerUtils.compareVersion(cloneInfo)

                        if (HttpServerUtils.restoreSettings(cloneInfo)) {
                            XToastUtils.success(getString(R.string.import_succeeded))
                        }
                    } else {
                        XToastUtils.error(getString(R.string.request_failed) + resp.msg)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, e.toString())
                    XToastUtils.error(getString(R.string.request_failed) + response)
                }
                exportCountDownHelper?.finish()
            }
        })

    }

    override fun onDestroyView() {
        if (pushCountDownHelper != null) pushCountDownHelper!!.recycle()
        if (pullCountDownHelper != null) pullCountDownHelper!!.recycle()
        if (exportCountDownHelper != null) exportCountDownHelper!!.recycle()
        if (importCountDownHelper != null) importCountDownHelper!!.recycle()
        super.onDestroyView()
    }
}