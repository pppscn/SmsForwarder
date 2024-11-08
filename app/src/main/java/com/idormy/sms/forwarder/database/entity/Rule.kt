package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import androidx.room.*
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.App.Companion.CALL_TYPE_MAP
import com.idormy.sms.forwarder.App.Companion.CHECK_MAP
import com.idormy.sms.forwarder.App.Companion.FILED_MAP
import com.idormy.sms.forwarder.App.Companion.SIM_SLOT_MAP
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.database.ext.ConvertersSenderList
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.*
import com.xuexiang.xutil.resource.ResUtils.getString
import kotlinx.parcelize.Parcelize
import java.util.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Parcelize
@Entity(
    tableName = "Rule",
    foreignKeys = [
        ForeignKey(
            entity = Sender::class,
            parentColumns = ["id"],
            childColumns = ["sender_id"],
            onDelete = ForeignKey.CASCADE, //级联操作
            onUpdate = ForeignKey.CASCADE //级联操作
        )
    ],
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["sender_id"]),
        Index(value = ["sender_list"])
    ]
)
@TypeConverters(ConvertersSenderList::class)
data class Rule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "type", defaultValue = "sms") var type: String,
    @ColumnInfo(name = "filed", defaultValue = "transpond_all") var filed: String,
    @ColumnInfo(name = "check", defaultValue = "is") var check: String,
    @ColumnInfo(name = "value", defaultValue = "") var value: String,
    @ColumnInfo(name = "sender_id", defaultValue = "0") var senderId: Long = 0,
    @ColumnInfo(name = "sms_template", defaultValue = "") var smsTemplate: String = "",
    @ColumnInfo(name = "regex_replace", defaultValue = "") var regexReplace: String = "",
    @ColumnInfo(name = "sim_slot", defaultValue = "ALL") var simSlot: String = "",
    @ColumnInfo(name = "status", defaultValue = "1") var status: Int = 1,
    @ColumnInfo(name = "time") var time: Date = Date(),
    @ColumnInfo(name = "sender_list", defaultValue = "") var senderList: List<Sender>,
    @ColumnInfo(name = "sender_logic", defaultValue = "ALL") var senderLogic: String = "ALL",
    //免打扰(禁用转发)时间段
    @ColumnInfo(name = "silent_period_start", defaultValue = "0") var silentPeriodStart: Int = 0,
    @ColumnInfo(name = "silent_period_end", defaultValue = "0") var silentPeriodEnd: Int = 0,
    @ColumnInfo(name = "silent_day_of_week", defaultValue = "") var silentDayOfWeek: String = "",
) : Parcelable {

    companion object {
        val TAG: String = Rule::class.java.simpleName

        fun getRuleMatch(type: String?, filed: String?, check: String?, value: String?, simSlot: String?, senderList: List<Sender>? = null): String {
            val blank = if (App.isNeedSpaceBetweenWords) " " else ""
            val sb = StringBuilder()
            if (type != "app") sb.append(SIM_SLOT_MAP[simSlot]).append(blank).append(getString(R.string.rule_card)).append(blank)
            when (filed) {
                null, FILED_TRANSPOND_ALL -> sb.append(getString(R.string.rule_all_fw_to))
                FILED_CALL_TYPE -> sb.append(getString(R.string.rule_when))
                    .append(blank)
                    .append(FILED_MAP[filed])
                    .append(blank)
                    .append(CHECK_MAP[check])
                    .append(blank)
                    .append(CALL_TYPE_MAP[value])
                    .append(blank)
                    .append(getString(R.string.rule_fw_to))

                else -> sb.append(getString(R.string.rule_when))
                    .append(blank)
                    .append(FILED_MAP[filed])
                    .append(blank)
                    .append(CHECK_MAP[check])
                    .append(blank)
                    .append(value)
                    .append(blank)
                    .append(getString(R.string.rule_fw_to))
            }
            if (!senderList.isNullOrEmpty()) {
                sb.append(blank).append(senderList.joinToString(",") { it.name })
            }
            return sb.toString()
        }

    }

    val description: String
        get() {
            val blank = if (App.isNeedSpaceBetweenWords) " " else ""
            val card = SIM_SLOT_MAP[simSlot].toString() + blank + getString(R.string.rule_card) + blank
            val sb = StringBuilder()
            when (type) {
                "app" -> sb.append(getString(R.string.task_app_when))
                "call" -> sb.append(String.format(getString(R.string.task_call_when), card))
                "sms" -> sb.append(String.format(getString(R.string.task_sms_when), card))
            }
            sb.append(blank)
            when (filed) {
                FILED_TRANSPOND_ALL -> sb.append("")
                FILED_CALL_TYPE -> sb.append(getString(R.string.rule_when))
                    .append(blank)
                    .append(FILED_MAP[filed])
                    .append(blank)
                    .append(CHECK_MAP[check])
                    .append(blank)
                    .append(CALL_TYPE_MAP[value])

                else -> sb.append(getString(R.string.rule_when))
                    .append(blank)
                    .append(FILED_MAP[filed])
                    .append(blank)
                    .append(CHECK_MAP[check])
                    .append(blank)
                    .append(value)
            }
            return sb.toString()
        }

    fun getName(appendSenderList: Boolean = true): String {
        return if (appendSenderList) {
            getRuleMatch(type, filed, check, value, simSlot, senderList)
        } else {
            getRuleMatch(type, filed, check, value, simSlot, null)
        }
    }

    val statusChecked: Boolean
        get() = status != STATUS_OFF

    val imageId: Int
        get() = when (simSlot) {
            CHECK_SIM_SLOT_1 -> R.drawable.ic_sim1
            CHECK_SIM_SLOT_2 -> R.drawable.ic_sim2
            CHECK_SIM_SLOT_ALL -> if (type == "app") R.drawable.ic_app else R.drawable.ic_sim
            else -> if (type == "app") R.drawable.ic_app else R.drawable.ic_sim
        }

    val statusImageId: Int
        get() = when (status) {
            STATUS_OFF -> R.drawable.ic_stop
            else -> R.drawable.ic_start
        }

    fun getSenderLogicCheckId(): Int {
        return when (senderLogic) {
            SENDER_LOGIC_UNTIL_FAIL -> R.id.rb_sender_logic_until_fail
            SENDER_LOGIC_UNTIL_SUCCESS -> R.id.rb_sender_logic_until_success
            else -> R.id.rb_sender_logic_all
        }
    }

    fun getSimSlotCheckId(): Int {
        return when (simSlot) {
            CHECK_SIM_SLOT_1 -> R.id.rb_sim_slot_1
            CHECK_SIM_SLOT_2 -> R.id.rb_sim_slot_2
            else -> R.id.rb_sim_slot_all
        }
    }

    fun getFiledCheckId(): Int {
        return when (filed) {
            FILED_MSG_CONTENT -> R.id.rb_content
            FILED_PHONE_NUM -> R.id.rb_phone
            FILED_CALL_TYPE -> R.id.rb_call_type
            FILED_PACKAGE_NAME -> R.id.rb_package_name
            FILED_UID -> R.id.rb_uid
            FILED_INFORM_CONTENT -> R.id.rb_inform_content
            FILED_MULTI_MATCH -> R.id.rb_multi_match
            else -> R.id.rb_transpond_all
        }
    }

    fun getCheckCheckId(): Int {
        return when (check) {
            CHECK_CONTAIN -> R.id.rb_contain
            CHECK_NOT_CONTAIN -> R.id.rb_not_contain
            CHECK_START_WITH -> R.id.rb_start_with
            CHECK_END_WITH -> R.id.rb_end_with
            CHECK_REGEX -> R.id.rb_regex
            else -> R.id.rb_is
        }
    }

    //字段分支
    @Throws(Exception::class)
    fun checkMsg(msg: MsgInfo?): Boolean {

        //检查这一行和上一行合并的结果是否命中
        var mixChecked = false
        if (msg != null) {
            //先检查规则是否命中
            when (this.filed) {
                FILED_TRANSPOND_ALL -> mixChecked = true
                FILED_PHONE_NUM, FILED_PACKAGE_NAME -> mixChecked = checkValue(msg.from)
                FILED_UID -> mixChecked = checkValue(msg.uid.toString())
                FILED_CALL_TYPE -> mixChecked = checkValue(msg.callType.toString())
                FILED_MSG_CONTENT, FILED_INFORM_CONTENT -> mixChecked = checkValue(msg.content)
                FILED_MULTI_MATCH -> mixChecked = RuleLineUtils.checkRuleLines(msg, this.value)
                else -> {}
            }
        }
        Log.i(TAG, "rule:$this checkMsg:$msg checked:$mixChecked")
        return mixChecked
    }

    //内容分支
    private fun checkValue(msgValue: String?): Boolean {
        if (msgValue == null) return false

        fun evaluateCondition(condition: String): Boolean {
            return when (check) {
                CHECK_IS -> msgValue == condition
                CHECK_NOT_IS -> msgValue != condition
                CHECK_CONTAIN -> msgValue.contains(condition)
                CHECK_NOT_CONTAIN -> !msgValue.contains(condition)
                CHECK_START_WITH -> msgValue.startsWith(condition)
                CHECK_END_WITH -> msgValue.endsWith(condition)
                CHECK_REGEX -> try {
                    val pattern = Pattern.compile(condition, Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(msgValue)
                    matcher.find()
                } catch (e: PatternSyntaxException) {
                    Log.i(TAG, "PatternSyntaxException: ${e.description}, Index: ${e.index}, Message: ${e.message}, Pattern: ${e.pattern}")
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

        Log.i(TAG, "checkValue $msgValue $check $value checked:$checked")
        return checked
    }

}