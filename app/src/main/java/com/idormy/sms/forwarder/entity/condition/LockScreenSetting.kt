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
) : Serializable {

    constructor(actionCheckId: Int, timeAfterOff: Int, timeAfterOn: Int, timeAfterLocked: Int, timeAfterUnlocked: Int) : this() {
        when (actionCheckId) {
            R.id.rb_action_screen_on -> {
                val duration = if (timeAfterOn > 0) String.format(getString(R.string.duration_minute), timeAfterOn.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_on_description), duration)
                action = Intent.ACTION_SCREEN_ON
            }

            R.id.rb_action_screen_unlocked -> {
                val duration = if (timeAfterUnlocked > 0) String.format(getString(R.string.duration_minute), timeAfterUnlocked.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_unlocked_description), duration)
                action = Intent.ACTION_USER_PRESENT
            }

            R.id.rb_action_screen_locked -> {
                val duration = if (timeAfterLocked > 0) String.format(getString(R.string.duration_minute), timeAfterLocked.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_locked_description), duration)
                action = Intent.ACTION_SCREEN_OFF + "_LOCKED"
            }

            else -> {
                val duration = if (timeAfterOff > 0) String.format(getString(R.string.duration_minute), timeAfterOff.toString()) else ""
                description = String.format(getString(R.string.time_after_screen_off_description), duration)
                action = Intent.ACTION_SCREEN_OFF
            }
        }

        timeAfterScreenOff = timeAfterOff
        timeAfterScreenOn = timeAfterOn
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
