package com.idormy.sms.forwarder.database.entity

import android.os.Parcelable
import android.util.Log
import androidx.room.*
import com.idormy.sms.forwarder.R
import com.idormy.sms.forwarder.entity.MsgInfo
import com.idormy.sms.forwarder.utils.*
import com.xuexiang.xui.utils.ResUtils.getString
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
        Index(value = ["sender_id"])
    ]
)
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
) : Parcelable {

    companion object {
        val TAG: String = Rule::class.java.simpleName

        fun getRuleMatch(filed: String?, check: String?, value: String?, simSlot: String?): Any {
            val sb = StringBuilder()
            sb.append(SIM_SLOT_MAP[simSlot]).append(getString(R.string.rule_card))
            if (filed == null || filed == FILED_TRANSPOND_ALL) {
                sb.append(getString(R.string.rule_all_fw_to))
            } else {
                sb.append(getString(R.string.rule_when)).append(FILED_MAP[filed]).append(CHECK_MAP[check]).append(value).append(getString(R.string.rule_fw_to))
            }
            return sb.toString()
        }

    }

    val ruleMatch: String
        get() {
            val simStr = if ("app" == type) "" else SIM_SLOT_MAP[simSlot].toString() + getString(R.string.rule_card)
            return if (filed == FILED_TRANSPOND_ALL) {
                simStr + getString(R.string.rule_all_fw_to)
            } else {
                simStr + getString(R.string.rule_when) + FILED_MAP[filed] + CHECK_MAP[check] + value + getString(R.string.rule_fw_to)
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
            STATUS_OFF -> R.drawable.icon_off
            else -> R.drawable.icon_on
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
            FILED_PACKAGE_NAME -> R.id.rb_package_name
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
        var checked = false
        when (this.check) {
            CHECK_IS -> checked = this.value == msgValue
            CHECK_NOT_IS -> checked = this.value != msgValue
            CHECK_CONTAIN -> if (msgValue != null) {
                checked = msgValue.contains(this.value)
            }
            CHECK_NOT_CONTAIN -> if (msgValue != null) {
                checked = !msgValue.contains(this.value)
            }
            CHECK_START_WITH -> if (msgValue != null) {
                checked = msgValue.startsWith(this.value)
            }
            CHECK_END_WITH -> if (msgValue != null) {
                checked = msgValue.endsWith(this.value)
            }
            CHECK_REGEX -> if (msgValue != null) {
                try {
                    //checked = Pattern.matches(this.value, msgValue);
                    val pattern = Pattern.compile(this.value, Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(msgValue)
                    while (matcher.find()) {
                        checked = true
                        break
                    }
                } catch (e: PatternSyntaxException) {
                    Log.d(TAG, "PatternSyntaxException: ")
                    Log.d(TAG, "Description: " + e.description)
                    Log.d(TAG, "Index: " + e.index)
                    Log.d(TAG, "Message: " + e.message)
                    Log.d(TAG, "Pattern: " + e.pattern)
                }
            }
            else -> {}
        }
        Log.i(TAG, "checkValue " + msgValue + " " + this.check + " " + this.value + " checked:" + checked)
        return checked
    }
}