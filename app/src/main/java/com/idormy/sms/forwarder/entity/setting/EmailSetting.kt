package com.idormy.sms.forwarder.entity.setting

import com.idormy.sms.forwarder.R
import java.io.Serializable

data class EmailSetting(
    var mailType: String = "",
    var fromEmail: String = "",
    var pwd: String = "",
    var nickname: String = "",
    var host: String = "",
    var port: String = "",
    var ssl: Boolean = false,
    var startTls: Boolean = false,
    var title: String = "",
    var recipients: MutableMap<String, Pair<String, String>> = mutableMapOf(),
    var toEmail: String = "",
    var keystore: String = "",
    var password: String = "",
    var encryptionProtocol: String = "Plain", //加密协议: S/MIME、OpenPGP、Plain（不传证书）
) : Serializable {

    fun getEncryptionProtocolCheckId(): Int {
        return when (encryptionProtocol) {
            "S/MIME" -> R.id.rb_encryption_protocol_smime
            "OpenPGP" -> R.id.rb_encryption_protocol_openpgp
            else -> R.id.rb_encryption_protocol_plain
        }
    }
}
