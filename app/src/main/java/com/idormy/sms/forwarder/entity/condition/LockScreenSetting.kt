package com.idormy.sms.forwarder.entity.condition

import android.content.Intent
import com.idormy.sms.forwarder.R
import com.xuexiang.xutil.resource.ResUtils.getString
import java.io.Serializable

data class LockScreenSetting(
    var description: String = "", //描述
    var action: String = Intent.ACTION_SCREEN_OFF, //事件
    var timeAfterScreenOff: Int = 5, //熄屏后时间
    var timeAfterScreenOn: Int = 5, //开锁后时间
    var timeAfterScreenLocked: Int = 5, //锁屏后时间
    var timeAfterScreenUnlocked: Int = 5, //解锁后时间
    var checkAgain: Boolean = false, //是否再次校验
) : Serializable {

    constructor(actionCheckId: Int, timeAfterOff: Int, timeAfterOn: Int, timeAfterLocked: Int, timeAfterUnlocked: Int, checkAgain: Boolean = false) : this() {
        val duration = when (actionCheckId) {
            R.id.rb_action_screen_on -> {
                val durationStr = if (timeAfterOn > 0) String.format(getString(R.string.duration_minute), timeAfterOn.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_on_description), durationStr)
                action = Intent.ACTION_SCREEN_ON
                timeAfterOn
            }

            R.id.rb_action_screen_unlocked -> {
                val durationStr = if (timeAfterUnlocked > 0) String.format(getString(R.string.duration_minute), timeAfterUnlocked.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_unlocked_description), durationStr)
                action = Intent.ACTION_USER_PRESENT
                timeAfterUnlocked
            }

            R.id.rb_action_screen_locked -> {
                val durationStr = if (timeAfterLocked > 0) String.format(getString(R.string.duration_minute), timeAfterLocked.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_locked_description), durationStr)
                action = Intent.ACTION_SCREEN_OFF + "_LOCKED"
                timeAfterLocked
            }

            else -> {
                val durationStr = if (timeAfterOff > 0) String.format(getString(R.string.duration_minute), timeAfterOff.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_off_description), durationStr)
                action = Intent.ACTION_SCREEN_OFF
                timeAfterOff
            }
        }

        timeAfterScreenOff = timeAfterOff
        timeAfterScreenOn = timeAfterOn
        timeAfterScreenLocked = timeAfterLocked
        timeAfterScreenUnlocked = timeAfterUnlocked
        this.checkAgain = checkAgain
        if (checkAgain && duration > 0) {
            description += ", " + getString(R.string.task_condition_check_again)
        }
    }

    fun getActionCheckId(): Int {
        return when (action) {
            Intent.ACTION_SCREEN_ON -> R.id.rb_action_screen_on
            Intent.ACTION_SCREEN_OFF -> R.id.rb_action_screen_off
            Intent.ACTION_USER_PRESENT -> R.id.rb_action_screen_unlocked
            else -> R.id.rb_action_screen_locked
        }
    }

}
