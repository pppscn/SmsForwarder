package com.idormy.sms.forwarder.service

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.idormy.sms.forwarder.utils.ACTION_RESTART
import com.idormy.sms.forwarder.utils.ACTION_START
import com.idormy.sms.forwarder.utils.ACTION_STOP
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.task.TaskUtils

@Suppress("PrivatePropertyName", "DEPRECATION")
class BluetoothScanService : Service() {

    private val TAG: String = BluetoothScanService::class.java.simpleName
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    companion object {
        var isRunning = false
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        Log.i(TAG, "onStartCommand: ${intent.action}")

        when (intent.action) {
            ACTION_START -> startDiscovery()
            ACTION_STOP -> stopDiscovery()
            ACTION_RESTART -> {
                stopDiscovery()
                startDiscovery()
            }
        }
        return START_NOT_STICKY
    }

    // 开始扫描蓝牙设备
    private fun startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (isRunning) return
        // 清空已发现设备
        TaskUtils.discoveredDevices = mutableMapOf()
        bluetoothAdapter?.startDiscovery()
        isRunning = true
    }

    // 停止蓝牙扫描
    private fun stopDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (!isRunning) return
        bluetoothAdapter?.cancelDiscovery()
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopDiscovery()
    }

}