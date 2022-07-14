package com.idormy.sms.forwarder.entity.result

data class DingtalkInnerRobotResult(
    //获取access_token返回
    var accessToken: String?,
    var expireIn: Long?,
    //消息id
    var processQueryKey: String?,
    //无效的用户userid列表
    //var invalidStaffIdList: String[],
    //被限流的userid列表
    //var flowControlledStaffIdList: String[],
)