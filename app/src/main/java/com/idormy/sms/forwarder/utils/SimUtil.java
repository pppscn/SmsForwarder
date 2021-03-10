package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.util.Log;


public class SimUtil {
    private static String TAG = "SimUtil";

    //获取卡槽ID
    public static int getSimId(Bundle bundle) {
        int whichSIM = -1;
        if (bundle == null) {
            return whichSIM;
        }

        if (bundle.containsKey("subscription")) {
            whichSIM = bundle.getInt("subscription");
        }
        if (whichSIM >= 0 && whichSIM < 5) {
            /*In some device Subscription id is return as subscriber id*/
            //TODO：不确定能不能直接返回
            Log.d(TAG, "whichSIM >= 0 && whichSIM < 5：" + whichSIM);
        } else {
            if (bundle.containsKey("simId")) {
                whichSIM = bundle.getInt("simId");
            } else if (bundle.containsKey("com.android.phone.extra.slot")) {
                whichSIM = bundle.getInt("com.android.phone.extra.slot");
            } else {
                String keyName = "";
                for (String key : bundle.keySet()) {
                    if (key.contains("sim"))
                        keyName = key;
                }
                if (bundle.containsKey(keyName)) {
                    whichSIM = bundle.getInt(keyName);
                }
            }
        }

        Log.d(TAG, " Slot Number " + whichSIM);
        return whichSIM;
    }
}
