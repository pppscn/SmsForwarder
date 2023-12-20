package com.idormy.sms.forwarder.entity.action

import com.idormy.sms.forwarder.utils.HttpServerUtils
import java.io.Serializable

data class HttpServerSetting(
    var description: String = "", //描述
    var action: String = "start", //动作: start=启动, stop=停止
    var enableApiClone: Boolean = HttpServerUtils.enableApiClone, //是否启用一键克隆
    var enableApiSmsSend: Boolean = HttpServerUtils.enableApiSmsSend, //是否启用远程发短信
    var enableApiSmsQuery: Boolean = HttpServerUtils.enableApiSmsQuery, //是否启用远程查短信
    var enableApiCallQuery: Boolean = HttpServerUtils.enableApiCallQuery, //是否启用远程查通话记录
    var enableApiContactQuery: Boolean = HttpServerUtils.enableApiContactQuery, //是否启用远程查联系人
    var enableApiContactAdd: Boolean = HttpServerUtils.enableApiContactAdd, //是否启用远程加联系人
    var enableApiWol: Boolean = HttpServerUtils.enableApiWol, //是否启用远程WOL
    var enableApiLocation: Boolean = HttpServerUtils.enableApiLocation, //是否启用远程找手机
    var enableApiBatteryQuery: Boolean = HttpServerUtils.enableApiBatteryQuery, //是否启用远程查电量
) : Serializable
