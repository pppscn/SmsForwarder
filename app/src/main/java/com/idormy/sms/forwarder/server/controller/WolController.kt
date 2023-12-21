package com.idormy.sms.forwarder.server.controller

import com.idormy.sms.forwarder.utils.Log
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.idormy.sms.forwarder.server.model.WolData
import com.xuexiang.xrouter.utils.TextUtils
import com.yanzhenjie.andserver.annotation.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Locale

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

        if (TextUtils.isEmpty(wolData.mac)) return "mac is empty"
        val port = if (wolData.port > 0) wolData.port else 9
        val host = if (TextUtils.isEmpty(wolData.ip)) null else wolData.ip

        wakeOnLAN(wolData.mac, host, port)

        return "success"
    }

    private fun wakeOnLAN(macAddress: String, broadcastAddress: String? = null, port: Int = 9) {
        try {
            val macBytes = macAddress.replace("-", ":").split(":").map { it.uppercase(Locale.getDefault()).toInt(16).toByte() }.toByteArray()
            val magicPacket = ByteArray(102)

            // 首先添加6个0xFF字节
            for (i in 0 until 6) {
                magicPacket[i] = 0xFF.toByte()
            }

            // 之后添加16次MAC地址
            for (i in 6 until magicPacket.size step macBytes.size) {
                macBytes.copyInto(magicPacket, i, 0, macBytes.size)
            }

            val broadcastIP = if (broadcastAddress != null) {
                InetAddress.getByName(broadcastAddress)
            } else {
                InetAddress.getByName("255.255.255.255")
            }

            // 创建 UDP 数据包
            val packet = DatagramPacket(magicPacket, magicPacket.size, broadcastIP, port)

            // 发送数据包
            val socket = DatagramSocket()
            socket.send(packet)
            socket.close()

            Log.d(TAG, "WOL packet sent successfully.")
        } catch (e: Exception) {
            Log.d(TAG, "Error sending WOL packet: ${e.message}")
        }
    }
}