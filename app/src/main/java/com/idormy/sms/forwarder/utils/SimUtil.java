package com.idormy.sms.forwarder.utils;

import android.os.Bundle;
import android.util.Log;

import com.idormy.sms.forwarder.MyApplication;


@SuppressWarnings("unused")
public class SimUtil {
    private static final String TAG = "SimUtil";

    //获取卡槽信息ID
    public static int getSimId(Bundle bundle) {
        int whichSIM = -1;
        if (bundle == null) {
            return whichSIM;
        }

        if (bundle.containsKey("simId")) {
            whichSIM = bundle.getInt("simId");
            Log.d(TAG, "simId = " + whichSIM);
        } else if (bundle.containsKey("com.android.phone.extra.slot")) {
            whichSIM = bundle.getInt("com.android.phone.extra.slot");
            Log.d(TAG, "com.android.phone.extra.slot = " + whichSIM);
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

        Log.d(TAG, "Slot Number " + whichSIM);
        return whichSIM + 1;
    }

    //通过SubscriptionId获取卡槽信息ID
    public static int getSimIdBySubscriptionId(int subscriptionId) {
        try {
            for (PhoneUtils.SimInfo simInfo : MyApplication.SimInfoList) {
                if (simInfo.mSubscriptionId == subscriptionId) {
                    return simInfo.mSimSlotIndex + 1;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getSimExtra Fail: " + e.getMessage());
        }

        return 1;
    }


    //通过卡槽ID获取SubscriptionId
    public static int getSubscriptionIdBySimId(int simId) {
        try {
            for (PhoneUtils.SimInfo simInfo : MyApplication.SimInfoList) {
                Log.d(TAG, "mSimSlotIndex = " + simInfo.mSimSlotIndex);
                if (simInfo.mSimSlotIndex != -1 && simInfo.mSimSlotIndex == simId) {
                    return simInfo.mSubscriptionId;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getSimExtra Fail: " + e.getMessage());
        }

        return 0;
    }

    //获取卡槽备注
    public static String getSimInfo(int simId) {
        String res = "";
        try {
            for (PhoneUtils.SimInfo simInfo : MyApplication.SimInfoList) {
                Log.d(TAG, String.valueOf(simInfo));
                if (simInfo.mSimSlotIndex != -1 && simInfo.mSimSlotIndex + 1 == simId) {
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
