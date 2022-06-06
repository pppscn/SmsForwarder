package com.idormy.sms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.idormy.sms.forwarder.App
import com.idormy.sms.forwarder.utils.PhoneUtils

class SimStateReceiver : BroadcastReceiver() {
    /**
     * 更换SIM卡，如果不杀后台并重启，则发送出的「卡槽信息」仍然是刚启动应用时读取的SIM卡
     * 增加这个Receiver，接收SIM卡插拔状态广播，自动更新缓存
     */
    override fun onReceive(context: Context, intent: Intent) {
        val receiveAction = intent.action
        Log.d(TAG, "onReceive intent $receiveAction")
        if (ACTION_SIM_STATE_CHANGED == receiveAction) {
            //SIM状态的额外信息
            val state = intent.extras!!.getString(EXTRA_SIM_STATE)
            Log.d(TAG, state!!)
            //只需要最后一个SIM加载完毕的 LOADED 状态
            if (SIM_STATE_LOADED == state) {
                //刷新SimInfoList
                App.SimInfoList = PhoneUtils.getSimMultiInfo()
                Log.d(TAG, App.SimInfoList.toString())
            }
        }
    }

    companion object {
        private const val TAG = "SimStateReceiver"
        const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"
        private const val EXTRA_SIM_STATE = "ss"
        private const val SIM_STATE_LOADED = "LOADED"
    }
}