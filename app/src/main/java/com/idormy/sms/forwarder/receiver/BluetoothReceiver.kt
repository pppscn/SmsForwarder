package com.idormy.sms.forwarder.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.service.BluetoothScanService
import com.idormy.sms.forwarder.utils.ACTION_RESTART
import com.idormy.sms.forwarder.utils.ACTION_START
import com.idormy.sms.forwarder.utils.ACTION_STOP
import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.utils.SettingUtils
import com.idormy.sms.forwarder.utils.TASK_CONDITION_BLUETOOTH
import com.idormy.sms.forwarder.utils.TaskWorker
import com.idormy.sms.forwarder.utils.task.TaskUtils
import com.idormy.sms.forwarder.workers.BluetoothWorker

@Suppress("PrivatePropertyName", "DEPRECATION")
@SuppressLint("MissingPermission")
class BluetoothReceiver : BroadcastReceiver() {

    private val TAG: String = BluetoothReceiver::class.java.simpleName
    private val handler = Handler()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        try {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> handleActionFound(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> handleDiscoveryFinished(context)
                BluetoothAdapter.ACTION_STATE_CHANGED -> handleStateChanged(context, intent)
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> handleScanModeChanged()
                BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED -> handleLocalNameChanged()
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> handleConnectionStateChanged()
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> handleBondStateChanged()
                BluetoothDevice.ACTION_ACL_CONNECTED -> handleAclConnected(context, intent)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleAclDisconnected(context, intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Bluetooth action: ${intent.action}", e)
        }
    }

    // 处理发现设备
    private fun handleActionFound(intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        if (ActivityCompat.checkSelfPermission(App.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        if (SettingUtils.bluetoothIgnoreAnonymous && device.name.isNullOrEmpty()) return

        //TODO: 实测这里一台设备会收到两次广播
        Log.d(TAG, "Discovered device: ${device.name} - ${device.address}")
        val discoveredDevices = TaskUtils.discoveredDevices
        discoveredDevices[device.address] = device.name ?: ""
        TaskUtils.discoveredDevices = discoveredDevices
    }

    // 处理扫描完成
    private fun handleDiscoveryFinished(context: Context) {
        //TODO: 放在这里去判断是否已经发现某个设备（避免 ACTION_FOUND 重复广播）
        Log.d(TAG, "Bluetooth scan finished, discoveredDevices: ${TaskUtils.discoveredDevices}")
        if (TaskUtils.discoveredDevices.isNotEmpty()) {
            handleWorkRequest(context, BluetoothAdapter.ACTION_DISCOVERY_FINISHED, Gson().toJson(TaskUtils.discoveredDevices))
        }

        restartBluetoothService(ACTION_STOP)
        if (SettingUtils.enableBluetooth) {
            Log.d(TAG, "Bluetooth scan finished, restart in ${SettingUtils.bluetoothScanInterval}ms")
            handler.postDelayed({
                restartBluetoothService(ACTION_START)
            }, SettingUtils.bluetoothScanInterval)
        }
    }

    // 处理蓝牙状态变化
    private fun handleStateChanged(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        handleBluetoothStateChanged(state)
        handleWorkRequest(context, BluetoothAdapter.ACTION_STATE_CHANGED, state.toString())
    }

    // 处理蓝牙扫描模式变化
    private fun handleScanModeChanged() {
        if (SettingUtils.enableBluetooth) {
            restartBluetoothService()
        }
    }

    // 处理本地蓝牙名称变化
    private fun handleLocalNameChanged() {
        // handle local name changed logic
    }

    // 处理蓝牙连接状态变化
    private fun handleConnectionStateChanged() {
        // handle connection state changed logic
    }

    // 处理蓝牙设备的配对状态变化
    private fun handleBondStateChanged() {
        // handle bond state changed logic
    }

    // 处理蓝牙设备连接
    private fun handleAclConnected(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        Log.d(TAG, "Connected device: ${device.name} - ${device.address}")
        TaskUtils.connectedDevices[device.address] = device.name
        handleWorkRequest(context, BluetoothDevice.ACTION_ACL_CONNECTED, Gson().toJson(mutableMapOf(device.address to device.name)))
    }

    // 处理蓝牙设备断开连接
    private fun handleAclDisconnected(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        Log.d(TAG, "Disconnected device: ${device.name} - ${device.address}")
        TaskUtils.connectedDevices.remove(device.address)
        handleWorkRequest(context, BluetoothDevice.ACTION_ACL_DISCONNECTED, Gson().toJson(mutableMapOf(device.address to device.name)))
    }

    // 处理蓝牙状态变化
    private fun handleBluetoothStateChanged(state: Int) {
        when (state) {
            // 蓝牙已关闭
            BluetoothAdapter.STATE_OFF -> {
                Log.d(TAG, "BluetoothAdapter.STATE_OFF")
                TaskUtils.bluetoothState = state
                // 停止扫描 & 删除任何挂起的延迟扫描任务
                restartBluetoothService(ACTION_STOP)
                handler.removeCallbacksAndMessages(null)
            }

            // 蓝牙已打开
            BluetoothAdapter.STATE_ON -> {
                Log.d(TAG, "BluetoothAdapter.STATE_ON")
                TaskUtils.bluetoothState = state
                if (SettingUtils.enableBluetooth) {
                    restartBluetoothService(ACTION_START)
                }
            }

            // 蓝牙正在打开
            BluetoothAdapter.STATE_TURNING_ON -> {
                Log.d(TAG, "BluetoothAdapter.STATE_TURNING_ON")
            }

            // 蓝牙正在关闭
            BluetoothAdapter.STATE_TURNING_OFF -> {
                Log.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF")
            }

            // 蓝牙正在连接
            BluetoothAdapter.STATE_CONNECTING -> {
                Log.d(TAG, "BluetoothAdapter.STATE_CONNECTING")
            }

            // 蓝牙已连接
            BluetoothAdapter.STATE_CONNECTED -> {
                Log.d(TAG, "BluetoothAdapter.STATE_CONNECTED")
            }

            // 蓝牙正在断开连接
            BluetoothAdapter.STATE_DISCONNECTING -> {
                Log.d(TAG, "BluetoothAdapter.STATE_DISCONNECTING")
            }

            // 蓝牙已断开连接
            BluetoothAdapter.STATE_DISCONNECTED -> {
                Log.d(TAG, "BluetoothAdapter.STATE_DISCONNECTED")
            }
        }
    }

    //重启蓝牙扫描服务
    private fun restartBluetoothService(action: String = ACTION_RESTART) {
        Log.d(TAG, "restartBluetoothService, action: $action")
        val serviceIntent = Intent(App.context, BluetoothScanService::class.java)
        serviceIntent.action = action
        App.context.startService(serviceIntent)
    }

    // 发送任务请求
    private fun handleWorkRequest(context: Context, action: String, msg: String) {
        val request = OneTimeWorkRequestBuilder<BluetoothWorker>()
            .setInputData(
                workDataOf(
                    TaskWorker.CONDITION_TYPE to TASK_CONDITION_BLUETOOTH,
                    TaskWorker.ACTION to action,
                    TaskWorker.MSG to msg,
                )
            ).build()
        WorkManager.getInstance(context).enqueue(request)
    }
}