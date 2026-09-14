package cn.ppps.forwarder.entity.result

data class WxpusherResult(
    var code: Long = 0,
    var msg: String = "",
    var data: Any? = null,
    var success: Boolean = false,
)
