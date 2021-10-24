package com.idormy.sms.forwarder.sender;

import android.os.Handler;
import android.util.Log;

import com.idormy.sms.forwarder.utils.LogUtil;
import com.idormy.sms.forwarder.utils.SimUtil;
import com.idormy.sms.forwarder.utils.SmsUtil;

public class SenderSmsMsg extends SenderBaseMsg {

    static final String TAG = "SenderSmsMsg";

    public static void sendMsg(final long logId, final Handler handError, int simSlot, String mobiles, Boolean onlyNoNetwork, String from, String text) throws Exception {
        Log.i(TAG, "sendMsg simSlot:" + simSlot + " mobiles:" + mobiles + " onlyNoNetwork:" + onlyNoNetwork + " from:" + from + " text:" + text);

        //TODO：simSlot转subId
        final int subId = SimUtil.getSubscriptionIdBySimId(simSlot);
        String res = SmsUtil.sendSms(subId, mobiles, text);

        //TODO:粗略解析是否发送成功
        if (res == null) {
            LogUtil.updateLog(logId, 1, "发送成功");
        } else {
            LogUtil.updateLog(logId, 0, res);
            Toast(handError, TAG, "短信发送失败");
        }

    }

}
