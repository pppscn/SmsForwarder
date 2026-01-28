package cn.ppps.forwarder.fragment

import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import cn.ppps.forwarder.App
import cn.ppps.forwarder.R
import cn.ppps.forwarder.core.BaseFragment
import cn.ppps.forwarder.databinding.FragmentServerBinding
import cn.ppps.forwarder.service.HttpServerService
import cn.ppps.forwarder.service.LocationService
import cn.ppps.forwarder.utils.ACTION_RESTART
import cn.ppps.forwarder.utils.Base64
import cn.ppps.forwarder.utils.HTTP_SERVER_PORT
import cn.ppps.forwarder.utils.HttpServerUtils
import cn.ppps.forwarder.utils.Log
import cn.ppps.forwarder.utils.RandomUtils
import cn.ppps.forwarder.utils.SM4Crypt
import cn.ppps.forwarder.utils.SettingUtils
import cn.ppps.forwarder.utils.XToastUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.picker.XSeekBar
import com.xuexiang.xutil.app.ServiceUtils
import com.xuexiang.xutil.data.ConvertTools
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.system.ClipboardUtils
import java.io.File
import java.net.InetAddress
import java.security.KeyPairGenerator

@Page(name = "主动控制·服务端")
class ServerFragment : BaseFragment<FragmentServerBinding?>(), View.OnClickListener {

    private var appContext: App? = null
    private var inetAddress: InetAddress? = null

    //定时更新界面
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 1000) //每隔1秒刷新一次
            refreshButtonText()
        }
    }

    override fun initViews() {
        appContext = requireActivity().application as App
    }

    override fun initTitle(): TitleBar? {
        val titleBar = super.initTitle()!!.setImmersive(false)
        titleBar.setTitle(R.string.menu_server)
        return titleBar
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentServerBinding {
        return FragmentServerBinding.inflate(inflater, container, false)
    }

    override fun initListeners() {
        binding!!.tvServerTips.setOnClickListener(this)
        binding!!.ivCopy.setOnClickListener(this)
        binding!!.btnToggleServer.setOnClickListener(this)

        binding!!.scbServerAutorun.isChecked = HttpServerUtils.enableServerAutorun
        binding!!.scbServerAutorun.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            HttpServerUtils.enableServerAutorun = isChecked
        }
        //启动更新UI定时器
        handler.post(runnable)

        //监听端口
        binding!!.etServerPort.setText(HttpServerUtils.serverPort.toString())
        binding!!.etServerPort.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val portText = binding!!.etServerPort.text.toString().trim()
                val port = try {
                    portText.toInt()
                } catch (e: Exception) {
                    HTTP_SERVER_PORT
                }
                if (port < 1 || port > 65535) {
                    XToastUtils.error(getString(R.string.wol_port_error))
                    return
                }
                HttpServerUtils.serverPort = port
            }
        })

        //安全措施
        var safetyMeasuresId = R.id.rb_safety_measures_none
        when (HttpServerUtils.safetyMeasures) {
            1 -> {
                safetyMeasuresId = R.id.rb_safety_measures_sign
                binding!!.layoutSignKey.visibility = View.VISIBLE
                binding!!.layoutTimeTolerance.visibility = View.VISIBLE
            }

            2 -> {
                safetyMeasuresId = R.id.rb_safety_measures_rsa
                binding!!.layoutPrivateKey.visibility = View.VISIBLE
                binding!!.layoutPublicKey.visibility = View.VISIBLE
            }

            3 -> {
                safetyMeasuresId = R.id.rb_safety_measures_sm4
                binding!!.layoutSm4Key.visibility = View.VISIBLE
            }

            else -> {}
        }
        binding!!.rgSafetyMeasures.check(safetyMeasuresId)
        binding!!.rgSafetyMeasures.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            var safetyMeasures = 0
            binding!!.layoutSignKey.visibility = View.GONE
            binding!!.layoutTimeTolerance.visibility = View.GONE
            binding!!.layoutPrivateKey.visibility = View.GONE
            binding!!.layoutPublicKey.visibility = View.GONE
            binding!!.layoutSm4Key.visibility = View.GONE
            when (checkedId) {
                R.id.rb_safety_measures_sign -> {
                    safetyMeasures = 1
                    binding!!.layoutSignKey.visibility = View.VISIBLE
                    binding!!.layoutTimeTolerance.visibility = View.VISIBLE
                }

                R.id.rb_safety_measures_rsa -> {
                    safetyMeasures = 2
                    binding!!.layoutPrivateKey.visibility = View.VISIBLE
                    binding!!.layoutPublicKey.visibility = View.VISIBLE
                }

                R.id.rb_safety_measures_sm4 -> {
                    safetyMeasures = 3
                    binding!!.layoutSm4Key.visibility = View.VISIBLE
                }

                else -> {}
            }
            HttpServerUtils.safetyMeasures = safetyMeasures
        }

        //SM4密钥
        binding!!.btnSm4Key.setOnClickListener(this)
        binding!!.etSm4Key.setText(HttpServerUtils.serverSm4Key)
        binding!!.etSm4Key.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverSm4Key = binding!!.etSm4Key.text.toString().trim()
            }
        })

        //RSA公私钥
        binding!!.btnCopyPublicKey.setOnClickListener(this)
        binding!!.btnGenerateKey.setOnClickListener(this)
        binding!!.etPublicKey.setText(HttpServerUtils.serverPublicKey)
        binding!!.etPublicKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverPublicKey = binding!!.etPublicKey.text.toString().trim()
            }
        })
        binding!!.etPrivateKey.setText(HttpServerUtils.serverPrivateKey)
        binding!!.etPrivateKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverPrivateKey = binding!!.etPrivateKey.text.toString().trim()
            }
        })

        //签名密钥
        binding!!.btnSignKey.setOnClickListener(this)
        binding!!.btnPathPicker.setOnClickListener(this)
        binding!!.etSignKey.setText(HttpServerUtils.serverSignKey)
        binding!!.etSignKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverSignKey = binding!!.etSignKey.text.toString().trim()
            }
        })
        //时间容差
        binding!!.xsbTimeTolerance.setDefaultValue(HttpServerUtils.timeTolerance)
        binding!!.xsbTimeTolerance.setOnSeekBarListener { _: XSeekBar?, newValue: Int ->
            HttpServerUtils.timeTolerance = newValue
        }

        //web客户端
        binding!!.etWebPath.setText(HttpServerUtils.serverWebPath)
        binding!!.etWebPath.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverWebPath = binding!!.etWebPath.text.toString().trim()
            }
        })

        binding!!.sbApiClone.isChecked = HttpServerUtils.enableApiClone
        binding!!.sbApiClone.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiClone = isChecked
        }

        binding!!.sbApiSendSms.isChecked = HttpServerUtils.enableApiSmsSend
        binding!!.sbApiSendSms.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiSmsSend = isChecked
            if (isChecked) checkSendSmsPermission()
        }

        binding!!.sbApiQuerySms.isChecked = HttpServerUtils.enableApiSmsQuery
        binding!!.sbApiQuerySms.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiSmsQuery = isChecked
            if (isChecked) checkReadSmsPermission()
        }

        binding!!.sbApiQueryCall.isChecked = HttpServerUtils.enableApiCallQuery
        binding!!.sbApiQueryCall.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiCallQuery = isChecked
            if (isChecked) checkCallPermission()
        }

        binding!!.sbApiQueryContacts.isChecked = HttpServerUtils.enableApiContactQuery
        binding!!.sbApiQueryContacts.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiContactQuery = isChecked
            if (isChecked) checkContactsPermission()
        }

        binding!!.sbApiAddContacts.isChecked = HttpServerUtils.enableApiContactAdd
        binding!!.sbApiAddContacts.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiContactAdd = isChecked
            if (isChecked) checkContactsPermission()
        }

        binding!!.sbApiQueryBattery.isChecked = HttpServerUtils.enableApiBatteryQuery
        binding!!.sbApiQueryBattery.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiBatteryQuery = isChecked
        }

        binding!!.sbApiWol.isChecked = HttpServerUtils.enableApiWol
        binding!!.sbApiWol.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiWol = isChecked
        }

        binding!!.sbApiLocation.isChecked = HttpServerUtils.enableApiLocation
        binding!!.sbApiLocation.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked && !SettingUtils.enableLocation) {
                XToastUtils.error(getString(R.string.api_location_permission_tips))
                binding!!.sbApiLocation.isChecked = false
                return@setOnCheckedChangeListener
            }

            HttpServerUtils.enableApiLocation = isChecked
            if (ServiceUtils.isServiceRunning("cn.ppps.forwarder.service.HttpServerService")) {
                Log.d("ServerFragment", "onClick: 重启服务")
                Intent(appContext, HttpServerService::class.java).also {
                    appContext?.stopService(it)
                    Thread.sleep(500)
                    appContext?.startService(it)
                }
                refreshButtonText()
            }
            //重启前台服务，启动/停止定位服务
            val serviceIntent = Intent(requireContext(), LocationService::class.java)
            serviceIntent.action = ACTION_RESTART
            requireContext().startService(serviceIntent)
        }

    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_toggle_server -> {
                //检查权限是否获取
                checkSendSmsPermission()
                checkReadSmsPermission()
                checkCallPermission()
                checkContactsPermission()
                checkLocationPermission()
                Intent(appContext, HttpServerService::class.java).also {
                    if (ServiceUtils.isServiceRunning("cn.ppps.forwarder.service.HttpServerService")) {
                        appContext?.stopService(it)
                    } else {
                        appContext?.startService(it)
                    }
                }
                refreshButtonText()
            }

            R.id.btn_sm4_key -> {
                val key = ConvertTools.bytes2HexString(SM4Crypt.createSM4Key())
                println("SM4密钥：$key")
                ClipboardUtils.copyText(key)
                binding!!.etSm4Key.setText(key)
                XToastUtils.info(getString(R.string.sign_key_tips))
            }

            R.id.btn_generate_key -> {
                val generator = KeyPairGenerator.getInstance("RSA") //密钥生成器
                generator.initialize(2048)
                val keyPair = generator.genKeyPair()
                val publicKey = keyPair.public
                val privateKey = keyPair.private

                val publicKeyEncoded = Base64.encode(publicKey.encoded)
                val privateKeyEncoded = Base64.encode(privateKey.encoded)

                println("publicKey=$publicKeyEncoded")
                println("privateKey=$privateKeyEncoded")

                binding!!.etPublicKey.setText(publicKeyEncoded)
                binding!!.etPrivateKey.setText(privateKeyEncoded)

                ClipboardUtils.copyText(publicKeyEncoded)
                XToastUtils.info(getString(R.string.rsa_key_tips))
            }

            R.id.btn_copy_public_key -> {
                ClipboardUtils.copyText(binding!!.etPublicKey.text)
                XToastUtils.info(getString(R.string.rsa_key_tips2))
            }

            R.id.btn_sign_key -> {
                val sign = RandomUtils.getRandomNumbersAndLetters(16)
                ClipboardUtils.copyText(sign)
                binding!!.etSignKey.setText(sign)
                XToastUtils.info(getString(R.string.sign_key_tips))
            }

            R.id.tv_server_tips, R.id.iv_copy -> {
                var hostAddress: String = if (inetAddress != null) "${inetAddress?.hostAddress}" else "127.0.0.1"
                hostAddress = if (hostAddress.indexOf(':', 0, false) > 0) "[${hostAddress}]" else hostAddress
                val url = "http://${hostAddress}:${HttpServerUtils.serverPort}"
                ClipboardUtils.copyText(url)
                XToastUtils.info(String.format(getString(R.string.copied_to_clipboard), url))
            }

            R.id.btn_path_picker -> {
                // 申请储存权限
                XXPermissions.with(this)
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .request(object : OnPermissionCallback {
                        override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                            val allGranted = deniedList.isEmpty()
                            if (!allGranted) {
                                // 判断请求失败的权限是否被用户勾选了不再询问的选项
                                val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                                if (doNotAskAgain) {
                                    XToastUtils.error(R.string.toast_denied_never)
                                    XXPermissions.startPermissionActivity(requireContext(), deniedList)
                                }
                                // 处理权限请求失败的逻辑
                                XToastUtils.error(R.string.toast_denied)
                                binding!!.etWebPath.setText(getString(R.string.storage_permission_tips))
                                return
                            }

                            // 处理权限请求成功的逻辑
                            val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
                            val dirList = listSubDir(downloadPath)
                            if (dirList.isEmpty()) {
                                XToastUtils.error(String.format(getString(R.string.download_first), downloadPath))
                                return
                            }
                            MaterialDialog.Builder(requireContext()).title(getString(R.string.select_web_client_directory)).content(String.format(getString(R.string.root_directory), downloadPath)).items(dirList).itemsCallbackSingleChoice(0) { _: MaterialDialog?, _: View?, _: Int, text: CharSequence ->
                                val webPath = "$downloadPath/$text"
                                binding!!.etWebPath.setText(webPath)
                                HttpServerUtils.serverWebPath = webPath

                                XToastUtils.info(getString(R.string.restarting_httpserver))
                                Intent(appContext, HttpServerService::class.java).also {
                                    if (ServiceUtils.isServiceRunning("cn.ppps.forwarder.service.HttpServerService")) {
                                        appContext?.stopService(it)
                                        Thread.sleep(500)
                                        appContext?.startService(it)
                                    } else {
                                        appContext?.startService(it)
                                    }
                                }
                                refreshButtonText()
                                true // allow selection
                            }.positiveText(R.string.select).negativeText(R.string.cancel).show()
                        }
                    })
            }

            else -> {}
        }
    }

    //刷新按钮
    private fun refreshButtonText() {
        if (ServiceUtils.isServiceRunning("cn.ppps.forwarder.service.HttpServerService")) {
            binding!!.btnToggleServer.text = resources.getText(R.string.stop_server)
            binding!!.ivCopy.visibility = View.VISIBLE
            try {
                inetAddress = NetworkUtils.getLocalInetAddress()
                binding!!.tvServerTips.text = getString(R.string.http_server_running, inetAddress!!.hostAddress, HttpServerUtils.serverPort)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ServerFragment", "refreshButtonText error: ${e.message}")
                binding!!.tvServerTips.text = getString(R.string.http_server_running, "127.0.0.1", HttpServerUtils.serverPort)
            }
        } else {
            binding!!.btnToggleServer.text = resources.getText(R.string.start_server)
            binding!!.tvServerTips.text = getString(R.string.http_server_stopped)
            binding!!.ivCopy.visibility = View.GONE
        }
    }

    //发送短信权限
    private fun checkSendSmsPermission() {
        XXPermissions.with(this)
            // 发送短信
            .permission(PermissionLists.getSendSmsPermission())
            .request(object : OnPermissionCallback {
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    val allGranted = deniedList.isEmpty()
                    if (!allGranted) {
                        // 判断请求失败的权限是否被用户勾选了不再询问的选项
                        val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                        if (doNotAskAgain) {
                            XToastUtils.error(R.string.toast_denied_never)
                            XXPermissions.startPermissionActivity(requireContext(), deniedList)
                        }
                        // 处理权限请求失败的逻辑
                        XToastUtils.error(R.string.toast_denied)
                        HttpServerUtils.enableApiSmsSend = false
                        binding!!.sbApiSendSms.isChecked = false
                        return
                    }
                }
            })
    }

    //读取短信权限
    private fun checkReadSmsPermission() {
        XXPermissions.with(this)
            // 接收短信
            .permission(PermissionLists.getReceiveSmsPermission())
            // 发送短信
            .permission(PermissionLists.getSendSmsPermission())
            // 读取短信
            .permission(PermissionLists.getReadSmsPermission())
            .request(object : OnPermissionCallback {
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    val allGranted = deniedList.isEmpty()
                    if (!allGranted) {
                        // 判断请求失败的权限是否被用户勾选了不再询问的选项
                        val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                        if (doNotAskAgain) {
                            XToastUtils.error(R.string.toast_denied_never)
                            XXPermissions.startPermissionActivity(requireContext(), deniedList)
                        }
                        // 处理权限请求失败的逻辑
                        XToastUtils.error(R.string.toast_denied)
                        HttpServerUtils.enableApiSmsQuery = false
                        binding!!.sbApiQuerySms.isChecked = false
                        return
                    }
                }
            })
    }

    //电话权限
    private fun checkCallPermission() {
        XXPermissions.with(this)
            // 读取电话状态
            .permission(PermissionLists.getReadPhoneStatePermission())
            // 读取手机号码
            .permission(PermissionLists.getReadPhoneNumbersPermission())
            // 读取通话记录
            .permission(PermissionLists.getReadCallLogPermission())
            .request(object : OnPermissionCallback {
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    val allGranted = deniedList.isEmpty()
                    if (!allGranted) {
                        // 判断请求失败的权限是否被用户勾选了不再询问的选项
                        val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                        if (doNotAskAgain) {
                            XToastUtils.error(R.string.toast_denied_never)
                            XXPermissions.startPermissionActivity(requireContext(), deniedList)
                        }
                        // 处理权限请求失败的逻辑
                        XToastUtils.error(R.string.toast_denied)
                        HttpServerUtils.enableApiCallQuery = false
                        binding!!.sbApiQueryCall.isChecked = false
                        return
                    }
                }
            })
    }

    //联系人权限
    private fun checkContactsPermission() {
        XXPermissions.with(this)
            .permission(PermissionLists.getReadContactsPermission())
            .permission(PermissionLists.getWriteContactsPermission())
            .request(object : OnPermissionCallback {
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    val allGranted = deniedList.isEmpty()
                    if (!allGranted) {
                        // 判断请求失败的权限是否被用户勾选了不再询问的选项
                        val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                        if (doNotAskAgain) {
                            XToastUtils.error(R.string.toast_denied_never)
                            XXPermissions.startPermissionActivity(requireContext(), deniedList)
                        }
                        // 处理权限请求失败的逻辑
                        XToastUtils.error(R.string.toast_denied)
                        HttpServerUtils.enableApiCallQuery = false
                        binding!!.sbApiQueryCall.isChecked = false
                        return
                    }
                }
            })
    }

    //定位权限
    private fun checkLocationPermission() {
        XXPermissions.with(this)
            .permission(PermissionLists.getAccessCoarseLocationPermission())
            .permission(PermissionLists.getAccessFineLocationPermission())
            .permission(PermissionLists.getAccessBackgroundLocationPermission())
            .permission(PermissionLists.getReadPhoneStatePermission())
            .request(object : OnPermissionCallback {
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    val allGranted = deniedList.isEmpty()
                    if (!allGranted) {
                        // 判断请求失败的权限是否被用户勾选了不再询问的选项
                        val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(requireActivity(), deniedList)
                        if (doNotAskAgain) {
                            XToastUtils.error(R.string.toast_denied_never)
                            XXPermissions.startPermissionActivity(requireContext(), deniedList)
                        }
                        // 处理权限请求失败的逻辑
                        XToastUtils.error(R.string.toast_denied)
                        HttpServerUtils.enableApiLocation = false
                        binding!!.sbApiLocation.isChecked = false
                        return
                    }
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        //取消定时器
        handler.removeCallbacks(runnable)
    }

    //获取Download的子目录
    private fun listSubDir(downloadPath: String): List<String> {
        val dirList = mutableListOf<String>()
        val downloadDir = File(downloadPath)
        val files = downloadDir.listFiles() ?: return dirList

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".") && !file.name.startsWith("leakcanary-")) {
                dirList.add(file.name)
            }
        }
        return dirList
    }

}