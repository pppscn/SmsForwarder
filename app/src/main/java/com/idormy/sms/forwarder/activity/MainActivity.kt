package com.idormy.sms.forwarder.activity

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.utils.SettingUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etWebhookUrl = findViewById<EditText>(R.id.et_webhook_url)
        val etFallbackPhone = findViewById<EditText>(R.id.et_fallback_phone)
        val cbEnableForwarding = findViewById<CheckBox>(R.id.cb_enable_forwarding)
        val btnSave = findViewById<Button>(R.id.btn_save)

        etWebhookUrl.setText(SettingUtils.webhookUrl)
        etFallbackPhone.setText(SettingUtils.fallbackSmsPhone)
        etWebhookUrl.isEnabled = false
        etFallbackPhone.isEnabled = false
        cbEnableForwarding.isChecked = SettingUtils.enableSmsForwarding

        btnSave.setOnClickListener {
            SettingUtils.enableSmsForwarding = cbEnableForwarding.isChecked
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        }
    }
}
