package com.idormy.sms.forwarder.server.controller

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.WolData
import com.xuexiang.xrouter.utils.TextUtils
import com.yanzhenjie.andserver.annotation.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@Suppress("PrivatePropertyName")
@RestController
@RequestMapping(path = ["/wol"])
class WolController {

    private val TAG: String = WolController::class.java.simpleName

    //远程WOL
    @CrossOrigin(methods = [RequestMethod.POST])
    @PostMapping("/send")
    fun send(@RequestBody bean: BaseRequest<WolData>): String {
        val wolData = bean.data
        Log.d(TAG, wolData.toString())

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        DatagramSocket().use { socket ->
            try {
                val macBytes = getMacBytes(wolData.mac)
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0..5) {
                    bytes[i] = 0xff.toByte()
                }
                var i = 6
                while (i < bytes.size) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                    i += macBytes.size
                }
                val host = if (TextUtils.isEmpty(wolData.ip)) "230.0.0.1" else wolData.ip
                val port = if (wolData.port > 0) wolData.port else 9
                val address: InetAddress = InetAddress.getByName(host)
                val packet = DatagramPacket(bytes, bytes.size, address, port)
                socket.send(packet)
                socket.close()
                Log.d(TAG, "Wake-on-LAN packet sent.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Wake-on-LAN packet: $e")
            }
        }

        return "success"
    }

    @Throws(IllegalArgumentException::class)
    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.replace("-", ":").split(":").toTypedArray()
        require(hex.size == 6) { "Invalid MAC address." }
        try {
            for (i in 0..5) {
                bytes[i] = hex[i].toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex digit in MAC address. $e")
        }
        return bytes
    }
}