package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;


public class SimUtil {
    private static String TAG = "SimUtil";

    //获取卡槽信息ID
    public static int getSimId(Bundle bundle) {
        int whichSIM = -1;
        if (bundle != null) {
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
        return whichSIM + 1;
    }

    //获取卡槽备注
    public static String getSimInfo(int simId) {
        String res = "";
        try {
            for (PhoneUtils.SimInfo simInfo : MyApplication.SimInfoList) {
                if (simInfo.mSimSlotIndex + 1 == simId) {
                    res = simInfo.mCarrierName + "_" + simInfo.mNumber;
                    break;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getSimExtra Fail: " + e.getMessage());
        }

        return res.replace("null", "unknown");
    }
}
