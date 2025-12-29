package com.idormy.sms.forwarder.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.service.ForegroundService
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "MainActivity onCreate")

        checkPermissions()

        if (!ForegroundService.isRunning) {
            Log.d(TAG, "Starting ForegroundService")
            val intent = Intent(this, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        val etWebhookUrl = findViewById<EditText>(R.id.et_webhook_url)
        val etFallbackPhone = findViewById<EditText>(R.id.et_fallback_phone)
        val cbEnableForwarding = findViewById<CheckBox>(R.id.cb_enable_forwarding)
        val btnSave = findViewById<Button>(R.id.btn_save)

        etWebhookUrl.setText(SettingUtils.webhookUrl)
        etFallbackPhone.setText(SettingUtils.fallbackSmsPhone)
        cbEnableForwarding.isChecked = SettingUtils.enableSmsForwarding

        btnSave.setOnClickListener {
            SettingUtils.webhookUrl = etWebhookUrl.text.toString()
            SettingUtils.fallbackSmsPhone = etFallbackPhone.text.toString()
            SettingUtils.enableSmsForwarding = cbEnableForwarding.isChecked
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "All permissions granted")
            } else {
                Log.e(TAG, "Some permissions denied")
                Toast.makeText(this, "Permissions are required for the app to function", Toast.LENGTH_LONG).show()
            }
        }
    }
}
