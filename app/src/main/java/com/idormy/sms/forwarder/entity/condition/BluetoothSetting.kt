package com.idormy.sms.forwarder.entity.condition

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class BluetoothSetting(
    var description: String = "", //描述
    var action: String = BluetoothAdapter.ACTION_STATE_CHANGED, //事件
    var state: Int = BluetoothAdapter.STATE_ON, //蓝牙状态
    var result: Int = 1, //搜索结果：1-已发现，0-未发现
    var device: String = "", //设备MAC地址
) : Serializable {

    constructor(actionCheckId: Int, stateCheckId: Int, resultCheckId: Int, deviceAddress: String) : this() {
        device = deviceAddress
        action = when (actionCheckId) {
            R.id.rb_action_discovery_finished -> BluetoothAdapter.ACTION_DISCOVERY_FINISHED
            R.id.rb_action_acl_connected -> BluetoothDevice.ACTION_ACL_CONNECTED
            R.id.rb_action_acl_disconnected -> BluetoothDevice.ACTION_ACL_DISCONNECTED
            else -> BluetoothAdapter.ACTION_STATE_CHANGED
        }
        state = when (stateCheckId) {
            R.id.rb_state_off -> BluetoothAdapter.STATE_OFF
            else -> BluetoothAdapter.STATE_ON
        }
        result = when (resultCheckId) {
            R.id.rb_undiscovered -> 0
            else -> 1
        }
        val sb = StringBuilder()

        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            sb.append(getString(R.string.bluetooth_state_changed)).append(", ").append(getString(R.string.specified_state)).append(": ")
            if (state == BluetoothAdapter.STATE_ON) {
                sb.append(getString(R.string.state_on))
            } else {
                sb.append(getString(R.string.state_off))
            }
        } else if (action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
            sb.append(getString(R.string.bluetooth_discovery_finished)).append(", ")
            if (result == 1) {
                sb.append(getString(R.string.discovered))
            } else {
                sb.append(getString(R.string.undiscovered))
            }
            val blank = if (App.isNeedSpaceBetweenWords) " " else ""
            sb.append(blank).append(getString(R.string.specified_device)).append(": ").append(device)
        } else {
            if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                sb.append(getString(R.string.bluetooth_acl_connected))
            } else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                sb.append(getString(R.string.bluetooth_acl_disconnected))
            }
            sb.append(", ").append(getString(R.string.specified_device)).append(": ").append(device)
        }

        description = sb.toString()
    }

    fun getActionCheckId(): Int {
        return when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> R.id.rb_action_state_changed
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> R.id.rb_action_discovery_finished
            BluetoothDevice.ACTION_ACL_CONNECTED -> R.id.rb_action_acl_connected
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> R.id.rb_action_acl_disconnected
            else -> R.id.rb_action_state_changed
        }
    }

    fun getStateCheckId(): Int {
        return when (state) {
            BluetoothAdapter.STATE_ON -> R.id.rb_state_on
            BluetoothAdapter.STATE_OFF -> R.id.rb_state_off
            else -> R.id.rb_state_on
        }
    }

    fun getResultCheckId(): Int {
        return when (result) {
            0 -> R.id.rb_undiscovered
            else -> R.id.rb_discovered
        }
    }
}
