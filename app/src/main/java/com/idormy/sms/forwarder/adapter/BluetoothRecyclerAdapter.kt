package com.idormy.sms.forwarder.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.idormy.sms.forwarder.R

class BluetoothRecyclerAdapter(
    private val itemList: List<BluetoothDevice>,
    private var itemClickListener: ((Int) -> Unit)? = null,
    private var removeClickListener: ((Int) -> Unit)? = null,
    private var editClickListener: ((Int) -> Unit)? = null,
) : RecyclerView.Adapter<BluetoothRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_bluetooth_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemList.size


    @Suppress("DEPRECATION")
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val textDeviceName: TextView = itemView.findViewById(R.id.text_device_name)
        private val textDeviceAddress: TextView = itemView.findViewById(R.id.text_device_address)
        private val imageDeviceIcon: ImageView = itemView.findViewById(R.id.image_device_icon)
        private val editIcon: ImageView = itemView.findViewById(R.id.iv_edit)
        private val removeIcon: ImageView = itemView.findViewById(R.id.iv_remove)

        init {
            if (removeClickListener == null) {
                removeIcon.visibility = View.GONE
            } else {
                removeIcon.setOnClickListener(this)
            }

            if (editClickListener == null) {
                editIcon.visibility = View.GONE
            } else {
                editIcon.setOnClickListener(this)
            }

            if (itemClickListener != null) {
                itemView.setOnClickListener(this)
            }
        }

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice) {
            // 设置设备名称和地址
            textDeviceName.text = device.name ?: "Unknown Device"
            textDeviceAddress.text = device.address

            // 根据设备类型设置图标
            val deviceType = getDeviceType(device)
            val iconResId = when (deviceType) {
                DeviceType.CELLPHONE -> R.drawable.ic_bt_cellphone
                DeviceType.HEADPHONES -> R.drawable.ic_bt_headphones
                DeviceType.HEADSET_HFP -> R.drawable.ic_bt_headset_hfp
                DeviceType.IMAGING -> R.drawable.ic_bt_imaging
                DeviceType.LAPTOP -> R.drawable.ic_bt_laptop
                DeviceType.MISC_HID -> R.drawable.ic_bt_misc_hid
                DeviceType.NETWORK_PAN -> R.drawable.ic_bt_network_pan
                DeviceType.WRISTBAND -> R.drawable.ic_bt_wristband
                else -> R.drawable.ic_bt_bluetooth
            }
            imageDeviceIcon.setImageResource(iconResId)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                when (v?.id) {
                    R.id.iv_edit -> editClickListener?.let { it(position) }
                    R.id.iv_remove -> removeClickListener?.let { it(position) }
                    else -> itemClickListener?.let { it(position) }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun getDeviceType(device: BluetoothDevice): DeviceType {
            val deviceClass = device.bluetoothClass?.majorDeviceClass ?: BluetoothClass.Device.Major.MISC
            @Suppress("DUPLICATE_LABEL_IN_WHEN")
            return when (deviceClass) {
                BluetoothClass.Device.Major.PHONE -> DeviceType.CELLPHONE
                BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceType.HEADPHONES
                BluetoothClass.Device.Major.PERIPHERAL -> DeviceType.HEADSET_HFP
                BluetoothClass.Device.Major.IMAGING -> DeviceType.IMAGING
                BluetoothClass.Device.Major.COMPUTER -> DeviceType.LAPTOP
                BluetoothClass.Device.Major.PERIPHERAL -> DeviceType.MISC_HID
                BluetoothClass.Device.Major.NETWORKING -> DeviceType.NETWORK_PAN
                BluetoothClass.Device.Major.WEARABLE -> DeviceType.WRISTBAND
                else -> DeviceType.UNKNOWN
            }
        }
    }

    enum class DeviceType {
        CELLPHONE,
        HEADPHONES,
        HEADSET_HFP,
        IMAGING,
        LAPTOP,
        MISC_HID,
        NETWORK_PAN,
        WRISTBAND,
        UNKNOWN
    }
}
