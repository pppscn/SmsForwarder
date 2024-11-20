package com.idormy.sms.forwarder.fragment.senders

import android.annotation.SuppressLint
import android.os.Environment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.core.BaseFragment
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.database.entity.Sender
import com.idormy.sms.forwarder.database.viewmodel.BaseViewModelFactory
import com.idormy.sms.forwarder.database.viewmodel.SenderViewModel
import com.idormy.sms.forwarder.databinding.FragmentSendersEmailBinding
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.entity.setting.EmailSetting
import com.idormy.sms.forwarder.utils.Base64
import com.idormy.sms.forwarder.utils.CommonUtils
import com.idormy.sms.forwarder.utils.EVENT_TOAST_ERROR
import com.idormy.sms.forwarder.utils.KEY_SENDER_CLONE
import com.idormy.sms.forwarder.utils.KEY_SENDER_ID
import com.idormy.sms.forwarder.utils.KEY_SENDER_TEST
import com.idormy.sms.forwarder.utils.KEY_SENDER_TYPE
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.XToastUtils
import com.idormy.sms.forwarder.utils.sender.EmailUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xaop.annotation.SingleClick
import com.xuexiang.xpage.annotation.Page
import com.xuexiang.xrouter.annotation.AutoWired
import com.xuexiang.xrouter.launcher.XRouter
import com.xuexiang.xui.utils.CountDownButtonHelper
import com.xuexiang.xui.widget.actionbar.TitleBar
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog
import com.xuexiang.xui.widget.spinner.materialspinner.MaterialSpinner
import com.xuexiang.xutil.resource.ResUtils.getStringArray
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.pgpainless.PGPainless
import org.pgpainless.key.info.KeyRingInfo
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date

@Page(name = "Email")
@Suppress("PrivatePropertyName")
class EmailFragment : BaseFragment<FragmentSendersEmailBinding?>(), View.OnClickListener {

    private val TAG: String = EmailFragment::class.java.simpleName
    private var titleBar: TitleBar? = null
    private val viewModel by viewModels<SenderViewModel> { BaseViewModelFactory(context) }
    private var mCountDownHelper: CountDownButtonHelper? = null
    private var mailType: String = getString(R.string.other_mail_type) //邮箱类型
    private var recipientItemMap: MutableMap<Int, LinearLayout> = mutableMapOf()
    private val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

    //加密协议: S/MIME、OpenPGP、Plain（不传证书）
    private var encryptionProtocol: String = "Plain"

    @JvmField
    @AutoWired(name = KEY_SENDER_ID)
    var senderId: Long = 0

    @JvmField
    @AutoWired(name = KEY_SENDER_TYPE)
    var senderType: Int = 0

    @JvmField
    @AutoWired(name = KEY_SENDER_CLONE)
    var isClone: Boolean = false

    override fun initArgs() {
        XRouter.getInstance().inject(this)
    }

    override fun viewBindingInflate(
        inflater: LayoutInflater,
        container: ViewGroup,
    ): FragmentSendersEmailBinding {
        return FragmentSendersEmailBinding.inflate(inflater, container, false)
    }

    override fun initTitle(): TitleBar? {
        titleBar = super.initTitle()!!.setImmersive(false).setTitle(R.string.email)
        return titleBar
    }

    /**
     * 初始化控件
     */
    override fun initViews() {
        //测试按钮增加倒计时，避免重复点击
        mCountDownHelper = CountDownButtonHelper(binding!!.btnTest, SettingUtils.requestTimeout)
        mCountDownHelper!!.setOnCountDownListener(object : CountDownButtonHelper.OnCountDownListener {
            override fun onCountDown(time: Int) {
                binding!!.btnTest.text = String.format(getString(R.string.seconds_n), time)
            }

            override fun onFinished() {
                binding!!.btnTest.text = getString(R.string.test)
            }
        })

        val mailTypeArray = getStringArray(R.array.MailType)
        binding!!.spMailType.setOnItemSelectedListener { _: MaterialSpinner?, position: Int, _: Long, item: Any ->
            mailType = item.toString()
            //XToastUtils.warning(mailType)
            binding!!.layoutServiceSetting.visibility = if (position == mailTypeArray.size - 1) View.VISIBLE else View.GONE
        }
        binding!!.spMailType.setOnNothingSelectedListener {
            mailType = mailTypeArray[mailTypeArray.size - 1]
            binding!!.spMailType.selectedIndex = mailTypeArray.size - 1
            binding!!.layoutServiceSetting.visibility = View.VISIBLE
        }
        binding!!.spMailType.selectedIndex = mailTypeArray.size - 1
        binding!!.layoutServiceSetting.visibility = View.VISIBLE

        binding!!.rgEncryptionProtocol.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_encryption_protocol_smime -> {
                    encryptionProtocol = "S/MIME"
                    binding!!.layoutSenderKeystore.visibility = View.VISIBLE
                    binding!!.tvSenderKeystore.text = getString(R.string.sender_smime_keystore)
                    binding!!.tvEmailTo.text = getString(R.string.email_to_smime)
                    binding!!.tvEmailToTips.text = getString(R.string.email_to_smime_tips)
                }

                R.id.rb_encryption_protocol_openpgp -> {
                    encryptionProtocol = "OpenPGP"
                    binding!!.layoutSenderKeystore.visibility = View.VISIBLE
                    binding!!.tvSenderKeystore.text = getString(R.string.sender_openpgp_keystore)
                    binding!!.tvEmailTo.text = getString(R.string.email_to_openpgp)
                    binding!!.tvEmailToTips.text = getString(R.string.email_to_openpgp_tips)
                }

                else -> {
                    encryptionProtocol = "Plain"
                    binding!!.layoutSenderKeystore.visibility = View.GONE
                    binding!!.tvEmailTo.text = getString(R.string.email_to)
                    binding!!.tvEmailToTips.text = getString(R.string.email_to_tips)
                }
            }

            //遍历 layout_recipients 子元素，设置 layout_recipient_keystore 可见性
            for (recipientItem in recipientItemMap.values) {
                val layoutRecipientKeystore = recipientItem.findViewById<LinearLayout>(R.id.layout_recipient_keystore)
                layoutRecipientKeystore.visibility = if (encryptionProtocol == "Plain") View.GONE else View.VISIBLE
            }
        }

        //创建标签按钮
        CommonUtils.createTagButtons(requireContext(), binding!!.glTitleTemplate, binding!!.etTitleTemplate)
        CommonUtils.createTagButtons(requireContext(), binding!!.glNickname, binding!!.etNickname)

        //新增
        if (senderId <= 0) {
            titleBar?.setSubTitle(getString(R.string.add_sender))
            binding!!.btnDel.setText(R.string.discard)
            return
        }

        //编辑
        binding!!.btnDel.setText(R.string.del)
        Core.sender.get(senderId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : SingleObserver<Sender> {
            override fun onSubscribe(d: Disposable) {}

            override fun onError(e: Throwable) {
                e.printStackTrace()
                Log.e(TAG, "onError:$e")
            }

            override fun onSuccess(sender: Sender) {
                if (isClone) {
                    titleBar?.setSubTitle(getString(R.string.clone_sender) + ": " + sender.name)
                    binding!!.btnDel.setText(R.string.discard)
                } else {
                    titleBar?.setSubTitle(getString(R.string.edit_sender) + ": " + sender.name)
                }
                binding!!.etName.setText(sender.name)
                binding!!.sbEnable.isChecked = sender.status == 1
                val settingVo = Gson().fromJson(sender.jsonSetting, EmailSetting::class.java)
                Log.d(TAG, settingVo.toString())
                if (settingVo != null) {
                    if (!TextUtils.isEmpty(settingVo.mailType)) {
                        mailType = settingVo.mailType
                        //TODO: 替换mailType为当前语言，避免切换语言后失效，历史包袱怎么替换比较优雅？
                        if (mailType == "other" || mailType == "其他邮箱" || mailType == "其他郵箱") {
                            mailType = getString(R.string.other_mail_type)
                        }
                        binding!!.spMailType.setSelectedItem(mailType)
                        if (mailType != getString(R.string.other_mail_type)) {
                            binding!!.layoutServiceSetting.visibility = View.GONE
                        }
                    }
                    binding!!.etFromEmail.setText(settingVo.fromEmail)
                    binding!!.etPwd.setText(settingVo.pwd)
                    binding!!.etNickname.setText(settingVo.nickname)
                    binding!!.etHost.setText(settingVo.host)
                    binding!!.etPort.setText(settingVo.port)
                    binding!!.sbSsl.isChecked = settingVo.ssl == true
                    binding!!.sbStartTls.isChecked = settingVo.startTls == true
                    binding!!.etTitleTemplate.setText(settingVo.title)
                    encryptionProtocol = settingVo.encryptionProtocol
                    binding!!.rgEncryptionProtocol.check(settingVo.getEncryptionProtocolCheckId())
                    if (settingVo.recipients.isNotEmpty()) {
                        for ((email, cert) in settingVo.recipients) {
                            addRecipientItem(email, cert)
                        }
                    } else {
                        //兼容旧版本
                        val emails = settingVo.toEmail.split(",")
                        if (emails.isNotEmpty()) {
                            for (email in emails.toTypedArray()) {
                                addRecipientItem(email)
                            }
                        }
                    }
                    binding!!.etSenderKeystore.setText(settingVo.keystore)
                    binding!!.etSenderPassword.setText(settingVo.password)
                }
            }
        })
    }

    override fun initListeners() {
        binding!!.btnTest.setOnClickListener(this)
        binding!!.btnDel.setOnClickListener(this)
        binding!!.btnSave.setOnClickListener(this)
        binding!!.btnAddRecipient.setOnClickListener {
            addRecipientItem()
        }
        binding!!.btnSenderKeystorePicker.setOnClickListener {
            pickCert(binding!!.etSenderKeystore)
        }
        LiveEventBus.get(KEY_SENDER_TEST, String::class.java).observe(this) { mCountDownHelper?.finish() }
    }

    @SingleClick
    override fun onClick(v: View) {
        try {
            when (v.id) {

                R.id.btn_test -> {
                    mCountDownHelper?.start()
                    Thread {
                        try {
                            val settingVo = checkSetting()
                            Log.d(TAG, settingVo.toString())
                            val name = binding!!.etName.text.toString().trim().takeIf { it.isNotEmpty() } ?: getString(R.string.test_sender_name)
                            val msgInfo = MsgInfo("sms", getString(R.string.test_phone_num), String.format(getString(R.string.test_sender_sms), name), Date(), getString(R.string.test_sim_info))
                            EmailUtils.sendMsg(settingVo, msgInfo)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, "onClick error:$e")
                            LiveEventBus.get(EVENT_TOAST_ERROR, String::class.java).post(e.message.toString())
                        }
                        LiveEventBus.get(KEY_SENDER_TEST, String::class.java).post("finish")
                    }.start()
                    return
                }

                R.id.btn_del -> {
                    if (senderId <= 0 || isClone) {
                        popToBack()
                        return
                    }

                    MaterialDialog.Builder(requireContext()).title(R.string.delete_sender_title).content(R.string.delete_sender_tips).positiveText(R.string.lab_yes).negativeText(R.string.lab_no).onPositive { _: MaterialDialog?, _: DialogAction? ->
                        viewModel.delete(senderId)
                        XToastUtils.success(R.string.delete_sender_toast)
                        popToBack()
                    }.show()
                    return
                }

                R.id.btn_save -> {
                    val name = binding!!.etName.text.toString().trim()
                    if (TextUtils.isEmpty(name)) {
                        throw Exception(getString(R.string.invalid_name))
                    }

                    val status = if (binding!!.sbEnable.isChecked) 1 else 0
                    val settingVo = checkSetting()
                    if (isClone) senderId = 0
                    val senderNew = Sender(senderId, senderType, name, Gson().toJson(settingVo), status)
                    Log.d(TAG, senderNew.toString())

                    viewModel.insertOrUpdate(senderNew)
                    XToastUtils.success(R.string.tipSaveSuccess)
                    popToBack()
                    return
                }
            }
        } catch (e: Exception) {
            XToastUtils.error(e.message.toString())
            e.printStackTrace()
            Log.e(TAG, "onClick error:$e")
        }
    }

    private fun checkSetting(): EmailSetting {
        val fromEmail = binding!!.etFromEmail.text.toString().trim()
        val pwd = binding!!.etPwd.text.toString().trim()
        val recipients = getRecipientsFromRecipientItemMap()
        if (TextUtils.isEmpty(fromEmail) || TextUtils.isEmpty(pwd) || recipients.isEmpty()) {
            throw Exception(getString(R.string.invalid_email))
        }
        for ((email, cert) in recipients) {
            if (!CommonUtils.checkEmail(email)) {
                throw Exception(String.format(getString(R.string.invalid_recipient_email), email))
            }
            Log.d(TAG, "email: $email, cert: $cert")
            when (encryptionProtocol) {
                "S/MIME" -> {
                    when {
                        cert.first.isNotEmpty() && cert.second.isNotEmpty() -> {
                            try {
                                // 判断是否有效的PKCS12私钥证书
                                val fileInputStream = if (cert.first.startsWith("/")) {
                                    FileInputStream(cert.first)
                                } else {
                                    val decodedBytes = Base64.decode(cert.first)
                                    ByteArrayInputStream(decodedBytes)
                                }
                                val keyStore = KeyStore.getInstance("PKCS12")
                                keyStore.load(fileInputStream, cert.second.toCharArray())
                                val alias = keyStore.aliases().nextElement()
                                val recipientPublicKey = keyStore.getCertificate(alias) as X509Certificate
                                Log.d(TAG, "PKCS12 Certificate: $recipientPublicKey")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                throw Exception(String.format(getString(R.string.invalid_pkcs12_certificate), email))
                            }
                        }

                        cert.first.isNotEmpty() && cert.second.isEmpty() -> {
                            try {
                                // 判断是否有效的X.509公钥证书
                                val fileInputStream = if (cert.first.startsWith("/")) {
                                    FileInputStream(cert.first)
                                } else {
                                    val decodedBytes = Base64.decode(cert.first)
                                    ByteArrayInputStream(decodedBytes)
                                }
                                val certFactory = CertificateFactory.getInstance("X.509")
                                val recipientPublicKey = certFactory.generateCertificate(fileInputStream) as X509Certificate
                                Log.d(TAG, "X.509 Certificate: $recipientPublicKey")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                throw Exception(String.format(getString(R.string.invalid_x509_certificate), email))
                            }
                        }
                    }
                }

                "OpenPGP" -> {
                    when {
                        cert.first.isNotEmpty() && cert.second.isNotEmpty() -> {
                            try {
                                //从私钥证书文件提取公钥
                                val recipientPrivateKeyStream = if (cert.first.startsWith("/")) {
                                    FileInputStream(cert.first)
                                } else {
                                    val decodedBytes = Base64.decode(cert.first)
                                    ByteArrayInputStream(decodedBytes)
                                }
                                val recipientPGPSecretKeyRing = PGPainless.readKeyRing().secretKeyRing(recipientPrivateKeyStream)
                                val recipientPGPPublicKeyRing = PGPainless.extractCertificate(recipientPGPSecretKeyRing!!)
                                val keyInfo = KeyRingInfo(recipientPGPPublicKeyRing)
                                Log.d(TAG, "recipientPGPPublicKeyRing: $keyInfo")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                throw Exception(String.format(getString(R.string.invalid_x509_certificate), email))
                            }
                        }

                        cert.first.isNotEmpty() && cert.second.isEmpty() -> {
                            try {
                                //从证书文件提取公钥
                                val recipientPublicKeyStream = if (cert.first.startsWith("/")) {
                                    FileInputStream(cert.first)
                                } else {
                                    val decodedBytes = Base64.decode(cert.first)
                                    ByteArrayInputStream(decodedBytes)
                                }
                                val recipientPGPPublicKeyRing = PGPainless.readKeyRing().publicKeyRing(recipientPublicKeyStream)
                                val keyInfo = KeyRingInfo(recipientPGPPublicKeyRing!!)
                                Log.d(TAG, "recipientPGPPublicKeyRing: $keyInfo")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                throw Exception(String.format(getString(R.string.invalid_x509_certificate), email))
                            }
                        }
                    }
                }
            }
        }
        val host = binding!!.etHost.text.toString().trim()
        val port = binding!!.etPort.text.toString().trim()
        if (mailType == getString(R.string.other_mail_type) && (TextUtils.isEmpty(host) || TextUtils.isEmpty(port))) {
            throw Exception(getString(R.string.invalid_email_server))
        }

        val nickname = binding!!.etNickname.text.toString().trim()
        val ssl = binding!!.sbSsl.isChecked
        val startTls = binding!!.sbStartTls.isChecked
        val title = binding!!.etTitleTemplate.text.toString().trim()
        val keystore = binding!!.etSenderKeystore.text.toString().trim()
        val password = binding!!.etSenderPassword.text.toString().trim()
        if (keystore.isNotEmpty()) {
            val senderPrivateKeyStream = if (keystore.startsWith("/")) {
                FileInputStream(keystore)
            } else {
                val decodedBytes = Base64.decode(keystore)
                ByteArrayInputStream(decodedBytes)
            }
            if (senderPrivateKeyStream.available() <= 0) {
                throw Exception(getString(R.string.invalid_sender_keystore))
            }
            when (encryptionProtocol) {
                "S/MIME" -> {
                    try {
                        // 判断是否有效的PKCS12私钥证书
                        val keyStore = KeyStore.getInstance("PKCS12")
                        keyStore.load(senderPrivateKeyStream, password.toCharArray())
                        val alias = keyStore.aliases().nextElement()
                        val certificate = keyStore.getCertificate(alias) as X509Certificate
                        Log.d(TAG, "PKCS12 Certificate: $certificate")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw Exception(getString(R.string.invalid_sender_keystore))
                    }
                }

                "OpenPGP" -> {
                    try {
                        val senderPGPSecretKeyRing = PGPainless.readKeyRing().secretKeyRing(senderPrivateKeyStream)
                        val keyInfo = KeyRingInfo(senderPGPSecretKeyRing!!)
                        Log.d(TAG, "senderPGPSecretKeyRing: $keyInfo")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw Exception(getString(R.string.invalid_sender_keystore))
                    }
                }
            }

        }

        return EmailSetting(mailType, fromEmail, pwd, nickname, host, port, ssl, startTls, title, recipients, "", keystore, password, encryptionProtocol)
    }

    //recipient序号
    private var recipientItemId = 0

    /**
     * 动态增删recipient
     *
     * @param email            recipient的email
     * @param cert             recipient的cert，为空则不设置
     */
    private fun addRecipientItem(email: String = "", cert: Any? = null) {
        val itemAddRecipient = View.inflate(requireContext(), R.layout.item_add_recipient, null) as LinearLayout
        val etRecipientEmail = itemAddRecipient.findViewById<EditText>(R.id.et_recipient_email)
        val etRecipientKeystore = itemAddRecipient.findViewById<EditText>(R.id.et_recipient_keystore)
        val etRecipientPassword = itemAddRecipient.findViewById<EditText>(R.id.et_recipient_password)
        etRecipientEmail.setText(email)
        Log.d(TAG, "cert: $cert")
        when (cert) {
            is String -> etRecipientKeystore.setText(cert)
            is Pair<*, *> -> {
                Log.d(TAG, "cert.first: ${cert.first}")
                Log.d(TAG, "cert.second: ${cert.second}")
                etRecipientKeystore.setText(cert.first.toString())
                etRecipientPassword.setText(cert.second.toString())
            }
        }

        val ivDel = itemAddRecipient.findViewById<ImageView>(R.id.iv_del)
        ivDel.tag = recipientItemId
        ivDel.setOnClickListener {
            val itemId = it.tag as Int
            binding!!.layoutRecipients.removeView(recipientItemMap[itemId])
            recipientItemMap.remove(itemId)
        }

        val btnFilePicker = itemAddRecipient.findViewById<Button>(R.id.btn_file_picker)
        btnFilePicker.tag = recipientItemId
        btnFilePicker.setOnClickListener {
            val itemId = it.tag as Int
            val etKeyStore = recipientItemMap[itemId]!!.findViewById<EditText>(R.id.et_recipient_keystore)
            pickCert(etKeyStore)
        }

        val layoutRecipientKeystore = itemAddRecipient.findViewById<LinearLayout>(R.id.layout_recipient_keystore)
        layoutRecipientKeystore.visibility = if (encryptionProtocol == "Plain") View.GONE else View.VISIBLE

        binding!!.layoutRecipients.addView(itemAddRecipient)
        recipientItemMap[recipientItemId] = itemAddRecipient
        recipientItemId++
    }

    /**
     * 从EditText控件中获取全部recipients
     *
     * @return 全部recipients
     */
    private fun getRecipientsFromRecipientItemMap(): MutableMap<String, Pair<String, String>> {
        val recipients: MutableMap<String, Pair<String, String>> = mutableMapOf()
        for (recipientItem in recipientItemMap.values) {
            val etRecipientEmail = recipientItem.findViewById<EditText>(R.id.et_recipient_email)
            val etRecipientKeystore = recipientItem.findViewById<EditText>(R.id.et_recipient_keystore)
            val etRecipientPassword = recipientItem.findViewById<EditText>(R.id.et_recipient_password)
            val email = etRecipientEmail.text.toString().trim()
            val keystore = etRecipientKeystore.text.toString().trim()
            val password = etRecipientPassword.text.toString().trim()
            recipients[email] = Pair(keystore, password)
        }
        Log.d(TAG, "recipients: $recipients")
        return recipients
    }

    //选择证书文件
    private fun pickCert(etKeyStore: EditText) {
        XXPermissions.with(this)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                @SuppressLint("SetTextI18n")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    val fileList = findSupportedFiles(downloadPath)
                    if (fileList.isEmpty()) {
                        XToastUtils.error(String.format(getString(R.string.download_certificate_first), downloadPath))
                        return
                    }
                    MaterialDialog.Builder(requireContext())
                        .title(getString(R.string.keystore_base64))
                        .content(String.format(getString(R.string.root_directory), downloadPath))
                        .items(fileList)
                        .itemsCallbackSingleChoice(0) { _: MaterialDialog?, _: View?, _: Int, text: CharSequence ->
                            val webPath = "$downloadPath/$text"
                            etKeyStore.setText(convertCertToBase64String(webPath))
                            true // allow selection
                        }
                        .positiveText(R.string.select)
                        .negativeText(R.string.cancel)
                        .show()
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        XToastUtils.error(R.string.toast_denied_never)
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(requireContext(), permissions)
                    } else {
                        XToastUtils.error(R.string.toast_denied)
                    }
                }
            })
    }

    private fun findSupportedFiles(directoryPath: String): List<String> {
        val audioFiles = mutableListOf<String>()
        val directory = File(directoryPath)

        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.let { files ->
                files.filter { it.isFile && isSupportedFile(it) }.forEach { audioFiles.add(it.name) }
            }
        }

        return audioFiles
    }

    private fun isSupportedFile(file: File): Boolean {
        val supportedExtensions = if (encryptionProtocol == "OpenPGP") {
            listOf("asc", "pgp")
        } else {
            listOf("pfx", "p12", "pem", "cer", "crt", "der")
        }
        return supportedExtensions.any { it.equals(file.extension, ignoreCase = true) }
    }

    private fun convertCertToBase64String(pfxFilePath: String): String {
        val pfxInputStream = FileInputStream(pfxFilePath)
        val pfxBytes = pfxInputStream.readBytes()
        return Base64.encode(pfxBytes)
    }

    override fun onDestroyView() {
        if (mCountDownHelper != null) mCountDownHelper!!.recycle()
        super.onDestroyView()
    }

}
