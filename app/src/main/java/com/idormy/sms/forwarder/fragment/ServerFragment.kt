package com.idormy.sms.forwarder.fragment

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.databinding.FragmentServerBinding
import com.idormy.sms.forwarder.service.HttpService
import com.idormy.sms.forwarder.utils.HTTP_SERVER_PORT
import com.idormy.sms.forwarder.utils.HttpServerUtils
import com.idormy.sms.forwarder.utils.RandomUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.button.SmoothCheckBox
import com.xuexiang.xutil.app.ServiceUtils
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.system.ClipboardUtils
import java.net.InetAddress

@Suppress("PrivatePropertyName")
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
        binding!!.toggleServerBtn.setOnClickListener(this)

        binding!!.scbServerAutorun.isChecked = HttpServerUtils.enableServerAutorun
        binding!!.scbServerAutorun.setOnCheckedChangeListener { _: SmoothCheckBox, isChecked: Boolean ->
            HttpServerUtils.enableServerAutorun = isChecked
        }
        //启动更新UI定时器
        handler.post(runnable)

        binding!!.btnSignKey.setOnClickListener(this)
        binding!!.etSignKey.setText(HttpServerUtils.serverSignKey)
        binding!!.etSignKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                HttpServerUtils.serverSignKey = binding!!.etSignKey.text.toString().trim()
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

        binding!!.sbApiQueryBattery.isChecked = HttpServerUtils.enableApiBatteryQuery
        binding!!.sbApiQueryBattery.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiBatteryQuery = isChecked
        }

        binding!!.sbApiWol.isChecked = HttpServerUtils.enableApiWol
        binding!!.sbApiWol.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            HttpServerUtils.enableApiWol = isChecked
        }

    }

    @SingleClick
    override fun onClick(v: View) {
        when (v.id) {
            R.id.toggle_server_btn -> {
                //检查权限是否获取
                checkSendSmsPermission()
                checkReadSmsPermission()
                checkCallPermission()
                checkContactsPermission()
                if (ServiceUtils.isServiceRunning("com.idormy.sms.forwarder.service.HttpService")) {
                    appContext?.stopService(Intent(appContext, HttpService::class.java))
                } else {
                    appContext?.startService(Intent(appContext, HttpService::class.java))
                }
                refreshButtonText()
            }
            R.id.btn_sign_key -> {
                val sign = RandomUtils.getRandomNumbersAndLetters(16)
                ClipboardUtils.copyText(sign)
                binding!!.etSignKey.setText(sign)
                XToastUtils.info(getString(R.string.sign_key_tips))
            }
            R.id.tv_server_tips, R.id.iv_copy -> {
                val url = if (inetAddress != null) "http://${inetAddress?.hostAddress}:5000" else "http://127.0.0.1:5000"
                ClipboardUtils.copyText(url)
                XToastUtils.info(String.format(getString(R.string.copied_to_clipboard), url))
            }
            else -> {}
        }
    }

    //刷新按钮
    private fun refreshButtonText() {
        if (ServiceUtils.isServiceRunning("com.idormy.sms.forwarder.service.HttpService")) {
            binding!!.toggleServerBtn.text = resources.getText(R.string.stop_server)
            binding!!.ivCopy.visibility = View.VISIBLE
            try {
                inetAddress = NetworkUtils.getLocalInetAddress()
                binding!!.tvServerTips.text = getString(R.string.http_server_running, inetAddress!!.hostAddress, HTTP_SERVER_PORT)

            } catch (e: Exception) {
                e.printStackTrace()
                binding!!.tvServerTips.text = getString(R.string.http_server_running, "127.0.0.1", HTTP_SERVER_PORT)
            }
        } else {
            binding!!.toggleServerBtn.text = resources.getText(R.string.start_server)
            binding!!.tvServerTips.text = getString(R.string.http_server_stopped)
            binding!!.ivCopy.visibility = View.GONE
        }
    }

    //发送短信权限
    private fun checkSendSmsPermission() {
        XXPermissions.with(this)
            // 发送短信
            .permission(Permission.SEND_SMS)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        XToastUtils.error(R.string.toast_denied_never)
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        XToastUtils.error(R.string.toast_denied)
                    }
                    HttpServerUtils.enableApiSmsSend = false
                    binding!!.sbApiSendSms.isChecked = false
                }
            })
    }

    //读取短信权限
    private fun checkReadSmsPermission() {
        XXPermissions.with(this)
            // 接收短信
            .permission(Permission.RECEIVE_SMS)
            // 发送短信
            .permission(Permission.SEND_SMS)
            // 读取短信
            .permission(Permission.READ_SMS)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        XToastUtils.error(R.string.toast_denied_never)
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        XToastUtils.error(R.string.toast_denied)
                    }
                    HttpServerUtils.enableApiSmsQuery = false
                    binding!!.sbApiQuerySms.isChecked = false
                }
            })
    }

    //电话权限
    private fun checkCallPermission() {
        XXPermissions.with(this)
            // 读取电话状态
            .permission(Permission.READ_PHONE_STATE)
            // 读取手机号码
            .permission(Permission.READ_PHONE_NUMBERS)
            // 读取通话记录
            .permission(Permission.READ_CALL_LOG)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        XToastUtils.error(R.string.toast_denied_never)
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        XToastUtils.error(R.string.toast_denied)
                    }
                    HttpServerUtils.enableApiCallQuery = false
                    binding!!.sbApiQueryCall.isChecked = false
                }
            })
    }

    //联系人权限
    private fun checkContactsPermission() {
        XXPermissions.with(this)
            .permission(*Permission.Group.CONTACTS)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        XToastUtils.error(R.string.toast_denied_never)
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        XToastUtils.error(R.string.toast_denied)
                    }
                    HttpServerUtils.enableApiContactQuery = false
                    binding!!.sbApiQueryContacts.isChecked = false
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        //取消定时器
        handler.removeCallbacks(runnable)
    }

}