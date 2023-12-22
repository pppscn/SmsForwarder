package com.idormy.sms.forwarder.entity.action

import com.idormy.sms.forwarder.database.entity.Rule
import java.io.Serializable

data class RuleSetting(
    var description: String = "", //描述
    var status: String = "enable", //状态: enable=启用，disable=禁用
    var ruleList: List<Rule>, //转发规则列表
) : Serializable
