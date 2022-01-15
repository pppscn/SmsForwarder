package com.idormy.sms.forwarder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;
import com.idormy.sms.forwarder.utils.PhoneUtils;

public class SimStateReceiver extends BroadcastReceiver {

    private static final String TAG = "SimStateReceiver";
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private static final String EXTRA_SIM_STATE = "ss";
    private static final String SIM_STATE_LOADED = "LOADED";

    /**
     * 更换SIM卡，如果不杀后台并重启，则发送出的「卡槽信息」仍然是刚启动应用时读取的SIM卡
     * 增加这个Receiver，接收SIM卡插拔状态广播，自动更新缓存
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String receiveAction = intent.getAction();
        Log.d(TAG, "onReceive intent " + receiveAction);
        if (ACTION_SIM_STATE_CHANGED.equals(receiveAction)) {
            //SIM状态的额外信息
            String state = intent.getExtras().getString(EXTRA_SIM_STATE);
            Log.d(TAG, state);
            //只需要最后一个SIM加载完毕的 LOADED 状态
            if (SIM_STATE_LOADED.equals(state)) {
                //刷新SimInfoList
                MyApplication.SimInfoList = PhoneUtils.getSimMultiInfo();
                Log.d(TAG, MyApplication.SimInfoList.toString());
            }
        }
    }
}
