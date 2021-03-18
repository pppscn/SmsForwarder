package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import com.idormy.sms.forwarder.utils.SimUtil;
import com.idormy.sms.forwarder.utils.SmsUtil;

public class SenderSmsMsg {

    static String TAG = "SenderSmsMsg";

    public static void sendMsg(final Handler handError, int simSlot, String mobiles, Boolean onlyNoNetwork, String from, String text) throws Exception {
        Log.i(TAG, "sendMsg simSlot:" + simSlot + " mobiles:" + mobiles + " onlyNoNetwork:" + onlyNoNetwork + " from:" + from + " text:" + text);

        //TODO：simSlot转subId
        int subId = SimUtil.getSubscriptionIdBySimId(simSlot);
        SmsUtil.sendSms(subId, mobiles, text);
    }

}
