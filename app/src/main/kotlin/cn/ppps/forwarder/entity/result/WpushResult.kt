package cn.ppps.forwarder.entity.result

data class WpushResult(
    var code: Long,
    var message: String?,
    var data: Any?,
)
