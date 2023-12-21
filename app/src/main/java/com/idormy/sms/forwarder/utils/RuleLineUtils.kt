package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.MsgInfo
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.*

@Suppress("unused")
object RuleLineUtils {
    const val TAG = "RuleLineUtils"
    private var START_LOG = false

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val a = """并且 是 手机号 相等 10086
 或者 是 手机号 结尾 哈哈哈
  并且 是 短信内容 包含 test
 或者 是 手机号 结尾 pppscn
并且 是 手机号 相等 100861
并且 是 手机号 相等 100861"""
        val msg = MsgInfo("sms", "10086", "哈哈哈", Date(), "15888888888")
        logg("check:" + checkRuleLines(msg, a))
    }

    fun startLog(startLog: Boolean) {
        START_LOG = startLog
    }

    private fun logg(msg: String?) {
        if (START_LOG) {
            Log.i(TAG, msg!!)
        }
    }

    @Throws(Exception::class)
    fun checkRuleLines(msg: MsgInfo, ruleLines: String?): Boolean {
        val scanner = Scanner(ruleLines)
        var lineNum = 0
        var headRuleLine: RuleLine? = null
        var beforeRuleLine: RuleLine? = null
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            logg("$lineNum : $line")
            //第一行
            if (lineNum == 0) {
                //第一行不允许缩进
                if (line.startsWith(" ")) {
                    throw Exception(getString(R.string.no_indentation_allowed_on_the_first_line))
                }
            }

            // process the line
            beforeRuleLine = generateRuleTree(line, lineNum, beforeRuleLine)
            if (lineNum == 0) {
                headRuleLine = beforeRuleLine
            }
            lineNum++
        }
        assert(headRuleLine != null)
        return checkRuleTree(msg, headRuleLine)
    }

    /**
     * 使用规则树判断消息是否命中规则
     * Rule节点是否命中取决于：该节点是否命中、该节点子结点（如果有的话）是否命中、该节点下节点（如果有的话）是否命中
     * 递归检查
     */
    @Throws(Exception::class)
    fun checkRuleTree(msg: MsgInfo, currentRuleLine: RuleLine?): Boolean {
        //该节点是否命中
        var currentAll = currentRuleLine!!.checkMsg(msg)
        logg("current:$currentRuleLine checked:$currentAll")

        //该节点子结点（如果有的话）是否命中
        if (currentRuleLine.getChildRuleLine() != null) {
            logg(" child:" + currentRuleLine.getChildRuleLine())
            currentAll = when (currentRuleLine.getChildRuleLine()!!.conjunction) {
                RuleLine.CONJUNCTION_AND -> currentAll && checkRuleTree(msg, currentRuleLine.getChildRuleLine())
                RuleLine.CONJUNCTION_OR -> currentAll || checkRuleTree(msg, currentRuleLine.getChildRuleLine())
                else -> throw Exception("child wrong conjunction")
            }
        }

        //该节点下节点（如果有的话）是否命中
        if (currentRuleLine.getNextRuleLine() != null) {
            logg("next:" + currentRuleLine.getNextRuleLine())
            currentAll = when (currentRuleLine.getNextRuleLine()!!.conjunction) {
                RuleLine.CONJUNCTION_AND -> currentAll && checkRuleTree(msg, currentRuleLine.getNextRuleLine())
                RuleLine.CONJUNCTION_OR -> currentAll || checkRuleTree(msg, currentRuleLine.getNextRuleLine())
                else -> throw Exception("next wrong conjunction")
            }
        }
        return currentAll
    }

    /**
     * 生成规则树
     * 一行代表一个规则
     */
    @Throws(Exception::class)
    fun generateRuleTree(line: String, lineNum: Int, parentRuleLine: RuleLine?): RuleLine {
        //val words = line.split(" ".toRegex()).toTypedArray()
        return RuleLine(line, lineNum, parentRuleLine)
    }
}