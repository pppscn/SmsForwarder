package com.idormy.sms.forwarder.sender;

import static com.idormy.sms.forwarder.SenderActivity.NOTIFY;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SenderBaseMsg {

    public static void Toast(Handler handError, String Tag, String data) {
        Log.i(Tag, data);
        if (handError != null) {
            Message msg = new Message();
            msg.what = NOTIFY;
            Bundle bundle = new Bundle();
            bundle.putString("DATA", Tag + "-" + data);
            msg.setData(bundle);
            handError.sendMessage(msg);
        }
    }
}
