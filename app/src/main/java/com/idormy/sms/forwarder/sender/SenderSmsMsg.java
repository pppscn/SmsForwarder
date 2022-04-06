package com.idormy.sms.forwarder.sender;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import com.idormy.sms.forwarder.utils.LogUtils;
import com.idormy.sms.forwarder.utils.SimUtils;
import com.idormy.sms.forwarder.utils.SmsUtils;

@SuppressWarnings("RedundantThrows")
public class SenderSmsMsg extends SenderBaseMsg {

    static final String TAG = "SenderSmsMsg";

    @SuppressLint("NewApi")
    public static void sendMsg(final long logId, final Handler handError, int simSlot, String mobiles, Boolean onlyNoNetwork, String from, String text) throws Exception {
        Log.i(TAG, "sendMsg simSlot:" + simSlot + " mobiles:" + mobiles + " onlyNoNetwork:" + onlyNoNetwork + " from:" + from + " text:" + text);

        //TODO：simSlot转subId
        final int subId = SimUtils.getSubscriptionIdBySimId(simSlot);
        String res = SmsUtils.sendSms(subId, mobiles, text);

        //TODO:粗略解析是否发送成功
        if (res == null) {
            LogUtils.updateLog(logId, 2, "发送成功");
        } else {
            LogUtils.updateLog(logId, 0, res);
            Toast(handError, TAG, "短信发送失败");
        }

    }

}
