package com.idormy.sms.forwarder.utils

import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.MsgInfo
import com.xuexiang.xutil.resource.ResUtils.getString
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Suppress("unused")
class RuleLine(line: String, lineNum: Int, beforeRuleLine: RuleLine?) {
    companion object {
        val CONJUNCTION_AND: String = getString(R.string.CONJUNCTION_AND)
        val CONJUNCTION_OR: String = getString(R.string.CONJUNCTION_OR)
        val FILED_PHONE_NUM: String = getString(R.string.FILED_PHONE_NUM)
        val FILED_MSG_CONTENT: String = getString(R.string.FILED_MSG_CONTENT)
        val FILED_PACKAGE_NAME: String = getString(R.string.FILED_PACKAGE_NAME)
        val FILED_UID: String = getString(R.string.FILED_UID)
        val FILED_INFORM_TITLE: String = getString(R.string.FILED_INFORM_TITLE)
        val FILED_INFORM_CONTENT: String = getString(R.string.FILED_INFORM_CONTENT)
        val FILED_SIM_SLOT_INFO: String = getString(R.string.FILED_SIM_SLOT_INFO)
        val FILED_CALL_TYPE: String = getString(R.string.FILED_CALL_TYPE)
        val SURE_YES: String = getString(R.string.SURE_YES)
        val SURE_NOT: String = getString(R.string.SURE_NOT)
        val CHECK_EQUALS: String = getString(R.string.CHECK_EQUALS)
        val CHECK_CONTAIN: String = getString(R.string.CHECK_CONTAIN)
        val CHECK_NOT_CONTAIN: String = getString(R.string.CHECK_NOT_CONTAIN)
        val CHECK_START_WITH: String = getString(R.string.CHECK_START_WITH)
        val CHECK_END_WITH: String = getString(R.string.CHECK_END_WITH)
        val CHECK_REGEX: String = getString(R.string.CHECK_REGEX)
        val CONJUNCTION_LIST: MutableList<String> = ArrayList()
        val FILED_LIST: MutableList<String> = ArrayList()
        val SURE_LIST: MutableList<String> = ArrayList()
        val CHECK_LIST: MutableList<String> = ArrayList()

        const val TAG = "RuleLine"
        private var START_LOG = true
        fun startLog(startLog: Boolean) {
            START_LOG = startLog
        }

        fun logg(msg: String?) {
            if (START_LOG) {
                Log.i(TAG, msg!!)
            }
        }

        init {
            CONJUNCTION_LIST.add("and")
            CONJUNCTION_LIST.add("or")
            CONJUNCTION_LIST.add(CONJUNCTION_AND)
            CONJUNCTION_LIST.add(CONJUNCTION_OR)
        }

        init {
            FILED_LIST.add(FILED_PHONE_NUM)
            FILED_LIST.add(FILED_PACKAGE_NAME)
            FILED_LIST.add(FILED_MSG_CONTENT)
            FILED_LIST.add(FILED_INFORM_CONTENT)
            FILED_LIST.add(FILED_INFORM_TITLE)
            FILED_LIST.add(FILED_SIM_SLOT_INFO)
            FILED_LIST.add(FILED_CALL_TYPE)
            FILED_LIST.add(FILED_UID)
        }

        init {
            SURE_LIST.add(SURE_YES)
            SURE_LIST.add(SURE_NOT)
        }

        init {
            CHECK_LIST.add(CHECK_EQUALS)
            CHECK_LIST.add(CHECK_CONTAIN)
            CHECK_LIST.add(CHECK_NOT_CONTAIN)
            CHECK_LIST.add(CHECK_START_WITH)
            CHECK_LIST.add(CHECK_END_WITH)
            CHECK_LIST.add(CHECK_REGEX)
        }
    }

    //开头有几个空格
    private var headSpaceNum = 0
    private var beforeRuleLine: RuleLine? = null
    private var nextRuleLine: RuleLine? = null
    private var parentRuleLine: RuleLine? = null
    private var childRuleLine: RuleLine? = null

    //and or
    var conjunction: String

    //手机号 短信内容 APP包名 通知标题 通知内容 卡槽信息
    private var field: String

    // 是否
    private var sure: String
    private var check: String
    private var value: String

    //字段分支
    fun checkMsg(msg: MsgInfo): Boolean {

        //检查这一行和上一行合并的结果是否命中
        var mixChecked = false
        when (field) {
            FILED_PHONE_NUM, FILED_PACKAGE_NAME -> mixChecked = checkValue(msg.from)
            FILED_UID -> mixChecked = checkValue(msg.uid.toString())
            FILED_CALL_TYPE -> mixChecked = checkValue(msg.callType.toString())
            FILED_MSG_CONTENT, FILED_INFORM_CONTENT -> mixChecked = checkValue(msg.content)
            FILED_INFORM_TITLE, FILED_SIM_SLOT_INFO -> mixChecked = checkValue(msg.simInfo)
            else -> {}
        }
        when (sure) {
            SURE_YES -> {}
            SURE_NOT -> mixChecked = !mixChecked
            else -> mixChecked = false
        }
        logg("rule:$this checkMsg:$msg checked:$mixChecked")
        return mixChecked
    }

    //内容分支
    private fun checkValue(msgValue: String?): Boolean {
        if (msgValue == null) return false

        fun evaluateCondition(condition: String): Boolean {
            return when (check) {
                CHECK_EQUALS -> msgValue == condition
                CHECK_CONTAIN -> msgValue.contains(condition)
                CHECK_NOT_CONTAIN -> !msgValue.contains(condition)
                CHECK_START_WITH -> msgValue.startsWith(condition)
                CHECK_END_WITH -> msgValue.endsWith(condition)
                CHECK_REGEX -> try {
                    val pattern = Pattern.compile(condition, Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(msgValue)
                    matcher.find()
                } catch (e: PatternSyntaxException) {
                    logg("PatternSyntaxException: ${e.description}, Index: ${e.index}, Message: ${e.message}, Pattern: ${e.pattern}")
                    false
                }

                else -> false
            }
        }

        fun parseAndEvaluate(expression: String): Boolean {
            // Split by "||" and evaluate each segment joined by "&&"
            val orGroups = expression.split("||")
            return orGroups.any { orGroup ->
                val andGroups = orGroup.split("&&")
                andGroups.all { andGroup ->
                    val trimmedCondition = andGroup.trim()
                    evaluateCondition(trimmedCondition)
                }
            }
        }

        val checked = if (value.contains("&&") || value.contains("||")) {
            parseAndEvaluate(value)
        } else {
            evaluateCondition(value)
        }

        logg("checkValue $msgValue $check $value checked:$checked")
        return checked
    }

    override fun toString(): String {
        return "RuleLine{" +
                "headSpaceNum='" + headSpaceNum + '\'' +
                "conjunction='" + conjunction + '\'' +
                ", field='" + field + '\'' +
                ", sure='" + sure + '\'' +
                ", check='" + check + '\'' +
                ", value='" + value + '\'' +
                '}'
    }

    fun getNextRuleLine(): RuleLine? {
        return nextRuleLine
    }

    fun setNextRuleLine(nextRuleLine: RuleLine?) {
        this.nextRuleLine = nextRuleLine
    }

    fun getChildRuleLine(): RuleLine? {
        return childRuleLine
    }

    fun setChildRuleLine(childRuleLine: RuleLine?) {
        this.childRuleLine = childRuleLine
    }

    init {
        logg("----------$lineNum-----------------")
        logg(line)
        //规则检验：
        //并且 是 手机号 相等 10086
        //[并且, 是, 手机号, 相等, 10086]
        //  并且 是 内容 包含 test
        //[, , 并且, 是, 内容, 包含, sms]

        //处理头空格数用来确认跟上一行节点的相对位置：是同级还是子级
        //处理4个字段，之后的全部当做value

        //标记3个阶段
        var isCountHeading = false
        var isDealMiddle = false
        var isDealValue = false

        //用于保存4个中间体： 并且, 是, 内容, 包含
        val middleList: MutableList<String> = ArrayList(4)
        //保存每个中间体字符串
        var buildMiddleWord = StringBuilder()
        val valueBuilder = StringBuilder()
        for (i in line.indices) {
            val w = line[i].toString()
            logg("walk over:$w")

            //控制阶段
            //开始处理头
            if (i == 0) {
                if (" " == w) {
                    logg("start to isCountHeading:")
                    isCountHeading = true
                } else {
                    //直接进入处理中间体阶段
                    isDealMiddle = true
                    logg("start to isDealMiddle:")
                }
            }
            //正在数空格头，但是遇到非空格，阶段变更:由处理空头阶段  变为  处理 中间体阶段
            if (isCountHeading && " " != w) {
                logg("isCountHeading to isDealMiddle:")
                isCountHeading = false
                isDealMiddle = true
            }

            //正在处理中间体，中间体数量够了，阶段变更：由处理中间体  变为  处理 value
            if (isDealMiddle && middleList.size == 4) {
                logg("isDealMiddle done middleList:$middleList")
                logg("isDealMiddle to isDealValue:")
                isDealMiddle = false
                isDealValue = true
            }
            logg("isCountHeading:$isCountHeading")
            logg("isDealMiddle:$isDealMiddle")
            logg("isDealValue:$isDealValue")
            if (isCountHeading) {
                logg("headSpaceNum++:$headSpaceNum")
                headSpaceNum++
            }
            if (isDealMiddle) {
                //遇到空格
                if (" " == w) {
                    buildMiddleWord = if (buildMiddleWord.isEmpty()) {
                        throw Exception(lineNum.toString() + "行：语法错误不允许出现连续空格！")
                    } else {
                        //生成了一个中间体
                        middleList.add(buildMiddleWord.toString())
                        logg("get Middle++:$buildMiddleWord")
                        StringBuilder()
                    }
                } else {
                    //把w拼接到中间体上
                    buildMiddleWord.append(w)
                    logg("buildMiddleWord length:" + buildMiddleWord.length + "buildMiddleWord:" + buildMiddleWord)
                }
            }
            if (isDealValue) {
                //把余下的所有字符都拼接给value
                valueBuilder.append(w)
            }
        }
        logg("isDealValue done valueBuilder:$valueBuilder")
        if (middleList.size != 4) {
            throw Exception(lineNum.toString() + "行配置错误：每行必须有4段组成，例如： 并且 手机号 是 相等 ")
        }


        //规则对齐
        if (beforeRuleLine != null) {
            logg("beforeRuleLine :$beforeRuleLine")
            logg("thisRuleLine :$this")

            //同级别
            if (headSpaceNum == beforeRuleLine.headSpaceNum) {
                logg("同级别")
                this.beforeRuleLine = beforeRuleLine
                beforeRuleLine.nextRuleLine = this
            }
            //子级
            if (headSpaceNum - 1 == beforeRuleLine.headSpaceNum) {
                logg("子级")
                parentRuleLine = beforeRuleLine
                beforeRuleLine.childRuleLine = this
            }
            //查找父级别
            if (headSpaceNum < beforeRuleLine.headSpaceNum) {
                //匹配到最近一个同级
                var fBeforeRuleLine = beforeRuleLine.beforeRuleLine
                if (fBeforeRuleLine == null) {
                    fBeforeRuleLine = beforeRuleLine.parentRuleLine
                }
                while (fBeforeRuleLine != null) {
                    logg("fBeforeRuleLine$fBeforeRuleLine")

                    //查找到同级别
                    if (headSpaceNum == fBeforeRuleLine.headSpaceNum) {
                        logg("父级别")
                        this.beforeRuleLine = fBeforeRuleLine
                        fBeforeRuleLine.nextRuleLine = this
                        break
                    } else {
                        //向上查找
                        var pBeforeRuleLine = fBeforeRuleLine.beforeRuleLine
                        if (pBeforeRuleLine == null) {
                            pBeforeRuleLine = fBeforeRuleLine.parentRuleLine
                        }
                        fBeforeRuleLine = pBeforeRuleLine
                    }
                }
            }
        } else {
            logg("根级别")
        }
        conjunction = middleList[0]
        sure = middleList[1]
        field = middleList[2]
        check = middleList[3]
        value = valueBuilder.toString()
        if (!CONJUNCTION_LIST.contains(conjunction)) {
            throw Exception(lineNum.toString() + "行配置错误：连接词只支持：" + CONJUNCTION_LIST + " 但提供了" + conjunction)
        }
        if (!FILED_LIST.contains(field)) {
            throw Exception(lineNum.toString() + "行配置错误：字段只支持：" + FILED_LIST + " 但提供了" + field)
        }
        if (!SURE_LIST.contains(sure)) {
            throw Exception(lineNum.toString() + "行配置错误 " + sure + " 确认词只支持：" + SURE_LIST + " 但提供了" + sure)
        }
        if (!CHECK_LIST.contains(check)) {
            throw Exception(lineNum.toString() + "行配置错误：比较词只支持：" + CHECK_LIST + " 但提供了" + check)
        }
        logg("----------$lineNum==$this")
    }
}