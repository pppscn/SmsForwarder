package cn.ppps.forwarder.entity.action

class WolSetting(
    var description: String = "",
    var mac: String = "",
    var ip: String = "",
    var port: String = "",
    var wakeMethod: Int = 0 // 0: 通过本地服务API, 1: 直接发送幻数据包
)
